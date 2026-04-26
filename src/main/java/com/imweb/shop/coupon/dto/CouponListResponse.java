package com.imweb.shop.coupon.dto;

import com.imweb.shop.coupon.domain.CouponDiscountType;
import com.imweb.shop.product.domain.ProductCategory;

import java.time.LocalDateTime;
import java.util.List;

public record CouponListResponse(List<CouponItem> coupons) {

    public record CouponItem(
            Long userCouponId,
            Long couponId,
            String couponName,
            CouponDiscountType discountType,
            Long discountValue,
            boolean available,
            String unavailableReason,
            Long minPurchaseAmount,
            Long maxDiscountAmount,
            ProductCategory applicableCategory,
            LocalDateTime startedAt,
            LocalDateTime endedAt
    ) {}
}
