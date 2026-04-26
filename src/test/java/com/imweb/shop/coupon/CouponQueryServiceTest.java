package com.imweb.shop.coupon;

import com.imweb.shop.auth.domain.User;
import com.imweb.shop.auth.infrastructure.UserRepository;
import com.imweb.shop.coupon.application.CouponQueryService;
import com.imweb.shop.coupon.domain.Coupon;
import com.imweb.shop.coupon.domain.CouponDiscountType;
import com.imweb.shop.coupon.domain.UserCoupon;
import com.imweb.shop.coupon.domain.UserCouponStatus;
import com.imweb.shop.coupon.infrastructure.UserCouponRepository;
import com.imweb.shop.coupon.dto.CouponListResponse;
import com.imweb.shop.global.exception.PaymentDomainException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CouponQueryServiceTest {

    @Mock
    private UserCouponRepository userCouponRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CouponQueryService couponQueryService;

    private User mockUser;
    private Coupon activeCoupon;
    private Coupon expiredCoupon;

    @BeforeEach
    void setUp() throws Exception {
        mockUser = User.builder()
                .username("testuser")
                .password("encoded")
                .email("test@example.com")
                .enabled(true)
                .build();
        setId(mockUser, 1L);

        LocalDateTime now = LocalDateTime.now();
        activeCoupon = new Coupon("Active Coupon", CouponDiscountType.FIXED_AMOUNT, 5000L,
                10000L, null, null,
                now.minusDays(1), now.plusDays(30));
        setId(activeCoupon, 1L);

        expiredCoupon = new Coupon("Expired Coupon", CouponDiscountType.FIXED_RATE, 10L,
                5000L, null, null,
                now.minusDays(60), now.minusDays(1));
        setId(expiredCoupon, 2L);
    }

    @Test
    @DisplayName("사용자의 쿠폰 목록을 정상적으로 조회한다")
    void getUserCoupons_success() throws Exception {
        // given
        UserCoupon availableUC = new UserCoupon(1L, activeCoupon, UserCouponStatus.AVAILABLE);
        setId(availableUC, 1L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));
        when(userCouponRepository.findByUserId(1L)).thenReturn(List.of(availableUC));

        // when
        CouponListResponse response = couponQueryService.getUserCoupons(1L);

        // then
        assertThat(response.coupons()).hasSize(1);
        CouponListResponse.CouponItem item = response.coupons().get(0);
        assertThat(item.available()).isTrue();
        assertThat(item.unavailableReason()).isNull();
        assertThat(item.couponName()).isEqualTo("Active Coupon");
    }

    @Test
    @DisplayName("존재하지 않는 사용자 조회 시 예외가 발생한다")
    void getUserCoupons_userNotFound() {
        // given
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> couponQueryService.getUserCoupons(999L))
                .isInstanceOf(PaymentDomainException.class)
                .satisfies(ex -> {
                    PaymentDomainException pde = (PaymentDomainException) ex;
                    assertThat(pde.getErrorCode().name()).isEqualTo("USER_NOT_FOUND");
                });
    }

    @Test
    @DisplayName("만료된 쿠폰은 available=false로 반환된다")
    void getUserCoupons_expiredCoupon() throws Exception {
        // given
        UserCoupon expiredUC = new UserCoupon(1L, expiredCoupon, UserCouponStatus.AVAILABLE);
        setId(expiredUC, 2L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));
        when(userCouponRepository.findByUserId(1L)).thenReturn(List.of(expiredUC));

        // when
        CouponListResponse response = couponQueryService.getUserCoupons(1L);

        // then
        assertThat(response.coupons()).hasSize(1);
        CouponListResponse.CouponItem item = response.coupons().get(0);
        assertThat(item.available()).isFalse();
        assertThat(item.unavailableReason()).isNotNull();
    }

    @Test
    @DisplayName("이미 사용된 쿠폰은 available=false로 반환된다")
    void getUserCoupons_usedCoupon() throws Exception {
        // given
        UserCoupon usedUC = new UserCoupon(1L, activeCoupon, UserCouponStatus.USED);
        setId(usedUC, 3L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));
        when(userCouponRepository.findByUserId(1L)).thenReturn(List.of(usedUC));

        // when
        CouponListResponse response = couponQueryService.getUserCoupons(1L);

        // then
        assertThat(response.coupons()).hasSize(1);
        CouponListResponse.CouponItem item = response.coupons().get(0);
        assertThat(item.available()).isFalse();
        assertThat(item.unavailableReason()).contains("사용");
    }

    @Test
    @DisplayName("쿠폰이 없는 사용자는 빈 목록을 반환한다")
    void getUserCoupons_noCoupons() {
        // given
        when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));
        when(userCouponRepository.findByUserId(1L)).thenReturn(List.of());

        // when
        CouponListResponse response = couponQueryService.getUserCoupons(1L);

        // then
        assertThat(response.coupons()).isEmpty();
    }

    // Helper to set private id field via reflection
    private void setId(Object entity, Long id) throws Exception {
        Field field = findIdField(entity.getClass());
        if (field == null) return;
        field.setAccessible(true);
        field.set(entity, id);
    }

    private Field findIdField(Class<?> clazz) {
        if (clazz == null) return null;
        try {
            return clazz.getDeclaredField("id");
        } catch (NoSuchFieldException e) {
            return findIdField(clazz.getSuperclass());
        }
    }
}
