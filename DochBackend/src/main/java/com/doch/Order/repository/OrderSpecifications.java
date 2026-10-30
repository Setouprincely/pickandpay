package com.doch.Order.repository;

import com.doch.Order.model.Order;
import com.doch.Order.model.OrderStatus;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;

import java.util.Date;

/**
 * Reusable filter predicates for the Admin orders page
 * (GET /api/orders/all with status, date range, seller and buyer filters).
 */
public final class OrderSpecifications {

    private OrderSpecifications() {
    }

    public static Specification<Order> hasStatus(OrderStatus status) {
        return (root, query, cb) -> status == null ? cb.conjunction() : cb.equal(root.get("status"), status);
    }

    public static Specification<Order> hasBuyer(String buyerId) {
        return (root, query, cb) -> buyerId == null ? cb.conjunction()
                : cb.equal(root.get("customer").get("id"), buyerId);
    }

    public static Specification<Order> hasSeller(String sellerId) {
        return (root, query, cb) -> {
            if (sellerId == null) return cb.conjunction();
            query.distinct(true);
            Join<Object, Object> items = root.join("items", JoinType.INNER);
            Join<Object, Object> product = items.join("product", JoinType.INNER);
            return cb.equal(product.get("seller").get("id"), sellerId);
        };
    }

    public static Specification<Order> createdAfter(Date start) {
        return (root, query, cb) -> start == null ? cb.conjunction()
                : cb.greaterThanOrEqualTo(root.get("createdAt"), start);
    }

    public static Specification<Order> createdBefore(Date end) {
        return (root, query, cb) -> end == null ? cb.conjunction()
                : cb.lessThanOrEqualTo(root.get("createdAt"), end);
    }

    /** Free-text search: exact order ID, order reference, package tracking number, or customer email/name. */
    public static Specification<Order> matchesQuery(String q) {
        return (root, query, cb) -> {
            if (q == null || q.isBlank()) return cb.conjunction();
            String term = q.trim();
            String like = "%" + term.toLowerCase() + "%";
            Join<Object, Object> customer = root.join("customer", JoinType.LEFT);

            jakarta.persistence.criteria.Subquery<String> byTracking = query.subquery(String.class);
            var delivery = byTracking.from(com.doch.Logistic.model.Delivery.class);
            byTracking.select(delivery.get("marketDeliveryRequest").get("order").get("id"))
                    .where(cb.equal(cb.upper(delivery.get("trackingNumber")), term.toUpperCase()));

            return cb.or(
                    cb.equal(root.get("id"), term),
                    cb.equal(cb.upper(root.get("reference")), term.toUpperCase()),
                    cb.like(cb.lower(customer.get("email")), like),
                    cb.like(cb.lower(cb.concat(cb.concat(customer.get("firstName"), " "), customer.get("lastName"))), like),
                    root.get("id").in(byTracking)
            );
        };
    }
}
