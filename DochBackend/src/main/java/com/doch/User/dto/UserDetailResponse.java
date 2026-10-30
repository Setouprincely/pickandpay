package com.doch.User.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserDetailResponse {

    private String id;
    private String email;
    private String firstName;
    private String lastName;
    private String userName;
    private String phone;
    private String role;
    private java.util.Set<String> roles;

}
