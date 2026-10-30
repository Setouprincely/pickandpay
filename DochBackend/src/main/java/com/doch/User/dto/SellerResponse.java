package com.doch.User.dto;

import com.doch.User.model.Seller;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Date;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SellerResponse {
    private String id;
    private String userId;
    private String storeName;
    private String city;
    private String address;
    private String rating;
    private String contactNumber;
    private String ownerName;
    private String ownerEmail;
    private Boolean isVerified;
    private LocalDateTime verifiedAt;
    private String verificationNote;
    private Date createdAt;

    public static SellerResponse fromSeller(Seller seller) {
        return SellerResponse.builder()
                .id(seller.getId())
                .userId(seller.getUser() != null ? seller.getUser().getId() : null)
                .storeName(seller.getName())
                .city(seller.getCity())
                .address(seller.getAddress())
                .rating(seller.getRating())
                .contactNumber(seller.getContactNumber())
                .ownerName(seller.getUser() != null ? seller.getUser().getFirstName() + " " + seller.getUser().getLastName() : null)
                .ownerEmail(seller.getUser() != null ? seller.getUser().getEmail() : null)
                .isVerified(seller.getIsVerified())
                .verifiedAt(seller.getVerifiedAt())
                .verificationNote(seller.getVerificationNote())
                .createdAt(seller.getCreatedAt())
                .build();
    }
}