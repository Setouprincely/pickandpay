package com.doch.common.jobs;

import com.doch.Order.service.interfaces.OrderService;
import com.doch.Payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Background reconciliation. Fapshi webhooks are sent only once and never retried, so pending payments
 * are periodically re-checked against the provider, and orders that were never paid release their stock.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "application.jobs.enabled", havingValue = "true", matchIfMissing = true)
public class OperationsJobs {

    private final PaymentService paymentService;
    private final OrderService orderService;

    @Scheduled(fixedDelayString = "${application.payments.reconcile-interval-ms:120000}", initialDelay = 60_000)
    public void reconcilePendingPayments() {
        var ids = paymentService.findPaymentsToReconcile();
        if (ids.isEmpty()) return;
        int processed = 0;
        for (String id : ids) {
            try {
                paymentService.reconcile(id);
                processed++;
            } catch (Exception e) {
                log.warn("JOB: reconciliation failed for payment {}: {}", id, e.getMessage());
            }
        }
        log.info("JOB: reconciled {} / {} pending payment(s)", processed, ids.size());
    }

    @Scheduled(cron = "${application.orders.unpaid-cancel-cron:0 */15 * * * *}")
    public void cancelStaleUnpaidOrders() {
        try {
            int cancelled = orderService.cancelStaleUnpaidOrders();
            if (cancelled > 0) log.info("JOB: cancelled {} unpaid order(s)", cancelled);
        } catch (Exception e) {
            log.error("JOB: unpaid order cancellation failed", e);
        }
    }
}
