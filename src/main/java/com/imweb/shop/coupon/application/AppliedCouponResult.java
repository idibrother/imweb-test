package com.imweb.shop.coupon.application;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AppliedCouponResult {
    private Long userCouponId;
    private Long couponId;
    private String couponName;
    private Long discountAmount;
    private int appliedOrder;
}
