package com.imweb.shop.order;

import com.imweb.shop.auth.domain.User;
import com.imweb.shop.auth.infrastructure.UserRepository;
import com.imweb.shop.coupon.application.AppliedCouponResult;
import com.imweb.shop.coupon.application.CouponDiscountCalculator;
import com.imweb.shop.coupon.domain.Coupon;
import com.imweb.shop.coupon.domain.CouponDiscountType;
import com.imweb.shop.coupon.domain.UserCoupon;
import com.imweb.shop.coupon.domain.UserCouponStatus;
import com.imweb.shop.coupon.infrastructure.UserCouponRepository;
import com.imweb.shop.global.exception.ErrorCode;
import com.imweb.shop.global.exception.PaymentDomainException;
import com.imweb.shop.order.application.OrderCommand;
import com.imweb.shop.order.application.PaymentService;
import com.imweb.shop.order.domain.Order;
import com.imweb.shop.order.dto.PaymentRequest;
import com.imweb.shop.order.dto.PaymentResponse;
import com.imweb.shop.product.domain.Product;
import com.imweb.shop.product.domain.ProductCategory;
import com.imweb.shop.product.domain.ProductSaleStatus;
import com.imweb.shop.product.infrastructure.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.lang.reflect.Field;
import static org.mockito.Mockito.mock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private ProductRepository productRepository;
    @Mock
    private UserCouponRepository userCouponRepository;
    @Mock
    private OrderCommand orderCommand;
    @Mock
    private UserRepository userRepository;
    @Mock
    private CouponDiscountCalculator discountCalculator;
    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    @InjectMocks
    private PaymentService paymentService;

    private User testUser;
    private Product testProduct;
    private Coupon testCoupon;
    private UserCoupon testUserCoupon;

    @BeforeEach
    void setUp() throws Exception {
        testUser = User.builder()
                .username("testuser")
                .password("encoded")
                .email("test@example.com")
                .enabled(true)
                .build();
        setId(testUser, 1L);

        testProduct = new Product("청바지", 59000L, 100, ProductCategory.FASHION, ProductSaleStatus.ON_SALE);
        setId(testProduct, 1L);

        LocalDateTime now = LocalDateTime.now();
        testCoupon = new Coupon("5000원 할인", CouponDiscountType.FIXED_AMOUNT, 5000L,
                10000L, null, null, now.minusDays(1), now.plusDays(30));
        setId(testCoupon, 1L);

        testUserCoupon = new UserCoupon(1L, testCoupon, UserCouponStatus.AVAILABLE);
        setId(testUserCoupon, 1L);
    }

    @Test
    @DisplayName("쿠폰 없이 정상 결제가 완료된다")
    void pay_success_withoutCoupon() throws Exception {
        // given
        PaymentRequest request = createPaymentRequest(1L, List.of(
                new PaymentRequest.PaymentItemRequest(1L, 2)
        ), null);

        Order savedOrder = Order.create(1L, 118000L, 0L);
        setId(savedOrder, 100L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(productRepository.findAllByIdInForUpdate(List.of(1L))).thenReturn(List.of(testProduct));
        when(discountCalculator.calculateWithCategorySubtotals(any(), anyLong(), any()))
                .thenReturn(List.of());
        when(orderCommand.create(any(PaymentRequest.class), anyLong(), anyLong(), anyMap(), anyList())).thenReturn(savedOrder);

        // when
        PaymentResponse response = paymentService.pay(request);

        // then
        assertThat(response).isNotNull();
        assertThat(response.originalAmount()).isEqualTo(118000L);
        assertThat(response.discountAmount()).isEqualTo(0L);
    }

    @Test
    @DisplayName("쿠폰 적용 후 정상 결제가 완료된다")
    void pay_success_withCoupon() throws Exception {
        // given
        PaymentRequest request = createPaymentRequest(1L, List.of(
                new PaymentRequest.PaymentItemRequest(1L, 1)
        ), List.of(1L));

        Order savedOrder = Order.create(1L, 59000L, 5000L);
        setId(savedOrder, 100L);

        AppliedCouponResult appliedResult = AppliedCouponResult.builder()
                .userCouponId(1L)
                .couponId(1L)
                .couponName("5000원 할인")
                .discountAmount(5000L)
                .appliedOrder(1)
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(productRepository.findAllByIdInForUpdate(any())).thenReturn(List.of(testProduct));
        when(userCouponRepository.findAllByIdInForUpdate(any())).thenReturn(List.of(testUserCoupon));
        when(discountCalculator.calculateWithCategorySubtotals(any(), anyLong(), any()))
                .thenReturn(List.of(appliedResult));
        when(orderCommand.create(any(PaymentRequest.class), anyLong(), anyLong(), anyMap(), anyList())).thenReturn(savedOrder);

        // when
        PaymentResponse response = paymentService.pay(request);

        // then
        assertThat(response).isNotNull();
        assertThat(response.appliedCoupons()).hasSize(1);
    }

    @Test
    @DisplayName("존재하지 않는 사용자로 결제 시 예외가 발생한다")
    void pay_userNotFound() {
        // given
        PaymentRequest request = createPaymentRequest(999L, List.of(
                new PaymentRequest.PaymentItemRequest(1L, 1)
        ), null);

        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> paymentService.pay(request))
                .isInstanceOf(PaymentDomainException.class)
                .satisfies(ex -> {
                    PaymentDomainException pde = (PaymentDomainException) ex;
                    assertThat(pde.getErrorCode()).isEqualTo(ErrorCode.USER_NOT_FOUND);
                });
    }

    @Test
    @DisplayName("재고 부족 시 예외가 발생한다")
    void pay_insufficientStock() throws Exception {
        // given
        Product lowStockProduct = new Product("한정판", 100000L, 1, ProductCategory.FASHION, ProductSaleStatus.ON_SALE);
        setId(lowStockProduct, 2L);

        PaymentRequest request = createPaymentRequest(1L, List.of(
                new PaymentRequest.PaymentItemRequest(2L, 5)
        ), null);

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(productRepository.findAllByIdInForUpdate(any())).thenReturn(List.of(lowStockProduct));

        // when & then
        assertThatThrownBy(() -> paymentService.pay(request))
                .isInstanceOf(PaymentDomainException.class)
                .satisfies(ex -> {
                    PaymentDomainException pde = (PaymentDomainException) ex;
                    assertThat(pde.getErrorCode()).isEqualTo(ErrorCode.INSUFFICIENT_STOCK);
                });
    }

    @Test
    @DisplayName("판매 중이 아닌 상품 구매 시 예외가 발생한다")
    void pay_productNotOnSale() throws Exception {
        // given
        Product stoppedProduct = new Product("단종상품", 50000L, 100, ProductCategory.FOOD, ProductSaleStatus.STOPPED);
        setId(stoppedProduct, 3L);

        PaymentRequest request = createPaymentRequest(1L, List.of(
                new PaymentRequest.PaymentItemRequest(3L, 1)
        ), null);

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(productRepository.findAllByIdInForUpdate(any())).thenReturn(List.of(stoppedProduct));

        // when & then
        assertThatThrownBy(() -> paymentService.pay(request))
                .isInstanceOf(PaymentDomainException.class)
                .satisfies(ex -> {
                    PaymentDomainException pde = (PaymentDomainException) ex;
                    assertThat(pde.getErrorCode()).isEqualTo(ErrorCode.PRODUCT_NOT_ON_SALE);
                });
    }

    @Test
    @DisplayName("중복 쿠폰 ID가 포함된 경우 예외가 발생한다")
    void pay_duplicateCouponIds() {
        // given
        PaymentRequest request = createPaymentRequest(1L, List.of(
                new PaymentRequest.PaymentItemRequest(1L, 1)
        ), List.of(1L, 1L));

        // when & then
        assertThatThrownBy(() -> paymentService.pay(request))
                .isInstanceOf(PaymentDomainException.class)
                .satisfies(ex -> {
                    PaymentDomainException pde = (PaymentDomainException) ex;
                    assertThat(pde.getErrorCode()).isEqualTo(ErrorCode.DUPLICATE_COUPON_IDS);
                });
    }

    @Test
    @DisplayName("타인의 쿠폰 사용 시 예외가 발생한다")
    void pay_couponNotOwned() throws Exception {
        // given - userCoupon belongs to userId=2, but request userId=1
        UserCoupon anotherUserCoupon = new UserCoupon(2L, testCoupon, UserCouponStatus.AVAILABLE);
        setId(anotherUserCoupon, 5L);

        PaymentRequest request = createPaymentRequest(1L, List.of(
                new PaymentRequest.PaymentItemRequest(1L, 1)
        ), List.of(5L));

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(productRepository.findAllByIdInForUpdate(any())).thenReturn(List.of(testProduct));
        when(userCouponRepository.findAllByIdInForUpdate(any())).thenReturn(List.of(anotherUserCoupon));

        // when & then
        assertThatThrownBy(() -> paymentService.pay(request))
                .isInstanceOf(PaymentDomainException.class)
                .satisfies(ex -> {
                    PaymentDomainException pde = (PaymentDomainException) ex;
                    assertThat(pde.getErrorCode()).isEqualTo(ErrorCode.COUPON_NOT_OWNED);
                });
    }

    // Helper methods
    private PaymentRequest createPaymentRequest(Long userId,
                                                List<PaymentRequest.PaymentItemRequest> items,
                                                List<Long> userCouponIds) {
        try {
            PaymentRequest request = new PaymentRequest();
            setField(request, "userId", userId);
            setField(request, "items", items);
            setField(request, "userCouponIds", userCouponIds);
            return request;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void setField(Object obj, String fieldName, Object value) throws Exception {
        Field field = obj.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(obj, value);
    }

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
