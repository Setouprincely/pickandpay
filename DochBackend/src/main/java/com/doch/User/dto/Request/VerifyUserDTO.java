package com.doch.User.dto.Request;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class VerifyUserDTO {
    private String email;
    private String token;
}
