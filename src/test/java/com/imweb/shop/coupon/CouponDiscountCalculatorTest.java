package com.imweb.shop.coupon;

import com.imweb.shop.coupon.application.AppliedCouponResult;
import com.imweb.shop.coupon.application.CouponDiscountCalculator;
import com.imweb.shop.coupon.domain.Coupon;
import com.imweb.shop.coupon.domain.CouponDiscountType;
import com.imweb.shop.coupon.domain.UserCoupon;
import com.imweb.shop.coupon.domain.UserCouponStatus;
import com.imweb.shop.coupon.drools.DroolsConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.kie.api.runtime.KieContainer;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class CouponDiscountCalculatorTest {

    private CouponDiscountCalculator calculator;

    @BeforeEach
    void setUp() {
        KieContainer kieContainer = new DroolsConfig().kieContainer();
        calculator = new CouponDiscountCalculator(kieContainer);
    }

    @Test
    @DisplayName("FIXED_AMOUNT 쿠폰: 정액 할인이 적용된다")
    void fixedAmount_discount() throws Exception {
        UserCoupon uc = createUserCoupon(1L, CouponDiscountType.FIXED_AMOUNT, 5000L, null);

        List<AppliedCouponResult> results = calculator.calculateWithCategorySubtotals(
                List.of(uc), 50000L, Map.of());

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getDiscountAmount()).isEqualTo(5000L);
    }

    @Test
    @DisplayName("FIXED_AMOUNT 쿠폰: 할인액이 남은 금액을 초과하지 않는다")
    void fixedAmount_cappedByRemainingAmount() throws Exception {
        UserCoupon uc = createUserCoupon(1L, CouponDiscountType.FIXED_AMOUNT, 10000L, null);

        List<AppliedCouponResult> results = calculator.calculateWithCategorySubtotals(
                List.of(uc), 3000L, Map.of());

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getDiscountAmount()).isEqualTo(3000L);
    }

    @Test
    @DisplayName("FIXED_RATE 쿠폰: 정율 할인이 적용된다")
    void fixedRate_discount() throws Exception {
        UserCoupon uc = createUserCoupon(2L, CouponDiscountType.FIXED_RATE, 10L, null);

        List<AppliedCouponResult> results = calculator.calculateWithCategorySubtotals(
                List.of(uc), 100000L, Map.of());

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getDiscountAmount()).isEqualTo(10000L);
    }

    @Test
    @DisplayName("FIXED_RATE 쿠폰: maxDiscountAmount로 할인 상한이 적용된다")
    void fixedRate_cappedByMaxDiscountAmount() throws Exception {
        UserCoupon uc = createUserCoupon(2L, CouponDiscountType.FIXED_RATE, 20L, 5000L);

        List<AppliedCouponResult> results = calculator.calculateWithCategorySubtotals(
                List.of(uc), 100000L, Map.of());

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getDiscountAmount()).isEqualTo(5000L);
    }

    @Test
    @DisplayName("복수 쿠폰: FIXED_RATE가 먼저 적용된다")
    void multipleCoupons_fixedRateFirst() throws Exception {
        UserCoupon fixedAmount = createUserCoupon(1L, CouponDiscountType.FIXED_AMOUNT, 5000L, null);
        UserCoupon fixedRate = createUserCoupon(2L, CouponDiscountType.FIXED_RATE, 10L, null);

        List<AppliedCouponResult> results = calculator.calculateWithCategorySubtotals(
                List.of(fixedAmount, fixedRate), 100000L, Map.of());

        assertThat(results).hasSize(2);
        AppliedCouponResult first = results.stream()
                .filter(r -> r.getAppliedOrder() == 1).findFirst().orElseThrow();
        assertThat(first.getUserCouponId()).isEqualTo(2L); // FIXED_RATE coupon applied first
        assertThat(first.getDiscountAmount()).isEqualTo(10000L);
    }

    @Test
    @DisplayName("쿠폰 목록이 빈 경우 빈 결과를 반환한다")
    void emptyCoupons_returnsEmpty() {
        List<AppliedCouponResult> results = calculator.calculateWithCategorySubtotals(
                List.of(), 50000L, Map.of());

        assertThat(results).isEmpty();
    }

    private UserCoupon createUserCoupon(Long id, CouponDiscountType type, long value, Long maxDiscount)
            throws Exception {
        LocalDateTime now = LocalDateTime.now();
        Coupon coupon = new Coupon("테스트 쿠폰", type, value,
                0L, maxDiscount, null,
                now.minusDays(1), now.plusDays(30));
        setId(coupon, id * 10);

        UserCoupon uc = new UserCoupon(1L, coupon, UserCouponStatus.AVAILABLE);
        setId(uc, id);
        return uc;
    }

    private void setId(Object entity, Long id) throws Exception {
        Field field = findField(entity.getClass(), "id");
        if (field == null) return;
        field.setAccessible(true);
        field.set(entity, id);
    }

    private Field findField(Class<?> clazz, String name) {
        if (clazz == null) return null;
        try {
            return clazz.getDeclaredField(name);
        } catch (NoSuchFieldException e) {
            return findField(clazz.getSuperclass(), name);
        }
    }
}
