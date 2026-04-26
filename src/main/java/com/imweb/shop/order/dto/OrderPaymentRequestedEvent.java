package com.imweb.shop.order.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class OrderPaymentRequestedEvent {
    private Long orderId;
    private List<Long> userCouponIds;

    public OrderPaymentRequestedEvent(Long orderId, List<Long> userCouponIds) {
        this.orderId = orderId;
        this.userCouponIds = userCouponIds;
    }
}
