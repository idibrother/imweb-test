package com.imweb.shop.order;

import com.imweb.shop.auth.domain.Role;
import com.imweb.shop.auth.domain.User;
import com.imweb.shop.auth.infrastructure.UserRepository;
import com.imweb.shop.global.exception.PaymentDomainException;
import com.imweb.shop.order.application.PaymentService;
import com.imweb.shop.order.dto.PaymentRequest;
import com.imweb.shop.product.domain.Product;
import com.imweb.shop.product.domain.ProductCategory;
import com.imweb.shop.product.domain.ProductSaleStatus;
import com.imweb.shop.product.infrastructure.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.*;

/**
 * Concurrency test for payment service using pessimistic locking.
 * Requires H2 in-memory database (configured via application-test.yml).
 * Note: For true deadlock prevention testing, use MySQL/real DB.
 */
@SpringBootTest
@ActiveProfiles("test")
class PaymentConcurrencyTest {

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Long productId;
    private List<Long> userIds = new ArrayList<>();

    @BeforeEach
    void setUp() {
        // Clean up
        productRepository.deleteAll();
        userRepository.deleteAll();

        // Create a product with limited stock = 5
        Product product = new Product("한정판 상품", 10000L, 5, ProductCategory.FOOD, ProductSaleStatus.ON_SALE);
        product = productRepository.save(product);
        productId = product.getId();

        // Create 10 users
        for (int i = 0; i < 10; i++) {
            User user = userRepository.save(User.builder()
                    .username("concurrent_user_" + i)
                    .password(passwordEncoder.encode("password"))
                    .email("concurrent" + i + "@test.com")
                    .roles(Set.of(Role.USER))
                    .enabled(true)
                    .build());
            userIds.add(user.getId());
        }
    }

    @Test
    @DisplayName("동시에 10명이 재고 5개 상품을 구매할 때 정확히 5개만 성공한다")
    void concurrentPayment_onlySuccessfulUpToStock() throws InterruptedException {
        int threadCount = 10;
        int stockQuantity = 5;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        for (int i = 0; i < threadCount; i++) {
            final Long userId = userIds.get(i);
            executor.submit(() -> {
                try {
                    startLatch.await();
                    PaymentRequest request = buildRequest(userId, productId, 1);
                    paymentService.pay(request);
                    successCount.incrementAndGet();
                } catch (PaymentDomainException e) {
                    failCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        doneLatch.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        // Exactly 5 should succeed (matching stock quantity)
        assertThat(successCount.get()).isEqualTo(stockQuantity);
        assertThat(failCount.get()).isEqualTo(threadCount - stockQuantity);

        // Verify final stock is 0
        Product finalProduct = productRepository.findById(productId).orElseThrow();
        assertThat(finalProduct.getStockQuantity()).isEqualTo(0);
    }

    private PaymentRequest buildRequest(Long userId, Long productId, int quantity) {
        try {
            PaymentRequest request = new PaymentRequest();
            setField(request, "userId", userId);
            setField(request, "items", List.of(new PaymentRequest.PaymentItemRequest(productId, quantity)));
            setField(request, "userCouponIds", List.of());
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
}
