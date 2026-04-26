package com.imweb.shop.order.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "order_coupons")
@Getter
@Setter
@NoArgsConstructor
public class OrderCoupon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private Order order;

    @Column(nullable = false)
    private Long userCouponId;

    @Column(nullable = false)
    private Long couponId;

    @Column(nullable = false)
    private String couponName;

    @Column(nullable = false)
    private Long discountAmount;

    @Column(nullable = false)
    private Integer appliedOrder;

    public OrderCoupon(Long userCouponId, Long couponId, String couponName,
                       Long discountAmount, Integer appliedOrder) {
        this.userCouponId = userCouponId;
        this.couponId = couponId;
        this.couponName = couponName;
        this.discountAmount = discountAmount;
        this.appliedOrder = appliedOrder;
    }
}
