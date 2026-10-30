package com.doch.User.dto.Request;

import lombok.Data;

@Data
public class UpdateProfileRequest {
    // Basic User Fields
    private String firstName;
    private String lastName;
    private String phone;
    private String bio;
    private String gender;

    // Seller Specific Fields
    private String storeName;
    private String storeCity;
    private String storeAddress;
    private String storeContactNumber;

    // Driver Specific Fields
    private String vehicleType;
    private String plateNumber;
}