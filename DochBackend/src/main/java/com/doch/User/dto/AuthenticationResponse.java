package com.doch.User.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AuthenticationResponse {
    @JsonProperty("access_token")
    private String accessToken;

    @JsonProperty("refresh_token")
    private String refreshToken;

    @JsonProperty("message")
    private String message;
    
    @JsonProperty("user_id")       
    private String userId;

    @JsonProperty("role")
    private String role;

    /** Every role the account holds (primary role first). */
    @JsonProperty("roles")
    private java.util.Set<String> roles;
}
