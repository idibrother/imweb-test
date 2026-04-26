# Spring Boot JWT 인증 서버 요구사항 정의서

## 1. 개요
Spring Boot 기반 인증 서버를 구축하며 JWT를 활용한 인증/인가 체계를 구현한다.
Docker 환경에서 구동 가능해야 한다.

---

## 2. 기본 기능 요구사항

### 인증
- ID/PW 기반 로그인
- Access Token / Refresh Token 발급
- JWT 기반 Access Token 사용

### 토큰
- Access Token: 5~15분 만료
- Refresh Token: 7~30일 만료
- Refresh Token Rotation 적용

### 권한
- Role 기반 접근 제어 (USER, ADMIN 등)
- Scope 기반 확장 가능

### 엔드포인트
- POST /auth/login
- POST /auth/refresh
- POST /auth/logout
- GET /.well-known/jwks.json
- OAuth2 표준 엔드포인트 포함 가능

---

## 3. 보안 요구사항

### JWT
- RS256 또는 ES256 사용
- Claims: sub, iss, aud, exp, iat, jti, scope
- 서명키는 환경변수 또는 Secret Manager로 관리

### 인증 보안
- 비밀번호 BCrypt 또는 Argon2 해싱
- HTTPS 필수
- 로그인 실패 횟수 제한

### 토큰 보안
- Access Token 짧은 TTL 유지
- Refresh Token은 DB 또는 Redis 저장
- 토큰 폐기 기능 구현

### 기타
- CORS 제한
- 관리자 API Role 제한
- 감사 로그 기록

---

## 4. 기술 스택

- Spring Boot
- Spring Security
- Spring Authorization Server
- Spring Data JPA
- MySQL 또는 PostgreSQL
- Redis (선택)

---

## 5. Dockerfile

```dockerfile
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

RUN addgroup -S spring && adduser -S spring -G spring

COPY build/libs/*.jar app.jar

USER spring

EXPOSE 8080

ENV JAVA_OPTS=""

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
```

---

## 6. docker-compose

```yaml
version: '3.8'

services:
  auth-server:
    build: .
    ports:
      - "8080:8080"
    environment:
      DB_HOST: mysql
      DB_NAME: auth
      DB_USERNAME: auth
      DB_PASSWORD: auth_password
      JWT_ISSUER: http://localhost:8080
    depends_on:
      - mysql

  mysql:
    image: mysql:8
    environment:
      MYSQL_DATABASE: auth
      MYSQL_USER: auth
      MYSQL_PASSWORD: auth_password
      MYSQL_ROOT_PASSWORD: root_password
    ports:
      - "3306:3306"

```

---

## 7. 운영 체크리스트

- HTTPS 적용
- JWT 키 로테이션
- Secret 외부 관리
- Rate Limiting 적용
- Actuator 보안 설정

---

## 8. 결론

Access Token은 Stateless로 짧게 유지하고,
Refresh Token은 서버에서 통제하여 보안성을 확보한다.
