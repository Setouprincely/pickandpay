package com.doch.User.service;

import com.doch.Catalog.repository.ProductRepository;
import com.doch.Logistic.dto.driver.DriverProfileDTO;
import com.doch.Logistic.model.DeliveryStatus;
import com.doch.Logistic.repository.DeliveryRepository;
import com.doch.Payment.service.WalletService;
import com.doch.User.dto.*;
import com.doch.User.dto.Request.UpdateProfileRequest;
import com.doch.User.model.*;
import com.doch.User.repository.*;
import com.doch.User.service.interfaces.UserService;
import com.doch.common.audit.AuditService;
import com.doch.common.exceptions.BusinessException;
import com.doch.common.exceptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final SellerRepository sellerRepository;
    private final DriverProfileRepository driverProfileRepository;
    private final RoleRepository roleRepository;
    private final WalletService walletService;
    private final TokenRepository tokenRepository;
    private final DeliveryRepository deliveryRepository;
    private final AuditService auditService;

    @Override
    public UserProfileResponse getUserProfile(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        UserProfileResponse.UserProfileResponseBuilder builder = UserProfileResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .phone(user.getTel())
                .role(user.getUserRole().getName())
                .roles(user.getRoleNames());

        // Attach Seller data if applicable
        sellerRepository.findByUserId(userId).stream().findFirst().ifPresent(s -> {
            builder.sellerProfile(SellerResponse.fromSeller(s));
        });

        // Attach Driver data if applicable
        driverProfileRepository.findByUser(user).ifPresent(d -> {
            builder.driverProfile(DriverProfileDTO.builder()
                    .profileId(d.getId())
                    .vehicleType(d.getVehicleType())
                    .plateNumber(d.getPlateNumber())
                    .rating(d.getRating())
                    .build());
        });

        return builder.build();
    }

    @Override
    @Transactional
    public UserProfileResponse updateUserProfile(String userId, UpdateProfileRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // 1. Role Detection
        Optional<Seller> sellerOpt = sellerRepository.findByUserId(userId).stream().findFirst();
        Optional<DriverProfile> driverOpt = driverProfileRepository.findByUser(user);

        // 2. STRICT VALIDATION: Seller Data
        boolean hasSellerData = request.getStoreName() != null || request.getStoreCity() != null ||
                request.getStoreAddress() != null || request.getStoreContactNumber() != null;

        if (hasSellerData && sellerOpt.isEmpty()) {
            throw new BusinessException("Update Denied: You must have a Seller Profile to update store information.");
        }

        // 3. STRICT VALIDATION: Driver Data
        boolean hasDriverData = request.getVehicleType() != null || request.getPlateNumber() != null;

        if (hasDriverData && driverOpt.isEmpty()) {
            throw new BusinessException("Update Denied: You must have a Driver Profile to update vehicle information.");
        }

        // 4. PERFORM UPDATES

        // --- Update Base User Info ---
        if (request.getFirstName() != null) user.setFirstName(request.getFirstName());
        if (request.getLastName() != null) user.setLastName(request.getLastName());
        if (request.getPhone() != null) user.setTel(request.getPhone());
        userRepository.save(user);

        // --- Update Seller Info (only if authorized) ---
        sellerOpt.ifPresent(seller -> {
            if (request.getStoreName() != null) seller.setName(request.getStoreName());
            if (request.getStoreCity() != null) seller.setCity(request.getStoreCity());
            if (request.getStoreAddress() != null) seller.setAddress(request.getStoreAddress());
            if (request.getStoreContactNumber() != null) seller.setContactNumber(request.getStoreContactNumber());
            sellerRepository.save(seller);
        });

        // --- Update Driver Info (only if authorized) ---
        driverOpt.ifPresent(driver -> {
            if (request.getVehicleType() != null) driver.setVehicleType(request.getVehicleType());
            if (request.getPlateNumber() != null) driver.setPlateNumber(request.getPlateNumber());
            driverProfileRepository.save(driver);
        });

        return getUserProfile(userId);
    }

    @Override
    public List<UserProfileResponse> getAllUserProfiles() {
        return userRepository.findAll().stream()
                .map(user -> getUserProfile(user.getId()))
                .collect(Collectors.toList());
    }

    /** The bootstrap Super Admin account created by the Initializer. Its role must never change. */
    private static final String BOOTSTRAP_SUPER_ADMIN_EMAIL = "superadmin@doch.com";

    @Override
    @Transactional
    public void updateUserRole(String userId, String roleName) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // GUARD: The bootstrap super admin can never be demoted (see incident where
        // superadmin@doch.com ended up with ROLE_BUYER and locked admins out).
        if (BOOTSTRAP_SUPER_ADMIN_EMAIL.equalsIgnoreCase(user.getEmail())
                && !"ROLE_SUPER_ADMIN".equals(roleName)) {
            throw new BusinessException("The bootstrap super admin account's role cannot be changed.");
        }

        Role newRole = roleRepository.findByNameAndIsDeletedFalse(roleName)
                .orElseThrow(() -> new BusinessException("Role " + roleName + " does not exist in the system."));

        // Update the user's role
        user.setUserRole(newRole);
        userRepository.save(user);

        // LOGIC: If we just turned this user into a Seller, ensure they have a Wallet
        if (roleName.equals("ROLE_SELLER")) {
            walletService.getOrCreateWallet(user);
        }
    }

    @Override
    @Transactional
    public void updateUserStatus(String userId, boolean enabled, User actingAdmin) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // GUARDS
        if (actingAdmin != null && actingAdmin.getId().equals(userId)) {
            throw new BusinessException("You cannot suspend or reactivate your own account.");
        }
        if (!enabled && BOOTSTRAP_SUPER_ADMIN_EMAIL.equalsIgnoreCase(user.getEmail())) {
            throw new BusinessException("The bootstrap super admin account cannot be suspended.");
        }

        user.setEnabled(enabled);
        userRepository.save(user);

        // On suspension, immediately invalidate every active session for the user
        if (!enabled) {
            tokenRepository.findAllAccessTokensByUser(user.getId()).forEach(token -> {
                token.setLoggedOut(true);
                tokenRepository.save(token);
            });
        }
    }

    private static final List<DeliveryStatus> FINISHED_DELIVERIES =
            List.of(DeliveryStatus.DELIVERED, DeliveryStatus.CANCELLED, DeliveryStatus.RETURNED);

    private void guardAdminAction(User user, User actingAdmin, String what) {
        if (actingAdmin != null && actingAdmin.getId().equals(user.getId())) {
            throw new BusinessException("You cannot " + what + " your own account.");
        }
        if (BOOTSTRAP_SUPER_ADMIN_EMAIL.equalsIgnoreCase(user.getEmail())) {
            throw new BusinessException("The bootstrap super admin account cannot be changed this way.");
        }
    }

    private void ensureNoActiveDeliveries(User user) {
        long active = deliveryRepository.findByDriverAndStatusNotIn(user, FINISHED_DELIVERIES).size();
        if (active > 0) {
            throw new BusinessException("This driver still has " + active
                    + " active package(s). Reassign or finish them in Dispatch first.");
        }
    }

    /** Takes driver access away: the driver goes offline. */
    private void stopDriving(User user) {
        ensureNoActiveDeliveries(user);
        user.setIsAvailable(false);
    }

    /** Takes seller access away: the store is un-verified, which hides all its products from the marketplace. */
    private void closeStore(User user) {
        sellerRepository.findByUserId(user.getId()).forEach(s -> {
            s.setIsVerified(false);
            sellerRepository.save(s);
        });
    }

    @Override
    @Transactional
    public UserProfileResponse removeUserRole(String userId, String roleName, User actingAdmin) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        guardAdminAction(user, actingAdmin, "remove a role from");
        if (!user.hasRole(roleName)) {
            throw new BusinessException("This user does not have the role " + roleName + ".");
        }
        if (user.getRoleNames().size() == 1) {
            throw new BusinessException("This is the user's only role. Delete the user instead.");
        }

        if ("ROLE_DELIVERY".equals(roleName)) stopDriving(user);
        if ("ROLE_SELLER".equals(roleName)) closeStore(user);

        boolean removedExtra = user.getExtraRoles().removeIf(r -> r.getName().equals(roleName));
        if (!removedExtra) {
            // It was the primary role: promote one of the remaining roles.
            Role next = user.getExtraRoles().iterator().next();
            user.getExtraRoles().remove(next);
            user.setUserRole(next);
        }
        userRepository.save(user);
        auditService.record(actingAdmin, "USER_ROLE_REMOVED", "USER", user.getId(),
                roleName + " removed from " + user.getEmail());
        log.info("ADMIN: {} removed {} from {}", actingAdmin != null ? actingAdmin.getEmail() : "system", roleName, user.getEmail());
        return getUserProfile(userId);
    }

    @Override
    @Transactional
    public void deleteUser(String userId, User actingAdmin) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        guardAdminAction(user, actingAdmin, "delete");
        if (user.hasRole("ROLE_DELIVERY")) stopDriving(user);
        if (user.hasRole("ROLE_SELLER")) closeStore(user);

        // Soft delete: orders, payments and the audit trail keep pointing at this record, the account
        // disappears everywhere else, can no longer sign in, and the email can be registered again.
        user.setEnabled(false);
        user.setIsDeleted(true);
        userRepository.save(user);
        tokenRepository.findAllAccessTokensByUser(user.getId()).forEach(token -> {
            token.setLoggedOut(true);
            tokenRepository.save(token);
        });
        auditService.record(actingAdmin, "USER_DELETED", "USER", user.getId(),
                user.getEmail() + " (" + String.join(", ", user.getRoleNames()) + ") deleted");
        log.info("ADMIN: {} deleted user {}", actingAdmin != null ? actingAdmin.getEmail() : "system", user.getEmail());
    }
}
