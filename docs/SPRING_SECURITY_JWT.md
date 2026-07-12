# Spring Security + JWT 인증 로직 설명

## 목차
1. [아키텍처 개요](#1-아키텍처-개요)
2. [인증 흐름 (Request Lifecycle)](#2-인증-흐름-request-lifecycle)
3. [핵심 컴포넌트 상세](#3-핵심-컴포넌트-상세)
4. [토큰 생성 및 검증](#4-토큰-생성-및-검증)
5. [에러 핸들링](#5-에러-핸들링)
6. [API 엔드포인트](#6-api-엔드포인트)
7. [문제 해결 기록 (Troubleshooting)](#7-문제-해결-기록-troubleshooting)

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
│  │   SecurityContextHolderFilter    │   │
│  │   (SecurityContext 로드/저장)     │   │
│  └──────────────────────────────────┘   │
│           │                              │
│           ▼                              │
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
│           │                              │
│           ▼                              │
│  ┌──────────────────────────────────┐   │
│  │   AnonymousAuthenticationFilter  │   │
│  │   (인증 없으면 Anonymous 설정)    │   │
│  └──────────────────────────────────┘   │
│           │                              │
│           ▼                              │
│  ┌──────────────────────────────────┐   │
│  │   AuthorizationFilter            │   │
│  │   (인증 여부 최종 판단)           │   │
│  │   - isAuthenticated() == true?   │   │
│  │   - Pass → Controller            │   │
│  │   - Fail → 403 Forbidden         │   │
│  └──────────────────────────────────┘   │
│           │                              │
│           ▼                              │
│  ┌──────────────────────────────────┐   │
│  │   DispatcherServlet → Controller │   │
│  └──────────────────────────────────┘   │
└─────────────────────────────────────────┘
```

이 프로젝트는 **Spring Boot 4.1.0 + Spring Security 6** 기반의 **Stateless JWT 인증**을 사용합니다. 세션을 사용하지 않고, 클라이언트가 발급받은 JWT 토큰을 매 요청마다 `Authorization` 헤더에 달아서 서버에 전송하면, 서버는 토큰을 검증하고 요청 주체를 식별합니다.

### 파일 구조

```
Global/
├── SecurityConfig.java            # Security 필터 체인 설정
├── JwtTokenProvider.java          # 토큰 생성/검증/파싱 + JWT 인증 객체
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
5. **로그인 시 `request.getPhone()`을 직접 사용하여 JWT 토큰의 subject에 전화번호를 저장** (`authentication.getName()`이 아닌 이유는 아래 [7-3절](#7-3-customuserdetail-getusername의-함정) 참고)

### 2-3. 인증된 요청 흐름 (JWT 검증)

```
Client                        Server
  │                              │
  │  GET /api/test/hello         │
  │  Authorization: Bearer eyJhbG...  │
  │ ───────────────────────────> │
  │                              │
  │  ┌───────────────────────────┤
  │  │ SecurityContextHolderFilter│
  │  │ - 빈 SecurityContext 로드  │
  │  └───────────────────────────┘
  │           │                   │
  │           ▼                   │
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
  │  │   - JwtAuthentication     │
  │  │     (isAuthenticated=true)│
  │  │     생성                  │
  │  │                           │
  │  │ ④ SecurityContextHolder   │
  │  │    .setAuthentication()   │
  │  │   - 현재 요청의 보안       │
  │  │     컨텍스트에 인증 저장   │
  │  └───────────────────────────┘
  │           │                   │
  │           ▼                   │
  │  ┌───────────────────────────┤
  │  │ AuthorizationFilter       │
  │  │ - authentication          │
  │  │   .isAuthenticated()      │
  │  │   == true? → 통과         │
  │  └───────────────────────────┘
  │           │                   │
  │           ▼                   │
  │  Controller 실행              │
  │  @AuthenticationPrincipal로  │
  │  유저 정보 주입               │
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
http
  .csrf(csrf -> csrf.disable())
  .sessionManagement(session -> session
      .sessionCreationPolicy(SessionCreationPolicy.STATELESS))  // 세션 사용 안 함
  .authorizeHttpRequests(auth -> auth
      .requestMatchers("/api/auth/**").permitAll()  // 로그인/회원가입은 인증 불필요
      .anyRequest().authenticated()                 // 나머지는 모두 인증 필요
  )
  .addFilterBefore(new JwtAuthenticationFilter(jwtTokenProvider),
                   UsernamePasswordAuthenticationFilter.class);
```

**`SessionCreationPolicy.STATELESS`의 의미:**
- Spring Security가 매 요청마다 세션을 생성/조회하지 않음
- JWT 방식에서는 서버가 세션 상태를 유지할 필요가 없으므로 필수 설정
- `SecurityContextHolderFilter`가 요청 시작 시 빈 컨텍스트를 로드하고, 요청 종료 시 정리

**`addFilterBefore`의 의미:**
- `UsernamePasswordAuthenticationFilter`는 기본적으로 폼 로그인을 처리하는 필터
- 우리의 JWT 필터를 이 **앞에** 배치함으로써, 모든 요청이 JWT 검증을 먼저 거치게 됨
- JWT가 유효하면 `SecurityContext`에 인증 정보를 넣고, 유효하지 않으면 그냥 통과시킴
- 통과 후 `AuthorizationFilter`에서 `isAuthenticated()` 여부를 최종 판단

**`new JwtAuthenticationFilter()`로 직접 생성하는 이유:**
- `@Component` 어노테이션을 붙이면 Spring이 자동으로 서블릿 필터로도 등록함
- 이 경우 Security 필터 체인에 **이중 등록**되어 예상치 못한 동작 발생 가능
- `addFilterBefore()`로 명시적으로 Security 필터 체인에만 등록하는 것이 안전

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

**인증 객체 생성 (JwtAuthentication):**
```java
public Authentication getAuthentication(String token) {
    Claims claims = parseClaims(token);
    String phone = claims.getSubject();                    // 페이로드에서 phone 추출
    CustomUserDetail principal = new CustomUserDetail(phone);
    return new JwtAuthentication(principal, token);        // 커스텀 Authentication
}
```

### 3-3. JwtAuthentication (커스텀 Authentication)

```java
// JwtTokenProvider 내부 static 클래스
public static class JwtAuthentication implements Authentication {

    private final CustomUserDetail principal;
    private final String token;

    @Override
    public boolean isAuthenticated() {
        return true;   // ★ 핵심: JWT 검증이 완료된 인증된 사용자
    }

    @Override
    public String getName() {
        return principal.getPhone();
    }
    // ... 나머지 메서드
}
```

**왜 `UsernamePasswordAuthenticationToken`을 사용하지 않는가?**

Spring Security 6에서 `UsernamePasswordAuthenticationToken.setAuthenticated(true)`를 호출하면
`IllegalArgumentException`이 발생합니다. 이는 `UsernamePasswordAuthenticationToken`이
"신뢰할 수 있는 인증"을 생성할 때 반드시 `GrantedAuthority` 리스트를 통해 생성하도록 강제하는 보안 정책입니다.

JWT 토큰 검증은 `AuthenticationManager.authenticate()`를 거치지 않으므로 (별도의 인증 과정 없이
토큰 서명만 검증), `UsernamePasswordAuthenticationToken`의 생성자를 통해서는 `isAuthenticated=true`인
객체를 만들 수 없습니다. 따라서 `Authentication` 인터페이스를 직접 구현한 `JwtAuthentication`을 사용합니다.

### 3-4. JwtAuthenticationFilter

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

### 3-5. CustomUserDetailsService

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

`JwtAuthenticationFilter`는 토큰이 없거나 유효하지 않으면 **조용히 통과**시킵니다 (인증 없이). 이후 `AuthorizationFilter`에서 `isAuthenticated()` 체크 후 403을 반환합니다.

```
토큰 없음/무효 → Filter 통과 (인증 설정 안 함)
    → AnonymousAuthenticationFilter에서 Anonymous 설정
    → AuthorizationFilter에서 isAuthenticated() == false
    → 403 Forbidden

토큰 유효 → JwtAuthentication (isAuthenticated=true) 설정
    → AuthorizationFilter에서 isAuthenticated() == true
    → Controller 접근 허용 → 200 OK
```

### 로그인 실패 시

```java
// authenticationManager.authenticate() 호출 시
// 비밀번호 불일치 → BadCredentialsException
// 유저 없음 → UsernameNotFoundException
```

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

---

## 7. 문제 해결 기록 (Troubleshooting)

### 7-1. `@Component` 필터 이중 등록 문제

**증상:** `JwtAuthenticationFilter`에 `@Component` 어노테이션이 있으면 Spring Boot가 자동으로 서블릿 필터로 등록하고, `addFilterBefore()`로 Security 필터 체인에도 등록됨. 이 이중 등록이 예상치 못한 동작을 유발.

**해결:** `@Component` 제거 → `SecurityConfig`에서 `new JwtAuthenticationFilter(jwtTokenProvider)`로 직접 생성하여 Security 필터 체인에만 등록.

### 7-2. `UsernamePasswordAuthenticationToken.setAuthenticated(true)` 예외

**증상:** 토큰 검증 후 `getAuthentication()`에서 `UsernamePasswordAuthenticationToken`을 생성하고 `setAuthenticated(true)`를 호출하면 `IllegalArgumentException` 발생. Spring Security 6의 `AuthorizationFilter`는 `isAuthenticated()`가 `true`인 Authentication만 통과시키므로, 이 경우 403 반환.

**원리:**
- `UsernamePasswordAuthenticationToken`은 Spring Security 내부에서 "인증된 토큰"을 생성할 때 반드시 `GrantedAuthority` 리스트를 통과하도록 설계됨
- `setAuthenticated(true)`는 명시적으로 차단: `throw new IllegalArgumentException("Cannot set this token to trusted - use constructor which takes a GrantedAuthority list instead")`
- 생성자로도 `authenticated = false`가 기본값
- `AuthenticationManager.authenticate()`를 거치면 `authenticated = true`로 설정되지만, JWT 검증은 이 과정을 거치지 않음

**해결:** `Authentication` 인터페이스를 직접 구현한 `JwtAuthentication` 클래스를 사용하여 `isAuthenticated()`가 항상 `true`를 반환하도록 변경.

```java
public static class JwtAuthentication implements Authentication {
    @Override
    public boolean isAuthenticated() { return true; }
    @Override
    public void setAuthenticated(boolean isAuthenticated) { }  // no-op
}
```

### 7-3. `CustomUserDetail.getUsername()`의 함정

**증상:** `AuthController.login()`에서 `authentication.getName()`을 JWT 토큰의 subject로 사용했는데, `getUsername()`이 유저 이름("홍길동")을 반환하여 토큰의 subject가 이름이 됨.

**원리:** Spring Security의 `DaoAuthenticationProvider`는 `UserDetails.getUsername()`을 반환하는데, 로그인 후 `authentication.getName()`은 이 값을 가져옴. `CustomUserDetail.getUsername()`이 `user.getName()`(한국어 이름)을 반환하도록 설계되어 있었음.

**해결:** `AuthController.login()`에서 `authentication.getName()` 대신 `request.getPhone()`을 직접 사용하여 JWT subject에 전화번호를 저장.

```java
// Before (문제)
String token = jwtTokenProvider.createToken(authentication.getName());  // "홍길동"

// After (해결)
String token = jwtTokenProvider.createToken(request.getPhone());        // "01099998888"
```

### 7-4. `application.properties` 프로퍼티명 불일치

**증상:** 서버 기동 시 `IllegalArgumentException: Cannot resolve placeholder 'jwt.access-token-validity'` 발생.

**원인:** `JwtTokenProvider` 생성자에서 `${jwt.access-token-validity}`를 주입받는데, `application.properties`에는 `jwt.expiration_time`으로 정의되어 있었음.

**해결:** `application.properties`에서 `jwt.expiration_time` → `jwt.access-token-validity`로 변경.

### 7-5. `CustomUserDetail` phone-only 생성자 NPE

**증상:** `getAuthentication()`에서 `new CustomUserDetail(phone)` 생성자를 사용할 때 `user` 필드가 null인데, `getUsername()`이 `user.getName()`을 호출하여 `NullPointerException` 발생.

**해결:** `user`가 null인지 체크하여 phone-only 생성자 사용 시 `getUsername()`이 phone을 반환하도록 수정.
