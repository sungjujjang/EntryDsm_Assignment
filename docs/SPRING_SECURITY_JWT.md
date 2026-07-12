# Spring Security + JWT 인증 로직 설명

## 목차
1. [아키텍처 개요](#1-아키텍처-개요)
2. [인증 흐름 (Request Lifecycle)](#2-인증-흐름-request-lifecycle)
3. [핵심 컴포넌트 상세](#3-핵심-컴포넌트-상세)
4. [토큰 생성 및 검증](#4-토큰-생성-및-검증)
5. [에러 핸들링](#5-에러-핸들링)
6. [API 엔드포인트](#6-api-엔드포인트)

---

## 1. 아키텍처 개요

```
Client Request
    │
    ▼
┌─────────────────────────────────────────┐
│         Spring Security Filter Chain     │
│                                          │
│  ┌──────────────────────────────────┐   │
│  │   JwtAuthenticationFilter        │   │
│  │   (OncePerRequestFilter)         │   │
│  │                                  │   │
│  │  1. Authorization 헤더에서       │   │
│  │     Bearer 토큰 추출             │   │
│  │  2. 토큰 유효성 검증             │   │
│  │  3. Authentication 객체 생성     │   │
│  │  4. SecurityContext에 저장       │   │
│  └──────────────────────────────────┘   │
│                                          │
│  ┌──────────────────────────────────┐   │
│  │   UsernamePasswordAuthentication  │   │
│  │   Filter (기본 필터)             │   │
│  └──────────────────────────────────┘   │
│                                          │
│  ┌──────────────────────────────────┐   │
│  │   DispatcherServlet              │   │
│  │   → Controller                   │   │
│  └──────────────────────────────────┘   │
└─────────────────────────────────────────┘
```

이 프로젝트는 **Stateless 기반 JWT 인증**을 사용합니다. 세션을 사용하지 않고, 클라이언트가 발급받은 JWT 토큰을 매 요청마다 `Authorization` 헤더에 달아서 서버에 전송하면, 서버는 토큰을 검증하고 요청 주체를 식별합니다.

### 파일 구조

```
Global/
├── SecurityConfig.java            # Security 필터 체인 설정
├── JwtTokenProvider.java          # 토큰 생성/검증/파싱
├── JwtAuthenticationFilter.java   # 매 요청마다 토큰 검증 필터
├── CustomUserDetailsService.java  # DB에서 유저 로드
└── CustomUserDetail.java          # UserDetails 구현체

Auth/
├── AuthController.java            # 회원가입/로그인 API
├── TestController.java            # JWT 테스트 API
├── User.java                      # JPA 엔티티
├── UserRepository.java            # JPA 리포지토리
└── dto/
    ├── RegisterRequest.java
    ├── LoginRequest.java
    └── LoginResponse.java
```

---

## 2. 인증 흐름 (Request Lifecycle)

### 2-1. 회원가입 흐름

```
Client                        Server
  │                              │
  │  POST /api/auth/register     │
  │  {phone, name, password}     │
  │ ───────────────────────────> │
  │                              │
  │                    ┌─────────┴─────────┐
  │                    │ 1. 요청 검증       │
  │                    │    (@Valid)        │
  │                    │                    │
  │                    │ 2. 중복 전화번호    │
  │                    │    확인            │
  │                    │                    │
  │                    │ 3. 비밀번호         │
  │                    │    BCrypt 해싱     │
  │                    │                    │
  │                    │ 4. DB 저장         │
  │                    └─────────┬─────────┘
  │                              │
  │  201 Created                 │
  │  {message: "회원가입 성공"}   │
  │ <─────────────────────────── │
```

**핵심 포인트:**
- 비밀번호는 **BCrypt**로 해싱되어 저장됩니다 (평문 저장 절대 금지)
- `POST /api/auth/**`는 `SecurityConfig`에서 `.permitAll()`로 설정되어 있어 인증 불필요

### 2-2. 로그인 흐름

```
Client                        Server
  │                              │
  │  POST /api/auth/login        │
  │  {phone, password}           │
  │ ───────────────────────────> │
  │                              │
  │                    ┌─────────┴─────────┐
  │                    │ 1. AuthenticationManager.authenticate() │
  │                    │    │               │
  │                    │    ▼               │
  │                    │ 2. CustomUserDetailsService              │
  │                    │    .loadUserByUsername(phone)            │
  │                    │    │               │
  │                    │    ▼               │
  │                    │ 3. DB에서 유저 조회 │
  │                    │    │               │
  │                    │    ▼               │
  │                    │ 4. BCryptPasswordEncoder.matches()       │
  │                    │    (입력 비밀번호 vs DB 해시 비교)        │
  │                    │    │               │
  │                    │    ▼               │
  │                    │ 5. JwtTokenProvider.createToken(phone)   │
  │                    │    (JWT 발급)       │
  │                    └─────────┬─────────┘
  │                              │
  │  200 OK                      │
  │  {accessToken: "eyJhbG..."}  │
  │ <─────────────────────────── │
```

**인증 과정 상세:**
1. `AuthenticationManager`는 등록된 `AuthenticationProvider`(기본: `DaoAuthenticationProvider`)를 사용
2. `DaoAuthenticationProvider`는 `UserDetailsService.loadUserByUsername()`을 호출하여 유저 정보 로드
3. 로드된 `UserDetails`의 비밀번호와 요청 비밀번호를 `PasswordEncoder.matches()`로 비교
4. 일치하면 `Authentication` 객체를 반환, 불일치하면 `BadCredentialsException` 발생

### 2-3. 인증된 요청 흐름 (JWT 검증)

```
Client                        Server
  │                              │
  │  GET /api/test/hello         │
  │  Authorization: Bearer eyJhbG...  │
  │ ───────────────────────────> │
  │                              │
  │  ┌───────────────────────────┤
  │  │ JwtAuthenticationFilter   │
  │  │ doFilterInternal()        │
  │  │                           │
  │  │ ① resolveToken(request)   │
  │  │   - "Authorization" 헤더  │
  │  │     읽기                  │
  │  │   - "Bearer " 접두사 제거 │
  │  │   - 토큰 문자열 추출      │
  │  │                           │
  │  │ ② validateToken(token)    │
  │  │   - JWT 서명 검증 (HS256) │
  │  │   - 만료 시간 검증        │
  │  │   - 위조 여부 확인        │
  │  │                           │
  │  │ ③ getAuthentication(token)│
  │  │   - 토큰에서 Claims 파싱  │
  │  │   - subject(phone) 추출   │
  │  │   - CustomUserDetail 생성 │
  │  │   - Authentication 객체   │
  │  │     생성                  │
  │  │                           │
  │  │ ④ SecurityContextHolder   │
  │  │    .setAuthentication()   │
  │  │   - 현재 요청의 보안       │
  │  │     컨텍스트에 인증 저장   │
  │  └───────────────────────────┘
  │                              │
  │                    ┌─────────┴─────────┐
  │                    │ Controller 실행    │
  │                    │ @AuthenticationPrincipal로 │
  │                    │ 유저 정보 주입     │
  │                    └─────────┬─────────┘
  │                              │
  │  200 OK                      │
  │  {message: "JWT 인증이       │
  │   잘 동작하고 있습니다!"}    │
  │ <─────────────────────────── │
```

---

## 3. 핵심 컴포넌트 상세

### 3-1. SecurityConfig

```java
// SecurityConfig.java
http
  .csrf(csrf -> csrf.disable())       // REST API이므로 CSRF 비활성화
  .authorizeHttpRequests(auth -> auth
      .requestMatchers("/api/auth/**").permitAll()  // 로그인/회원가입은 인증 불필요
      .anyRequest().authenticated()                 // 나머지는 모두 인증 필요
  )
  .addFilterBefore(jwtAuthenticationFilter,         // JWT 필터를 기본 필터보다 앞에 배치
                   UsernamePasswordAuthenticationFilter.class);
```

**`addFilterBefore`의 의미:**
- `UsernamePasswordAuthenticationFilter`는 기본적으로 폼 로그인을 처리하는 필터
- 우리의 JWT 필터를 이 **앞에** 배치함으로써, 모든 요청이 JWT 검증을 먼저 거치게 됨
- JWT가 유효하면 `SecurityContext`에 인증 정보를 넣고, 유효하지 않으면 그냥 통과시킴
- 통과 후 `.anyRequest().authenticated()`에서 인증 여부를 최종 판단

### 3-2. JwtTokenProvider

**토큰 생성:**
```java
public String createToken(String phone) {
    Claims claims = Jwts.claims().setSubject(phone).build();  // 페이로드에 phone 저장
    Date now = new Date();
    Date expiry = new Date(now.getTime() + validityInMs);     // 만료 시간 계산

    return Jwts.builder()
            .setClaims(claims)        // 페이로드
            .setIssuedAt(now)         // 발급 시간 (iat)
            .setExpiration(expiry)    // 만료 시간 (exp)
            .signWith(key, SignatureAlgorithm.HS256)  // HMAC-SHA256 서명
            .compact();
}
```

**토큰 구조 (JWT):**
```
Header.Payload.Signature

Header:  {"alg":"HS256","typ":"JWT"}
Payload: {"sub":"01012345678","iat":1720000000,"exp":1720086400}
Signature: HMAC-SHA256(base64(header) + "." + base64(payload), secret_key)
```

**토큰 검증:**
```java
public boolean validateToken(String token) {
    try {
        Jwts.parser().setSigningKey(key).build().parseClaimsJws(token);
        return true;  // 서명 일치 + 만료 안 됨
    } catch (ExpiredJwtException e) {
        return false;  // 토큰 만료
    } catch (JwtException | IllegalArgumentException e) {
        return false;  // 위조된 토큰, 잘못된 형식 등
    }
}
```

**인증 객체 생성:**
```java
public Authentication getAuthentication(String token) {
    Claims claims = parseClaims(token);
    String phone = claims.getSubject();                    // 페이로드에서 phone 추출
    CustomUserDetail principal = new CustomUserDetail(phone);
    return new UsernamePasswordAuthenticationToken(
        principal,   // Principal (誰?)
        token        // Credentials (증명 수단)
    );
}
```

### 3-3. JwtAuthenticationFilter

```java
// OncePerRequestFilter를 상속 → 요청당 정확히 한 번 실행
@Override
protected void doFilterInternal(HttpServletRequest request,
                                HttpServletResponse response,
                                FilterChain filterChain) {

    String token = resolveToken(request);  // Bearer 토큰 추출

    // 토큰이 있고 유효하면 인증 설정
    if (StringUtils.hasText(token) && jwtTokenProvider.validateToken(token)) {
        Authentication authentication = jwtTokenProvider.getAuthentication(token);
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    filterChain.doFilter(request, response);  // 다음 필터로 진행
}
```

**`resolveToken` 메서드:**
```java
private String resolveToken(HttpServletRequest request) {
    String bearer = request.getHeader("Authorization");  // 헤더 읽기
    if (StringUtils.hasText(bearer) && bearer.startsWith("Bearer ")) {
        return bearer.substring(7);  // "Bearer " 제거하고 토큰만 반환
    }
    return null;
}
```

### 3-4. CustomUserDetailsService

```java
@Override
public UserDetails loadUserByUsername(String phone) {
    User user = userRepository.findByPhone(phone)
            .orElseThrow(() -> new UsernameNotFoundException("전화번호가 없음"));
    return new CustomUserDetail(user);
}
```

- Spring Security의 `DaoAuthenticationProvider`가 자동으로 호출
- 전화번호로 DB에서 유저를 조회하여 `UserDetails`로 반환
- 반환된 `UserDetails`의 비밀번호와 입력 비밀번호를 `PasswordEncoder`가 비교

---

## 4. 토큰 생성 및 검증

### 설정값

```properties
# application.properties
jwt.access-token-validity=86400000                    # 24시간 (ms)
jwt.secret=RElBUllfQU5EX0... (Base64 인코딩된 시크릿 키)
```

### HMAC-SHA256 서명 방식

```
시크릿 키 (server-side)
    │
    ▼
┌───────────────────────────────────┐
│  Header + Payload를 시크릿 키로    │
│  HMAC-SHA256 해싱                 │
│                                    │
│  signature = HMAC_SHA256(          │
│    base64(header) + "." + payload,│
│    secret_key                      │
│  )                                │
└───────────────────────────────────┘
    │
    ▼
서명이 변경되면 서버가 감지 가능
→ 토큰 위조 시도 시 검증 실패
```

### 검증 과정

| 단계 | 검증 내용 | 실패 시 |
|------|-----------|---------|
| 1 | 시크릿 키로 서명 재생성하여 비교 | `JwtException` → false |
| 2 | `exp` 클레임이 현재 시간보다 미래인지 | `ExpiredJwtException` → false |
| 3 | 토큰 형식이 올바른지 | `IllegalArgumentException` → false |

---

## 5. 에러 핸들링

### 필터 수준

`JwtAuthenticationFilter`는 토큰이 없거나 유효하지 않으면 **조용히 통과**시킵니다 (인증 없이). 이후 `SecurityFilterChain`의 `.anyRequest().authenticated()`에서 403 Forbidden을 반환합니다.

```
토큰 없음/무효 → Filter 통과 → SecurityFilterChain에서 차단 → 403
토큰 유효 → Filter에서 Authentication 설정 → Controller 접근 허용
```

### 로그인 실패 시

```java
// authenticationManager.authenticate() 호출 시
// 비밀번호 불일치 → BadCredentialsException
// 유저 없음 → UsernameNotFoundException
```

클라이언트에는 기본적인 에러 메시지가 응답됩니다.

---

## 6. API 엔드포인트

| Method | URL | 인증 | 설명 |
|--------|-----|------|------|
| `POST` | `/api/auth/register` | 불필요 | 회원가입 (phone, name, password) |
| `POST` | `/api/auth/login` | 불필요 | 로그인 → JWT 토큰 발급 |
| `GET` | `/api/test/hello` | **필요** | JWT 인증 확인 테스트 |
| `GET` | `/api/test/me` | **필요** | 현재 유저 정보 조회 |

### 테스트 방법

```bash
# 1. 회원가입
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"phone":"01012345678","name":"홍길동","password":"test1234"}'

# 2. 로그인 (토큰 발급)
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"phone":"01012345678","password":"test1234"}'
# 응답: {"accessToken":"eyJhbGciOiJIUzI1NiJ9..."}

# 3. 인증된 요청
curl http://localhost:8080/api/test/hello \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..."

# 4. 내 정보 조회
curl http://localhost:8080/api/test/me \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..."

# 5. 토큰 없이 접근 (403 반환)
curl http://localhost:8080/api/test/hello
# 응답: 403 Forbidden
```
