package com.doch.User.dto;

import com.doch.Logistic.dto.driver.DriverProfileDTO;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserProfileResponse {
    private String id;
    private String email;
    private String firstName;
    private String lastName;
    private String phone;
    private String role;
    private java.util.Set<String> roles;
    private String profilePictureUrl;
    private String bio;

    // Role-specific data (Optional)
    private SellerResponse sellerProfile;
    private DriverProfileDTO driverProfile;
}