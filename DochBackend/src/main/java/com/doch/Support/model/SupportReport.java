package com.doch.Support.model;

import com.doch.User.model.User;
import com.doch.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.annotations.UuidGenerator;

@Setter
@Getter
@Entity
@Table(name = "support_report")
@Builder
@NoArgsConstructor
@AllArgsConstructor
@SQLRestriction("is_deleted = false")
public class SupportReport extends BaseEntity {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.RANDOM)
    @Column(columnDefinition = "VARCHAR(36)")
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reporter_id", nullable = false)
    private User reporter;

    /** e.g. VEHICLE_ISSUE, CUSTOMER_ISSUE, APP_BUG, PAYMENT_ISSUE, OTHER */
    private String category;

    private String subject;

    @Column(length = 2000)
    private String description;

    /** Optional link to the delivery this problem occurred on. */
    private String deliveryId;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private ReportStatus status = ReportStatus.OPEN;

    @Column(length = 1000)
    private String resolutionNote;
}
