package com.imweb.shop.global.dto;

import java.util.Map;

public record ApiErrorResponse(
        String code,
        String message,
        Map<String, Object> details
) {}
