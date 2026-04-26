package com.imweb.shop.order.application;

import com.imweb.shop.coupon.infrastructure.UserCouponRepository;
import com.imweb.shop.order.dto.OrderPaymentRequestedEvent;
import com.imweb.shop.order.infrastructure.OrderItemRepository;
import com.imweb.shop.product.infrastructure.ProductRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PaymentFailureHandler {

    private final OrderItemRepository orderItemRepository;
    private final ProductRepository productRepository;
    private final UserCouponRepository userCouponRepository;

    @Transactional
    public void run(OrderPaymentRequestedEvent event) {
        // rollback product stock, coupon usage, etc. if payment failed
        orderItemRepository.findByOrderId(event.getOrderId()).forEach(orderItem -> {
            Long productId = orderItem.getProductId();
            productRepository.findById(productId).ifPresent(product -> {
                product.increaseStock(orderItem.getQuantity());
                productRepository.save(product);
            });
        });
        userCouponRepository.findAllById(event.getUserCouponIds()).forEach(userCoupon -> {
            userCoupon.rollbackUsage();
            userCouponRepository.save(userCoupon);
        });
    }
}
