package com.imweb.shop.order.infrastructure;

public interface PaymentGateway {
    PaymentResult pay(PaymentCommand command);
}
