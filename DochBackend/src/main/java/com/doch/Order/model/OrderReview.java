package com.doch.Order.model;

import com.doch.User.model.User;
import com.doch.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

/** Customer rating and review of a delivered order (one per order). */
@Getter
@Setter
@Entity
@Table(name = "order_review")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderReview extends BaseEntity {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.RANDOM)
    @Column(columnDefinition = "VARCHAR(36)")
    private String id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false, unique = true)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private User customer;

    /** Overall rating of the purchase experience, 1 to 5. */
    @Column(nullable = false)
    private Integer rating;

    /** Optional rating of the delivery service (driver), 1 to 5. */
    @Column(name = "delivery_rating")
    private Integer deliveryRating;

    @Column(length = 1000)
    private String comment;
}
