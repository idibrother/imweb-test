package com.imweb.shop.coupon.api;

import com.imweb.shop.coupon.application.CouponQueryService;
import com.imweb.shop.coupon.dto.CouponListResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class CouponController {

    private final CouponQueryService couponQueryService;

    @GetMapping("/{userId}/coupons")
    public ResponseEntity<CouponListResponse> getUserCoupons(@PathVariable Long userId) {
        return ResponseEntity.ok(couponQueryService.getUserCoupons(userId));
    }
}
