package com.imweb.shop.order.application;

import com.imweb.shop.order.dto.OrderPaymentRequestedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class OrderPaymentEventHandler {

    private final PaymentProcessor paymentProcessor;
    private final OrderCommand orderCommand;
    private final PaymentFailureHandler paymentFailureHandler;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(OrderPaymentRequestedEvent event) {
        Long orderId = event.getOrderId();
        boolean isPaid = false;
        try {
            isPaid = paymentProcessor.process(orderId);
        } catch (Exception e) {
            orderCommand.update(orderId, isPaid);
            throw e;
        } finally {
            orderCommand.update(orderId, isPaid);
            if (!isPaid) {
                paymentFailureHandler.run(event);
            }
        }
    }
}
