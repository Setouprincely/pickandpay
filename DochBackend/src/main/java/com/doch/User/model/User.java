package com.doch.User.model;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.annotations.UuidGenerator;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.doch.common.BaseEntity;

@Setter
@Getter
@Entity
@Table(name = "users")
@Builder
@NoArgsConstructor
@AllArgsConstructor
@SQLRestriction("is_deleted = false")
public class User extends BaseEntity implements UserDetails{

    @Id
    @UuidGenerator(style = UuidGenerator.Style.RANDOM) // Auto-generates UUID
    @Column(columnDefinition = "VARCHAR(36)")
    private String id;
    private String userName;
    private String firstName;
    private String lastName;
    private String email;
    private String password;
    private String tel;

    @Builder.Default
    private String language = "EN";

    private Boolean enabled;

    @Builder.Default
    @Column(name = "is_available", nullable = false)
    private Boolean isAvailable = false;

    @ManyToOne
    private Role userRole;

    /** Roles held in addition to the primary {@link #userRole} (e.g. a seller who also buys). */
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "user_extra_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id"))
    @Builder.Default
    private Set<Role> extraRoles = new HashSet<>();

    public Set<String> getRoleNames() {
        Set<String> names = new LinkedHashSet<>();
        if (userRole != null) names.add(userRole.getName());
        if (extraRoles != null) extraRoles.forEach(r -> names.add(r.getName()));
        return names;
    }

    public boolean hasRole(String roleName) {
        return getRoleNames().contains(roleName);
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        Set<String> authorities = new LinkedHashSet<>();
        if (userRole != null) {
            userRole.getPermissions().forEach(p -> authorities.add(p.getName()));
        }
        if (extraRoles != null) {
            extraRoles.forEach(r -> r.getPermissions().forEach(p -> authorities.add(p.getName())));
        }
        authorities.addAll(getRoleNames());
        return authorities.stream().map(SimpleGrantedAuthority::new).collect(Collectors.toList());
    }


    @Override
    public String getUsername() {
        return this.email;
    }

    @Override
    public boolean isEnabled() {
        return this.enabled != null && this.enabled;
    }

    @Override
    public String getPassword() {
        return this.password;
    }

}
