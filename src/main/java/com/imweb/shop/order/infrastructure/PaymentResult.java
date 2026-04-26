package com.imweb.shop.order.infrastructure;

import lombok.Getter;

@Getter
public class PaymentResult {
    private final boolean success;
    private final String message;

    private PaymentResult(boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    public static PaymentResult success() {
        return new PaymentResult(true, "결제 성공");
    }

    public static PaymentResult failure(String message) {
        return new PaymentResult(false, message);
    }
}
