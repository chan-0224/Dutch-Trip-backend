# DutchTrip 개발 기록

## 2026-05-04

### CI/CD 세팅 완료
- GitHub Actions CI (`ci.yml`): push 시 JDK 21 + `./gradlew build -x test`
- GitHub Actions CD (`cd.yml`): main push 시 SSH → `git reset --hard` → `bootJar` → `nohup java -jar`
- Mac Mini 홈서버에 SSH 키 등록 (GitHub Secrets)
- Duck DNS(`dutchtrip.duckdns.org`) + Let's Encrypt SSL + Nginx 리버스 프록시 설정
- Branch Protection 설정 (main 직접 push 금지, PR 필수)

---

## 2026-05-04 ~ 05-06

### 백엔드 구현 (이찬영 담당 영역)

#### global 패키지
| 파일 | 설명 |
|------|------|
| `global/common/BaseEntity` | `createdAt`, `updatedAt` `@MappedSuperclass` 공통 상속 |
| `global/common/ApiResponse` | 모든 API 응답 `{ data, message }` 포맷 통일 |
| `global/config/JpaConfig` | `@EnableJpaAuditing` |
| `global/config/SecurityConfig` | `/api/auth/**` permitAll, 나머지 JWT 인증 필수, Stateless 세션 |
| `global/security/JwtTokenProvider` | JWT 발급/검증 (jjwt 0.12.6), raw 문자열 키 사용 |
| `global/security/JwtAuthFilter` | Authorization 헤더 파싱 → SecurityContext에 userId 저장 |
| `global/exception/ErrorCode` | HTTP 상태코드 + 메시지 매핑 enum |
| `global/exception/CustomException` | ErrorCode 기반 런타임 예외 |
| `global/exception/GlobalExceptionHandler` | `@RestControllerAdvice` 전역 예외 처리 |

#### auth 패키지
| 파일 | 설명 |
|------|------|
| `domain/auth/service/AuthService` | 카카오 인가코드 → 카카오 API 호출 → 유저 조회/자동가입 → JWT 발급 |
| `domain/auth/controller/AuthController` | `POST /api/auth/kakao` |

#### user 패키지
| 파일 | 설명 |
|------|------|
| `domain/user/entity/User` | `kakaoId`, `email`, `nickname`, `bankName`, `accountNumber` |
| `domain/user/repository/UserRepository` | `findByKakaoId()` |
| `domain/user/service/UserService` | 내 정보 조회/수정 |
| `domain/user/controller/UserController` | `GET /api/users/me`, `PUT /api/users/me` |

#### trip 패키지
| 파일 | 설명 |
|------|------|
| `domain/trip/entity/Trip` | `title`, `nation`, `startDate`, `endDate`, `inviteCode` |
| `domain/trip/entity/TripMember` | `trip`, `user`, `role(OWNER/MEMBER)` |
| `domain/trip/service/TripService` | 방 생성 시 UUID 초대코드 자동 발급, 멤버십 검증 |
| `domain/trip/controller/TripController` | `POST /api/trips`, `GET /api/trips`, `GET /api/trips/{id}`, `POST /api/trips/join` |

#### schedule 패키지
| 파일 | 설명 |
|------|------|
| `domain/schedule/entity/Schedule` | `scheduledAt`, `title`, `content` |
| `domain/schedule/service/ScheduleService` | 날짜 필터링: `?date=YYYY-MM-DD` 쿼리 파라미터 |
| `domain/schedule/controller/ScheduleController` | `POST /api/trips/{tripId}/schedules`, `GET /api/trips/{tripId}/schedules` |

#### 의존성 추가
- `io.jsonwebtoken:jjwt-api/impl/jackson:0.12.6`

---

### 트러블슈팅

#### 앱 기동 실패 (2026-05-06)
- **증상**: `nohup java -jar` 실행 시 exit 1로 즉시 종료
- **원인**: `@Value("${kakao.client-id}")` — application.properties에 해당 키 없으면 Bean 생성 실패로 앱 자체가 안 뜸
- **해결**: `@Value("${kakao.client-id:}")` 로 변경 (기본값 빈 문자열)

#### JWT Secret Base64 문제 (2026-05-06)
- **증상**: `.env`에 plain 문자열로 JWT_SECRET 설정 시 앱 실행 오류 가능
- **원인**: `Decoders.BASE64.decode()` 사용 시 `-` 같은 문자가 포함된 plain 문자열은 디코딩 실패
- **해결**: `secretKey.getBytes(UTF_8)` 방식으로 변경 → plain 문자열 그대로 사용 가능

---

## 2026-05-06

### Docker 기반 배포로 전환

**기존**: 서버에서 직접 `./gradlew bootJar` → `nohup java -jar`
**변경**: GitHub Actions에서 Docker 이미지 빌드 → ghcr.io push → 서버에서 `docker pull` → `docker-compose up`

#### 추가된 파일
| 파일 | 설명 |
|------|------|
| `Dockerfile` | 멀티스테이지 빌드 (JDK 빌드 → JRE 실행) |
| `docker-compose.yml` | MySQL, Redis, Spring Boot 앱 통합 관리 |
| `.dockerignore` | 불필요한 파일 Docker 빌드 제외 |
| `.env` | 서버 환경변수 (gitignore 처리됨) |

#### 변경된 CD 흐름
```
main push
  → GitHub Actions: Docker 이미지 빌드 → ghcr.io/chan-0224/dutchtrip:latest push
  → Mac Mini SSH: git reset --hard → docker pull → docker-compose up -d
```

#### 서버 설정 완료
- 기존 `nohup java -jar` 프로세스 종료
- 기존 MySQL, Redis 컨테이너 종료 (docker-compose로 통합 관리)
- `.env` 파일 생성 및 값 설정
- `ghcr.io` 로그인 완료 (`read:packages` 토큰)

---

---

## 2026-05-07

### 배포 파이프라인 안정화

#### 문제 1: SSH 세션에서 docker pull 실패
- **원인**: Mac의 Docker가 인증 정보를 macOS Keychain에 저장하는데, SSH 비대화형 세션에서 Keychain 접근 불가
- **해결**: CD 스크립트에 `echo "$GHCR_TOKEN" | docker login ghcr.io -u "$GHCR_ACTOR" --password-stdin` 추가
- `GHCR_TOKEN` GitHub Secret 등록 필요 (read:packages 권한 PAT)

#### 문제 2: ARM64 플랫폼 불일치
- **원인**: GitHub Actions (ubuntu-latest = x86_64)에서 빌드한 이미지를 Mac Mini (Apple Silicon = ARM64)에서 실행
- **해결**: QEMU + Docker Buildx로 `linux/amd64,linux/arm64` 멀티플랫폼 빌드

#### 문제 3: Docker 빌드 속도 (ARM64 QEMU 에뮬레이션)
- **원인**: Dockerfile 내에서 Gradle 빌드를 QEMU 에뮬레이션으로 실행 → 5분+ 소요
- **해결**: GitHub Actions에서 JAR를 x86 네이티브로 미리 빌드 후, Dockerfile은 JAR 복사만 수행
- `Dockerfile` 멀티스테이지 빌드 → 단일 스테이지로 단순화
- `.dockerignore`에 `!build/libs/*.jar` 예외 추가

#### 문제 4: 포트 충돌 (6379, 3306)
- **원인**: 이전에 수동으로 `docker run`으로 띄운 `dutchtrip-redis`, `dutchtrip-mysql` 컨테이너가 포트 점유
- **해결**: 해당 컨테이너 수동 제거, CD 스크립트에 `docker-compose down` 추가

#### 문제 5: MySQL 유저 권한 없음
- **원인**: `docker-compose.yml`의 MySQL에 `MYSQL_USER`/`MYSQL_PASSWORD` 없어 `dutchtrip` 유저 미생성
- **해결**: `docker-compose.yml` MySQL 환경변수에 `MYSQL_USER`, `MYSQL_PASSWORD` 추가 후 `docker-compose down -v`로 볼륨 재생성

#### 최종 CD 흐름
```
main push
  → GitHub Actions: JDK 21 + ./gradlew bootJar
  → Docker 멀티플랫폼 빌드 (amd64 + arm64) → ghcr.io push
  → Mac Mini SSH: docker login → docker pull → docker-compose down → docker-compose up -d
```

---

---

## 2026-05-09

### 카카오 로그인 연동 준비 완료

#### 카카오 개발자 콘솔 설정
- REST API 키 발급 완료 → `.env`에 `KAKAO_CLIENT_ID` 등록
- Redirect URI 등록:
  - `http://localhost:3000/oauth/kakao` (개발용)
  - `https://dutchtrip.duckdns.org/oauth/kakao` (배포용)

#### .env 업데이트
- `KAKAO_REDIRECT_URI=http://localhost:3000/oauth/kakao` 추가
- 프론트팀 콜백 경로: `localhost:3000/oauth/kakao` 로 협의

#### Homebrew Nginx → Traefik 전환
- **이유**: 맥미니에서 여러 프로젝트 운영 필요, Nginx 수동 관리 불편
- Traefik v3 (Docker 기반) 도입 — 컨테이너 라벨만 붙이면 자동 라우팅
- Docker 소켓 접근 문제(`tecnativa/docker-socket-proxy`로 해결)
  - Docker Desktop 소켓을 컨테이너에서 직접 마운트 불가 → socket-proxy TCP 프록시 경유
  - Traefik v3.0 → v3 (latest) 업그레이드 필요 (v3.0은 Docker API v1.24 사용, Docker Desktop 29.x 최소 v1.44 요구)
- Let's Encrypt SSL 자동 발급/갱신 (`acme.json` 저장)
- `/api/` → Spring Boot (8080) 라우팅 정상 확인
- 새 프로젝트 추가 시 해당 `docker-compose.yml`에 Traefik 라벨만 추가하면 됨

---

---

## 2026-05-17

### API 명세서 v2.0 반영 (PR #19, `feat/api-spec-v2`)

프론트팀 요구사항 반영으로 Notion 명세서가 v2.0으로 업데이트됨. 아래 5가지 변경 적용.

#### auth
| 항목 | 변경 전 | 변경 후 |
|------|---------|---------|
| 로그인 요청 필드 | `code` (인가 코드) | `kakaoAccessToken` (카카오 Access Token 직접 전달) |
| 로그인 응답 | `accessToken` 만 | `accessToken`, `userId`, `nickname`, `is_new_user` 추가 |

- 기존: 클라이언트 → 서버에 인가 코드 전달 → 서버가 카카오 토큰 교환
- 변경: 클라이언트가 카카오 SDK로 Access Token 직접 발급 → 서버로 전달
- `is_new_user`: 프론트 온보딩 화면 분기용 (`true`면 계좌 입력 화면으로 유도)
- `LoginResponse` DTO 신규 추가, `KakaoTokenResponse` 내부 클래스 및 token exchange 로직 제거

#### user
- `PUT /api/users/me` → `PUT /api/users/me/bank-info` (계좌 정보 전용 엔드포인트 분리)
- `UserUpdateRequest`에서 `nickname` 필드 제거 (계좌 정보만 수정)
- `User` 엔티티에 `updateBankInfo()` 메서드 추가

#### trip
- `GET /api/trips` 응답에 `memberCount`, `myRole` 필드 추가 → `TripListResponse` DTO 신규
- `GET /api/trips/{tripId}/members` 신규 API 추가 (영수증 메뉴 선택 UI용 팀원 목록)
  - `TripMemberResponse` DTO 신규, `TripMemberRepository`에 `findAllByTrip()`, `countByTrip()` 추가

#### schedule
- `scheduledAt` (단일 필드) → `scheduleDate` (LocalDate) + `scheduleTime` (LocalDateTime) 분리
- 요청/응답 DTO 모두 변경, 엔티티는 그대로 유지 (`scheduledAt`으로 저장)

---

### Expense/Settlement 도메인 구현 완료 (PR #18, 김태희 담당)

`feat/settlement-expense` 브랜치 코드 리뷰 3회 진행 후 main 머지 완료.

#### 구현 내용
| 도메인 | 주요 내용 |
|--------|-----------|
| Expense | OCR 더미 응답, 지출 등록 (메뉴별 참여자 분담 계산), 지출 목록 상세 조회 |
| Settlement | 최소 이체 횟수 알고리즘 (PriorityQueue 기반 Greedy), 정산 결과 저장/조회 |
| ExpenseItemParticipant | 메뉴별 참여자 매핑 엔티티 신규 추가 |

#### 코드 리뷰 피드백 반영 이력
| 회차 | 내용 |
|------|------|
| 1차 | `EntityManager` 직접 사용 → `TripMemberRepository`로 교체, N+1 쿼리 개선 (`findAllById` 일괄 조회), 주석 코드 삭제 |
| 2차 | `ExpenseService`에 `checkMembership()` 추가 (createExpense, getExpensesByTrip) |
| 3차 | OCR 엔드포인트에도 `checkMembership()` 추가 |

---

### 카카오 REST API 키 재발급

- **원인**: 팀원이 REST API 키를 코드에 노출시켜 재발급 필요
- `.env`의 `KAKAO_CLIENT_ID` 새 키로 업데이트
- `docker-compose down && docker-compose up -d` 재시작으로 반영

---

### API 테스트 확인

- `POST /api/auth/kakao`: 카카오 Access Token → JWT + userId + nickname + is_new_user 정상 응답
- `POST /api/trips`: JWT 인증 후 여행 방 생성, invite_code 자동 발급 정상 동작

---

---

## 2026-05-19 ~ 05-21

### API 명세서 v2.0 → v2.1 반영

#### CORS 핫픽스 (main 직접 push)
- 프론트 연동 테스트 중 CORS 오류 발생
- `SecurityConfig`에 `CorsConfigurationSource` 빈 추가 (`allowedOriginPatterns: *`, credentials: true)

#### 카카오 REST API 키 재발급
- 팀원이 REST API 키 코드에 노출 → 재발급 후 서버 `.env` 업데이트 및 `docker-compose` 재시작

#### 전역 snake_case JSON 직렬화 (JacksonConfig)
- Spring Boot 4.x 기준 `JsonMapperBuilderCustomizer` + `tools.jackson.databind.PropertyNamingStrategies.SNAKE_CASE` 적용
- 기존 DTO의 `@JsonProperty` 어노테이션 불필요해짐 (자동 변환)

#### 카카오 프로필 이미지 추가
- `User` 엔티티에 `profileImageUrl` 컬럼 추가
- `AuthService`: 카카오 `/v2/user/me` 응답의 `kakao_account.profile.profile_image_url` 파싱 및 저장
- `UserResponse`에 `profileImageUrl` 필드 추가

#### DTO 필드명 전체 명세서 동기화
| 파일 | 변경 전 | 변경 후 |
|------|---------|---------|
| `UserResponse` | `id` | `userId` → `user_id` |
| `ScheduleResponse` | `id` | `scheduleId` → `schedule_id` |
| `TripResponse` | `id` | `tripId` → `trip_id` |

#### TripCreateResponse 분리 (POST /trips)
- 명세서 기준 `POST /trips` 응답은 `trip_id`, `title`, `invite_code` 3개만 반환
- `TripCreateResponse` DTO 신규 추가, `TripService.createTrip()` 반환 타입 변경

#### PUT /users/me/bank-info 응답 변경
- 기존: `UserResponse` 전체 반환
- 변경: `{"data": null, "message": "계좌 정보가 성공적으로 업데이트되었습니다."}`

#### ErrorCode 수정
- `INVALID_INVITE_CODE`: `400 BAD_REQUEST` → `404 NOT_FOUND` (명세서 기준)

---

### PR #21 (fix/settlement-expense) 코드 리뷰

#### 리뷰 내용
| 회차 | 내용 |
|------|------|
| 1차 | `RedisCacheConfig`: `tools.jackson.databind.ObjectMapper` 타입 불일치 가능성 지적, `@EnableCaching`만 있고 `@Cacheable` 미적용, `ExpenseMemberRepository` 신규 메서드 미사용 |
| 2차 | `RedisCacheConfig` 기본 생성자로 수정 확인. `SettlementService`: `@Cacheable`+`@Transactional` 조합 주의 사항 전달, `debt.getExpense().getTitle()` N+1 재발생 지적, `@JsonProperty` 중복 지적 |
| CI 빌드 실패 | `new GenericJacksonJsonRedisSerializer()` no-arg 생성자 없음 → `new GenericJacksonJsonRedisSerializer(new ObjectMapper())` 로 수정 필요 |

#### 명세서 v2.1 업데이트 확인
- `related_expenses`: `List<String>` → `List<{expense_title, amount}>` 객체 배열로 변경
- `trip_name` 필드 추가
- 동료 PR 코드가 v2.1 기준으로 맞는 것으로 확인

---

### 기타
- Docker 컨테이너 이름 확인: `dutch-trip-backend-mysql-1`, `dutch-trip-backend-app-1`, `dutch-trip-backend-redis-1`
- DB 테스트 데이터 초기화 (전체 테이블 TRUNCATE)

---

## 남은 작업

### 이찬영
- [x] 도메인 코드 GitHub PR 올리기 (auth, user, trip, schedule)
- [x] 카카오 REST API 키 발급 후 `.env`에 추가
- [x] Nginx → Traefik 전환 (다중 프로젝트 대응)
- [x] API 명세서 v2.0 반영
- [ ] 프론트 배포 시 Traefik 라우팅 추가 (`location /` 라우터)

### 김태희 담당
- [x] Expense 도메인 (OCR, 지출 등록, 지출 목록)
- [x] Settlement 도메인 (최소 이체 알고리즘)
- [ ] Redis 활용
