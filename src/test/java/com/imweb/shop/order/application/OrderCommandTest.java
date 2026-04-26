package com.imweb.shop.order.application;

import com.imweb.shop.coupon.application.AppliedCouponResult;
import com.imweb.shop.order.domain.Order;
import com.imweb.shop.order.domain.OrderCoupon;
import com.imweb.shop.order.domain.OrderItem;
import com.imweb.shop.order.domain.OrderStatus;
import com.imweb.shop.order.dto.PaymentRequest;
import com.imweb.shop.order.infrastructure.OrderRepository;
import com.imweb.shop.product.domain.Product;
import com.imweb.shop.product.domain.ProductCategory;
import com.imweb.shop.product.domain.ProductSaleStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderCommandTest {

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private OrderCommand orderCommand;

    private Product testProduct;
    private Product anotherProduct;

    @BeforeEach
    void setUp() throws Exception {
        testProduct = new Product("청바지", 59000L, 100, ProductCategory.FASHION, ProductSaleStatus.ON_SALE);
        setId(testProduct, 1L);

        anotherProduct = new Product("니트", 39000L, 50, ProductCategory.FASHION, ProductSaleStatus.ON_SALE);
        setId(anotherProduct, 2L);
    }

    // ===== create() 테스트 =====

    @Test
    @DisplayName("단일 상품 주문 시 OrderItem이 상품 스냅샷으로 올바르게 생성된다")
    void create_singleItem_createsOrderItemCorrectly() throws Exception {
        // given
        PaymentRequest request = createPaymentRequest(1L, List.of(
                new PaymentRequest.PaymentItemRequest(1L, 2)
        ), null);
        Map<Long, Product> productMap = Map.of(1L, testProduct);

        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        // when
        Order result = orderCommand.create(request, 118000L, 0L, productMap, List.of());

        // then
        assertThat(result.getItems()).hasSize(1);
        OrderItem item = result.getItems().get(0);
        assertThat(item.getProductId()).isEqualTo(1L);
        assertThat(item.getProductName()).isEqualTo("청바지");
        assertThat(item.getUnitPrice()).isEqualTo(59000L);
        assertThat(item.getQuantity()).isEqualTo(2);
        assertThat(item.getTotalPrice()).isEqualTo(118000L);
    }

    @Test
    @DisplayName("여러 상품 주문 시 모든 OrderItem이 생성된다")
    void create_multipleItems_allItemsCreated() throws Exception {
        // given
        PaymentRequest request = createPaymentRequest(1L, List.of(
                new PaymentRequest.PaymentItemRequest(1L, 1),
                new PaymentRequest.PaymentItemRequest(2L, 3)
        ), null);
        Map<Long, Product> productMap = Map.of(1L, testProduct, 2L, anotherProduct);

        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        // when
        Order result = orderCommand.create(request, 176000L, 0L, productMap, List.of());

        // then
        assertThat(result.getItems()).hasSize(2);
        assertThat(result.getItems())
                .extracting(OrderItem::getProductId)
                .containsExactlyInAnyOrder(1L, 2L);
    }

    @Test
    @DisplayName("쿠폰 없이 주문 생성 시 OrderCoupon 목록이 비어있다")
    void create_withoutCoupons_orderCouponsEmpty() throws Exception {
        // given
        PaymentRequest request = createPaymentRequest(1L, List.of(
                new PaymentRequest.PaymentItemRequest(1L, 1)
        ), null);
        Map<Long, Product> productMap = Map.of(1L, testProduct);

        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        // when
        Order result = orderCommand.create(request, 59000L, 0L, productMap, List.of());

        // then
        assertThat(result.getCoupons()).isEmpty();
    }

    @Test
    @DisplayName("쿠폰 적용 시 OrderCoupon이 올바른 스냅샷으로 생성된다")
    void create_withCoupon_createsOrderCouponCorrectly() throws Exception {
        // given
        PaymentRequest request = createPaymentRequest(1L, List.of(
                new PaymentRequest.PaymentItemRequest(1L, 1)
        ), List.of(1L));
        Map<Long, Product> productMap = Map.of(1L, testProduct);

        AppliedCouponResult appliedCoupon = AppliedCouponResult.builder()
                .userCouponId(1L)
                .couponId(10L)
                .couponName("5000원 할인")
                .discountAmount(5000L)
                .appliedOrder(1)
                .build();

        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        // when
        Order result = orderCommand.create(request, 59000L, 5000L, productMap, List.of(appliedCoupon));

        // then
        assertThat(result.getCoupons()).hasSize(1);
        OrderCoupon coupon = result.getCoupons().get(0);
        assertThat(coupon.getUserCouponId()).isEqualTo(1L);
        assertThat(coupon.getCouponId()).isEqualTo(10L);
        assertThat(coupon.getCouponName()).isEqualTo("5000원 할인");
        assertThat(coupon.getDiscountAmount()).isEqualTo(5000L);
        assertThat(coupon.getAppliedOrder()).isEqualTo(1);
    }

    @Test
    @DisplayName("여러 쿠폰 적용 시 모든 OrderCoupon이 생성되고 appliedOrder가 유지된다")
    void create_multipleCoupons_allCouponsCreated() throws Exception {
        // given
        PaymentRequest request = createPaymentRequest(1L, List.of(
                new PaymentRequest.PaymentItemRequest(1L, 1)
        ), List.of(1L, 2L));
        Map<Long, Product> productMap = Map.of(1L, testProduct);

        AppliedCouponResult first = AppliedCouponResult.builder()
                .userCouponId(1L).couponId(10L).couponName("10% 할인")
                .discountAmount(5900L).appliedOrder(1).build();
        AppliedCouponResult second = AppliedCouponResult.builder()
                .userCouponId(2L).couponId(20L).couponName("3000원 할인")
                .discountAmount(3000L).appliedOrder(2).build();

        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        // when
        Order result = orderCommand.create(request, 59000L, 8900L, productMap, List.of(first, second));

        // then
        assertThat(result.getCoupons()).hasSize(2);
        assertThat(result.getCoupons())
                .extracting(OrderCoupon::getAppliedOrder)
                .containsExactly(1, 2);
    }

    @Test
    @DisplayName("주문 생성 시 금액(originalAmount, discountAmount, paymentAmount)이 올바르게 설정된다")
    void create_setsAmountsCorrectly() throws Exception {
        // given
        PaymentRequest request = createPaymentRequest(1L, List.of(
                new PaymentRequest.PaymentItemRequest(1L, 1)
        ), null);
        Map<Long, Product> productMap = Map.of(1L, testProduct);

        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        // when
        Order result = orderCommand.create(request, 59000L, 5000L, productMap, List.of());

        // then
        assertThat(result.getOriginalAmount()).isEqualTo(59000L);
        assertThat(result.getDiscountAmount()).isEqualTo(5000L);
        assertThat(result.getPaymentAmount()).isEqualTo(54000L);
    }

    @Test
    @DisplayName("할인 금액이 원래 금액을 초과해도 결제 금액은 0원 이상이다")
    void create_discountExceedsOriginal_paymentAmountIsZero() throws Exception {
        // given
        PaymentRequest request = createPaymentRequest(1L, List.of(
                new PaymentRequest.PaymentItemRequest(1L, 1)
        ), null);
        Map<Long, Product> productMap = Map.of(1L, testProduct);

        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        // when
        Order result = orderCommand.create(request, 59000L, 100000L, productMap, List.of());

        // then
        assertThat(result.getPaymentAmount()).isEqualTo(0L);
    }

    @Test
    @DisplayName("주문 생성 시 초기 상태는 PAYMENT_REQUESTED이다")
    void create_initialStatus_isPaymentRequested() throws Exception {
        // given
        PaymentRequest request = createPaymentRequest(1L, List.of(
                new PaymentRequest.PaymentItemRequest(1L, 1)
        ), null);
        Map<Long, Product> productMap = Map.of(1L, testProduct);

        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        // when
        Order result = orderCommand.create(request, 59000L, 0L, productMap, List.of());

        // then
        assertThat(result.getOrderStatus()).isEqualTo(OrderStatus.PAYMENT_REQUESTED);
    }

    @Test
    @DisplayName("주문 생성 시 userId가 올바르게 설정된다")
    void create_setsUserIdCorrectly() throws Exception {
        // given
        PaymentRequest request = createPaymentRequest(42L, List.of(
                new PaymentRequest.PaymentItemRequest(1L, 1)
        ), null);
        Map<Long, Product> productMap = Map.of(1L, testProduct);

        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        // when
        Order result = orderCommand.create(request, 59000L, 0L, productMap, List.of());

        // then
        assertThat(result.getUserId()).isEqualTo(42L);
    }

    @Test
    @DisplayName("주문 생성 시 orderRepository.save가 호출된다")
    void create_callsRepositorySave() throws Exception {
        // given
        PaymentRequest request = createPaymentRequest(1L, List.of(
                new PaymentRequest.PaymentItemRequest(1L, 1)
        ), null);
        Map<Long, Product> productMap = Map.of(1L, testProduct);

        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        // when
        orderCommand.create(request, 59000L, 0L, productMap, List.of());

        // then
        verify(orderRepository, times(1)).save(any(Order.class));
    }

    // ===== update() 테스트 =====

    @Test
    @DisplayName("결제 성공 시 주문 상태가 PAID로 변경된다")
    void update_paymentSuccess_orderStatusBecomePaid() throws Exception {
        // given
        Order order = Order.create(1L, 59000L, 0L);
        setId(order, 100L);

        when(orderRepository.findById(100L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        // when
        orderCommand.update(100L, true);

        // then
        assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.PAID);
        verify(orderRepository).save(order);
    }

    @Test
    @DisplayName("결제 실패 시 주문 상태가 FAILED로 변경된다")
    void update_paymentFailed_orderStatusBecomeFailed() throws Exception {
        // given
        Order order = Order.create(1L, 59000L, 0L);
        setId(order, 100L);

        when(orderRepository.findById(100L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        // when
        orderCommand.update(100L, false);

        // then
        assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.FAILED);
        verify(orderRepository).save(order);
    }

    @Test
    @DisplayName("존재하지 않는 주문 ID로 업데이트 시 예외 없이 종료된다")
    void update_orderNotFound_doesNothing() {
        // given
        when(orderRepository.findById(999L)).thenReturn(Optional.empty());

        // when & then
        assertThatNoException().isThrownBy(() -> orderCommand.update(999L, true));
        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("존재하지 않는 주문 ID로 실패 업데이트 시에도 예외 없이 종료된다")
    void update_orderNotFound_withFailed_doesNothing() {
        // given
        when(orderRepository.findById(999L)).thenReturn(Optional.empty());

        // when & then
        assertThatNoException().isThrownBy(() -> orderCommand.update(999L, false));
        verify(orderRepository, never()).save(any());
    }

    // ===== Helper methods =====

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