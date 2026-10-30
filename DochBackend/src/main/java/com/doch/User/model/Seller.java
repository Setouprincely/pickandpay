package com.doch.User.model;

import jakarta.persistence.*;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.annotations.UuidGenerator;

import com.doch.common.BaseEntity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@Entity
@Table(name = "seller")
@Builder
@NoArgsConstructor
@AllArgsConstructor
@SQLRestriction("is_deleted = false")
public class Seller extends BaseEntity{
    @Id
    @UuidGenerator(style = UuidGenerator.Style.RANDOM)
    @Column(columnDefinition = "VARCHAR(36)")
    private String id;
    private String name;
    private String city;
    private String address;
    private String rating;
    private String contactNumber;

    @Builder.Default
    private Boolean isVerified = false;

    @Column(name = "verified_at")
    private java.time.LocalDateTime verifiedAt;

    /** Reason given by the administrator when an application is rejected or suspended. */
    @Column(name = "verification_note", length = 500)
    private String verificationNote;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

}
