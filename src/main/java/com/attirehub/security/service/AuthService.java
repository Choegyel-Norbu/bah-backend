package com.attirehub.security.service;

import com.attirehub.security.dto.*;

public interface AuthService {

    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    AuthResponse refreshToken(RefreshTokenRequest request);

    /**
     * Verifies user email using the token sent in the verification email.
     */
    void verifyEmail(String token);

    /**
     * Initiates password reset flow. Sends reset email if the user exists.
     * Always returns successfully (no user enumeration).
     */
    void forgotPassword(String email);

    /**
     * Resets password using the token from the reset email.
     */
    void resetPassword(String token, String newPassword);
}
