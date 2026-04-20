package com.attirehub.security.service;

import com.attirehub.notification.service.EmailService;
import com.attirehub.security.dto.AuthResponse;
import com.attirehub.security.dto.LoginRequest;
import com.attirehub.security.dto.RefreshTokenRequest;
import com.attirehub.security.dto.RegisterRequest;
import com.attirehub.security.jwt.JwtTokenProvider;
import com.attirehub.shared.enums.Role;
import com.attirehub.shared.exception.BadRequestException;
import com.attirehub.shared.exception.DuplicateResourceException;
import com.attirehub.shared.exception.EmailNotVerifiedException;
import com.attirehub.shared.exception.UnauthorizedException;
import com.attirehub.user.dto.UserProfileResponse;
import com.attirehub.user.entity.EmailVerificationToken;
import com.attirehub.user.entity.PasswordResetToken;
import com.attirehub.user.entity.User;
import com.attirehub.user.mapper.UserMapper;
import com.attirehub.user.repository.EmailVerificationTokenRepository;
import com.attirehub.user.repository.PasswordResetTokenRepository;
import com.attirehub.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;
    private final EmailVerificationTokenRepository verificationTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final EmailService emailService;

    @Value("${app.jwt.access-token-expiration}")
    private long accessTokenExpiration;

    @Value("${app.verification-token-expiration-hours:24}")
    private int verificationTokenExpirationHours;

    @Value("${app.password-reset-token-expiration-hours:1}")
    private int passwordResetTokenExpirationHours;

    @Value("${app.frontend-base-url:http://localhost:5173}")
    private String frontendBaseUrl;

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        // Check for duplicate email
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("User", "email", request.getEmail());
        }

        // Build and save user
        User user = User.builder()
                .email(request.getEmail().toLowerCase().trim())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .phoneNumber(request.getPhoneNumber())
                .role(Role.CUSTOMER)
                .build();

        userRepository.save(user);

        // Create and send verification email
        String verificationToken = UUID.randomUUID().toString().replace("-", "");
        EmailVerificationToken evToken = EmailVerificationToken.builder()
                .user(user)
                .token(verificationToken)
                .expiresAt(LocalDateTime.now().plusHours(verificationTokenExpirationHours))
                .build();
        verificationTokenRepository.save(evToken);
        try {
            emailService.sendVerificationEmail(user.getEmail(), user.getFirstName(), verificationToken);
        } catch (Exception e) {
            String verifyUrl = frontendBaseUrl + "/verify-email?token=" + verificationToken;
            log.warn("Failed to send verification email to {}: {}. Registration completed. DEV: Use this link to verify: {}", 
                    user.getEmail(), e.getMessage(), verifyUrl);
        }

        log.info("New user registered: email={}", user.getEmail());

        // Generate tokens
        String accessToken = jwtTokenProvider.generateAccessToken(user);
        String refreshToken = jwtTokenProvider.generateRefreshToken(user);

        return buildAuthResponse(accessToken, refreshToken, user);
    }

    @Override
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getEmail().toLowerCase().trim(),
                            request.getPassword()
                    )
            );
        } catch (BadCredentialsException e) {
            throw new UnauthorizedException("Invalid email or password");
        }

        User user = (User) userDetailsService.loadUserByUsername(
                request.getEmail().toLowerCase().trim());

        if (!user.isActive()) {
            throw new UnauthorizedException("Account has been deactivated");
        }

        if (!user.isEmailVerified()) {
            throw new EmailNotVerifiedException(
                    "Please verify your email before signing in. Check your inbox for the verification link.");
        }

        String accessToken = jwtTokenProvider.generateAccessToken(user);
        String refreshToken = jwtTokenProvider.generateRefreshToken(user);

        log.info("User logged in: email={}", user.getEmail());
        return buildAuthResponse(accessToken, refreshToken, user);
    }

    @Override
    @Transactional(readOnly = true)
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        String refreshToken = request.getRefreshToken();

        // Validate it's actually a refresh token
        if (!jwtTokenProvider.isRefreshToken(refreshToken)) {
            throw new BadRequestException("Invalid refresh token");
        }

        String username = jwtTokenProvider.extractUsername(refreshToken);
        User user = (User) userDetailsService.loadUserByUsername(username);

        if (!jwtTokenProvider.isTokenValid(refreshToken, user)) {
            throw new UnauthorizedException("Refresh token is expired or invalid");
        }

        // Issue new access token (refresh token stays the same)
        String newAccessToken = jwtTokenProvider.generateAccessToken(user);

        log.info("Token refreshed for user: email={}", user.getEmail());
        return buildAuthResponse(newAccessToken, refreshToken, user);
    }

    @Override
    @Transactional
    public void verifyEmail(String token) {
        EmailVerificationToken evToken = verificationTokenRepository.findByToken(token)
                .orElseThrow(() -> new BadRequestException("Invalid or expired verification token"));

        if (evToken.isUsed()) {
            throw new BadRequestException("Verification token has already been used");
        }
        if (evToken.isExpired()) {
            throw new BadRequestException("Verification token has expired. Please request a new one.");
        }

        User user = evToken.getUser();
        user.setEmailVerified(true);
        userRepository.save(user);

        evToken.setUsedAt(LocalDateTime.now());
        verificationTokenRepository.save(evToken);

        log.info("Email verified for user: email={}", user.getEmail());
    }

    @Override
    @Transactional
    public void forgotPassword(String email) {
        String normalizedEmail = email.toLowerCase().trim();
        Optional<User> userOpt = userRepository.findByEmail(normalizedEmail);

        if (userOpt.isEmpty()) {
            log.debug("Forgot password requested for unknown email: {}", normalizedEmail);
            return;
        }

        User user = userOpt.get();
        passwordResetTokenRepository.deleteByUser_Id(user.getId());

        String resetToken = UUID.randomUUID().toString().replace("-", "");
        PasswordResetToken prToken = PasswordResetToken.builder()
                .user(user)
                .token(resetToken)
                .expiresAt(LocalDateTime.now().plusHours(passwordResetTokenExpirationHours))
                .build();
        passwordResetTokenRepository.save(prToken);
        try {
            emailService.sendPasswordResetEmail(user.getEmail(), resetToken);
        } catch (Exception e) {
            String resetUrl = frontendBaseUrl + "/reset-password?token=" + resetToken;
            log.warn("Failed to send password reset email to {}: {}. DEV: Use this link to reset: {}", 
                    normalizedEmail, e.getMessage(), resetUrl);
        }

        log.info("Password reset email sent to: {}", normalizedEmail);
    }

    @Override
    @Transactional
    public void resetPassword(String token, String newPassword) {
        PasswordResetToken prToken = passwordResetTokenRepository.findByToken(token)
                .orElseThrow(() -> new BadRequestException("Invalid or expired reset token"));

        if (prToken.isUsed()) {
            throw new BadRequestException("Reset token has already been used");
        }
        if (prToken.isExpired()) {
            throw new BadRequestException("Reset token has expired. Please request a new one.");
        }

        User user = prToken.getUser();
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        prToken.setUsedAt(LocalDateTime.now());
        passwordResetTokenRepository.save(prToken);

        log.info("Password reset completed for user: email={}", user.getEmail());
    }

    private AuthResponse buildAuthResponse(String accessToken, String refreshToken, User user) {
        UserProfileResponse userProfile = userMapper.toProfileResponse(user);
        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(accessTokenExpiration / 1000) // Convert to seconds
                .user(userProfile)
                .build();
    }
}
