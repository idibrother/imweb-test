package com.imweb.shop.coupon.domain;

import com.imweb.shop.product.domain.ProductCategory;
import com.imweb.shop.global.exception.ErrorCode;
import com.imweb.shop.global.exception.PaymentDomainException;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "coupons")
@Getter
@NoArgsConstructor
public class Coupon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CouponDiscountType discountType;

    @Column(nullable = false)
    private Long discountValue;

    @Column(nullable = false)
    private Long minPurchaseAmount;

    private Long maxDiscountAmount;

    @Enumerated(EnumType.STRING)
    private ProductCategory applicableCategory;

    @Column(nullable = false)
    private LocalDateTime startedAt;

    @Column(nullable = false)
    private LocalDateTime endedAt;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public Coupon(String name, CouponDiscountType discountType, Long discountValue,
                  Long minPurchaseAmount, Long maxDiscountAmount,
                  ProductCategory applicableCategory,
                  LocalDateTime startedAt, LocalDateTime endedAt) {
        this.name = name;
        this.discountType = discountType;
        this.discountValue = discountValue;
        this.minPurchaseAmount = minPurchaseAmount;
        this.maxDiscountAmount = maxDiscountAmount;
        this.applicableCategory = applicableCategory;
        this.startedAt = startedAt;
        this.endedAt = endedAt;
    }

    public void validateUsable(LocalDateTime now) {
        if (now.isBefore(startedAt)) {
            throw new PaymentDomainException(ErrorCode.COUPON_NOT_STARTED);
        }
        if (now.isAfter(endedAt)) {
            throw new PaymentDomainException(ErrorCode.COUPON_EXPIRED);
        }
    }
}
