package com.imweb.shop.order.infrastructure;

import com.imweb.shop.order.domain.OrderCoupon;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderCouponRepository extends JpaRepository<OrderCoupon, Long> {
}
