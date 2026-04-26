package com.imweb.shop.order.infrastructure;

import com.imweb.shop.order.domain.Order;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, Long> {
}
