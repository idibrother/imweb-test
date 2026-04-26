package com.imweb.shop.order.dto;

import com.imweb.shop.order.domain.OrderStatus;

import java.util.List;

public record PaymentResponse(
        Long orderId,
        OrderStatus orderStatus,
        Long originalAmount,
        Long discountAmount,
        Long paymentAmount,
        List<PaymentItemResult> items,
        List<AppliedCouponResponse> appliedCoupons
) {
    public record PaymentItemResult(
            Long productId,
            String productName,
            Long unitPrice,
            Integer quantity,
            Long totalPrice,
            Integer remainingStockQuantity
    ) {}

    public record AppliedCouponResponse(
            Long userCouponId,
            Long couponId,
            String couponName,
            Long discountAmount,
            int appliedOrder
    ) {}
}
