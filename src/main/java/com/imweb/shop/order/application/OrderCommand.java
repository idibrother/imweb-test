package com.imweb.shop.order.application;

import com.imweb.shop.coupon.application.AppliedCouponResult;
import com.imweb.shop.order.domain.Order;
import com.imweb.shop.order.domain.OrderCoupon;
import com.imweb.shop.order.domain.OrderItem;
import com.imweb.shop.order.dto.PaymentRequest;
import com.imweb.shop.order.infrastructure.OrderRepository;
import com.imweb.shop.product.domain.Product;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class OrderCommand {

    private final OrderRepository orderRepository;

    public Order create(PaymentRequest request, long originalAmount, long totalDiscount, Map<Long, Product> productMap, List<AppliedCouponResult> appliedCoupons) {
        Order order = Order.create(request.getUserId(), originalAmount, totalDiscount);

        // 13. Create OrderItems (snapshot)
        for (PaymentRequest.PaymentItemRequest itemReq : request.getItems()) {
            Product product = productMap.get(itemReq.productId());
            OrderItem orderItem = OrderItem.of(product, itemReq.quantity());
            order.addItem(orderItem);
        }

        // 14. Create OrderCoupons
        for (AppliedCouponResult result : appliedCoupons) {
            OrderCoupon orderCoupon = new OrderCoupon(
                    result.getUserCouponId(),
                    result.getCouponId(),
                    result.getCouponName(),
                    result.getDiscountAmount(),
                    result.getAppliedOrder()
            );
            order.addCoupon(orderCoupon);
        }

        // 15. Save order
        order = orderRepository.save(order);
        return order;
    }

    @Transactional
    public void update(Long orderId, boolean isPaid) {
        orderRepository.findById(orderId).ifPresent(order -> {
            if (isPaid) {
                order.markAsPaid();
            } else {
                order.markAsFailed();
            }
            orderRepository.save(order);
        });
    }
}
