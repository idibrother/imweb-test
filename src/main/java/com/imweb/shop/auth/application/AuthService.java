package com.imweb.shop.auth.application;

import com.imweb.shop.global.config.AppProperties;
import com.imweb.shop.auth.domain.RefreshToken;
import com.imweb.shop.auth.domain.User;
import com.imweb.shop.auth.infrastructure.RefreshTokenRepository;
import com.imweb.shop.auth.infrastructure.UserRepository;
import com.imweb.shop.auth.dto.LoginRequest;
import com.imweb.shop.auth.dto.LoginResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final LoginAttemptService loginAttemptService;
    private final AuditService auditService;
    private final AppProperties appProperties;

    @Transactional
    public LoginResponse login(LoginRequest request, String ipAddress, String userAgent) {
        String username = request.username();

        if (loginAttemptService.isBlocked(username)) {
            auditService.log("LOGIN_BLOCKED", null, username, ipAddress, userAgent, "Account locked");
            throw new LockedException("Account is temporarily locked due to too many failed attempts");
        }

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    loginAttemptService.recordFailure(username);
                    auditService.log("LOGIN_FAILURE", null, username, ipAddress, userAgent, "User not found");
                    return new UsernameNotFoundException("Invalid credentials");
                });

        if (!user.isEnabled()) {
            auditService.log("LOGIN_FAILURE", user.getId(), username, ipAddress, userAgent, "Account disabled");
            throw new LockedException("Account is disabled");
        }

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            loginAttemptService.recordFailure(username);
            auditService.log("LOGIN_FAILURE", user.getId(), username, ipAddress, userAgent, "Invalid password");
            throw new BadCredentialsException("Invalid credentials");
        }

        loginAttemptService.recordSuccess(username);

        try {
            String accessToken = jwtService.generateAccessToken(username, user.getRoles());
            String rawRefreshToken = UUID.randomUUID().toString();

            refreshTokenRepository.save(RefreshToken.builder()
                    .token(rawRefreshToken)
                    .user(user)
                    .expiresAt(LocalDateTime.now().plusDays(appProperties.getJwt().getRefreshTokenTtlDays()))
                    .deviceInfo(userAgent != null && userAgent.length() > 255 ? userAgent.substring(0, 255) : userAgent)
                    .build());

            auditService.log("LOGIN_SUCCESS", user.getId(), username, ipAddress, userAgent, null);

            long expiresIn = appProperties.getJwt().getAccessTokenTtlMinutes() * 60L;
            return LoginResponse.of(accessToken, rawRefreshToken, expiresIn);
        } catch (Exception e) {
            log.error("Token generation failed for user: {}", username, e);
            throw new RuntimeException("Token generation failed", e);
        }
    }

    @Transactional
    public LoginResponse refresh(String rawRefreshToken, String ipAddress, String userAgent) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(rawRefreshToken)
                .orElseThrow(() -> new BadCredentialsException("Invalid refresh token"));

        if (refreshToken.isRevoked()) {
            // Reuse detection: revoke all tokens for this user
            refreshTokenRepository.revokeAllByUser(refreshToken.getUser());
            auditService.log("TOKEN_REUSE_DETECTED", refreshToken.getUser().getId(),
                    refreshToken.getUser().getUsername(), ipAddress, userAgent, "All tokens revoked");
            throw new BadCredentialsException("Refresh token reuse detected - all sessions revoked");
        }

        if (refreshToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BadCredentialsException("Refresh token has expired");
        }

        User user = refreshToken.getUser();
        if (!user.isEnabled()) {
            throw new LockedException("Account is disabled");
        }

        // Rotate: revoke old token, issue new one
        refreshToken.setRevoked(true);
        refreshTokenRepository.save(refreshToken);

        try {
            String newAccessToken = jwtService.generateAccessToken(user.getUsername(), user.getRoles());
            String newRawRefreshToken = UUID.randomUUID().toString();

            refreshTokenRepository.save(RefreshToken.builder()
                    .token(newRawRefreshToken)
                    .user(user)
                    .expiresAt(LocalDateTime.now().plusDays(appProperties.getJwt().getRefreshTokenTtlDays()))
                    .deviceInfo(refreshToken.getDeviceInfo())
                    .build());

            auditService.log("TOKEN_REFRESH", user.getId(), user.getUsername(), ipAddress, userAgent, null);

            long expiresIn = appProperties.getJwt().getAccessTokenTtlMinutes() * 60L;
            return LoginResponse.of(newAccessToken, newRawRefreshToken, expiresIn);
        } catch (Exception e) {
            log.error("Token refresh failed for user: {}", user.getUsername(), e);
            throw new RuntimeException("Token generation failed", e);
        }
    }

    @Transactional
    public void logout(String rawRefreshToken, String ipAddress, String userAgent) {
        refreshTokenRepository.findByToken(rawRefreshToken).ifPresent(token -> {
            String username = token.getUser().getUsername();
            Long userId = token.getUser().getId();
            token.setRevoked(true);
            refreshTokenRepository.save(token);
            auditService.log("LOGOUT", userId, username, ipAddress, userAgent, null);
        });
    }
}
