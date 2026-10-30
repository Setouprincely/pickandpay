package com.doch.User.model;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.*;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;

import org.hibernate.annotations.SQLRestriction;
import org.hibernate.annotations.UuidGenerator;

import com.doch.common.BaseEntity;

@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "roles")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@SQLRestriction("is_deleted = false")
public class Role extends BaseEntity{

    @Id
    @GeneratedValue
    @UuidGenerator(style = UuidGenerator.Style.RANDOM) // Auto-generates UUID
    @Column(columnDefinition = "VARCHAR(36)")
    private String id;
    @Column(name = "name", unique = true)
    private String name;

    @Builder.Default
    private Boolean isDeleted = false;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "role_permissions", // Intermediate table
        joinColumns = @JoinColumn(name = "role_id"),
        inverseJoinColumns = @JoinColumn(name = "permission_id")
    )
    @Builder.Default
    private List<Permission> permissions = new ArrayList<>();


}
