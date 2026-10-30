package com.doch.User.dto.Request;

import lombok.Data;

@Data
public class SellerRequest {
    private String storeName;
    private String city;
    private String address;
    private String rating;
    private String contactNumber;

    /**
     * Optional. When omitted, the seller profile is created for the logged-in user.
     * Only admins (MANAGE_USERS / MANAGE_SYSTEM / MANAGE_MARKET) may create a
     * profile for a different user.
     */
    private String userId;
}
