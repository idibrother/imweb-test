package com.imweb.shop.global.config;

import com.imweb.shop.auth.domain.Role;
import com.imweb.shop.auth.domain.User;
import com.imweb.shop.auth.infrastructure.UserRepository;
import com.imweb.shop.coupon.domain.Coupon;
import com.imweb.shop.coupon.domain.CouponDiscountType;
import com.imweb.shop.coupon.domain.UserCoupon;
import com.imweb.shop.coupon.domain.UserCouponStatus;
import com.imweb.shop.product.domain.Product;
import com.imweb.shop.product.domain.ProductCategory;
import com.imweb.shop.product.domain.ProductSaleStatus;
import com.imweb.shop.coupon.infrastructure.CouponRepository;
import com.imweb.shop.product.infrastructure.ProductRepository;
import com.imweb.shop.coupon.infrastructure.UserCouponRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ProductRepository productRepository;
    private final CouponRepository couponRepository;
    private final UserCouponRepository userCouponRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (userRepository.count() == 0) {
            User admin = userRepository.save(User.builder()
                    .username("admin")
                    .password(passwordEncoder.encode("admin123"))
                    .email("admin@example.com")
                    .roles(Set.of(Role.ADMIN, Role.USER))
                    .enabled(true)
                    .build());

            User user = userRepository.save(User.builder()
                    .username("user")
                    .password(passwordEncoder.encode("user123"))
                    .email("user@example.com")
                    .roles(Set.of(Role.USER))
                    .enabled(true)
                    .build());

            log.info("Default users created: admin/admin123, user/user123");

            // Sample products
            Product fashionProduct = productRepository.save(new Product(
                    "프리미엄 청바지", 59000L, 100, ProductCategory.FASHION, ProductSaleStatus.ON_SALE));
            Product beautyProduct = productRepository.save(new Product(
                    "수분 크림 세트", 35000L, 50, ProductCategory.BEAUTY, ProductSaleStatus.ON_SALE));
            Product foodProduct = productRepository.save(new Product(
                    "유기농 과일 세트", 28000L, 200, ProductCategory.FOOD, ProductSaleStatus.ON_SALE));
            Product electronicsProduct = productRepository.save(new Product(
                    "무선 이어폰", 129000L, 30, ProductCategory.ELECTRONICS, ProductSaleStatus.ON_SALE));
            Product livingProduct = productRepository.save(new Product(
                    "아로마 캔들 세트", 22000L, 80, ProductCategory.LIVING, ProductSaleStatus.SOLD_OUT));

            log.info("Sample products created");

            // Sample coupons
            LocalDateTime now = LocalDateTime.now();
            Coupon fixedAmountCoupon = couponRepository.save(new Coupon(
                    "5000원 할인 쿠폰",
                    CouponDiscountType.FIXED_AMOUNT,
                    5000L,
                    20000L,
                    null,
                    null,
                    now.minusDays(1),
                    now.plusDays(30)
            ));

            Coupon fixedRateCoupon = couponRepository.save(new Coupon(
                    "패션 10% 할인 쿠폰",
                    CouponDiscountType.FIXED_RATE,
                    10L,
                    30000L,
                    10000L,
                    ProductCategory.FASHION,
                    now.minusDays(1),
                    now.plusDays(30)
            ));

            Coupon electronicsRateCoupon = couponRepository.save(new Coupon(
                    "전자제품 15% 할인 쿠폰",
                    CouponDiscountType.FIXED_RATE,
                    15L,
                    50000L,
                    20000L,
                    ProductCategory.ELECTRONICS,
                    now.minusDays(1),
                    now.plusDays(30)
            ));

            log.info("Sample coupons created");

            // Assign coupons to users
            userCouponRepository.saveAll(List.of(
                    new UserCoupon(admin.getId(), fixedAmountCoupon, UserCouponStatus.AVAILABLE),
                    new UserCoupon(admin.getId(), fixedRateCoupon, UserCouponStatus.AVAILABLE),
                    new UserCoupon(admin.getId(), electronicsRateCoupon, UserCouponStatus.AVAILABLE),
                    new UserCoupon(user.getId(), fixedAmountCoupon, UserCouponStatus.AVAILABLE),
                    new UserCoupon(user.getId(), fixedRateCoupon, UserCouponStatus.AVAILABLE)
            ));

            log.info("Sample user coupons assigned");
        }
    }
}
