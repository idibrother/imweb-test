package com.imweb.shop.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."),
    PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND, "상품을 찾을 수 없습니다."),
    PRODUCT_NOT_ON_SALE(HttpStatus.BAD_REQUEST, "판매 중인 상품이 아닙니다."),
    INSUFFICIENT_STOCK(HttpStatus.CONFLICT, "상품 재고가 부족합니다."),
    COUPON_NOT_FOUND(HttpStatus.NOT_FOUND, "쿠폰을 찾을 수 없습니다."),
    COUPON_NOT_OWNED(HttpStatus.FORBIDDEN, "사용자가 보유한 쿠폰이 아닙니다."),
    COUPON_ALREADY_USED(HttpStatus.BAD_REQUEST, "이미 사용된 쿠폰입니다."),
    COUPON_EXPIRED(HttpStatus.BAD_REQUEST, "만료된 쿠폰입니다."),
    COUPON_NOT_STARTED(HttpStatus.BAD_REQUEST, "아직 사용 시작 전인 쿠폰입니다."),
    COUPON_NOT_APPLICABLE(HttpStatus.BAD_REQUEST, "주문 상품에 적용할 수 없는 쿠폰입니다."),
    COUPON_MIN_AMOUNT_NOT_MET(HttpStatus.BAD_REQUEST, "최소 구매 금액 조건을 만족하지 않습니다."),
    DUPLICATE_COUPON_IDS(HttpStatus.BAD_REQUEST, "중복된 쿠폰 ID가 포함되어 있습니다."),
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "잘못된 요청입니다."),
    PAYMENT_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "결제에 실패했습니다.");

    private final HttpStatus status;
    private final String message;
}
