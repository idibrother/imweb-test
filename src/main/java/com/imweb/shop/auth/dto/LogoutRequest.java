package com.imweb.shop.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

public record LogoutRequest(
        @JsonProperty("refresh_token") @NotBlank(message = "Refresh token is required") String refreshToken
) {}
