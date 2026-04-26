package com.imweb.shop.order.application;

import com.imweb.shop.auth.infrastructure.UserRepository;
import com.imweb.shop.coupon.application.AppliedCouponResult;
import com.imweb.shop.coupon.application.CouponDiscountCalculator;
import com.imweb.shop.coupon.domain.UserCoupon;
import com.imweb.shop.coupon.infrastructure.UserCouponRepository;
import com.imweb.shop.global.exception.ErrorCode;
import com.imweb.shop.global.exception.PaymentDomainException;
import com.imweb.shop.order.domain.Order;
import com.imweb.shop.order.dto.OrderPaymentRequestedEvent;
import com.imweb.shop.order.dto.PaymentRequest;
import com.imweb.shop.order.dto.PaymentResponse;
import com.imweb.shop.product.domain.Product;
import com.imweb.shop.product.domain.ProductCategory;
import com.imweb.shop.product.infrastructure.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final ProductRepository productRepository;
    private final UserCouponRepository userCouponRepository;
    private final OrderCommand orderCommand;
    private final UserRepository userRepository;
    private final CouponDiscountCalculator discountCalculator;
    private final ApplicationEventPublisher applicationEventPublisher;

    @Transactional
    public PaymentResponse pay(PaymentRequest request) {
        LocalDateTime now = LocalDateTime.now();

        // 1. Validate no duplicate coupon IDs
        List<Long> userCouponIds = request.getUserCouponIds() != null ? request.getUserCouponIds() : List.of();
        if (userCouponIds.size() != new HashSet<>(userCouponIds).size()) {
            throw new PaymentDomainException(ErrorCode.DUPLICATE_COUPON_IDS);
        }

        // 2. Validate user exists
        userRepository.findById(request.getUserId())
                .orElseThrow(() -> new PaymentDomainException(ErrorCode.USER_NOT_FOUND));

        // 3. Acquire pessimistic lock on products (sorted by id)
        List<Long> productIds = request.getItems().stream()
                .map(PaymentRequest.PaymentItemRequest::productId)
                .sorted()
                .distinct()
                .collect(Collectors.toList());
        List<Product> lockedProducts = productRepository.findAllByIdInForUpdate(productIds);

        // Build map for easy access
        Map<Long, Product> productMap = lockedProducts.stream()
                .collect(Collectors.toMap(Product::getId, p -> p));

        // 4. Validate each product: exists, ON_SALE, sufficient stock
        for (PaymentRequest.PaymentItemRequest itemReq : request.getItems()) {
            if (!productMap.containsKey(itemReq.productId())) {
                throw new PaymentDomainException(ErrorCode.PRODUCT_NOT_FOUND,
                        "productId", itemReq.productId());
            }
            Product product = productMap.get(itemReq.productId());
            product.validatePurchasable(itemReq.quantity());
        }

        // 7. Calculate original amount
        long originalAmount = 0L;
        for (PaymentRequest.PaymentItemRequest itemReq : request.getItems()) {
            Product product = productMap.get(itemReq.productId());
            long itemTotal = product.getPrice() * itemReq.quantity();
            originalAmount += itemTotal;
        }

        // 8. Validate coupon min purchase amount and applicable category
        payment payment = calculatePayment(request, productMap, userCouponIds, originalAmount, now);

        // 9. Create Order
        Order order = orderCommand.create(request, originalAmount, payment.totalDiscount(), productMap, payment.appliedCoupons());

        // Build response
        PaymentResponse paymentResponse = buildResponse(order, request.getItems(), productMap, payment.appliedCoupons());

        // 10. Process payment (simulate)
        applicationEventPublisher.publishEvent(
                new OrderPaymentRequestedEvent(order.getId(), userCouponIds)
        );
        return paymentResponse;
    }

    private payment calculatePayment(PaymentRequest request, Map<Long, Product> productMap, List<Long> userCouponIds, long originalAmount, LocalDateTime now) {
        List<UserCoupon> lockedUserCoupons = List.of();
        if (userCouponIds != null && !userCouponIds.isEmpty()) {
            List<Long> sortedCouponIds = userCouponIds.stream().sorted().collect(Collectors.toList());
            lockedUserCoupons = userCouponRepository.findAllByIdInForUpdate(sortedCouponIds);

            Map<Long, UserCoupon> userCouponMap = lockedUserCoupons.stream()
                    .collect(Collectors.toMap(UserCoupon::getId, uc -> uc));

            // 6. Validate each coupon
            for (Long ucId : userCouponIds) {
                UserCoupon uc = userCouponMap.get(ucId);
                if (uc == null) {
                    throw new PaymentDomainException(ErrorCode.COUPON_NOT_FOUND, "userCouponId", ucId);
                }
                // Validate ownership
                if (!uc.getUserId().equals(request.getUserId())) {
                    throw new PaymentDomainException(ErrorCode.COUPON_NOT_OWNED, "userCouponId", ucId);
                }
                // Validate status and dates
                uc.validateUsable(now);
            }
        }

        Map<ProductCategory, Long> categorySubtotals = buildCategorySubtotals(request.getItems(), productMap);

        if (!lockedUserCoupons.isEmpty()) {
            for (UserCoupon uc : lockedUserCoupons) {
                // Validate min purchase amount
                if (originalAmount < uc.getCoupon().getMinPurchaseAmount()) {
                    throw new PaymentDomainException(ErrorCode.COUPON_MIN_AMOUNT_NOT_MET,
                            Map.of("couponId", uc.getCoupon().getId(),
                                    "minPurchaseAmount", uc.getCoupon().getMinPurchaseAmount(),
                                    "currentAmount", originalAmount));
                }

                // Validate applicable category
                if (uc.getCoupon().getApplicableCategory() != null) {
                    boolean hasCategoryItem = request.getItems().stream()
                            .anyMatch(item -> productMap.get(item.productId()).getCategory()
                                    == uc.getCoupon().getApplicableCategory());
                    if (!hasCategoryItem) {
                        throw new PaymentDomainException(ErrorCode.COUPON_NOT_APPLICABLE,
                                "couponId", uc.getCoupon().getId());
                    }
                }
            }
        }

        // 9. Calculate discount
        List<AppliedCouponResult> appliedCoupons = discountCalculator.calculateWithCategorySubtotals(
                lockedUserCoupons, originalAmount, categorySubtotals);

        long totalDiscount = appliedCoupons.stream()
                .mapToLong(AppliedCouponResult::getDiscountAmount)
                .sum();

        // 10. Decrease stock for each product
        for (PaymentRequest.PaymentItemRequest itemReq : request.getItems()) {
            Product product = productMap.get(itemReq.productId());
            product.decreaseStock(itemReq.quantity());
        }

        // 11. Mark each user coupon as USED
        for (UserCoupon uc : lockedUserCoupons) {
            uc.use(now);
        }
        return new payment(appliedCoupons, totalDiscount);
    }

    private record payment(List<AppliedCouponResult> appliedCoupons, long totalDiscount) {
    }

    private Map<ProductCategory, Long> buildCategorySubtotals(
            List<PaymentRequest.PaymentItemRequest> items,
            Map<Long, Product> productMap
    ) {
        Map<ProductCategory, Long> subtotals = new HashMap<>();
        for (PaymentRequest.PaymentItemRequest item : items) {
            Product product = productMap.get(item.productId());
            if (product != null) {
                subtotals.merge(product.getCategory(),
                        product.getPrice() * item.quantity(),
                        Long::sum);
            }
        }
        return subtotals;
    }

    private PaymentResponse buildResponse(
            Order order,
            List<PaymentRequest.PaymentItemRequest> itemRequests,
            Map<Long, Product> productMap,
            List<AppliedCouponResult> appliedCoupons
    ) {
        List<PaymentResponse.PaymentItemResult> itemResults = itemRequests.stream()
                .map(req -> {
                    Product product = productMap.get(req.productId());
                    return new PaymentResponse.PaymentItemResult(
                            product.getId(),
                            product.getName(),
                            product.getPrice(),
                            req.quantity(),
                            product.getPrice() * req.quantity(),
                            product.getStockQuantity()
                    );
                })
                .collect(Collectors.toList());

        List<PaymentResponse.AppliedCouponResponse> couponResponses = appliedCoupons.stream()
                .map(r -> new PaymentResponse.AppliedCouponResponse(
                        r.getUserCouponId(),
                        r.getCouponId(),
                        r.getCouponName(),
                        r.getDiscountAmount(),
                        r.getAppliedOrder()
                ))
                .collect(Collectors.toList());

        return new PaymentResponse(
                order.getId(),
                order.getOrderStatus(),
                order.getOriginalAmount(),
                order.getDiscountAmount(),
                order.getPaymentAmount(),
                itemResults,
                couponResponses
        );
    }
}
