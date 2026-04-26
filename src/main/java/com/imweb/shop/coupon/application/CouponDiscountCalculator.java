package com.imweb.shop.coupon.application;

import com.imweb.shop.coupon.domain.CouponDiscountType;
import com.imweb.shop.coupon.domain.UserCoupon;
import com.imweb.shop.coupon.drools.CouponDiscountFact;
import com.imweb.shop.product.domain.ProductCategory;
import lombok.RequiredArgsConstructor;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class CouponDiscountCalculator {

    private final KieContainer kieContainer;

    public List<AppliedCouponResult> calculateWithCategorySubtotals(
            List<UserCoupon> userCoupons,
            long originalAmount,
            Map<ProductCategory, Long> categorySubtotals
    ) {
        if (userCoupons == null || userCoupons.isEmpty()) {
            return List.of();
        }

        record CouponEntry(UserCoupon userCoupon, long baseAmount) {}

        List<CouponEntry> entries = userCoupons.stream().map(uc -> {
            long base = originalAmount;
            if (uc.getCoupon().getApplicableCategory() != null) {
                base = categorySubtotals.getOrDefault(uc.getCoupon().getApplicableCategory(), 0L);
            }
            return new CouponEntry(uc, base);
        }).collect(Collectors.toList());

        // Sort: FIXED_RATE first, then by larger estimated discount
        entries.sort(Comparator
                .<CouponEntry, Integer>comparing(e ->
                        e.userCoupon().getCoupon().getDiscountType() == CouponDiscountType.FIXED_RATE ? 0 : 1)
                .thenComparing(e -> -runDiscount(e.userCoupon(), e.baseAmount(), originalAmount))
        );

        List<AppliedCouponResult> results = new ArrayList<>();
        long remainingAmount = originalAmount;
        int order = 1;

        for (CouponEntry entry : entries) {
            if (remainingAmount <= 0) break;

            long actualDiscount = runDiscount(entry.userCoupon(), entry.baseAmount(), remainingAmount);

            if (actualDiscount > 0) {
                results.add(AppliedCouponResult.builder()
                        .userCouponId(entry.userCoupon().getId())
                        .couponId(entry.userCoupon().getCoupon().getId())
                        .couponName(entry.userCoupon().getCoupon().getName())
                        .discountAmount(actualDiscount)
                        .appliedOrder(order++)
                        .build());
                remainingAmount -= actualDiscount;
            }
        }

        return results;
    }

    private long runDiscount(UserCoupon uc, long baseAmount, long remainingAmount) {
        CouponDiscountFact fact = new CouponDiscountFact();
        fact.setUserCouponId(uc.getId());
        fact.setCouponId(uc.getCoupon().getId());
        fact.setCouponName(uc.getCoupon().getName());
        fact.setDiscountType(uc.getCoupon().getDiscountType());
        fact.setDiscountValue(uc.getCoupon().getDiscountValue());
        fact.setMaxDiscountAmount(uc.getCoupon().getMaxDiscountAmount());
        fact.setBaseAmount(baseAmount);
        fact.setRemainingAmount(remainingAmount);

        KieSession session = kieContainer.newKieSession();
        try {
            session.insert(fact);
            session.fireAllRules();
        } finally {
            session.dispose();
        }

        return fact.getCalculatedDiscount();
    }
}
