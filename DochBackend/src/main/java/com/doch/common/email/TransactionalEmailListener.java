package com.doch.common.email;

import com.doch.Logistic.model.Delivery;
import com.doch.Logistic.model.DeliveryStatus;
import com.doch.Logistic.repository.DeliveryRepository;
import com.doch.Order.model.Order;
import com.doch.Order.repository.OrderRepository;
import com.doch.User.model.User;
import com.doch.common.events.DeliveryStatusChangedEvent;
import com.doch.common.events.OrderPaidEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.*;

/**
 * Sends customer e-mails for order confirmation and package milestones.
 * Runs after the business transaction commits (never for rolled-back changes) and asynchronously
 * (an e-mail outage never blocks or fails a payment or delivery).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TransactionalEmailListener {

    private static final Set<DeliveryStatus> EMAILED = EnumSet.of(
            DeliveryStatus.DRIVER_ASSIGNED, DeliveryStatus.PICKED_UP, DeliveryStatus.IN_TRANSIT,
            DeliveryStatus.DELIVERED, DeliveryStatus.FAILED, DeliveryStatus.RETURNED, DeliveryStatus.CANCELLED);

    private final EmailService emailService;
    private final OrderRepository orderRepository;
    private final DeliveryRepository deliveryRepository;

    @Value("${application.frontend.market-base-url:http://localhost:5173}")
    private String marketBaseUrl;

    @Value("${application.frontend.xender-base-url:http://localhost:5174}")
    private String xenderBaseUrl;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public void onOrderPaid(OrderPaidEvent event) {
        Order order = orderRepository.findById(event.orderId()).orElse(null);
        if (order == null) return;
        List<Map<String, Object>> items = order.getItems().stream().map(i -> Map.<String, Object>of(
                "name", i.getProduct().getName(),
                "quantity", i.getQuantity(),
                "total", String.format("%,.0f XAF", i.getTotal_price()))).toList();
        List<Map<String, Object>> packages = deliveryRepository.findByMarketDeliveryRequest_Order(order).stream()
                .map(d -> Map.<String, Object>of(
                        "tracking", d.getTrackingNumber(),
                        "seller", d.getMarketDeliveryRequest().getSeller() != null ? d.getMarketDeliveryRequest().getSeller().getName() : "",
                        "otp", d.getDeliveryOtp() != null ? d.getDeliveryOtp() : "",
                        "trackUrl", xenderBaseUrl + "/track/" + d.getTrackingNumber()))
                .toList();
        Map<String, Object> ctx = new HashMap<>();
        ctx.put("name", order.getCustomer().getFirstName());
        ctx.put("reference", order.getReference());
        ctx.put("items", items);
        ctx.put("packages", packages);
        ctx.put("productTotal", String.format("%,.0f XAF", order.getProductTotal()));
        ctx.put("deliveryFee", String.format("%,.0f XAF", order.getDeliveryFee()));
        ctx.put("total", String.format("%,.0f XAF", order.getTotalAmount()));
        ctx.put("address", order.getDeliveryAddress());
        ctx.put("ordersUrl", marketBaseUrl + "/orders");
        boolean sent = emailService.sendEmailFromTemplate(order.getCustomer().getEmail(),
                "Order " + order.getReference() + " confirmed", "order-confirmation", ctx);
        log.info("EMAIL: order confirmation {} -> {}", order.getReference(), sent ? "sent" : "FAILED");
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public void onDeliveryStatusChanged(DeliveryStatusChangedEvent event) {
        if (!EMAILED.contains(event.to())) return;
        Delivery delivery = deliveryRepository.findById(event.deliveryId()).orElse(null);
        if (delivery == null) return;
        User owner = delivery.getOwner();
        if (owner == null || owner.getEmail() == null) return;

        Map<String, Object> ctx = new HashMap<>();
        ctx.put("name", owner.getFirstName());
        ctx.put("tracking", delivery.getTrackingNumber());
        ctx.put("orderReference", delivery.getMarketDeliveryRequest() != null
                ? delivery.getMarketDeliveryRequest().getOrder().getReference() : null);
        ctx.put("statusText", describe(event.to()));
        ctx.put("detail", detail(delivery, event.to()));
        ctx.put("otp", event.to() == DeliveryStatus.IN_TRANSIT ? delivery.getDeliveryOtp() : null);
        ctx.put("trackUrl", xenderBaseUrl + "/track/" + delivery.getTrackingNumber());
        ctx.put("reviewUrl", event.to() == DeliveryStatus.DELIVERED && delivery.getMarketDeliveryRequest() != null
                ? marketBaseUrl + "/orders" : null);
        boolean sent = emailService.sendEmailFromTemplate(owner.getEmail(),
                "Package " + delivery.getTrackingNumber() + ": " + describe(event.to()), "delivery-update", ctx);
        log.info("EMAIL: delivery update {} {} -> {}", delivery.getTrackingNumber(), event.to(), sent ? "sent" : "FAILED");
    }

    private static String describe(DeliveryStatus status) {
        return switch (status) {
            case DRIVER_ASSIGNED -> "Driver assigned";
            case PICKED_UP -> "Collected by the driver";
            case IN_TRANSIT -> "Out for delivery";
            case DELIVERED -> "Delivered";
            case FAILED -> "Delivery attempt unsuccessful";
            case RETURNED -> "Returned to sender";
            case CANCELLED -> "Cancelled";
            default -> status.name();
        };
    }

    private static String detail(Delivery d, DeliveryStatus status) {
        return switch (status) {
            case DRIVER_ASSIGNED -> "A DOCH driver has been assigned and will collect your package soon.";
            case PICKED_UP -> "Your package has been collected and is on its way.";
            case IN_TRANSIT -> "Your package is out for delivery. Give the delivery code below to the driver only when you receive the package.";
            case DELIVERED -> "Your package was delivered and confirmed with your delivery code. Thank you for choosing DOCH!";
            case FAILED -> "We could not deliver your package" + (d.getFailureReason() != null ? " (" + d.getFailureReason() + ")" : "")
                    + ". Our team will contact you to reschedule.";
            case RETURNED -> "Your package is being returned to the sender.";
            case CANCELLED -> "This package has been cancelled.";
            default -> "";
        };
    }
}
