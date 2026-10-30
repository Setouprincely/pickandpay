package com.doch.User.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.annotations.UuidGenerator;

import com.doch.common.BaseEntity;

@Entity
@Table(name = "permissions")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@SQLRestriction("is_deleted = false")
public class Permission extends BaseEntity{

    @Id
    @GeneratedValue
    @UuidGenerator(style = UuidGenerator.Style.RANDOM) // Auto-generates UUID
    @Column(columnDefinition = "VARCHAR(36)")
    private String id;
    @Column(name = "name", unique = true)
    private String name;
    private String description;
    @Builder.Default
    private Boolean isDeleted = false;


}
