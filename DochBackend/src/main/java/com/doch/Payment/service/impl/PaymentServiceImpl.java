package com.doch.Payment.service.impl;

import com.doch.Logistic.model.Delivery;
import com.doch.Logistic.model.DeliveryStatus;
import com.doch.Logistic.model.DochXenderDeliveryRequest;
import com.doch.Logistic.repository.DeliveryRepository;
import com.doch.Logistic.repository.DochXenderDeliveryRequestRepository;
import com.doch.Logistic.service.FulfilmentService;
import com.doch.Notification.model.NotificationType;
import com.doch.Notification.service.NotificationService;
import com.doch.Order.model.Order;
import com.doch.Order.model.OrderItem;
import com.doch.Order.model.OrderStatus;
import com.doch.Order.repository.OrderRepository;
import com.doch.Payment.dto.PaymentMapper;
import com.doch.Payment.dto.PaymentRequestDTO;
import com.doch.Payment.dto.PaymentResponseDTO;
import com.doch.Payment.dto.fapshi.*;
import com.doch.Payment.model.Payment;
import com.doch.Payment.model.PaymentSplit;
import com.doch.Payment.model.PaymentSplit.SplitType;
import com.doch.Payment.model.PaymentStatus;
import com.doch.Payment.repository.PaymentRepository;
import com.doch.Payment.repository.PaymentSplitRepository;
import com.doch.Payment.service.PaymentService;
import com.doch.Payment.service.WalletService;
import com.doch.User.model.User;
import com.doch.common.audit.AuditService;
import com.doch.common.events.OrderPaidEvent;
import com.doch.common.exceptions.BusinessException;
import com.doch.common.exceptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

/**
 * Payment lifecycle on top of the Fapshi gateway.
 * <p>
 * Guarantees:
 * <ul>
 *   <li>Only the owner of an order / delivery request can pay for it; the amount always comes from the server.</li>
 *   <li>A payment is only marked COMPLETED after the provider itself reports SUCCESSFUL (webhook payloads are re-verified).</li>
 *   <li>Every status change happens under a row lock, so duplicate callbacks / polls cannot double-apply a payment.</li>
 *   <li>Failed or expired attempts move the order to PAYMENT_FAILED; the customer may retry with a new attempt.</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    public static final String FLOW_LINK = "LINK";
    public static final String FLOW_DIRECT = "DIRECT";

    @Value("${application.frontend.market-payment-redirect-url}")
    private String marketPaymentRedirectUrl;

    @Value("${application.frontend.xender-payment-redirect-url}")
    private String xenderPaymentRedirectUrl;

    @Value("${application.commission.marketplace-percent:10.0}")
    private double commissionPercent;

    @Value("${application.payments.expire-after-hours:24}")
    private long expireAfterHours;

    private final PaymentRepository paymentRepository;
    private final PaymentSplitRepository splitRepository;
    private final OrderRepository orderRepository;
    private final DochXenderDeliveryRequestRepository xenderRequestRepository;
    private final DeliveryRepository deliveryRepository;
    private final FapshiClient fapshiClient;
    private final NotificationService notificationService;
    private final WalletService walletService;
    private final FulfilmentService fulfilmentService;
    private final PaymentMapper paymentMapper;
    private final AuditService auditService;
    private final ApplicationEventPublisher eventPublisher;

    // ------------------------------------------------------------------ initiation

    @Override
    @Transactional
    public PaymentResponseDTO initiatePayment(PaymentRequestDTO request, User currentUser) {
        Order order = null;
        DochXenderDeliveryRequest xenderRequest = null;
        double amount;
        List<Payment> attempts;

        if (StringUtils.hasText(request.getXenderRequestId())) {
            xenderRequest = xenderRequestRepository.findById(request.getXenderRequestId())
                    .orElseThrow(() -> new ResourceNotFoundException("Delivery request not found"));
            if (xenderRequest.getSender() == null || !xenderRequest.getSender().getId().equals(currentUser.getId())) {
                throw new AccessDeniedException("You can only pay for your own delivery requests.");
            }
            Delivery delivery = deliveryRepository.findByDeliveryRequest(xenderRequest)
                    .orElseThrow(() -> new BusinessException("Delivery record missing for this request."));
            if (delivery.getStatus() != DeliveryStatus.PENDING_PAYMENT) {
                throw new BusinessException("This delivery request is not awaiting payment (status: " + delivery.getStatus() + ").");
            }
            amount = xenderRequest.getPrice() != null ? xenderRequest.getPrice() : 0;
            attempts = paymentRepository.findAllByXenderRequestOrderByCreatedAtDesc(xenderRequest);
        } else {
            order = orderRepository.findByIdForUpdate(request.getOrderId())
                    .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
            if (order.getCustomer() == null || !order.getCustomer().getId().equals(currentUser.getId())) {
                throw new AccessDeniedException("You can only pay for your own orders.");
            }
            if (order.getStatus() != OrderStatus.PENDING && order.getStatus() != OrderStatus.PAYMENT_FAILED) {
                throw new BusinessException("Order " + order.getReference() + " cannot be paid in status " + order.getStatus() + ".");
            }
            amount = order.getTotalAmount();
            attempts = paymentRepository.findAllByOrderOrderByCreatedAtDesc(order);
        }

        if (Math.round(amount) < 100) {
            throw new BusinessException("The minimum payable amount is 100 XAF.");
        }
        if (attempts.stream().anyMatch(p -> p.getStatus() == PaymentStatus.COMPLETED)) {
            throw new BusinessException("This has already been paid.");
        }

        boolean wantsDirect = StringUtils.hasText(request.getPhone());
        for (Payment pending : attempts.stream().filter(p -> p.getStatus() == PaymentStatus.PENDING).toList()) {
            Payment locked = paymentRepository.findByIdForUpdate(pending.getId()).orElse(pending);
            reconcileLocked(locked);
            if (locked.getStatus() == PaymentStatus.COMPLETED) {
                return withRouting(paymentMapper.toDto(locked), xenderRequest != null);
            }
            if (locked.getStatus() != PaymentStatus.PENDING) {
                continue;
            }
            if (FLOW_DIRECT.equals(locked.getPaymentFlow())) {
                PaymentResponseDTO dto = withRouting(paymentMapper.toDto(locked), xenderRequest != null);
                dto.setMessage("A payment request is already pending on " + locked.getPayerPhone()
                        + ". Approve it on the phone or wait for it to fail before retrying.");
                return dto;
            }
            if (!wantsDirect && StringUtils.hasText(locked.getPaymentUrl())) {
                return withRouting(paymentMapper.toDto(locked), xenderRequest != null);
            }
            // The customer switches from a payment link to a direct prompt: invalidate the old link first.
            supersedeLink(locked, "Superseded by a direct mobile money request");
            if (locked.getStatus() == PaymentStatus.COMPLETED) {
                return withRouting(paymentMapper.toDto(locked), xenderRequest != null);
            }
        }

        Payment payment = paymentRepository.save(Payment.builder()
                .user(currentUser)
                .order(order)
                .xenderRequest(xenderRequest)
                .amount(Math.round(amount))
                .paymentMethod(request.getPaymentMethod() != null ? request.getPaymentMethod().toUpperCase() : null)
                .paymentFlow(wantsDirect ? FLOW_DIRECT : FLOW_LINK)
                .payerPhone(wantsDirect ? request.getPhone() : null)
                .status(PaymentStatus.PENDING)
                .build());

        String redirectUrl = xenderRequest != null ? xenderPaymentRedirectUrl : marketPaymentRedirectUrl;
        String reference = order != null ? order.getReference() : "XENDER-" + xenderRequest.getId().substring(0, 8);
        try {
            if (wantsDirect) {
                FapshiDirectPayResponse response = fapshiClient.directPay(FapshiDirectPayRequest.builder()
                        .amount((int) Math.round(amount))
                        .phone(request.getPhone())
                        .medium(mediumFor(request.getPaymentMethod()))
                        .name(currentUser.getFirstName() + " " + currentUser.getLastName())
                        .email(currentUser.getEmail())
                        .userId(currentUser.getId())
                        .externalId(payment.getId())
                        .message("DOCH payment " + reference)
                        .build()).block();
                if (response == null || !StringUtils.hasText(response.getTransId())) {
                    throw new IllegalStateException("No transaction ID returned");
                }
                payment.setGatewayTransactionId(response.getTransId());
            } else {
                FapshiChargeResponse response = fapshiClient.initiatePaymentLink(FapshiChargeRequest.builder()
                        .amount((int) Math.round(amount))
                        .email(currentUser.getEmail())
                        .redirectUrl(redirectUrl)
                        .externalId(payment.getId())
                        .userId(currentUser.getId())
                        .message("DOCH payment " + reference)
                        .build()).block();
                if (response == null || !StringUtils.hasText(response.getLink())) {
                    throw new IllegalStateException("No payment link returned");
                }
                payment.setGatewayTransactionId(response.getTransactionId());
                payment.setPaymentUrl(response.getLink());
            }
            payment.setProviderStatus("CREATED");
        } catch (Exception e) {
            log.error("PAYMENT: provider call failed for payment {}: {}", payment.getId(), e.getMessage());
            payment.setStatus(PaymentStatus.FAILED);
            payment.setFailureReason("The payment provider could not be reached. Please try again.");
            paymentRepository.save(payment);
            markOrderPaymentFailed(order);
            auditService.record(currentUser, "PAYMENT_INITIATION_FAILED", "PAYMENT", payment.getId(), e.getMessage());
            PaymentResponseDTO dto = withRouting(paymentMapper.toDto(payment), xenderRequest != null);
            dto.setMessage(payment.getFailureReason());
            return dto;
        }

        paymentRepository.save(payment);
        if (order != null && order.getStatus() == OrderStatus.PAYMENT_FAILED) {
            order.setStatus(OrderStatus.PENDING);
            orderRepository.save(order);
        }
        auditService.record(currentUser, "PAYMENT_INITIATED", "PAYMENT", payment.getId(),
                String.format("%s flow, %.0f XAF, %s, provider tx %s", payment.getPaymentFlow(), payment.getAmount(),
                        order != null ? "order " + order.getReference() : "xender " + xenderRequest.getId(),
                        payment.getGatewayTransactionId()));

        PaymentResponseDTO dto = withRouting(paymentMapper.toDto(payment), xenderRequest != null);
        dto.setRedirectUrl(redirectUrl);
        return dto;
    }

    private PaymentResponseDTO withRouting(PaymentResponseDTO dto, boolean xender) {
        dto.setContext(xender ? "XENDER" : "MARKET");
        dto.setRedirectUrl(xender ? xenderPaymentRedirectUrl : marketPaymentRedirectUrl);
        return dto;
    }

    private static String mediumFor(String method) {
        if (method == null) return null;
        return switch (method.toUpperCase()) {
            case "MTN_MOMO" -> "mobile money";
            case "ORANGE_MONEY" -> "orange money";
            default -> null;
        };
    }

    // ------------------------------------------------------------------ status & reconciliation

    @Override
    @Transactional
    public PaymentResponseDTO checkPaymentStatus(String paymentId, User currentUser) {
        Payment payment = paymentRepository.findByIdForUpdate(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));
        boolean isOwner = payment.getUser().getId().equals(currentUser.getId());
        boolean isStaff = currentUser.getAuthorities().stream().anyMatch(a ->
                a.getAuthority().equals("MANAGE_SYSTEM") || a.getAuthority().equals("MANAGE_MARKET")
                        || a.getAuthority().equals("MANAGE_XENDER"));
        if (!isOwner && !isStaff) {
            throw new AccessDeniedException("Unauthorized access to payment info.");
        }
        // Respect the provider rate limit (6 req/min per transaction).
        boolean recentlyChecked = payment.getProviderCheckedAt() != null
                && payment.getProviderCheckedAt().isAfter(LocalDateTime.now().minusSeconds(10));
        if (payment.getStatus() == PaymentStatus.PENDING && !recentlyChecked) {
            reconcileLocked(payment);
        }
        return withRouting(paymentMapper.toDto(payment), payment.getXenderRequest() != null);
    }

    @Override
    @Transactional
    public PaymentResponseDTO reconcile(String paymentId) {
        Payment payment = paymentRepository.findByIdForUpdate(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));
        reconcileLocked(payment);
        return withRouting(paymentMapper.toDto(payment), payment.getXenderRequest() != null);
    }

    @Override
    @Transactional
    public void handleProviderCallback(String externalId, String transactionId) {
        Optional<Payment> match = Optional.empty();
        if (StringUtils.hasText(externalId)) {
            match = paymentRepository.findById(externalId);
        }
        if (match.isEmpty() && StringUtils.hasText(transactionId)) {
            match = paymentRepository.findFirstByGatewayTransactionId(transactionId);
        }
        if (match.isEmpty()) {
            log.warn("WEBHOOK: no payment matches externalId={} transId={}", externalId, transactionId);
            return;
        }
        Payment payment = paymentRepository.findByIdForUpdate(match.get().getId()).orElseThrow();
        if (StringUtils.hasText(transactionId) && StringUtils.hasText(payment.getGatewayTransactionId())
                && !transactionId.equals(payment.getGatewayTransactionId())) {
            log.warn("WEBHOOK: transId {} does not match payment {} (expected {}). Ignored.",
                    transactionId, payment.getId(), payment.getGatewayTransactionId());
            return;
        }
        reconcileLocked(payment);
    }

    @Override
    @Transactional
    public void reconcileOrderPayments(Order order) {
        for (Payment attempt : paymentRepository.findAllByOrderOrderByCreatedAtDesc(order)) {
            if (attempt.getStatus() == PaymentStatus.PENDING) {
                Payment locked = paymentRepository.findByIdForUpdate(attempt.getId()).orElse(attempt);
                reconcileLocked(locked);
            }
        }
    }

    @Override
    @Transactional
    public void cancelPendingPayments(Order order, String reason) {
        for (Payment attempt : paymentRepository.findAllByOrderOrderByCreatedAtDesc(order)) {
            if (attempt.getStatus() != PaymentStatus.PENDING) continue;
            Payment locked = paymentRepository.findByIdForUpdate(attempt.getId()).orElse(attempt);
            if (FLOW_LINK.equals(locked.getPaymentFlow())) {
                supersedeLink(locked, reason);
            } else {
                // Direct mobile money prompts cannot be withdrawn; the reconciliation job keeps watching them
                // and flags a refund if the customer approves it after cancellation.
                log.info("PAYMENT: direct payment {} left pending after cancellation of order {}", locked.getId(), order.getReference());
            }
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> findPaymentsToReconcile() {
        Date before = Date.from(LocalDateTime.now().minusSeconds(60).atZone(ZoneId.systemDefault()).toInstant());
        return paymentRepository.findPendingCreatedBefore(before).stream().map(Payment::getId).toList();
    }

    /** Expires a pending payment link at the provider and marks it CANCELLED (or applies it if it was paid meanwhile). */
    private void supersedeLink(Payment payment, String reason) {
        if (StringUtils.hasText(payment.getGatewayTransactionId())) {
            try {
                fapshiClient.expirePaymentLink(payment.getGatewayTransactionId()).block();
            } catch (Exception e) {
                log.warn("PAYMENT: could not expire link {}: {}", payment.getGatewayTransactionId(), e.getMessage());
                reconcileLocked(payment);
                if (payment.getStatus() != PaymentStatus.PENDING) {
                    return;
                }
            }
        }
        payment.setStatus(PaymentStatus.CANCELLED);
        payment.setFailureReason(reason);
        paymentRepository.save(payment);
        auditService.record("PAYMENT_CANCELLED", "PAYMENT", payment.getId(), reason);
    }

    /** Core reconciliation. Caller must hold the row lock on {@code payment}. */
    private void reconcileLocked(Payment payment) {
        if (payment.getStatus() != PaymentStatus.PENDING) {
            return;
        }
        if (!StringUtils.hasText(payment.getGatewayTransactionId())) {
            if (isOlderThanHours(payment, 1)) {
                applyFailure(payment, "Payment was never initiated with the provider.");
            }
            return;
        }

        FapshiStatusResponse status;
        try {
            status = fapshiClient.getPaymentStatus(payment.getGatewayTransactionId()).block();
        } catch (Exception e) {
            log.warn("PAYMENT: status check failed for {}: {}", payment.getId(), e.getMessage());
            return;
        }
        if (status == null || status.getStatus() == null) {
            return;
        }
        payment.setProviderStatus(status.getStatus().name());
        payment.setProviderCheckedAt(LocalDateTime.now());

        switch (status.getStatus()) {
            case SUCCESSFUL -> {
                long expected = Math.round(payment.getAmount());
                if (status.getAmount() != null && status.getAmount() < expected) {
                    applyFailure(payment, "Amount mismatch: provider reported " + status.getAmount() + " XAF, expected " + expected + " XAF.");
                    notificationService.sendNotificationToRole("ROLE_SUPER_ADMIN", NotificationType.PAYMENT_FAILED,
                            "Payment " + payment.getId() + " was rejected because of an amount mismatch. Manual review required.");
                } else {
                    applySuccess(payment);
                }
            }
            case FAILED -> applyFailure(payment, "The payment was declined or failed at the mobile money operator.");
            case EXPIRED -> applyFailure(payment, "The payment link expired before payment was completed.");
            case CREATED, PENDING -> {
                if (isOlderThanHours(payment, expireAfterHours)) {
                    if (FLOW_LINK.equals(payment.getPaymentFlow())) {
                        try {
                            fapshiClient.expirePaymentLink(payment.getGatewayTransactionId()).block();
                        } catch (Exception e) {
                            log.warn("PAYMENT: could not expire stale link {}: {}", payment.getGatewayTransactionId(), e.getMessage());
                            paymentRepository.save(payment);
                            return;
                        }
                        applyFailure(payment, "The payment was not completed in time.");
                    }
                    // Direct pay transactions never expire on the provider side: keep waiting for the final status.
                }
            }
        }
        paymentRepository.save(payment);
    }

    private boolean isOlderThanHours(Payment payment, long hours) {
        if (payment.getCreatedAt() == null) return false;
        LocalDateTime created = payment.getCreatedAt().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
        return created.isBefore(LocalDateTime.now().minusHours(hours));
    }

    private void applySuccess(Payment payment) {
        payment.setStatus(PaymentStatus.COMPLETED);
        payment.setPaidAt(LocalDateTime.now());
        payment.setFailureReason(null);
        paymentRepository.save(payment);

        if (payment.getOrder() != null) {
            Order order = orderRepository.findByIdForUpdate(payment.getOrder().getId()).orElseThrow();
            boolean awaitingPayment = order.getPaidAt() == null
                    && (order.getStatus() == OrderStatus.PENDING || order.getStatus() == OrderStatus.PAYMENT_FAILED);
            if (!awaitingPayment) {
                flagRefund(payment, "Order " + order.getReference() + " was already " + order.getStatus()
                        + " when this payment succeeded.");
                return;
            }
            order.setStatus(OrderStatus.PROCESSING);
            order.setPaidAt(LocalDateTime.now());
            orderRepository.save(order);

            recordMarketplaceSplits(payment, order);
            fulfilmentService.activateAfterPayment(payment);

            notificationService.sendNotification(order.getCustomer(), NotificationType.ORDER_CONFIRMED,
                    String.format("Payment confirmed! Order %s (%.0f XAF) is now being processed.", order.getReference(), order.getTotalAmount()));
            notificationService.sendNotificationToRole("ROLE_MARKET_ADMIN", NotificationType.PAYMENT_SUCCESS,
                    String.format("Order %s paid (%.0f XAF). Sellers notified to prepare packages.", order.getReference(), order.getTotalAmount()));
            notificationService.sendNotificationToRole("ROLE_SUPER_ADMIN", NotificationType.PAYMENT_SUCCESS,
                    String.format("Order %s paid (%.0f XAF).", order.getReference(), order.getTotalAmount()));
            auditService.record(payment.getUser(), "PAYMENT_COMPLETED", "PAYMENT", payment.getId(),
                    "Order " + order.getReference() + " paid, provider tx " + payment.getGatewayTransactionId());
            eventPublisher.publishEvent(new OrderPaidEvent(order.getId(), payment.getId()));
        } else if (payment.getXenderRequest() != null) {
            Delivery delivery = deliveryRepository.findByDeliveryRequest(payment.getXenderRequest()).orElse(null);
            if (delivery == null || delivery.getStatus() != DeliveryStatus.PENDING_PAYMENT) {
                flagRefund(payment, "Delivery request was no longer awaiting payment.");
                return;
            }
            saveSplit(payment, null, payment.getAmount(), SplitType.DELIVERY_FEE);
            fulfilmentService.activateAfterPayment(payment);
            notificationService.sendNotification(payment.getUser(), NotificationType.PAYMENT_SUCCESS,
                    "Payment received. Your package " + delivery.getTrackingNumber() + " is waiting for a driver.");
            auditService.record(payment.getUser(), "PAYMENT_COMPLETED", "PAYMENT", payment.getId(),
                    "Xender request " + payment.getXenderRequest().getId() + " paid");
        }
    }

    private void flagRefund(Payment payment, String why) {
        log.warn("PAYMENT: refund required for payment {}: {}", payment.getId(), why);
        auditService.record(payment.getUser(), "PAYMENT_REFUND_REQUIRED", "PAYMENT", payment.getId(), why);
        String msg = String.format("REFUND REQUIRED: payment %s (%.0f XAF, provider tx %s). %s",
                payment.getId(), payment.getAmount(), payment.getGatewayTransactionId(), why);
        notificationService.sendNotificationToRole("ROLE_SUPER_ADMIN", NotificationType.REFUND_PROCESSED, msg);
        notificationService.sendNotificationToRole("ROLE_MARKET_ADMIN", NotificationType.REFUND_PROCESSED, msg);
        notificationService.sendNotification(payment.getUser(), NotificationType.REFUND_PROCESSED,
                String.format("We received your payment of %.0f XAF but it could not be applied (%s). Our team will refund you.",
                        payment.getAmount(), why));
    }

    private void applyFailure(Payment payment, String reason) {
        payment.setStatus(PaymentStatus.FAILED);
        payment.setFailureReason(reason);
        paymentRepository.save(payment);
        auditService.record(payment.getUser(), "PAYMENT_FAILED", "PAYMENT", payment.getId(), reason);

        if (payment.getOrder() != null) {
            Order order = orderRepository.findByIdForUpdate(payment.getOrder().getId()).orElseThrow();
            boolean otherPending = paymentRepository.findAllByOrderOrderByCreatedAtDesc(order).stream()
                    .anyMatch(p -> !p.getId().equals(payment.getId()) && p.getStatus() == PaymentStatus.PENDING);
            if (!otherPending) {
                markOrderPaymentFailed(order);
            }
        } else if (payment.getXenderRequest() != null) {
            DochXenderDeliveryRequest request = payment.getXenderRequest();
            request.setStatus("PAYMENT_FAILED");
            xenderRequestRepository.save(request);
        }
        notificationService.sendNotification(payment.getUser(), NotificationType.PAYMENT_FAILED,
                "Payment failed: " + reason + " You can retry the payment from your orders.");
    }

    private void markOrderPaymentFailed(Order order) {
        if (order != null && order.getStatus() == OrderStatus.PENDING) {
            order.setStatus(OrderStatus.PAYMENT_FAILED);
            orderRepository.save(order);
            auditService.record("ORDER_STATUS_CHANGED", "ORDER", order.getId(), "PENDING -> PAYMENT_FAILED");
        }
    }

    // ------------------------------------------------------------------ ledger

    /** Records how much of a marketplace payment goes to each seller, the platform commission and logistics. */
    private void recordMarketplaceSplits(Payment payment, Order order) {
        Map<String, User> sellers = new HashMap<>();
        Map<String, Double> sellerNet = new HashMap<>();
        double commissionTotal = 0;

        for (OrderItem item : order.getItems()) {
            User sellerUser = item.getProduct().getSeller().getUser();
            double itemTotal = item.getTotal_price();
            double commission = itemTotal * (commissionPercent / 100.0);
            commissionTotal += commission;
            sellers.put(sellerUser.getId(), sellerUser);
            sellerNet.merge(sellerUser.getId(), itemTotal - commission, Double::sum);
        }

        saveSplit(payment, null, commissionTotal, SplitType.PLATFORM_COMMISSION);
        sellerNet.forEach((userId, net) -> {
            User seller = sellers.get(userId);
            saveSplit(payment, seller, net, SplitType.SELLER_EARNING);
            walletService.creditPending(seller, net, order.getReference());
        });
        saveSplit(payment, null, order.getDeliveryFee(), SplitType.DELIVERY_FEE);
    }

    private void saveSplit(Payment payment, User recipient, Double amount, SplitType type) {
        splitRepository.save(PaymentSplit.builder()
                .payment(payment)
                .recipient(recipient)
                .amount(amount)
                .type(type)
                .status(PaymentSplit.SplitStatus.PENDING)
                .build());
    }
}
