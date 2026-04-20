package com.attirehub.notification.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

import java.io.UnsupportedEncodingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

/**
 * Sends transactional emails for verification and password reset.
 * When mail is not configured (no JavaMailSender or empty username), logs instead of sending.
 */
@Service
public class EmailServiceImpl implements EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailServiceImpl.class);

    private final JavaMailSender mailSender;

    @Value("${app.frontend-base-url:http://localhost:3001}")
    private String frontendBaseUrl;

    @Value("${spring.mail.username:}")
    private String mailUsername;

    public EmailServiceImpl(@Autowired(required = false) JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Override
    public void sendVerificationEmail(String toEmail, String name, String token) {
        String verificationUrl = frontendBaseUrl + "/verify-email?token=" + token;
        String subject = "Verify your AttireHub account";
        String htmlBody = buildVerificationEmailHtml(name, verificationUrl);

        sendOrLog(toEmail, subject, htmlBody, "verification");
    }

    @Override
    public void sendPasswordResetEmail(String toEmail, String token) {
        String resetUrl = frontendBaseUrl + "/reset-password?token=" + token;
        String subject = "Reset your AttireHub password";
        String htmlBody = buildPasswordResetEmailHtml(resetUrl);

        sendOrLog(toEmail, subject, htmlBody, "password reset");
    }

    private void sendOrLog(String toEmail, String subject, String htmlBody, String emailType) {
        if (mailSender == null || mailUsername == null || mailUsername.isBlank()) {
            log.info("[MAIL NOT CONFIGURED] Would send {} email to {}: subject='{}'", 
                    emailType, toEmail, subject);
            log.debug("Email body: {}", htmlBody);
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            try {
                helper.setFrom(mailUsername, "AttireHub");
            } catch (UnsupportedEncodingException e) {
                helper.setFrom(mailUsername);
            }
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            mailSender.send(message);
            log.info("Sent {} email to {} (from {})", emailType, toEmail, mailUsername);
        } catch (MessagingException e) {
            log.error("Failed to send {} email to {}: {}", emailType, toEmail, e.getMessage());
            throw new IllegalStateException("Failed to send email. Please try again later.", e);
        }
    }

    private String buildVerificationEmailHtml(String name, String verificationUrl) {
        String displayName = (name != null && !name.isBlank()) ? name : "there";
        return """
            <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;">
                <h2 style="color: #333;">Verify your email</h2>
                <p>Hi %s,</p>
                <p>Thanks for signing up for AttireHub. Please click the link below to verify your email address:</p>
                <p><a href="%s" style="display: inline-block; padding: 12px 24px; background: #2563eb; color: white; text-decoration: none; border-radius: 6px;">Verify Email</a></p>
                <p>Or copy and paste this link into your browser:</p>
                <p style="word-break: break-all; color: #666;">%s</p>
                <p>This link expires in 24 hours. If you didn't create an account, you can ignore this email.</p>
                <p>— The AttireHub Team</p>
            </div>
            """.formatted(displayName, verificationUrl, verificationUrl);
    }

    private String buildPasswordResetEmailHtml(String resetUrl) {
        return """
            <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;">
                <h2 style="color: #333;">Reset your password</h2>
                <p>You requested a password reset for your AttireHub account. Click the link below to set a new password:</p>
                <p><a href="%s" style="display: inline-block; padding: 12px 24px; background: #2563eb; color: white; text-decoration: none; border-radius: 6px;">Reset Password</a></p>
                <p>Or copy and paste this link into your browser:</p>
                <p style="word-break: break-all; color: #666;">%s</p>
                <p>This link expires in 1 hour. If you didn't request a reset, you can safely ignore this email.</p>
                <p>— The AttireHub Team</p>
            </div>
            """.formatted(resetUrl, resetUrl);
    }
}
