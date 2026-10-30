package com.doch.User.service.interfaces;

import com.doch.User.dto.Request.UpdateProfileRequest;
import com.doch.User.dto.UserProfileResponse;

import java.util.List;

public interface UserService {
    UserProfileResponse getUserProfile(String userId);
    UserProfileResponse updateUserProfile(String userId, UpdateProfileRequest request);
    List<UserProfileResponse> getAllUserProfiles();
    void updateUserRole(String userId, String roleName);
    void updateUserStatus(String userId, boolean enabled, com.doch.User.model.User actingAdmin);
    com.doch.User.dto.UserProfileResponse removeUserRole(String userId, String roleName, com.doch.User.model.User actingAdmin);
    void deleteUser(String userId, com.doch.User.model.User actingAdmin);
}
