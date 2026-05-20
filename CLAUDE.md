# DutchTrip Backend - CLAUDE.md

## 프로젝트 개요
**더치트립(DutchTrip)** — 여행 경비 더치페이 정산 앱 (오픈소스 SW 수업 팀 프로젝트)
- OCR로 영수증 인식 후 지출 기록
- 최소 이체 횟수 알고리즘으로 정산
- GitHub: https://github.com/chan-0224/Dutch-Trip-backend

## 역할 분담
- **이찬영 (본인)**: 인프라 + 백엔드 개발
- 김태희: 백엔드, Redis 개발
- 프론트엔드 팀원 3명 별도

## 기술 스택
- **언어/프레임워크**: Java 21, Spring Boot 4.0.6
- **빌드**: Gradle
- **DB**: MySQL 8.0 (Docker)
- **캐시**: Redis (Docker, Alpine)
- **보안**: Spring Security + OAuth2 Client
- **기타**: JPA, Lombok, Validation

## 주요 의존성 (build.gradle)
- spring-boot-starter-data-jpa
- spring-boot-starter-data-redis
- spring-boot-starter-security
- spring-boot-starter-security-oauth2-client
- spring-boot-starter-webmvc
- mysql-connector-j
- lombok
- jjwt-api / jjwt-impl / jjwt-jackson `0.12.6` (JWT 발급/검증)

## 인프라 구성
- **서버**: Mac Mini (홈서버)
- **도메인**: dutchtrip.duckdns.org (Duck DNS, DDNS)
- **SSL**: Let's Encrypt (Traefik 자동 발급/갱신, `acme.json` 저장)
- **리버스 프록시**: Traefik v3 (Docker 기반) — `/api/` → Spring Boot (8080)
- **컨테이너**: Docker + docker-compose (Traefik, socket-proxy, MySQL, Redis, Spring Boot 통합 관리)
- **배포 포트**: 80, 443 (Traefik), 8080 (Spring Boot 내부)
- **환경변수**: 서버의 `.env` 파일로 관리 (gitignore 처리)

### Traefik 구조
- `traefik:v3` — 80/443 포트, Let's Encrypt HTTP Challenge, Docker provider
- `tecnativa/docker-socket-proxy` — Docker 소켓을 TCP로 안전하게 노출 (Docker Desktop 소켓 접근 제한 우회)
- 새 프로젝트 추가 시: 해당 서비스 `docker-compose.yml`에 Traefik 라벨만 추가하면 자동 라우팅
- 프론트 배포 시: app 서비스에 `location /` 라우터 라벨 추가 예정

## CI/CD (GitHub Actions)
- **CI** (`.github/workflows/ci.yml`): push 시 JDK 21 세팅 + `./gradlew build -x test`
- **CD** (`.github/workflows/cd.yml`): main push → JDK 21 세팅 + `./gradlew bootJar` → Docker 멀티플랫폼 이미지 빌드(linux/amd64, linux/arm64) → ghcr.io push → SSH로 Mac Mini 접속 → `docker login` → `docker pull` → `docker-compose down` → `docker-compose up -d`
- SSH key 기반 배포 (GitHub Secrets에 등록)
- 이미지 레지스트리: `ghcr.io/chan-0224/dutchtrip:latest`
- `GHCR_TOKEN` Secret 필요 (read:packages 권한 PAT)

## 보안 주의사항
- `src/main/resources/application.properties` — **gitignore 처리됨** (서버에만 존재)
- `src/main/resources/application.properties.example` — 템플릿 파일 (커밋됨)
- DB 비밀번호, Redis 설정 등 민감 정보는 서버에서 직접 관리

## application.properties 구조
```properties
spring.application.name=dutchtrip

# Database
spring.datasource.url=jdbc:mysql://localhost:3306/dutchtrip?useSSL=false&allowPublicKeyRetrieval=true&characterEncoding=UTF-8&serverTimezone=Asia/Seoul
spring.datasource.username=YOUR_USERNAME
spring.datasource.password=YOUR_PASSWORD
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true

# Redis
spring.data.redis.host=localhost
spring.data.redis.port=6379

# Server
server.port=8080

# Kakao OAuth2
kakao.client-id=YOUR_KAKAO_REST_API_KEY
kakao.redirect-uri=http://localhost:3000/oauth/kakao

# JWT
jwt.secret=YOUR_BASE64_ENCODED_SECRET_KEY_MIN_32BYTES
jwt.expiration=86400000
```

## DB 스키마 (ERD)
ddl-auto=update 사용 → JPA Entity 클래스 작성 시 테이블 자동 생성

### users
| 컬럼 | 타입 | 설명 |
|------|------|------|
| id | integer PK | 사용자 고유 식별자 |
| kakao_id | varchar | 카카오 로그인 연동 고유 식별자 |
| email | varchar | 이메일 주소 |
| nickname | varchar | 서비스 내 닉네임 |
| profile_image_url | varchar | 카카오 프로필 이미지 URL |
| bank_name | varchar | 정산금 수령 은행 |
| account_number | varchar | 정산금 수령 계좌번호 |
| created_at | timestamp | 계정 생성 일시 |
| updated_at | timestamp | 정보 수정 일시 |

### trips
| 컬럼 | 타입 | 설명 |
|------|------|------|
| id | integer PK | 여행 방 고유 식별자 |
| creator_user_id | integer FK→users | 방장 |
| title | varchar | 여행 방 이름 |
| nation | varchar | 목적지 국가 (환율/통화 설정용) |
| start_date | date | 여행 시작일 |
| end_date | date | 여행 종료일 |
| invite_code | varchar UNIQUE | 초대 코드 |
| created_at | timestamp | 생성 일시 |
| updated_at | timestamp | 수정 일시 |

### trip_members
| 컬럼 | 타입 | 설명 |
|------|------|------|
| id | integer PK | 매핑 고유 식별자 |
| trip_id | integer FK→trips | 여행 방 |
| user_id | integer FK→users | 참여 사용자 |
| role | varchar | 권한 (방장/일반 참여자) |

### schedules
| 컬럼 | 타입 | 설명 |
|------|------|------|
| id | integer PK | 일정 고유 식별자 |
| trip_id | integer FK→trips | 여행 방 |
| scheduled_at | datetime | 일정 날짜+시간 (통합) |
| title | varchar | 일정 제목 |
| content | text | 일정 상세 내용 |

### expenses (영수증/지출)
| 컬럼 | 타입 | 설명 |
|------|------|------|
| id | integer PK | 지출 고유 식별자 |
| trip_id | integer FK→trips | 여행 방 |
| payer_user_id | integer FK→users | 선결제한 사용자 |
| title | varchar | 가맹점 이름 (OCR 반영) |
| total_amount | decimal | 총 결제 금액 |
| payment_time | datetime | 실제 결제 일시 |
| expense_type | varchar | 고정금액/추가금액 구분 |
| currency | varchar | 현지 통화 (KRW, THB, USD 등) |
| exchange_rate | decimal | 결제 당일 환율 |
| receipt_image_url | varchar | 영수증 이미지 저장 경로 |
| created_at | timestamp | 등록 일시 |
| deleted_at | timestamp | 삭제 일시 (Soft Delete) |

### expense_items (영수증 내 세부 메뉴)
| 컬럼 | 타입 | 설명 |
|------|------|------|
| id | integer PK | 메뉴 항목 고유 식별자 |
| expense_id | integer FK→expenses | 지출 내역 |
| item_name | varchar | 메뉴/상품 이름 (OCR 반영) |
| unit_price | decimal | 개별 메뉴 가격 |
| quantity | integer | 수량 |

### expense_item_participants (메뉴별 참여자)
| 컬럼 | 타입 | 설명 |
|------|------|------|
| id | integer PK | 매핑 고유 식별자 |
| expense_item_id | integer FK→expense_items | 세부 메뉴 |
| user_id | integer FK→users | 해당 메뉴 분담 사용자 |

### expense_members (지출별 정산 정보)
| 컬럼 | 타입 | 설명 |
|------|------|------|
| id | integer PK | 고유 식별자 |
| expense_id | integer FK→expenses | 지출 내역 |
| user_id | integer FK→users | 관여 사용자 |
| amount_paid | decimal | 실제 결제 금액 (미결제 시 0) |
| amount_owed | decimal | 최종 부담 금액 |

### settlement_transfers (최소 송금 정산 결과)
| 컬럼 | 타입 | 설명 |
|------|------|------|
| id | integer PK | 고유 식별자 |
| trip_id | integer FK→trips | 여행 방 |
| sender_user_id | integer FK→users | 송금자 |
| receiver_user_id | integer FK→users | 수신자 |
| amount_to_send | decimal | 송금 금액 |
| created_at | timestamp | 정산 결과 도출 일시 |

## API 개요

### 1. 👤 유저 및 인증 (Auth & User)
| 메서드 | 엔드포인트 | 설명 |
|--------|-----------|------|
| POST | `/api/auth/kakao` | 카카오 로그인 처리 (토큰 발급, is_new_user 포함) |
| GET | `/api/users/me` | 내 정보 조회 (닉네임, 프로필 이미지, 계좌번호 등) |
| PUT | `/api/users/me/bank-info` | 계좌 정보 등록/변경 |

### 2. ✈️ 여행 방 (Trip)
| 메서드 | 엔드포인트 | 설명 |
|--------|-----------|------|
| POST | `/api/trips` | 여행 방 생성 → trip_id, title, invite_code 반환 |
| GET | `/api/trips` | 내가 참여 중인 여행 방 목록 (홈 화면용) |
| GET | `/api/trips/{tripId}` | 여행 방 상세 정보 조회 |
| GET | `/api/trips/{tripId}/members` | 여행 방 참여자 목록 (영수증 메뉴 선택 UI용) |
| POST | `/api/trips/join` | 초대 코드로 여행 방 참여 (409/404 에러 처리) |

### 3. 🕒 타임라인 - 일정 (Schedule)
| 메서드 | 엔드포인트 | 설명 |
|--------|-----------|------|
| POST | `/api/trips/{tripId}/schedules` | 일정 등록 |
| GET | `/api/trips/{tripId}/schedules` | 전체 일정 목록 조회 (날짜별 필터링 포함) |

### 4. 💰 타임라인 - 지출 (Expense)
| 메서드 | 엔드포인트 | 설명 |
|--------|-----------|------|
| POST | `/api/trips/{tripId}/expenses/ocr` | 영수증 이미지 업로드 + OCR 분석 (가게명, 메뉴 목록 반환) |
| POST | `/api/trips/{tripId}/expenses` | 지출 내역 최종 등록 (선결제자 포함) |
| POST | `/api/expenses/{expenseId}/items` | 세부 메뉴 + 메뉴별 참여자 매핑 저장 |
| GET | `/api/trips/{tripId}/expenses` | 타임라인용 지출 목록 조회 |

### 5. 🧮 정산 (Settlement)
| 메서드 | 엔드포인트 | 설명 |
|--------|-----------|------|
| GET | `/api/trips/{tripId}/settlements` | 최소 송금 횟수 알고리즘 결과 조회 (누가 누구에게 얼마) |

## 패키지 구조

```
com.dutchtrip.dutchtrip
├── domain/
│   ├── auth/        dto, service, controller  ← 완료
│   ├── user/        entity, repository, dto, service, controller  ← 완료
│   ├── trip/        entity, repository, dto, service, controller  ← 완료
│   ├── schedule/    entity, repository, dto, service, controller  ← 완료
│   ├── expense/     entity, repository, dto, service, controller  ← 완료 (김태희)
│   └── settlement/  entity, repository, dto, service, controller  ← 완료 (김태희)
└── global/
    ├── common/      BaseEntity, ApiResponse
    ├── config/      JpaConfig, SecurityConfig, JacksonConfig
    ├── security/    JwtTokenProvider, JwtAuthFilter
    └── exception/   ErrorCode, CustomException, GlobalExceptionHandler
```

## 구현 현황

### 완료 (이찬영 담당)
| 영역 | 주요 파일 | 비고 |
|------|-----------|------|
| global | BaseEntity, ApiResponse, JpaConfig | createdAt/updatedAt 공통 상속, JPA Auditing |
| global | JwtTokenProvider, JwtAuthFilter | JWT 발급/검증, 요청 필터 |
| global | SecurityConfig | `/api/auth/**` permitAll, 나머지 JWT 인증, CORS 설정 |
| global | JacksonConfig | Spring Boot 4.x `JsonMapperBuilderCustomizer`로 전역 snake_case 직렬화 |
| global | ErrorCode, CustomException, GlobalExceptionHandler | |
| auth | AuthService, AuthController | `POST /api/auth/kakao` — 카카오 Access Token → 유저 조회/가입 → JWT 발급, `is_new_user` 포함 |
| user | User entity, UserRepository | `profileImageUrl` 컬럼 포함 |
| user | UserService, UserController | `GET /api/users/me`, `PUT /api/users/me/bank-info` |
| trip | Trip, TripMember entity, TripMemberRole enum | |
| trip | TripService, TripController | `POST /api/trips`(TripCreateResponse), `GET /api/trips`, `GET /api/trips/{id}`, `GET /api/trips/{id}/members`, `POST /api/trips/join` |
| trip | TripCreateResponse | POST /trips 전용 응답 DTO (trip_id, title, invite_code) |
| schedule | Schedule entity | |
| schedule | ScheduleService, ScheduleController | `POST/GET /api/trips/{tripId}/schedules`, 날짜 필터링 포함 |

### 완료 (김태희 담당)
| 영역 | 주요 파일 | 비고 |
|------|-----------|------|
| expense | Expense, ExpenseItem, ExpenseMember, ExpenseItemParticipant entity | |
| expense | ExpenseService, ExpenseController | OCR 더미 응답, 지출 등록/조회 |
| settlement | SettlementTransfer entity | |
| settlement | SettlementService, SettlementController | 최소 이체 알고리즘 (PriorityQueue Greedy) |
| global | RedisCacheConfig | Redis 캐시 설정 (정산 결과 10분 TTL) |

### 인증 방식
- 모든 API: `Authorization: Bearer {JWT}` 헤더로 인증
- `JwtAuthFilter` → SecurityContext에 `userId(Long)` 저장 → `@AuthenticationPrincipal Long userId` 로 컨트롤러에서 꺼냄

---

## TODO

### 이찬영 — 남은 작업
- [x] 도메인 코드 GitHub PR 올리기 (auth, user, trip, schedule)
- [x] 카카오 REST API 키 발급 → `.env`에 `KAKAO_CLIENT_ID`, `KAKAO_REDIRECT_URI` 등록
- [x] 카카오 redirect-uri 등록 완료
- [x] API 명세서 v2.0 → v2.1 반영
- [ ] 프론트 배포 시 Traefik 라우팅 추가 (`location /` 라우터)

### 팀원(김태희) 담당 영역
- [x] Expense 도메인 (OCR, 지출 등록, 지출 목록)
- [x] Settlement 도메인 (최소 이체 알고리즘)
- [x] Redis 캐시 설정 (RedisCacheConfig, 정산 결과 캐싱)
- [ ] `fix/settlement-expense` PR CI 빌드 오류 수정 (`RedisCacheConfig`: `new GenericJacksonJsonRedisSerializer(new ObjectMapper())` 로 변경 필요)

---

## 브랜치 전략
- `main`: 배포 브랜치 (push 시 자동 배포)
- `feat/*`: 기능 개발 브랜치
- Branch Protection: main 직접 push 제한, PR 필수

## 로컬 개발 환경 설정
1. Docker로 MySQL + Redis 실행
2. `application.properties` 직접 생성 (example 참고)
3. `./gradlew bootRun`으로 실행

## 자주 쓰는 명령어
```bash
# 빌드
./gradlew build -x test

# 실행 가능한 JAR 생성
./gradlew bootJar

# 로컬 실행
./gradlew bootRun

# Docker (Mac Mini 서버에서)
docker-compose up -d
```
