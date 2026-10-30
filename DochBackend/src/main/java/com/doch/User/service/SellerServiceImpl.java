package com.doch.User.service;

import java.util.List;

import com.doch.Notification.model.NotificationType;
import com.doch.Notification.service.NotificationService;
import org.springframework.stereotype.Service;

import com.doch.User.dto.SellerResponse;
import com.doch.User.dto.Request.SellerRequest;
import com.doch.User.model.Seller;
import com.doch.User.model.User;
import com.doch.User.repository.SellerRepository;
import com.doch.User.repository.UserRepository;
import com.doch.User.service.interfaces.SellerService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SellerServiceImpl implements SellerService {

    private final SellerRepository sellerRepository;

    private final UserRepository userRepository;

    private final NotificationService notificationService;

    private final com.doch.Payment.service.WalletService walletService;

    private final com.doch.common.audit.AuditService auditService;

    public String createSeller(SellerRequest request, User currentUser) {

        // 1. Resolve the target user: default to the logged-in user (self-service).
        User targetUser;
        if (request.getUserId() == null || request.getUserId().isBlank()
                || request.getUserId().equals(currentUser.getId())) {
            targetUser = currentUser;
        } else {
            // Creating a profile for SOMEONE ELSE requires admin authority.
            boolean isAdmin = currentUser.getAuthorities().stream().anyMatch(a ->
                    a.getAuthority().equals("MANAGE_USERS")
                            || a.getAuthority().equals("MANAGE_SYSTEM")
                            || a.getAuthority().equals("MANAGE_MARKET"));
            if (!isAdmin) {
                throw new com.doch.common.exceptions.BusinessException(
                        "You can only create a seller profile for your own account.");
            }
            targetUser = userRepository.findById(request.getUserId())
                    .orElseThrow(() -> new com.doch.common.exceptions.ResourceNotFoundException("User not found"));
        }

        // 2. Prevent duplicate profiles for the same user.
        if (!sellerRepository.findByUserId(targetUser.getId()).isEmpty()) {
            throw new com.doch.common.exceptions.BusinessException(
                    "This user already has a seller profile.");
        }

        Seller seller = Seller.builder()
                .name(request.getStoreName())
                .city(request.getCity())
                .address(request.getAddress())
                .rating(request.getRating())
                .contactNumber(request.getContactNumber())
                .user(targetUser)
                .isVerified(false)
                .build();
        sellerRepository.save(seller);

        // 3. Ensure the seller has a wallet so payments can be split to them.
        walletService.getOrCreateWallet(targetUser);

        // Alert admins that a new store awaits verification.
        notificationService.sendNotificationToRole("ROLE_SUPER_ADMIN", NotificationType.SYSTEM_ANNOUNCEMENT,
                String.format("New seller profile created: %s (%s). Verification pending.",
                        request.getStoreName(), targetUser.getEmail()));

        return seller.getId();
    }

    public List<SellerResponse> getSellerByUserId(String userId) {
        // Implementation here
        List<Seller> sellers = sellerRepository.findByUserId(userId);
        return sellers.stream().map(SellerResponse::fromSeller).toList();
    }

    public List<SellerResponse> getSellersByCity(String city) {
        // Implementation here
        List<Seller> sellers = sellerRepository.findByCity(city);
        return sellers.stream().map(SellerResponse::fromSeller).toList();
    }

    public List<SellerResponse> getAllSellers() {
        // Implementation here
        List<Seller> sellers = sellerRepository.findAll();
        return sellers.stream().map(SellerResponse::fromSeller).toList();
    }

    public void deleteSeller(String sellerId) {
        // Implementation here
        Seller seller = sellerRepository.findById(sellerId)
                .orElseThrow(() -> new RuntimeException("Seller not found"));
        seller.setIsDeleted(true);
        sellerRepository.save(seller);
    }

    @org.springframework.transaction.annotation.Transactional
    public SellerResponse updateSellerVerification(String sellerId, Boolean isVerified, String note) {
        Seller seller = sellerRepository.findById(sellerId)
                .orElseThrow(() -> new com.doch.common.exceptions.ResourceNotFoundException("Seller not found"));
        if (!Boolean.TRUE.equals(isVerified) && (note == null || note.isBlank())) {
            throw new com.doch.common.exceptions.BusinessException("A reason is required to reject or suspend a seller.");
        }
        seller.setIsVerified(isVerified);
        seller.setVerifiedAt(Boolean.TRUE.equals(isVerified) ? java.time.LocalDateTime.now() : null);
        seller.setVerificationNote(note != null && !note.isBlank() ? note.trim() : null);
        sellerRepository.save(seller);
        auditService.record(Boolean.TRUE.equals(isVerified) ? "SELLER_APPROVED" : "SELLER_REJECTED", "SELLER", seller.getId(),
                seller.getName() + (note != null && !note.isBlank() ? ": " + note.trim() : ""));

        String verificationMsg = String.format("Seller %s verification status changed to: %s",
                seller.getName(), isVerified ? "VERIFIED" : "UNVERIFIED");
        notificationService.sendNotificationToRole("ROLE_MARKET_ADMIN", NotificationType.SYSTEM_ANNOUNCEMENT, verificationMsg);
        notificationService.sendNotificationToRole("ROLE_SUPER_ADMIN", NotificationType.SYSTEM_ANNOUNCEMENT, verificationMsg);

        User sellerUser = seller.getUser();
        if (isVerified) {
            notificationService.sendNotification(sellerUser, NotificationType.ACCOUNT_VERIFIED,
                    "Congratulations! Your seller account has been verified. Your approved products are now visible to customers.");
        } else {
            notificationService.sendNotification(sellerUser, NotificationType.SYSTEM_ANNOUNCEMENT,
                    "Your seller account is not approved: " + note.trim() + ". Your products are hidden from customers.");
        }
        return SellerResponse.fromSeller(seller);
    }

    public SellerResponse getSellerById(String sellerId) {
        // Implementation here
        Seller seller = sellerRepository.findById(sellerId)
                .orElseThrow(() -> new RuntimeException("Seller not found"));
        return SellerResponse.fromSeller(seller);
    }

}
