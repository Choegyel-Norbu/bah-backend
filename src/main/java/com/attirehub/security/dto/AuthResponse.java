package com.attirehub.security.dto;

import com.attirehub.user.dto.UserProfileResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {

    private String accessToken;
    private String refreshToken;
    private String tokenType;
    private long expiresIn;
    /** User details including id, email, name, role — for login/register/refresh. */
    private UserProfileResponse user;
}
