package com.doch.Logistic.service;

import com.doch.Catalog.model.Product;
import com.doch.Catalog.repository.ProductRepository;
import com.doch.Logistic.model.*;
import com.doch.Logistic.repository.DeliveryRepository;
import com.doch.Logistic.repository.DochXenderDeliveryRequestRepository;
import com.doch.Notification.model.NotificationType;
import com.doch.Notification.service.NotificationService;
import com.doch.Order.model.Order;
import com.doch.Order.model.OrderItem;
import com.doch.Order.model.OrderStatus;
import com.doch.Order.repository.OrderRepository;
import com.doch.Payment.model.Payment;
import com.doch.Payment.model.PaymentSplit;
import com.doch.Payment.repository.PaymentRepository;
import com.doch.Payment.repository.PaymentSplitRepository;
import com.doch.Payment.service.WalletService;
import com.doch.User.model.Seller;
import com.doch.User.model.User;
import com.doch.common.ReferenceGenerator;
import com.doch.common.audit.AuditService;
import com.doch.common.events.DeliveryStatusChangedEvent;
import com.doch.common.events.OrderStatusChangedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * Connects payments, packages, orders and seller earnings.
 * <ul>
 *   <li>activates packages once a payment is confirmed;</li>
 *   <li>keeps the parent order status consistent with its packages;</li>
 *   <li>releases seller earnings on delivery and reverses them (and restocks) on cancellation / return.</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FulfilmentService {

    private final DeliveryRepository deliveryRepository;
    private final DochXenderDeliveryRequestRepository xenderRequestRepository;
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentSplitRepository splitRepository;
    private final WalletService walletService;
    private final TrackingEventService trackingEventService;
    private final NotificationService notificationService;
    private final AuditService auditService;
    private final ApplicationEventPublisher eventPublisher;

    /** Called exactly once per order / Xender request, when its payment becomes COMPLETED. */
    @Transactional
    public void activateAfterPayment(Payment payment) {
        if (payment.getXenderRequest() != null) {
            DochXenderDeliveryRequest request = payment.getXenderRequest();
            request.setStatus("PAID");
            xenderRequestRepository.save(request);
            deliveryRepository.findByDeliveryRequest(request).ifPresent(delivery -> {
                if (delivery.getStatus() == DeliveryStatus.PENDING_PAYMENT) {
                    activate(delivery, DeliveryStatus.PENDING_ASSIGNMENT);
                    notificationService.sendNotificationToRole("ROLE_XENDER_ADMIN", NotificationType.DELIVERY_REQUEST_RECEIVED,
                            "Xender package " + delivery.getTrackingNumber() + " is paid and waiting for driver assignment.");
                }
            });
            return;
        }

        Order order = payment.getOrder();
        if (order == null) return;
        for (Delivery delivery : deliveryRepository.findByMarketDeliveryRequest_Order(order)) {
            if (delivery.getStatus() != DeliveryStatus.PENDING_PAYMENT) continue;
            activate(delivery, DeliveryStatus.PENDING_PREPARATION);
            Seller seller = delivery.getMarketDeliveryRequest().getSeller();
            if (seller != null && seller.getUser() != null) {
                notificationService.sendNotification(seller.getUser(), NotificationType.ORDER_CONFIRMED,
                        String.format("Order %s is paid. Please prepare and seal package %s, then mark it ready in your seller portal.",
                                order.getReference(), delivery.getTrackingNumber()));
            }
        }
    }

    private void activate(Delivery delivery, DeliveryStatus next) {
        DeliveryStatus from = delivery.getStatus();
        DeliveryStateMachine.assertTransition(from, next);
        delivery.setDeliveryOtp(ReferenceGenerator.numericOtp(6));
        delivery.setOtpFailedAttempts(0);
        delivery.setStatus(next);
        deliveryRepository.save(delivery);
        trackingEventService.record(delivery, TrackingEventService.STATUS_CHANGE, from, next,
                TrackingEventService.describe(next), "Payment confirmed", null);
        eventPublisher.publishEvent(new DeliveryStatusChangedEvent(delivery.getId(), from, next));
    }

    /**
     * Must be called after every package status change: settles seller funds for the leg and
     * recomputes the parent order status.
     */
    @Transactional
    public void afterDeliveryStatusChange(Delivery delivery, DeliveryStatus from, User actor) {
        eventPublisher.publishEvent(new DeliveryStatusChangedEvent(delivery.getId(), from, delivery.getStatus()));

        if (delivery.getMarketDeliveryRequest() == null) {
            return;
        }
        Order order = delivery.getMarketDeliveryRequest().getOrder();
        boolean orderWasPaid = order.getPaidAt() != null;

        if (delivery.getStatus() == DeliveryStatus.DELIVERED && orderWasPaid) {
            settleLegEarnings(delivery, PaymentSplit.SplitStatus.AVAILABLE);
        } else if ((delivery.getStatus() == DeliveryStatus.CANCELLED || delivery.getStatus() == DeliveryStatus.RETURNED)) {
            if (orderWasPaid) {
                settleLegEarnings(delivery, PaymentSplit.SplitStatus.REVERSED);
            }
            restockLeg(delivery);
        }
        syncOrderStatus(order, actor);
    }

    /** Derives the order status from the status of all its packages. */
    @Transactional
    public void syncOrderStatus(Order order, User actor) {
        List<Delivery> legs = deliveryRepository.findByMarketDeliveryRequest_Order(order);
        if (legs.isEmpty() || order.getPaidAt() == null) {
            return;
        }
        OrderStatus current = order.getStatus();
        OrderStatus target = current;

        boolean allTerminal = legs.stream().allMatch(l -> DeliveryStateMachine.isTerminal(l.getStatus()));
        long delivered = legs.stream().filter(l -> l.getStatus() == DeliveryStatus.DELIVERED).count();
        long cancelled = legs.stream().filter(l -> l.getStatus() == DeliveryStatus.CANCELLED).count();
        boolean anyMoving = legs.stream().anyMatch(l -> DeliveryStateMachine.isInDochCustody(l.getStatus())
                || l.getStatus() == DeliveryStatus.DELIVERED);

        if (allTerminal) {
            if (delivered > 0) target = OrderStatus.DELIVERED;
            else if (cancelled == legs.size()) target = OrderStatus.CANCELLED;
            else target = OrderStatus.RETURNED;
        } else if (anyMoving) {
            target = OrderStatus.SHIPPED;
        } else {
            target = OrderStatus.PROCESSING;
        }

        if (target != current) {
            order.setStatus(target);
            if (target == OrderStatus.CANCELLED && order.getCancelledAt() == null) {
                order.setCancelledAt(LocalDateTime.now());
            }
            orderRepository.save(order);
            auditService.record(actor, "ORDER_STATUS_CHANGED", "ORDER", order.getId(),
                    current + " -> " + target + " (derived from package statuses)");
            eventPublisher.publishEvent(new OrderStatusChangedEvent(order.getId(), current, target));

            switch (target) {
                case SHIPPED -> notificationService.sendNotification(order.getCustomer(), NotificationType.ORDER_SHIPPED,
                        "Your order " + order.getReference() + " is on its way.");
                case DELIVERED -> notificationService.sendNotification(order.getCustomer(), NotificationType.ORDER_DELIVERED,
                        "Your order " + order.getReference() + " has been delivered. Tell us how it went by rating your order.");
                case RETURNED -> notificationService.sendNotification(order.getCustomer(), NotificationType.ORDER_CANCELLED,
                        "Your order " + order.getReference() + " could not be delivered and was returned. Our team will contact you about your refund.");
                case CANCELLED -> notificationService.sendNotification(order.getCustomer(), NotificationType.ORDER_CANCELLED,
                        "Your order " + order.getReference() + " has been cancelled.");
                default -> { }
            }
        }
    }

    /** Moves the seller's earning for this leg from PENDING to AVAILABLE (delivered) or REVERSED (cancelled/returned). */
    private void settleLegEarnings(Delivery delivery, PaymentSplit.SplitStatus targetStatus) {
        Order order = delivery.getMarketDeliveryRequest().getOrder();
        Seller seller = delivery.getMarketDeliveryRequest().getSeller();
        if (seller == null || seller.getUser() == null) return;
        Payment payment = paymentRepository.findByOrder(order).orElse(null);
        if (payment == null) return;

        splitRepository.findByPayment(payment).stream()
                .filter(split -> split.getType() == PaymentSplit.SplitType.SELLER_EARNING)
                .filter(split -> split.getRecipient() != null && split.getRecipient().getId().equals(seller.getUser().getId()))
                .filter(split -> split.getStatus() == null || split.getStatus() == PaymentSplit.SplitStatus.PENDING)
                .forEach(split -> {
                    if (targetStatus == PaymentSplit.SplitStatus.AVAILABLE) {
                        walletService.releaseToAvailable(seller.getUser(), split.getAmount(), order.getReference());
                    } else {
                        walletService.reversePending(seller.getUser(), split.getAmount(), order.getReference());
                    }
                    split.setStatus(targetStatus);
                    splitRepository.save(split);
                });
    }

    /** Returns the quantities of this leg's items to stock (cancelled before collection or returned to seller). */
    private void restockLeg(Delivery delivery) {
        Order order = delivery.getMarketDeliveryRequest().getOrder();
        Seller seller = delivery.getMarketDeliveryRequest().getSeller();
        if (order.getItems() == null || seller == null) return;
        for (OrderItem item : order.getItems()) {
            Product product = item.getProduct();
            if (product == null || product.getSeller() == null || !Objects.equals(product.getSeller().getId(), seller.getId())) {
                continue;
            }
            Product locked = productRepository.findByIdForUpdate(product.getId()).orElse(product);
            locked.setStockQuantity(locked.getStockQuantity() + item.getQuantity());
            locked.setQuantitySold(Math.max(0, locked.getQuantitySold() - item.getQuantity()));
            productRepository.save(locked);
        }
        log.info("STOCK: Restocked items of package {} (order {})", delivery.getTrackingNumber(), order.getReference());
    }
}
