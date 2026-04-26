package com.imweb.shop.order.infrastructure;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PaymentCommand {
    private final Long orderId;
    private final Long userId;
    private final Long amount;
}
