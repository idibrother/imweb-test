package com.imweb.shop.global.exception;

import lombok.Getter;

import java.util.Map;

@Getter
public class PaymentDomainException extends RuntimeException {

    private final ErrorCode errorCode;
    private final Map<String, Object> details;

    public PaymentDomainException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
        this.details = Map.of();
    }

    public PaymentDomainException(ErrorCode errorCode, Map<String, Object> details) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
        this.details = details;
    }

    public PaymentDomainException(ErrorCode errorCode, String detailKey, Object detailValue) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
        this.details = Map.of(detailKey, detailValue);
    }
}
