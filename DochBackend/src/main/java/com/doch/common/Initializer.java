package com.doch.common;

import com.doch.User.model.Permission;
import com.doch.User.model.Role;
import com.doch.User.model.User;
import com.doch.User.repository.PermissionRepository;
import com.doch.User.repository.RoleRepository;
import com.doch.User.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Arrays;
import java.util.List;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class Initializer {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final PasswordEncoder passwordEncoder;

    @org.springframework.beans.factory.annotation.Value("${application.bootstrap.super-admin-email:superadmin@doch.com}")
    private String superAdminEmail;

    /** Only used when the super admin account does not exist yet. Never hardcoded. */
    @org.springframework.beans.factory.annotation.Value("${application.bootstrap.super-admin-password:}")
    private String superAdminInitialPassword;

    @Bean
    @Transactional
    @org.springframework.core.annotation.Order(1)
    public CommandLineRunner initializeData() {
        return args -> {
            log.info("--- [STARTING SYSTEM INITIALIZATION] ---");
            initializePermissions();
            initializeRoles();
            initializeSuperAdminUser();
            log.info("--- [SYSTEM INITIALIZATION COMPLETED] ---");
        };
    }

    private void initializePermissions() {
        List<Permission> permissions = Arrays.asList(
                // --- SUPER ADMIN SCOPE ---
                new Permission(null, "MANAGE_SYSTEM", "Global financial and system configurations", false),
                new Permission(null, "MANAGE_USERS", "Create, delete and manage staff/admin accounts", false),
                new Permission(null, "VIEW_STATS", "Access general dashboard statistics", false),
                new Permission(null, "VIEW_FINANCIAL_STATS", "Access revenue and financial statistics", false),
                new Permission(null, "VIEW_ORDER_STATS", "Access order counts and daily orders", false),
                new Permission(null, "VIEW_CUSTOMER_STATS", "Access customer analytics", false),
                new Permission(null, "VIEW_PRODUCT_STATS", "Access product performance stats", false),
                new Permission(null, "VIEW_LOGISTICS_STATS", "Access logistics/delivery analytics", false),

                // --- DOCH MARKET (E-COMMERCE) SCOPE ---
                new Permission(null, "MANAGE_MARKET", "Manage marketplace products and seller orders", false),
                new Permission(null, "CREATE_PRODUCT", "Upload new products to the catalog", false),
                new Permission(null, "DELETE_PRODUCT", "Soft delete products from the marketplace", false),
                new Permission(null, "VIEW_MARKET_REPORTS", "View sales and seller performance analytics", false),

                // --- DOCH XENDER (LOGISTICS) SCOPE ---
                new Permission(null, "MANAGE_XENDER", "Manage ad-hoc delivery requests and logistics workflow", false),
                new Permission(null, "VIEW_LOGISTICS_REPORTS", "View driver performance and distance analytics", false),

                // --- SHARED STAFF/ADMIN OPERATIONS ---
                new Permission(null, "ASSIGN_DRIVER", "Manually assign or change drivers for deliveries", false),
                new Permission(null, "VIEW_ALL_DELIVERIES", "View master list of all platform deliveries", false),

                // --- CUSTOMER/DRIVER PERMISSIONS ---
                new Permission(null, "PLACE_ORDER", "Permission to purchase goods", false),
                new Permission(null, "DELIVER_PRODUCT", "Permission to perform delivery tasks", false),
                new Permission(null, "VIEW_OWN_FINANCE", "View personal wallet and transaction history", false)
        );

        for (Permission p : permissions) {
            if (!permissionRepository.existsByName(p.getName())) {
                permissionRepository.save(p);
            }
        }
    }

    /**
     * PERMISSION MATRIX (audited against every @PreAuthorize in the codebase).
     * createRoleIfNotExists() refreshes each role's permissions on every startup,
     * so changes here self-heal existing database rows on the next restart —
     * and because the JWT filter reloads authorities from the DB on every
     * request, they take effect without users having to log in again.
     */
    private void initializeRoles() {
        // 1. SUPER ADMIN - Full Platform Control
        createRoleIfNotExists("ROLE_SUPER_ADMIN", Arrays.asList(
                "MANAGE_SYSTEM", "MANAGE_USERS", "MANAGE_MARKET", "MANAGE_XENDER",
                "ASSIGN_DRIVER", "VIEW_ALL_DELIVERIES", "VIEW_MARKET_REPORTS", "VIEW_LOGISTICS_REPORTS",
                "CREATE_PRODUCT", "DELETE_PRODUCT",
                "VIEW_STATS", "VIEW_FINANCIAL_STATS", "VIEW_ORDER_STATS", "VIEW_CUSTOMER_STATS",
                "VIEW_PRODUCT_STATS", "VIEW_LOGISTICS_STATS"
        ));

        // 2. MARKET ADMIN - Specialized for DochMarket
        // FIX: had NO stats permissions, so the admin dashboard's very first
        // call (GET /api/stats/dashboard -> VIEW_STATS) returned 403, as did
        // revenue and order-count widgets. Also granted DELETE_PRODUCT so the
        // catalog moderation permission is explicitly held (previously it
        // existed in the catalog but was granted to no role at all).
        createRoleIfNotExists("ROLE_MARKET_ADMIN", Arrays.asList(
                "MANAGE_MARKET", "VIEW_MARKET_REPORTS", "VIEW_ALL_DELIVERIES", "ASSIGN_DRIVER",
                "CREATE_PRODUCT", "DELETE_PRODUCT",
                "VIEW_STATS", "VIEW_ORDER_STATS", "VIEW_FINANCIAL_STATS",
                "VIEW_CUSTOMER_STATS", "VIEW_PRODUCT_STATS"
        ));

        // 3. XENDER ADMIN - Specialized for DochXender
        // FIX: same dashboard blackout (no VIEW_STATS), and it lacked
        // VIEW_LOGISTICS_STATS — its own domain's analytics. VIEW_ORDER_STATS
        // added because delivery workload is driven by order volume.
        createRoleIfNotExists("ROLE_XENDER_ADMIN", Arrays.asList(
                "MANAGE_XENDER", "VIEW_LOGISTICS_REPORTS", "VIEW_ALL_DELIVERIES", "ASSIGN_DRIVER",
                "VIEW_STATS", "VIEW_ORDER_STATS", "VIEW_LOGISTICS_STATS"
        ));

        // 4. SELLER
        // FIX: added PLACE_ORDER — without it a seller could neither purchase
        // from the marketplace (POST /api/orders) nor create a DochXender
        // delivery request (both PLACE_ORDER-guarded). Sellers are customers
        // of the platform too.
        createRoleIfNotExists("ROLE_SELLER", Arrays.asList(
                "CREATE_PRODUCT", "VIEW_OWN_FINANCE", "PLACE_ORDER"
        ));

        // 5. BUYER
        createRoleIfNotExists("ROLE_BUYER", Arrays.asList(
                "PLACE_ORDER", "VIEW_OWN_FINANCE"
        ));

        // 6. DELIVERY (Driver)
        // FIX: added VIEW_OWN_FINANCE — the platform books DELIVERY_FEE
        // payment splits and /api/wallets/me is built to auto-create wallets,
        // but drivers got 403 on their own earnings/wallet endpoints.
        createRoleIfNotExists("ROLE_DELIVERY", Arrays.asList(
                "DELIVER_PRODUCT", "VIEW_OWN_FINANCE"
        ));
    }

    private void createRoleIfNotExists(String roleName, List<String> permissionNames) {
        // Find the role if it exists, or create a new one
        Role role = roleRepository.findByNameAndIsDeletedFalse(roleName)
                .orElseGet(() -> {
                    log.info("Creating new Role: {}", roleName);
                    Role newRole = new Role();
                    newRole.setName(roleName);
                    return newRole;
                });

        // Always refresh permissions to ensure new ones are added to existing roles
        List<Permission> permissions = permissionRepository.findAllByNameInAndIsDeletedFalse(permissionNames);
        role.setPermissions(permissions);

        roleRepository.save(role);

    }

    private void initializeSuperAdminUser() {
        Role superAdminRole = roleRepository.findByNameAndIsDeletedFalse("ROLE_SUPER_ADMIN")
                .orElseThrow(() -> new RuntimeException("Required role ROLE_SUPER_ADMIN not found"));

        userRepository.findByEmail(superAdminEmail).ifPresentOrElse(existing -> {
            // SELF-HEAL: If anything (a migration, manual SQL, or a bad API call) changed
            // the bootstrap super admin's role or disabled the account, repair it on startup.
            boolean repaired = false;
            if (existing.getUserRole() == null
                    || !"ROLE_SUPER_ADMIN".equals(existing.getUserRole().getName())) {
                log.warn("REPAIR: Super admin {} had role '{}'. Restoring ROLE_SUPER_ADMIN.",
                        superAdminEmail,
                        existing.getUserRole() != null ? existing.getUserRole().getName() : "null");
                existing.setUserRole(superAdminRole);
                repaired = true;
            }
            if (!existing.isEnabled()) {
                log.warn("REPAIR: Super admin {} was disabled. Re-enabling.", superAdminEmail);
                existing.setEnabled(true);
                repaired = true;
            }
            if (repaired) {
                userRepository.save(existing);
                log.info("SUCCESS: Super Admin account repaired.");
            }
        }, () -> {
            if (superAdminInitialPassword == null || superAdminInitialPassword.length() < 12) {
                log.warn("BOOTSTRAP: No super admin exists and BOOTSTRAP_SUPER_ADMIN_PASSWORD is not set "
                        + "(min. 12 characters). Skipping super admin creation.");
                return;
            }
            User admin = User.builder()
                    .firstName("DOCH")
                    .lastName("SuperAdmin")
                    .userName("superadmin")
                    .email(superAdminEmail)
                    .password(passwordEncoder.encode(superAdminInitialPassword))
                    .userRole(superAdminRole)
                    .enabled(true)
                    .build();

            userRepository.save(admin);
            log.info("SUCCESS: Default Super Admin created at {}", superAdminEmail);
        });
    }
}