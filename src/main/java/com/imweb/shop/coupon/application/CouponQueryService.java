package com.imweb.shop.coupon.application;

import com.imweb.shop.coupon.domain.UserCoupon;
import com.imweb.shop.coupon.domain.UserCouponStatus;
import com.imweb.shop.coupon.infrastructure.UserCouponRepository;
import com.imweb.shop.coupon.dto.CouponListResponse;
import com.imweb.shop.global.exception.ErrorCode;
import com.imweb.shop.global.exception.PaymentDomainException;
import com.imweb.shop.auth.infrastructure.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CouponQueryService {

    private final UserCouponRepository userCouponRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public CouponListResponse getUserCoupons(Long userId) {
        userRepository.findById(userId)
                .orElseThrow(() -> new PaymentDomainException(ErrorCode.USER_NOT_FOUND));

        List<UserCoupon> userCoupons = userCouponRepository.findByUserId(userId);
        LocalDateTime now = LocalDateTime.now();

        List<CouponListResponse.CouponItem> items = userCoupons.stream()
                .map(uc -> buildCouponItem(uc, now))
                .collect(Collectors.toList());

        return new CouponListResponse(items);
    }

    private CouponListResponse.CouponItem buildCouponItem(UserCoupon uc, LocalDateTime now) {
        boolean available = false;
        String unavailableReason = null;

        if (uc.getStatus() == UserCouponStatus.USED) {
            unavailableReason = "이미 사용된 쿠폰입니다.";
        } else if (uc.getStatus() == UserCouponStatus.DISABLED) {
            unavailableReason = "비활성화된 쿠폰입니다.";
        } else if (uc.getStatus() == UserCouponStatus.EXPIRED) {
            unavailableReason = "만료된 쿠폰입니다.";
        } else if (now.isBefore(uc.getCoupon().getStartedAt())) {
            unavailableReason = "아직 사용 시작 전인 쿠폰입니다.";
        } else if (now.isAfter(uc.getCoupon().getEndedAt())) {
            unavailableReason = "만료된 쿠폰입니다.";
        } else {
            available = true;
        }

        return new CouponListResponse.CouponItem(
                uc.getId(),
                uc.getCoupon().getId(),
                uc.getCoupon().getName(),
                uc.getCoupon().getDiscountType(),
                uc.getCoupon().getDiscountValue(),
                available,
                unavailableReason,
                uc.getCoupon().getMinPurchaseAmount(),
                uc.getCoupon().getMaxDiscountAmount(),
                uc.getCoupon().getApplicableCategory(),
                uc.getCoupon().getStartedAt(),
                uc.getCoupon().getEndedAt()
        );
    }
}
