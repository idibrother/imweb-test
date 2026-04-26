package com.imweb.shop.order.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
@Getter
@NoArgsConstructor
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus orderStatus;

    @Column(nullable = false)
    private Long originalAmount;

    @Column(nullable = false)
    private Long discountAmount;

    @Column(nullable = false)
    private Long paymentAmount;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL)
    private List<OrderItem> items = new ArrayList<>();

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL)
    private List<OrderCoupon> coupons = new ArrayList<>();

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public static Order create(Long userId, Long originalAmount, Long discountAmount) {
        Order order = new Order();
        order.userId = userId;
        order.orderStatus = OrderStatus.PAYMENT_REQUESTED;
        order.originalAmount = originalAmount;
        order.discountAmount = discountAmount;
        order.paymentAmount = Math.max(0L, originalAmount - discountAmount);
        return order;
    }

    public void markAsPaid() {
        this.orderStatus = OrderStatus.PAID;
    }

    public void markAsFailed() {
        this.orderStatus = OrderStatus.FAILED;
    }

    public void addItem(OrderItem item) {
        items.add(item);
        item.setOrder(this);
    }

    public void addCoupon(OrderCoupon coupon) {
        coupons.add(coupon);
        coupon.setOrder(this);
    }
}
