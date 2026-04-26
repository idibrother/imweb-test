package com.imweb.shop.auth.api;

import com.imweb.shop.auth.dto.LoginRequest;
import com.imweb.shop.auth.dto.LoginResponse;
import com.imweb.shop.auth.dto.LogoutRequest;
import com.imweb.shop.auth.dto.RefreshRequest;
import com.imweb.shop.auth.application.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {
        LoginResponse response = authService.login(request, getClientIp(httpRequest), httpRequest.getHeader("User-Agent"));
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    public ResponseEntity<LoginResponse> refresh(
            @Valid @RequestBody RefreshRequest request,
            HttpServletRequest httpRequest) {
        LoginResponse response = authService.refresh(request.refreshToken(), getClientIp(httpRequest), httpRequest.getHeader("User-Agent"));
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @Valid @RequestBody LogoutRequest request,
            HttpServletRequest httpRequest) {
        authService.logout(request.refreshToken(), getClientIp(httpRequest), httpRequest.getHeader("User-Agent"));
        return ResponseEntity.noContent().build();
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
