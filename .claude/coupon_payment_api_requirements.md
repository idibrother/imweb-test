# 쿠폰/상품재고/결제 API 구현 요구사항 명세서

## 1. 목적

본 문서는 사용자가 보유한 쿠폰 조회, 판매 상품 재고 조회, 쿠폰을 적용한 상품 결제 요청 API를 구현하기 위한 요구사항을 정의한다.

본 과제의 핵심은 단순 CRUD 구현이 아니라, **다수 사용자가 동시에 같은 상품을 주문하더라도 재고와 주문 데이터의 정합성이 깨지지 않는 구조**를 설계하고 구현하는 것이다.

또한 향후 다양한 쿠폰 정책, 포인트 시스템, 실제 PG 연동, 주문 취소/환불 등의 기능 확장을 고려하여 도메인 구조를 설계한다.

---

## 2. 구현 범위

### 2.1 필수 API

1. 사용자 보유 쿠폰 목록 조회 API
2. 판매 중인 상품 재고 조회 API
3. 상품 결제 요청 API

### 2.2 필수 고려사항

- 쿠폰 할인 정책 적용
- 상품 재고 차감
- 결제 금액 계산
- 결제 시뮬레이션 처리
- 트랜잭션 롤백 처리
- 동시 주문 시 재고 정합성 보장
- 테스트 코드 작성
- 확장 가능한 코드 구조 설계

---

## 3. 도메인 정의

## 3.1 사용자 User

사용자는 쿠폰을 보유하고 상품을 주문할 수 있다.

### 주요 속성

| 필드 | 타입 | 설명 |
|---|---|---|
| id | Long | 사용자 ID |
| name | String | 사용자명 |
| createdAt | DateTime | 생성일시 |
| updatedAt | DateTime | 수정일시 |

---

## 3.2 상품 Product

현재 판매 중인 상품 정보를 나타낸다.

### 주요 속성

| 필드 | 타입 | 설명 |
|---|---|---|
| id | Long | 상품 ID |
| name | String | 상품명 |
| price | BigDecimal 또는 Long | 상품 가격 |
| stockQuantity | Int | 현재 재고 수량 |
| category | ProductCategory | 상품 카테고리 |
| saleStatus | ProductSaleStatus | 판매 상태 |
| createdAt | DateTime | 생성일시 |
| updatedAt | DateTime | 수정일시 |
| version | Long | 낙관적 락 사용 시 버전 값 |

### 판매 상태 예시

```text
ON_SALE   : 판매 중
SOLD_OUT  : 품절
STOPPED   : 판매 중지
```

### 상품 카테고리 예시

```text
FASHION
BEAUTY
FOOD
ELECTRONICS
LIVING
```

---

## 3.3 쿠폰 Coupon

쿠폰 정책 자체를 나타낸다.

### 주요 속성

| 필드 | 타입 | 설명 |
|---|---|---|
| id | Long | 쿠폰 ID |
| name | String | 쿠폰명 |
| discountType | CouponDiscountType | 할인 방식 |
| discountValue | BigDecimal 또는 Long | 할인 금액 또는 할인율 |
| minPurchaseAmount | Long | 사용 가능한 최소 구매 금액 |
| maxDiscountAmount | Long nullable | 최대 할인 금액 |
| applicableCategory | ProductCategory nullable | 적용 가능한 상품 카테고리 |
| startedAt | DateTime | 사용 시작일 |
| endedAt | DateTime | 사용 종료일 |
| createdAt | DateTime | 생성일시 |
| updatedAt | DateTime | 수정일시 |

### 할인 방식

```text
FIXED_AMOUNT : 정액 할인
FIXED_RATE   : 정율 할인
```

### 할인 계산 예시

#### 정액 할인

```text
상품 금액 30,000원
쿠폰 할인 금액 5,000원
최종 할인 금액 = 5,000원
```

#### 정율 할인

```text
상품 금액 30,000원
쿠폰 할인율 10%
최대 할인 금액 2,000원
계산 할인 금액 = 3,000원
최종 할인 금액 = 2,000원
```

---

## 3.4 사용자 쿠폰 UserCoupon

사용자가 실제로 보유한 쿠폰을 나타낸다.

쿠폰 정책인 `Coupon`과 사용자의 보유 상태를 분리하여 관리한다.

### 주요 속성

| 필드 | 타입 | 설명 |
|---|---|---|
| id | Long | 사용자 쿠폰 ID |
| userId | Long | 사용자 ID |
| couponId | Long | 쿠폰 ID |
| status | UserCouponStatus | 쿠폰 상태 |
| usedAt | DateTime nullable | 사용 일시 |
| createdAt | DateTime | 발급 일시 |
| updatedAt | DateTime | 수정 일시 |

### 쿠폰 상태

```text
AVAILABLE : 사용 가능
USED      : 사용 완료
EXPIRED   : 만료
DISABLED  : 사용 불가
```

---

## 3.5 주문 Order

사용자의 주문 정보를 나타낸다.

### 주요 속성

| 필드 | 타입 | 설명 |
|---|---|---|
| id | Long | 주문 ID |
| userId | Long | 사용자 ID |
| orderStatus | OrderStatus | 주문 상태 |
| originalAmount | Long | 쿠폰 적용 전 상품 총액 |
| discountAmount | Long | 총 할인 금액 |
| paymentAmount | Long | 최종 결제 금액 |
| createdAt | DateTime | 생성일시 |
| updatedAt | DateTime | 수정일시 |

### 주문 상태

```text
CREATED          : 주문 생성
PAYMENT_REQUESTED: 결제 요청
PAID             : 결제 완료
FAILED           : 결제 실패
CANCELED         : 주문 취소
```

---

## 3.6 주문 상품 OrderItem

주문에 포함된 상품 목록을 나타낸다.

### 주요 속성

| 필드 | 타입 | 설명 |
|---|---|---|
| id | Long | 주문 상품 ID |
| orderId | Long | 주문 ID |
| productId | Long | 상품 ID |
| productName | String | 주문 당시 상품명 |
| unitPrice | Long | 주문 당시 상품 단가 |
| quantity | Int | 주문 수량 |
| totalPrice | Long | 상품별 총액 |

상품명과 가격은 주문 당시 기준으로 저장한다. 이후 상품명이 변경되거나 가격이 변경되어도 기존 주문 내역은 영향을 받지 않아야 한다.

---

## 3.7 주문 쿠폰 OrderCoupon

주문에 적용된 쿠폰 내역을 나타낸다.

### 주요 속성

| 필드 | 타입 | 설명 |
|---|---|---|
| id | Long | 주문 쿠폰 ID |
| orderId | Long | 주문 ID |
| userCouponId | Long | 사용자 쿠폰 ID |
| couponId | Long | 쿠폰 ID |
| discountAmount | Long | 해당 쿠폰으로 할인된 금액 |
| appliedOrder | Int | 쿠폰 적용 순서 |

---

## 4. API 상세 요구사항

# 4.1 사용자 보유 쿠폰 목록 조회 API

## 4.1.1 설명

사용자가 보유한 쿠폰 목록을 조회한다.

사용 가능 여부는 단순히 상태값만 보는 것이 아니라 다음 조건을 함께 고려해야 한다.

- 사용자 쿠폰 상태가 `AVAILABLE`인지
- 쿠폰 사용 기간 내인지
- 쿠폰이 비활성화되지 않았는지

## 4.1.2 Endpoint

```http
GET /api/v1/users/{userId}/coupons
```

## 4.1.3 Request

### Path Variable

| 이름 | 타입 | 필수 | 설명 |
|---|---|---|---|
| userId | Long | Y | 사용자 ID |

## 4.1.4 Response

```json
{
  "coupons": [
    {
      "userCouponId": 1001,
      "couponId": 501,
      "couponName": "10% 할인 쿠폰",
      "discountType": "FIXED_RATE",
      "discountValue": 10,
      "available": true,
      "unavailableReason": null,
      "minPurchaseAmount": 10000,
      "maxDiscountAmount": 5000,
      "applicableCategory": "FASHION",
      "startedAt": "2026-04-01T00:00:00",
      "endedAt": "2026-04-30T23:59:59"
    }
  ]
}
```

## 4.1.5 사용 가능 여부 판단 기준

| 조건 | 사용 가능 여부 |
|---|---|
| UserCoupon.status != AVAILABLE | 사용 불가 |
| 현재 시간이 startedAt 이전 | 사용 불가 |
| 현재 시간이 endedAt 이후 | 사용 불가 |
| Coupon 비활성화 상태 | 사용 불가 |
| 위 조건 모두 통과 | 사용 가능 |

## 4.1.6 unavailableReason 예시

```text
ALREADY_USED
EXPIRED
NOT_STARTED
DISABLED
```

---

# 4.2 상품 재고 조회 API

## 4.2.1 설명

현재 판매 중인 상품의 재고 현황을 조회한다.

판매 중지 상품은 기본 조회 대상에서 제외한다.

## 4.2.2 Endpoint

```http
GET /api/v1/products/stocks
```

## 4.2.3 Query Parameter

| 이름 | 타입 | 필수 | 설명 |
|---|---|---|---|
| category | String | N | 상품 카테고리 필터 |
| page | Int | N | 페이지 번호 |
| size | Int | N | 페이지 크기 |

## 4.2.4 Response

```json
{
  "products": [
    {
      "productId": 101,
      "productName": "기본 반팔 티셔츠",
      "price": 25000,
      "currentStockQuantity": 30,
      "category": "FASHION",
      "saleStatus": "ON_SALE"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 1
}
```

---

# 4.3 상품 결제 요청 API

## 4.3.1 설명

사용자가 상품 목록과 적용할 쿠폰 목록을 전달하면 다음 작업을 하나의 트랜잭션 안에서 처리한다.

1. 상품 존재 여부 확인
2. 상품 판매 상태 확인
3. 재고 수량 확인
4. 쿠폰 소유 여부 확인
5. 쿠폰 사용 가능 여부 확인
6. 쿠폰 적용 가능 상품/카테고리 검증
7. 할인 금액 계산
8. 재고 차감
9. 쿠폰 사용 처리
10. 주문 생성
11. 결제 시뮬레이션 호출
12. 결제 성공 시 주문 상태 PAID 처리
13. 결제 실패 또는 예외 발생 시 전체 롤백

## 4.3.2 Endpoint

```http
POST /api/v1/payments
```

## 4.3.3 Request

```json
{
  "userId": 1,
  "items": [
    {
      "productId": 101,
      "quantity": 2
    },
    {
      "productId": 102,
      "quantity": 1
    }
  ],
  "userCouponIds": [1001, 1002]
}
```

## 4.3.4 Request Validation

| 필드 | 검증 조건 |
|---|---|
| userId | 필수, 존재하는 사용자여야 함 |
| items | 필수, 1개 이상이어야 함 |
| items.productId | 필수, 존재하는 상품이어야 함 |
| items.quantity | 필수, 1 이상이어야 함 |
| userCouponIds | 선택 값, 중복 쿠폰 ID는 허용하지 않음 |

## 4.3.5 Response

```json
{
  "orderId": 9001,
  "orderStatus": "PAID",
  "originalAmount": 75000,
  "discountAmount": 7000,
  "paymentAmount": 68000,
  "items": [
    {
      "productId": 101,
      "productName": "기본 반팔 티셔츠",
      "unitPrice": 25000,
      "quantity": 2,
      "totalPrice": 50000,
      "remainingStockQuantity": 28
    },
    {
      "productId": 102,
      "productName": "양말 세트",
      "unitPrice": 25000,
      "quantity": 1,
      "totalPrice": 25000,
      "remainingStockQuantity": 14
    }
  ],
  "appliedCoupons": [
    {
      "userCouponId": 1001,
      "couponId": 501,
      "couponName": "10% 할인 쿠폰",
      "discountAmount": 5000,
      "appliedOrder": 1
    },
    {
      "userCouponId": 1002,
      "couponId": 502,
      "couponName": "2,000원 할인 쿠폰",
      "discountAmount": 2000,
      "appliedOrder": 2
    }
  ]
}
```

---

## 5. 쿠폰 적용 정책

쿠폰 적용 순서 정책은 자유 설계 가능하나, 구현 명확성을 위해 다음 정책을 기본으로 한다.

## 5.1 기본 쿠폰 적용 순서

1. 정율 쿠폰 먼저 적용
2. 정액 쿠폰 나중 적용
3. 같은 할인 방식이면 할인 금액이 큰 쿠폰 먼저 적용
4. 최종 결제 금액은 0원 미만이 될 수 없음

## 5.2 쿠폰 적용 대상 금액

쿠폰은 주문 전체 금액을 기준으로 적용한다.

단, 쿠폰에 적용 가능한 상품 카테고리가 지정되어 있으면 해당 카테고리에 속한 상품 금액 합계에 대해서만 할인한다.

예시:

```text
주문 상품
- FASHION 상품 30,000원
- FOOD 상품 20,000원

FASHION 전용 10% 쿠폰 적용 시
할인 기준 금액 = 30,000원
할인 금액 = 3,000원
```

## 5.3 정율 쿠폰 할인 계산

```text
할인 기준 금액 * 할인율 / 100
```

단, 최대 할인 금액이 존재하면 다음과 같이 계산한다.

```text
최종 할인 금액 = min(계산된 할인 금액, 최대 할인 금액)
```

## 5.4 정액 쿠폰 할인 계산

```text
최종 할인 금액 = min(쿠폰 할인 금액, 현재 남은 결제 대상 금액)
```

## 5.5 최소 구매 금액 조건

쿠폰의 `minPurchaseAmount`는 쿠폰 적용 전 주문 총액 기준으로 판단한다.

```text
주문 총액 >= 쿠폰 최소 구매 금액
```

조건을 만족하지 않으면 쿠폰은 적용할 수 없다.

## 5.6 쿠폰 중복 적용 정책

- 여러 장의 쿠폰 적용을 허용한다.
- 동일한 사용자 쿠폰 ID를 중복 전달할 수 없다.
- 이미 사용된 쿠폰은 사용할 수 없다.
- 쿠폰 적용 후 남은 결제 대상 금액이 0원이면 이후 쿠폰은 적용하지 않는다.

---

## 6. 재고 차감 정책

## 6.1 재고 차감 조건

다음 조건을 모두 만족해야 재고 차감이 가능하다.

- 상품이 존재해야 한다.
- 상품 상태가 `ON_SALE`이어야 한다.
- 현재 재고 수량이 주문 수량 이상이어야 한다.

## 6.2 재고 부족 시 처리

재고가 부족하면 결제 요청 전체를 실패 처리한다.

일부 상품만 결제 성공하는 부분 성공은 허용하지 않는다.

```text
예: 상품 A는 재고 충분, 상품 B는 재고 부족
결과: 전체 결제 실패
```

## 6.3 동시성 제어

다수 사용자가 동시에 같은 상품을 주문할 수 있으므로 재고 차감 로직은 반드시 동시성 제어가 필요하다.

권장 방식은 다음 중 하나다.

### 방식 1. 비관적 락 Pessimistic Lock

상품 재고 조회 시 `SELECT ... FOR UPDATE` 방식으로 해당 상품 row에 락을 건다.

장점:

- 구현이 직관적이다.
- 재고 수량처럼 충돌 가능성이 높은 데이터에 적합하다.

단점:

- 트래픽이 높을 경우 락 대기 시간이 길어질 수 있다.

예시:

```sql
SELECT *
FROM products
WHERE id IN (...)
FOR UPDATE;
```

JPA 예시:

```kotlin
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("select p from Product p where p.id in :productIds")
fun findAllByIdInForUpdate(productIds: List<Long>): List<Product>
```

### 방식 2. 조건부 UPDATE

재고 차감을 하나의 UPDATE 문으로 처리한다.

```sql
UPDATE products
SET stock_quantity = stock_quantity - :quantity
WHERE id = :productId
  AND stock_quantity >= :quantity;
```

업데이트된 row 수가 0이면 재고 부족으로 판단한다.

장점:

- DB 레벨에서 원자적으로 재고 차감 가능하다.
- 별도 select 후 update보다 효율적일 수 있다.

단점:

- 여러 상품을 동시에 주문하는 경우 구현 복잡도가 올라간다.
- 차감 후 주문 스냅샷 생성 시 추가 조회가 필요하다.

### 본 구현의 권장안

초기 구현은 **비관적 락 기반 재고 차감**을 권장한다.

이유:

- 과제 요구사항을 명확하게 만족한다.
- 주문 생성, 쿠폰 사용 처리, 재고 차감을 하나의 트랜잭션에서 이해하기 쉽다.
- 테스트 코드로 동시성 검증이 용이하다.

---

## 7. 트랜잭션 처리 요구사항

## 7.1 결제 요청 트랜잭션 범위

결제 요청 API는 다음 작업을 하나의 트랜잭션으로 묶는다.

```text
상품 조회 및 락 획득
→ 재고 검증
→ 쿠폰 검증
→ 주문 생성
→ 주문 상품 생성
→ 할인 금액 계산
→ 재고 차감
→ 쿠폰 사용 처리
→ 결제 시뮬레이션 호출
→ 주문 상태 변경
```

## 7.2 롤백 대상

결제 중 예외가 발생하면 다음 데이터는 모두 롤백되어야 한다.

- 상품 재고 차감
- 쿠폰 사용 상태 변경
- 주문 생성
- 주문 상품 생성
- 주문 쿠폰 생성
- 주문 상태 변경

## 7.3 결제 시뮬레이션 처리

실제 PG 연동은 하지 않는다.

단, 향후 실제 PG 연동을 고려하여 다음과 같은 인터페이스 기반 구조로 설계한다.

```kotlin
interface PaymentGateway {
    fun pay(command: PaymentCommand): PaymentResult
}
```

현재 구현에서는 빈 구현체 또는 항상 성공하는 Fake 구현체를 사용한다.

```kotlin
class FakePaymentGateway : PaymentGateway {
    override fun pay(command: PaymentCommand): PaymentResult {
        return PaymentResult.success()
    }
}
```

## 7.4 외부 PG 연동 확장 시 주의사항

실제 PG 연동이 추가되면 외부 API 호출을 DB 트랜잭션 내부에서 오래 잡고 있는 구조는 피해야 한다.

향후 확장 시에는 다음 구조를 고려한다.

1. 주문 생성
2. 재고 예약
3. 결제 요청
4. 결제 성공 이벤트 수신
5. 재고 확정 차감
6. 결제 실패 또는 타임아웃 시 재고 예약 해제

초기 과제 구현에서는 요구사항 단순화를 위해 하나의 트랜잭션에서 결제 시뮬레이션까지 처리한다.

---

## 8. 예외 처리 정책

## 8.1 공통 에러 응답 형식

```json
{
  "code": "INSUFFICIENT_STOCK",
  "message": "상품 재고가 부족합니다.",
  "details": {
    "productId": 101,
    "requestedQuantity": 5,
    "currentStockQuantity": 3
  }
}
```

## 8.2 주요 에러 코드

| 코드 | HTTP Status | 설명 |
|---|---:|---|
| USER_NOT_FOUND | 404 | 사용자를 찾을 수 없음 |
| PRODUCT_NOT_FOUND | 404 | 상품을 찾을 수 없음 |
| PRODUCT_NOT_ON_SALE | 400 | 판매 중인 상품이 아님 |
| INSUFFICIENT_STOCK | 409 | 재고 부족 |
| COUPON_NOT_FOUND | 404 | 쿠폰을 찾을 수 없음 |
| COUPON_NOT_OWNED | 403 | 사용자가 보유한 쿠폰이 아님 |
| COUPON_ALREADY_USED | 400 | 이미 사용된 쿠폰 |
| COUPON_EXPIRED | 400 | 만료된 쿠폰 |
| COUPON_NOT_APPLICABLE | 400 | 주문 상품에 적용할 수 없는 쿠폰 |
| INVALID_REQUEST | 400 | 잘못된 요청 |
| PAYMENT_FAILED | 500 | 결제 실패 |

---

## 9. 권장 패키지 구조

Kotlin/Spring Boot 기준 예시다.

```text
com.example.payment
 ├── api
 │   ├── CouponController.kt
 │   ├── ProductController.kt
 │   └── PaymentController.kt
 │
 ├── application
 │   ├── CouponQueryService.kt
 │   ├── ProductQueryService.kt
 │   └── PaymentService.kt
 │
 ├── domain
 │   ├── coupon
 │   │   ├── Coupon.kt
 │   │   ├── UserCoupon.kt
 │   │   ├── CouponDiscountPolicy.kt
 │   │   ├── FixedAmountCouponPolicy.kt
 │   │   └── FixedRateCouponPolicy.kt
 │   │
 │   ├── product
 │   │   └── Product.kt
 │   │
 │   ├── order
 │   │   ├── Order.kt
 │   │   ├── OrderItem.kt
 │   │   └── OrderCoupon.kt
 │   │
 │   └── payment
 │       ├── PaymentGateway.kt
 │       ├── FakePaymentGateway.kt
 │       └── PaymentResult.kt
 │
 ├── infrastructure
 │   ├── persistence
 │   │   ├── ProductRepository.kt
 │   │   ├── CouponRepository.kt
 │   │   ├── UserCouponRepository.kt
 │   │   └── OrderRepository.kt
 │   │
 │   └── config
 │
 └── support
     ├── exception
     └── response
```

---

## 10. 핵심 클래스 설계 방향

## 10.1 Product

```kotlin
class Product(
    val id: Long,
    val name: String,
    val price: Long,
    var stockQuantity: Int,
    val category: ProductCategory,
    var saleStatus: ProductSaleStatus,
) {
    fun validatePurchasable(quantity: Int) {
        if (saleStatus != ProductSaleStatus.ON_SALE) {
            throw ProductNotOnSaleException(id)
        }
        if (stockQuantity < quantity) {
            throw InsufficientStockException(id, quantity, stockQuantity)
        }
    }

    fun decreaseStock(quantity: Int) {
        validatePurchasable(quantity)
        stockQuantity -= quantity
    }
}
```

---

## 10.2 UserCoupon

```kotlin
class UserCoupon(
    val id: Long,
    val userId: Long,
    val coupon: Coupon,
    var status: UserCouponStatus,
    var usedAt: LocalDateTime?,
) {
    fun validateUsable(now: LocalDateTime) {
        if (status != UserCouponStatus.AVAILABLE) {
            throw CouponAlreadyUsedException(id)
        }
        coupon.validateUsable(now)
    }

    fun use(now: LocalDateTime) {
        validateUsable(now)
        status = UserCouponStatus.USED
        usedAt = now
    }
}
```

---

## 10.3 CouponDiscountPolicy

쿠폰 정책은 Strategy Pattern으로 분리한다.

```kotlin
interface CouponDiscountPolicy {
    fun supports(discountType: CouponDiscountType): Boolean
    fun calculate(command: CouponDiscountCommand): Long
}
```

정액 쿠폰:

```kotlin
class FixedAmountCouponPolicy : CouponDiscountPolicy {
    override fun supports(discountType: CouponDiscountType): Boolean {
        return discountType == CouponDiscountType.FIXED_AMOUNT
    }

    override fun calculate(command: CouponDiscountCommand): Long {
        return minOf(command.discountValue, command.remainingAmount)
    }
}
```

정율 쿠폰:

```kotlin
class FixedRateCouponPolicy : CouponDiscountPolicy {
    override fun supports(discountType: CouponDiscountType): Boolean {
        return discountType == CouponDiscountType.FIXED_RATE
    }

    override fun calculate(command: CouponDiscountCommand): Long {
        val calculated = command.baseAmount * command.discountValue / 100
        return command.maxDiscountAmount?.let { minOf(calculated, it) } ?: calculated
    }
}
```

---

## 10.4 PaymentService 처리 흐름

```kotlin
@Service
class PaymentService(
    private val productRepository: ProductRepository,
    private val userCouponRepository: UserCouponRepository,
    private val orderRepository: OrderRepository,
    private val paymentGateway: PaymentGateway,
    private val couponDiscountCalculator: CouponDiscountCalculator,
) {
    @Transactional
    fun pay(command: PaymentCommand): PaymentResponse {
        // 1. 상품 조회 및 비관적 락 획득
        // 2. 상품 재고 검증
        // 3. 사용자 쿠폰 조회 및 사용 가능 검증
        // 4. 주문 원금 계산
        // 5. 쿠폰 할인 계산
        // 6. 상품 재고 차감
        // 7. 사용자 쿠폰 사용 처리
        // 8. 주문/주문상품/주문쿠폰 저장
        // 9. 결제 시뮬레이션 호출
        // 10. 주문 상태 PAID 처리
        // 11. 응답 반환
    }
}
```

---

## 11. 데이터베이스 설계 예시

## 11.1 products

```sql
CREATE TABLE products (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    price BIGINT NOT NULL,
    stock_quantity INT NOT NULL,
    category VARCHAR(50) NOT NULL,
    sale_status VARCHAR(50) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL
);
```

## 11.2 coupons

```sql
CREATE TABLE coupons (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    discount_type VARCHAR(50) NOT NULL,
    discount_value BIGINT NOT NULL,
    min_purchase_amount BIGINT NOT NULL,
    max_discount_amount BIGINT NULL,
    applicable_category VARCHAR(50) NULL,
    started_at DATETIME NOT NULL,
    ended_at DATETIME NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL
);
```

## 11.3 user_coupons

```sql
CREATE TABLE user_coupons (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    coupon_id BIGINT NOT NULL,
    status VARCHAR(50) NOT NULL,
    used_at DATETIME NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    INDEX idx_user_coupons_user_id (user_id),
    INDEX idx_user_coupons_coupon_id (coupon_id)
);
```

## 11.4 orders

```sql
CREATE TABLE orders (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    order_status VARCHAR(50) NOT NULL,
    original_amount BIGINT NOT NULL,
    discount_amount BIGINT NOT NULL,
    payment_amount BIGINT NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL
);
```

## 11.5 order_items

```sql
CREATE TABLE order_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    product_name VARCHAR(255) NOT NULL,
    unit_price BIGINT NOT NULL,
    quantity INT NOT NULL,
    total_price BIGINT NOT NULL,
    INDEX idx_order_items_order_id (order_id)
);
```

## 11.6 order_coupons

```sql
CREATE TABLE order_coupons (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL,
    user_coupon_id BIGINT NOT NULL,
    coupon_id BIGINT NOT NULL,
    discount_amount BIGINT NOT NULL,
    applied_order INT NOT NULL,
    INDEX idx_order_coupons_order_id (order_id)
);
```

---

## 12. 테스트 코드 요구사항

## 12.1 쿠폰 목록 조회 테스트

### 테스트 케이스

1. 사용자가 보유한 쿠폰 목록을 조회할 수 있다.
2. 사용 완료 쿠폰은 `available=false`로 반환된다.
3. 만료된 쿠폰은 `available=false`로 반환된다.
4. 사용 시작 전 쿠폰은 `available=false`로 반환된다.
5. 사용 가능한 쿠폰은 `available=true`로 반환된다.

---

## 12.2 상품 재고 조회 테스트

### 테스트 케이스

1. 판매 중인 상품 목록과 재고를 조회할 수 있다.
2. 판매 중지 상품은 기본 조회에서 제외된다.
3. 카테고리 조건으로 상품 목록을 필터링할 수 있다.

---

## 12.3 결제 요청 테스트

### 정상 케이스

1. 쿠폰 없이 상품을 결제할 수 있다.
2. 정액 쿠폰을 적용하여 결제할 수 있다.
3. 정율 쿠폰을 적용하여 결제할 수 있다.
4. 최대 할인 금액이 있는 정율 쿠폰은 최대 할인 금액까지만 할인된다.
5. 여러 쿠폰을 정책에 맞는 순서로 적용할 수 있다.
6. 결제 성공 시 상품 재고가 주문 수량만큼 차감된다.
7. 결제 성공 시 사용한 쿠폰 상태가 `USED`로 변경된다.
8. 주문, 주문 상품, 주문 쿠폰 내역이 저장된다.

### 실패 케이스

1. 존재하지 않는 상품이면 결제 실패한다.
2. 판매 중이 아닌 상품이면 결제 실패한다.
3. 재고가 부족하면 결제 실패한다.
4. 존재하지 않는 쿠폰이면 결제 실패한다.
5. 사용자가 보유하지 않은 쿠폰이면 결제 실패한다.
6. 이미 사용된 쿠폰이면 결제 실패한다.
7. 만료된 쿠폰이면 결제 실패한다.
8. 최소 구매 금액 조건을 만족하지 않으면 결제 실패한다.
9. 적용 가능한 카테고리가 없는 쿠폰이면 결제 실패한다.
10. 결제 실패 시 재고 차감, 쿠폰 사용 처리, 주문 생성이 모두 롤백된다.

---

## 12.4 동시성 테스트

### 목적

여러 사용자가 동시에 같은 상품을 주문할 때 재고가 음수가 되거나 초과 판매가 발생하지 않아야 한다.

### 테스트 시나리오

```text
초기 상품 재고: 10개
동시 주문 요청: 20명
각 주문 수량: 1개

기대 결과:
- 성공 주문 수: 10건
- 실패 주문 수: 10건
- 최종 재고: 0개
- 상품 재고가 음수가 되지 않음
```

### 테스트 구현 방향

- `CountDownLatch` 사용
- `ExecutorService` 사용
- 실제 DB 또는 Testcontainers 기반 DB 사용 권장
- H2만 사용할 경우 DB 락 동작이 운영 DB와 다를 수 있으므로 주의

예시:

```kotlin
@Test
fun `동시에 주문해도 재고는 음수가 되지 않는다`() {
    val threadCount = 20
    val executorService = Executors.newFixedThreadPool(threadCount)
    val latch = CountDownLatch(threadCount)

    repeat(threadCount) {
        executorService.submit {
            try {
                paymentService.pay(
                    PaymentCommand(
                        userId = it.toLong() + 1,
                        items = listOf(PaymentItemCommand(productId = 1L, quantity = 1)),
                        userCouponIds = emptyList()
                    )
                )
            } catch (e: Exception) {
                // expected for insufficient stock
            } finally {
                latch.countDown()
            }
        }
    }

    latch.await()

    val product = productRepository.findById(1L).get()
    assertThat(product.stockQuantity).isEqualTo(0)
}
```

---

## 13. 확장 가능성 고려사항

## 13.1 다양한 쿠폰 정책 확장

향후 다음 쿠폰 정책이 추가될 수 있다.

- 특정 상품 전용 쿠폰
- 특정 브랜드 전용 쿠폰
- 첫 구매 전용 쿠폰
- 무료 배송 쿠폰
- N개 이상 구매 시 할인 쿠폰
- 장바구니 금액 구간별 할인 쿠폰

이를 고려하여 할인 정책은 if-else로 직접 구현하기보다 `CouponDiscountPolicy` 인터페이스와 구현체로 분리한다.

---

## 13.2 포인트 시스템 확장

향후 포인트 사용/적립 기능이 추가될 수 있다.

확장 시 고려사항:

- 포인트 사용은 결제 금액 계산 단계에 포함된다.
- 포인트 차감도 트랜잭션 롤백 대상이다.
- 결제 실패 시 포인트 차감은 롤백되어야 한다.
- 결제 성공 후 적립 포인트는 주문 완료 이벤트 기반으로 처리할 수 있다.

권장 구조:

```text
DiscountCalculator
 ├── CouponDiscountCalculator
 └── PointDiscountCalculator
```

---

## 13.3 실제 PG 연동 확장

현재는 FakePaymentGateway를 사용한다.

향후 PG 연동 시에는 다음 인터페이스를 유지하고 구현체만 교체한다.

```kotlin
interface PaymentGateway {
    fun pay(command: PaymentCommand): PaymentResult
}
```

예시:

```text
FakePaymentGateway
TossPaymentGateway
KakaoPayGateway
NaverPayGateway
```

---

## 13.4 주문 취소/환불 확장

주문 취소 기능 추가 시 다음 처리가 필요하다.

- 주문 상태 `CANCELED` 변경
- 상품 재고 복구
- 쿠폰 복구 여부 정책 결정
- PG 결제 취소 연동
- 포인트 사용 복구
- 포인트 적립 취소

쿠폰 복구 정책은 명확히 정의해야 한다.

예시:

```text
결제 직후 취소: 쿠폰 복구
배송 이후 환불: 쿠폰 복구하지 않음
관리자 취소: 쿠폰 복구 가능
```

---

## 14. 구현 시 주의사항

## 14.1 금액 타입

금액 계산에는 부동소수점 타입인 `Double`, `Float`를 사용하지 않는다.

권장:

- 원화 기준이면 `Long`
- 소수점 통화까지 고려하면 `BigDecimal`

본 구현에서는 원화 기준으로 `Long` 사용을 권장한다.

## 14.2 쿠폰 할인 금액 반올림

정율 할인 계산 시 소수점이 발생할 수 있다.

정책을 명확히 정해야 한다.

권장:

```text
원 단위 미만 절사
```

예시:

```text
10,999원 * 10% = 1,099.9원
할인 금액 = 1,099원
```

## 14.3 상품 가격 변경 이슈

주문 생성 후 상품 가격이 변경되어도 기존 주문 금액이 바뀌면 안 된다.

따라서 `OrderItem`에는 주문 당시의 상품명, 상품 단가를 스냅샷으로 저장한다.

## 14.4 쿠폰 사용 중복 방지

동일 쿠폰이 동시에 여러 결제 요청에 사용되지 않도록 사용자 쿠폰에도 동시성 제어가 필요하다.

권장 방식:

- `UserCoupon` 조회 시 비관적 락 적용
- 또는 `status = AVAILABLE` 조건부 update 사용

예시:

```sql
UPDATE user_coupons
SET status = 'USED', used_at = NOW()
WHERE id = :userCouponId
  AND user_id = :userId
  AND status = 'AVAILABLE';
```

업데이트 row 수가 0이면 이미 사용된 쿠폰으로 판단한다.

## 14.5 상품 조회 순서 고정

여러 상품에 비관적 락을 걸 때 데드락을 줄이기 위해 상품 ID 오름차순으로 조회하고 락을 획득한다.

```text
productIds.sorted()
```

쿠폰도 여러 개를 동시에 락 처리한다면 쿠폰 ID 오름차순으로 처리한다.

---

## 15. 인수 조건 Acceptance Criteria

## 15.1 쿠폰 목록 조회

- 사용자는 자신이 보유한 쿠폰 목록을 조회할 수 있다.
- 각 쿠폰은 할인 방식, 할인 값, 사용 가능 여부, 최소 구매 금액, 최대 할인 금액, 적용 가능 카테고리를 포함한다.
- 만료/사용완료/비활성 쿠폰은 사용 불가 사유와 함께 반환된다.

## 15.2 상품 재고 조회

- 사용자는 판매 중인 상품의 상품 ID, 상품명, 가격, 현재 재고 수량을 조회할 수 있다.
- 판매 중지 상품은 기본 조회 결과에 포함되지 않는다.

## 15.3 결제 요청

- 사용자는 여러 상품을 한 번에 결제할 수 있다.
- 사용자는 여러 쿠폰을 적용할 수 있다.
- 쿠폰은 정의된 정책 순서대로 적용된다.
- 결제 성공 시 재고가 정확히 차감된다.
- 결제 성공 시 사용 쿠폰은 사용 완료 처리된다.
- 결제 실패 시 재고, 쿠폰, 주문 데이터는 롤백된다.
- 동시에 여러 주문이 발생해도 재고가 음수가 되지 않는다.
- 재고보다 많은 주문이 동시에 발생하면 일부만 성공하고 나머지는 실패한다.

---

## 16. 구현 우선순위

## 16.1 1단계: 기본 도메인 및 조회 API

- Product 도메인 구현
- Coupon, UserCoupon 도메인 구현
- 쿠폰 목록 조회 API 구현
- 상품 재고 조회 API 구현

## 16.2 2단계: 결제 기본 기능

- Order, OrderItem, OrderCoupon 도메인 구현
- 쿠폰 할인 계산기 구현
- FakePaymentGateway 구현
- 결제 요청 API 구현

## 16.3 3단계: 트랜잭션 및 동시성 보강

- 상품 재고 비관적 락 적용
- 사용자 쿠폰 중복 사용 방지 처리
- 결제 실패 시 롤백 테스트 작성
- 동시성 테스트 작성

## 16.4 4단계: 테스트 및 리팩터링

- 단위 테스트 작성
- 통합 테스트 작성
- 예외 응답 정리
- 코드 구조 리팩터링

---

## 17. 최종 구현 체크리스트

- [ ] 쿠폰 목록 조회 API 구현
- [ ] 상품 재고 조회 API 구현
- [ ] 상품 결제 요청 API 구현
- [ ] 정액 쿠폰 할인 계산 구현
- [ ] 정율 쿠폰 할인 계산 구현
- [ ] 최대 할인 금액 제한 구현
- [ ] 최소 구매 금액 조건 구현
- [ ] 카테고리별 쿠폰 적용 구현
- [ ] 상품 재고 차감 구현
- [ ] 쿠폰 사용 처리 구현
- [ ] 주문/주문상품/주문쿠폰 저장 구현
- [ ] 결제 시뮬레이션 구현
- [ ] 트랜잭션 롤백 처리 구현
- [ ] 상품 재고 동시성 제어 구현
- [ ] 쿠폰 중복 사용 방지 구현
- [ ] 정상 결제 테스트 작성
- [ ] 결제 실패 테스트 작성
- [ ] 쿠폰 정책 테스트 작성
- [ ] 재고 동시성 테스트 작성
- [ ] API 예외 응답 테스트 작성

---

## 18. 요약

본 구현에서 가장 중요한 포인트는 다음과 같다.

1. 결제 요청은 주문 생성, 재고 차감, 쿠폰 사용 처리를 하나의 트랜잭션으로 묶어야 한다.
2. 재고는 동시성 제어가 반드시 필요하다.
3. 쿠폰도 동시에 여러 결제에서 중복 사용되지 않도록 제어해야 한다.
4. 결제 실패 시 재고, 쿠폰, 주문 데이터는 모두 롤백되어야 한다.
5. 쿠폰 정책은 향후 확장을 위해 전략 패턴 기반으로 분리하는 것이 좋다.
6. 실제 PG 연동은 인터페이스로 분리하고 현재는 Fake 구현체로 대체한다.
7. 동시성 테스트는 필수이며, 가능하면 Testcontainers 기반 실제 DB로 검증하는 것이 좋다.
