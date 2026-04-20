package com.attirehub.notification.service;

/**
 * Service for sending transactional emails (verification, password reset, etc.).
 */
public interface EmailService {

    /**
     * Sends a verification email to the user with a link containing the token.
     *
     * @param toEmail  recipient email
     * @param name     user's first name (or email if not available)
     * @param token    verification token for the link
     */
    void sendVerificationEmail(String toEmail, String name, String token);

    /**
     * Sends a password reset email with a link containing the token.
     *
     * @param toEmail recipient email
     * @param token   password reset token for the link
     */
    void sendPasswordResetEmail(String toEmail, String token);
}
