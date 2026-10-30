package com.doch.User.model;

import jakarta.persistence.*;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.annotations.UuidGenerator;

import com.doch.common.BaseEntity;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@Entity
@Slf4j
@NoArgsConstructor
@AllArgsConstructor
@SQLRestriction("is_deleted = false")
public class VerificationToken extends BaseEntity {
    @Id
    @GeneratedValue
    @UuidGenerator(style = UuidGenerator.Style.RANDOM) // Auto-generates UUID
    @Column(columnDefinition = "VARCHAR(36)")
    private String id;

    private String otpCode;

    private LocalDateTime createdTime;

    private LocalDateTime expiresAt;

    private boolean isUsed;

    @ManyToOne
    @JoinColumn(name = "c_user_id", nullable = false)
    private User user;

    // Method to check if the OTP is expired
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    // Method to mark the OTP as used
    public void markAsUsed() {
        this.isUsed = true;
    }
}
