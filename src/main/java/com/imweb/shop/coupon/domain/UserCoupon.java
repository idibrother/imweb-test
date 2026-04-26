package com.imweb.shop.coupon.domain;

import com.imweb.shop.global.exception.ErrorCode;
import com.imweb.shop.global.exception.PaymentDomainException;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_coupons")
@Getter
@NoArgsConstructor
public class UserCoupon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "coupon_id", nullable = false)
    private Coupon coupon;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserCouponStatus status;

    private LocalDateTime usedAt;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public UserCoupon(Long userId, Coupon coupon, UserCouponStatus status) {
        this.userId = userId;
        this.coupon = coupon;
        this.status = status;
    }

    public void validateUsable(LocalDateTime now) {
        if (this.status == UserCouponStatus.USED) {
            throw new PaymentDomainException(ErrorCode.COUPON_ALREADY_USED);
        }
        if (this.status != UserCouponStatus.AVAILABLE) {
            throw new PaymentDomainException(ErrorCode.COUPON_ALREADY_USED);
        }
        coupon.validateUsable(now);
    }

    public void use(LocalDateTime now) {
        validateUsable(now);
        this.status = UserCouponStatus.USED;
        this.usedAt = now;
    }

    public void rollbackUsage() {
        this.status = UserCouponStatus.AVAILABLE;
        this.usedAt = null;
    }
}
