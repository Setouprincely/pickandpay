package com.doch.Logistic.model;

import com.doch.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;

/**
 * Immutable tracking / audit record of everything that happened to a package:
 * status changes, scans, assignments, OTP failures, notes. Forms the tracking history.
 */
@Getter
@Setter
@Entity
@Table(name = "delivery_event", indexes = {
        @Index(name = "idx_delivery_event_delivery", columnList = "delivery_id, occurred_at")
})
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryEvent extends BaseEntity {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.RANDOM)
    @Column(columnDefinition = "VARCHAR(36)")
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "delivery_id", nullable = false)
    private Delivery delivery;

    @Column(name = "event_type", length = 40, nullable = false)
    private String eventType;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_status", length = 30)
    private DeliveryStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_status", length = 30)
    private DeliveryStatus toStatus;

    /** Customer-safe description, shown in tracking history. */
    @Column(name = "public_message", length = 500)
    private String publicMessage;

    /** Internal note (reasons, scan codes, ...). Visible to staff only. */
    @Column(name = "internal_note", length = 1000)
    private String internalNote;

    @Column(name = "actor_id", length = 36)
    private String actorId;

    @Column(name = "actor_name", length = 200)
    private String actorName;

    @Column(name = "actor_role", length = 50)
    private String actorRole;

    @Column(name = "hub_name", length = 120)
    private String hubName;

    private String latitude;
    private String longitude;

    @Column(name = "occurred_at", nullable = false)
    private LocalDateTime occurredAt;
}
