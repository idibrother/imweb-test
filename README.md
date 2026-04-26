# 쇼핑몰 웹 서버

JWT 기반 인증과 상품/쿠폰/결제 기능을 포함한 Spring Boot 쇼핑몰 백엔드 서버입니다.

## 기술 스택

- Java 21, Spring Boot 3.4.4
- Spring Security + JWT (RS256)
- Spring Data JPA + MySQL
- Docker / Docker Compose

## 패키지 구조

```
com.imweb.shop
├── auth/            인증 도메인 (로그인, 토큰 발급, 감사 로그)
├── product/         상품 도메인 (상품 재고 조회)
├── coupon/          쿠폰 도메인 (쿠폰 정책, 사용자 쿠폰 조회)
├── order/           주문/결제 도메인 (결제 요청, 재고 차감)
└── global/          공통 (보안 설정, 예외 처리, JWT 설정)
```

각 도메인은 `api` / `application` / `domain` / `infrastructure` / `dto` 레이어로 구성됩니다.

## 빠른 시작

### Docker Compose (권장)

Colima 미실행 시 자동으로 시작한 후 Docker Compose를 실행합니다.

```bash
./start.sh
```

MySQL과 서버가 함께 실행됩니다. 서버가 준비되면 `http://localhost:8080`으로 접근합니다.

### 로컬 실행

```bash
./gradlew bootRun
```

## 환경변수

| 변수 | 기본값                     | 설명 |
|------|-------------------------|------|
| `DB_HOST` | `localhost`             | MySQL 호스트 |
| `DB_NAME` | `shop`                  | 데이터베이스 이름 |
| `DB_USERNAME` | `shop`                  | DB 사용자 |
| `DB_PASSWORD` | `shop_password`         | DB 비밀번호 |
| `JWT_ISSUER` | `http://localhost:8080` | JWT issuer |
| `JWT_PRIVATE_KEY_BASE64` | *(자동 생성)*               | PKCS8 RSA 개인키 (Base64) |
| `JWT_ACCESS_TTL_MINUTES` | `15`                    | Access Token 만료 시간 (분) |
| `JWT_REFRESH_TTL_DAYS` | `30`                    | Refresh Token 만료 시간 (일) |
| `MAX_LOGIN_ATTEMPTS` | `5`                     | 최대 로그인 실패 횟수 |
| `LOCKOUT_DURATION_MINUTES` | `15`                    | 잠금 지속 시간 (분) |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:3000` | 허용 Origin (쉼표 구분) |

> **주의**: `JWT_PRIVATE_KEY_BASE64` 미설정 시 재시작할 때마다 키가 변경됩니다. 프로덕션에서는 반드시 설정하세요.

### RSA 키 생성 방법

```bash
openssl genpkey -algorithm RSA -out private.pem -pkeyopt rsa_keygen_bits:2048
openssl pkcs8 -topk8 -inform PEM -outform DER -in private.pem -nocrypt | base64
```

출력된 값을 `JWT_PRIVATE_KEY_BASE64`에 설정합니다.

---

## API 엔드포인트

### 인증

```bash
# 로그인
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username": "user", "password": "user123"}'
```

```json
{
  "access_token": "eyJ...",
  "refresh_token": "550e8400-...",
  "token_type": "Bearer",
  "expires_in": 900
}
```

```bash
# 토큰 갱신
curl -X POST http://localhost:8080/auth/refresh \
  -H "Content-Type: application/json" \
  -d '{"refresh_token": "550e8400-..."}'

# 로그아웃
curl -X POST http://localhost:8080/auth/logout \
  -H "Content-Type: application/json" \
  -d '{"refresh_token": "550e8400-..."}'
```

이하 API는 모두 `Authorization: Bearer {access_token}` 헤더가 필요합니다.

---

### 상품 재고 조회

```bash
# 전체 판매 중인 상품 조회
curl -X GET "http://localhost:8080/api/v1/products/stocks" \
  -H "Content-Type: application/json"

# 카테고리 필터 + 페이지네이션
curl -X GET "http://localhost:8080/api/v1/products/stocks?category=FASHION&page=0&size=20" \
  -H "Content-Type: application/json"
```

**카테고리**: `FASHION` | `BEAUTY` | `FOOD` | `ELECTRONICS` | `LIVING`

```json
{
  "products": [
    {
      "productId": 1,
      "productName": "프리미엄 청바지",
      "price": 59000,
      "currentStockQuantity": 100,
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

### 사용자 보유 쿠폰 조회

```bash
GET /api/v1/users/{userId}/coupons
curl -X GET "http://localhost:8080/api/v1/users/1/coupons" \
  -H "Content-Type: application/json"
```

```json
{
  "coupons": [
    {
      "userCouponId": 1,
      "couponId": 1,
      "couponName": "패션 10% 할인 쿠폰",
      "discountType": "FIXED_RATE",
      "discountValue": 10,
      "available": true,
      "unavailableReason": null,
      "minPurchaseAmount": 30000,
      "maxDiscountAmount": 10000,
      "applicableCategory": "FASHION",
      "startedAt": "2026-04-01T00:00:00",
      "endedAt": "2026-05-31T23:59:59"
    }
  ]
}
```

**unavailableReason**: `ALREADY_USED` | `EXPIRED` | `NOT_STARTED` | `DISABLED`

---

### 결제 요청

```bash
curl -X POST "http://localhost:8080/api/v1/payments" \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 1,
    "items": [
      { "productId": 1, "quantity": 2 }
    ],
    "userCouponIds": [1]
  }'
```

```json

{
  "userId": 1,
  "items": [
    { "productId": 1, "quantity": 2 }
  ],
  "userCouponIds": [1]
}
```

```json
{
  "orderId": 1,
  "orderStatus": "PAID",
  "originalAmount": 118000,
  "discountAmount": 10000,
  "paymentAmount": 108000,
  "items": [
    {
      "productId": 1,
      "productName": "프리미엄 청바지",
      "unitPrice": 59000,
      "quantity": 2,
      "totalPrice": 118000,
      "remainingStockQuantity": 98
    }
  ],
  "appliedCoupons": [
    {
      "userCouponId": 1,
      "couponId": 2,
      "couponName": "패션 10% 할인 쿠폰",
      "discountAmount": 10000,
      "appliedOrder": 1
    }
  ]
}
```

---

### 에러 응답 형식

```json
{
  "code": "INSUFFICIENT_STOCK",
  "message": "상품 재고가 부족합니다.",
  "details": {
    "productId": 1
  }
}
```

| 코드 | HTTP | 설명 |
|------|-----:|------|
| `USER_NOT_FOUND` | 404 | 사용자를 찾을 수 없음 |
| `PRODUCT_NOT_FOUND` | 404 | 상품을 찾을 수 없음 |
| `PRODUCT_NOT_ON_SALE` | 400 | 판매 중인 상품이 아님 |
| `INSUFFICIENT_STOCK` | 409 | 재고 부족 |
| `COUPON_NOT_FOUND` | 404 | 쿠폰을 찾을 수 없음 |
| `COUPON_NOT_OWNED` | 403 | 보유하지 않은 쿠폰 |
| `COUPON_ALREADY_USED` | 400 | 이미 사용된 쿠폰 |
| `COUPON_EXPIRED` | 400 | 만료된 쿠폰 |
| `COUPON_NOT_APPLICABLE` | 400 | 적용 불가 쿠폰 |
| `PAYMENT_FAILED` | 500 | 결제 실패 |

---

## 기본 사용자 및 초기 데이터

| 사용자 | 비밀번호 | 롤 |
|--------|----------|-----|
| `admin` | `admin123` | ADMIN, USER |
| `user` | `user123` | USER |

서버 최초 실행 시 샘플 상품 5개, 쿠폰 3개, 사용자 쿠폰이 자동으로 생성됩니다.

---

## 핵심 설계 사항

### 동시성 제어

- 상품 재고 차감: **비관적 락** (`SELECT ... FOR UPDATE`, id 오름차순 정렬로 데드락 방지)
- 쿠폰 중복 사용 방지: 사용자 쿠폰 조회 시 비관적 락 적용

### 쿠폰 적용 순서

1. 정율(FIXED_RATE) 쿠폰 먼저, 정액(FIXED_AMOUNT) 쿠폰 나중
2. 같은 유형 내에서는 할인 금액이 큰 쿠폰 먼저
3. 최종 결제 금액은 0원 미만 불가

### 트랜잭션

결제 요청의 재고 차감 → 쿠폰 사용 → 주문 생성 → 결제 시뮬레이션이 단일 트랜잭션으로 처리됩니다. 실패 시 전체 롤백됩니다.

---

## Docker 이미지 빌드

```bash
./gradlew bootJar
docker build -t shopping-mall-server .
```
