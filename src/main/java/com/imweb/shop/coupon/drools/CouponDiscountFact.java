package com.imweb.shop.coupon.drools;

import com.imweb.shop.coupon.domain.CouponDiscountType;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CouponDiscountFact {
    private Long userCouponId;
    private Long couponId;
    private String couponName;
    private CouponDiscountType discountType;
    private long discountValue;
    private Long maxDiscountAmount;
    private long baseAmount;
    private long remainingAmount;
    private long calculatedDiscount = 0L;
}
