package com.imweb.shop.order.infrastructure;

import org.springframework.stereotype.Component;

@Component
public class FakePaymentGateway implements PaymentGateway {

    @Override
    public PaymentResult pay(PaymentCommand command) {
        return PaymentResult.success();
    }
}
