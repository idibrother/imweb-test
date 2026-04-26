package com.imweb.shop.coupon.infrastructure;

import com.imweb.shop.coupon.domain.Coupon;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CouponRepository extends JpaRepository<Coupon, Long> {
}
