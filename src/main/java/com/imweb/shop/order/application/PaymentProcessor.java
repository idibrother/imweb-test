package com.imweb.shop.order.application;

import com.imweb.shop.order.domain.Order;
import com.imweb.shop.order.infrastructure.OrderRepository;
import com.imweb.shop.order.infrastructure.PaymentCommand;
import com.imweb.shop.order.infrastructure.PaymentGateway;
import com.imweb.shop.order.infrastructure.PaymentResult;
import lombok.RequiredArgsConstructor;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PaymentProcessor {

    private final OrderRepository orderRepository;
    private final PaymentGateway paymentGateway;

    @Retryable(
            maxAttempts = 3,
            backoff = @Backoff(delay = 200, multiplier = 2)
    )
    @Transactional
    public boolean process(Long orderId) {
        Order order = orderRepository.findById(orderId).orElseThrow();
        PaymentResult result = paymentGateway.pay(
                new PaymentCommand(
                        order.getId(),
                        order.getUserId(),
                        order.getPaymentAmount()
                )
        );
        return result.isSuccess();
    }
}