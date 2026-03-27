# BYB (Build Your Body) — Architecture Document

> Last updated: 2026-03-27

---

## Table of Contents

1. [System Overview](#1-system-overview)
2. [Tech Stack](#2-tech-stack)
3. [Project Structure](#3-project-structure)
4. [Data Model](#4-data-model)
5. [API Endpoints](#5-api-endpoints)
6. [Services Layer](#6-services-layer)
7. [Background Jobs](#7-background-jobs)
8. [Plan Generation Pipeline](#8-plan-generation-pipeline)
9. [Authentication & Security](#9-authentication--security)
10. [External Integrations](#10-external-integrations)
11. [Database Migrations](#11-database-migrations)
12. [Configuration](#12-configuration)
13. [Known Issues / Tech Debt](#13-known-issues--tech-debt)

---

## 1. System Overview

**BYB** is a Spring Boot RESTful microservice that generates personalised workout and diet plans using OpenAI. Users register, submit workout and diet profiles, and request AI-generated plans. Plans are stored in object storage (MinIO/S3) or local disk (testing). An asynchronous queue pipeline handles plan generation with retry/backoff so that OpenAI latency and transient failures do not block HTTP requests.

**Core user journey**:
1. Register / login → receive JWT
2. Submit workout profile
3. Submit diet profile
4. Request plan generation → queue entry created
5. Background pipeline generates plans via OpenAI → stores plans → updates profiles
6. Retrieve current week or food catalog via GET endpoints

---

## 2. Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 17 |
| Framework | Spring Boot 3.2.0 |
| Build | Maven 3.9+ |
| Database (prod/beta) | PostgreSQL |
| Database (test) | H2 in-memory |
| Migrations | Flyway |
| ORM | Spring Data JPA / Hibernate |
| Connection pool | HikariCP |
| Security | Spring Security + JWT (jjwt 0.11.5, HMAC-SHA256) |
| Reactive | Project Reactor (Mono/Flux via Spring WebFlux) |
| HTTP client | WebClient + RestTemplate |
| Object storage | AWS SDK S3 Client 2.20.26 pointed at MinIO |
| AI | OpenAI ChatGPT API (gpt-3.5-turbo-1106 / gpt-4) |
| Billing | RevenueCat (webhooks) |
| Scheduling | Spring `@Scheduled` + `@EnableScheduling` |
| Serialisation | Jackson (SNAKE_CASE, NON_NULL) |
| Testing | JUnit 5, Spring Test, Reactor Test, Spring Security Test |
| Deployment | Render (PaaS) |

---

## 3. Project Structure

```
src/main/java/com/workoutplanner/
├── WorkoutAiServiceApplication.java      Main entry point (@EnableScheduling)
├── config/
│   ├── DatabaseConfig.java              Prod-only: parse DATABASE_URL from Render
│   ├── ObjectStorageConfig.java         S3/MinIO client bean
│   ├── PasswordConfig.java              BCryptPasswordEncoder bean
│   ├── RevenueCatConfig.java            @ConfigurationProperties for RevenueCat
│   ├── SchedulerConfig.java             @EnableScheduling
│   ├── SecurityConfig.java              Filter chain, CORS, JWT filter wiring
│   └── WebClientConfig.java            WebClient & RestTemplate beans
├── controller/
│   ├── AdminController.java             Admin operations
│   ├── AuthController.java              Register, login, mobile/social auth
│   ├── BillingController.java           RevenueCat webhook ingestion
│   ├── ConfigController.java            Config info endpoints
│   ├── DatabaseHealthController.java    DB diagnostics
│   ├── DiagnosticController.java        System diagnostics
│   ├── DietController.java              Diet profile CRUD + plan endpoints
│   ├── MinIODebugController.java        MinIO debug
│   ├── MinIODiagnosticController.java   MinIO diagnostics
│   ├── MinIOUploadTestController.java   MinIO upload tests
│   ├── ObjectStorageController.java     Direct storage operations
│   ├── PlanController.java             Combined plan generate/retrieve
│   ├── QueueMetricsController.java      Queue statistics endpoint
│   ├── SimpleHealthController.java      Health checks
│   ├── TestController.java             Test utilities
│   ├── UserController.java             User management
│   └── WorkoutController.java           Workout profile CRUD + plan endpoints
├── dto/
│   ├── AuthRequest.java                 Login payload
│   ├── AuthResponse.java                Token + user response
│   ├── CombinedPlanResponseDto.java     Unified workout + diet response
│   ├── CurrentWeekResponseDto.java      Extracted weekly plan
│   ├── DietFoodCatalogResponseDto.java  Food/nutrition data
│   ├── DietPlanResponseDto.java         Diet plan structure
│   ├── MobileAuthResponse.java          Mobile-specific auth response
│   ├── MobileLoginRequest.java          Mobile login request
│   ├── OpenAIRequest.java               ChatGPT API request wrapper
│   ├── OpenAIResponse.java              ChatGPT API response wrapper
│   ├── RegisterRequest.java             Registration payload
│   ├── RevenueCatWebhookDto.java        RevenueCat event payload
│   ├── SocialLoginRequest.java          SSO/social auth request
│   └── WorkoutPlanResponseDto.java      Workout plan structure
├── exception/
│   └── GlobalExceptionHandler.java      @RestControllerAdvice error handling
├── job/
│   ├── QueueCleanupJob.java             TASK-BE-016E: Delete terminal rows nightly
│   ├── QueueOrchestrator.java           TASK-BE-016A–D: Claim → execute → persist
│   └── QueueScannerJob.java             TASK-BE-015: Poll queue every 30 s
├── model/
│   ├── BillingEntitlement.java          Subscription status & tiers
│   ├── DietProfile.java                 User diet preferences & goals
│   ├── DietProfileRequest.java          Diet profile request DTO
│   ├── Equipment.java                   Fitness equipment catalog
│   ├── MealLog.java                     Historical meal records
│   ├── PlanGenerationQueue.java         TASK-BE-014: Async queue entry
│   ├── QueueStatus.java                 Enum: PENDING, CLAIMED, COMPLETED, FAILED
│   ├── User.java                        UserDetails impl + core user entity
│   ├── WorkoutLog.java                  Historical workout records
│   └── WorkoutProfile.java              User workout preferences & goals
├── repository/
│   ├── BillingEntitlementRepository.java
│   ├── DietProfileRepository.java
│   ├── PlanGenerationQueueRepository.java
│   ├── UserRepository.java
│   └── WorkoutProfileRepository.java
├── security/
│   └── JwtAuthenticationFilter.java     OncePerRequestFilter: validate Bearer token
└── service/
    ├── BillingEntitlementService.java   Manage subscriptions
    ├── CombinedPlanService.java         Orchestrate plan generation (sync path)
    ├── DietService.java                 Diet profile CRUD
    ├── JwtService.java                  Token generation + validation
    ├── LocalFileStorageService.java     Test: write plans to local FS
    ├── ObjectStorageService.java        Prod: upload plans to MinIO/S3
    ├── OpenAIService.java               ChatGPT integration + plan parsing
    ├── PlanGenerationExecutorService.java  TASK-BE-016B: Execute generation
    ├── PlanParsingService.java          Extract normalised fields from AI response
    ├── PlanPersistenceService.java      TASK-BE-016C: Store plans + update profiles
    ├── PlanScanJobService.java          Scan for stale plans
    ├── PlanValidationService.java       Validate plan schema
    ├── PremiumAccessService.java        Check billing tier feature gates
    ├── QueueClaimService.java           TASK-BE-016A: Atomic claim + lock management
    ├── QueueRetryService.java           TASK-BE-016D: Retry policy + exponential backoff
    ├── StorageService.java              Strategy router: local vs object storage
    ├── UserService.java                 UserDetailsService + user CRUD
    ├── WorkoutPlanGeneratorService.java Workout plan generation logic
    └── WorkoutService.java              Workout profile CRUD

src/main/resources/
├── application.yml                     Base config (CORS, logging, server port)
├── application.properties              Base properties (DB, JWT, storage defaults)
├── application-test.properties         H2, local storage, beta.mode=true, port 8083
├── application-beta.properties         PostgreSQL, MinIO, beta.mode=true
├── application-prod.properties         PostgreSQL, MinIO, beta.mode=true (temp)
└── db/migration/
    ├── V001__create_billing_entitlements_table.sql
    ├── V002__create_workout_log_and_meal_log_tables.sql
    ├── V6__create_test_user.sql
    ├── V7__cleanup_test_environment.sql
    └── V008__create_plan_generation_queue.sql
```

---

## 4. Data Model

### Entity Relationship (summary)

```
User (1) ──── (0..1) WorkoutProfile
User (1) ──── (0..1) DietProfile
User (1) ──── (0..1) BillingEntitlement
User (1) ──── (*) WorkoutLog
User (1) ──── (*) MealLog
User (1) ──── (*) PlanGenerationQueue
```

All child tables have `ON DELETE CASCADE` FK constraints to `users(id)`.

---

### 4.1 User

**Table**: `users`

| Field | Type | Notes |
|---|---|---|
| id | UUID | PK |
| username | VARCHAR | UNIQUE |
| email | VARCHAR | UNIQUE |
| password | VARCHAR | BCrypt hashed |
| role | ENUM | USER, ADMIN, PREMIUM |
| first_name, last_name | VARCHAR | |
| phone_number | VARCHAR | |
| date_of_birth | DATE | |
| is_active | BOOLEAN | |
| email_verified | BOOLEAN | |
| created_at, updated_at, last_login | TIMESTAMP | |
| workout_profile_id, diet_profile_id | BIGINT | FK references |
| reset_token, reset_token_expiry | VARCHAR/TIMESTAMP | Password reset |
| verification_token | VARCHAR | Email verification |

Implements Spring `UserDetails`. `isEnabled()` = `isActive && emailVerified`.

---

### 4.2 WorkoutProfile

**Table**: `workout_profiles`

| Field | Type | Notes |
|---|---|---|
| id | BIGINT | PK |
| user_id | UUID | FK → users(id) CASCADE |
| fitness_level | ENUM | BEGINNER, INTERMEDIATE, ADVANCED, EXPERT |
| workout_frequency | INT | Days per week |
| session_duration | INT | Minutes |
| preferred_workout_types | ARRAY | |
| available_equipment | ARRAY | |
| target_goals | ARRAY | |
| height_cm, weight_kg | DECIMAL | |
| age | INT | |
| gender | ENUM | MALE, FEMALE, OTHER, PREFER_NOT_TO_SAY |
| activity_level | ENUM | SEDENTARY … EXTREMELY_ACTIVE |
| current_plan_storage_key | VARCHAR | Object storage key for current plan |
| current_plan_title | VARCHAR | |
| current_plan_created_at | TIMESTAMP | |
| current_plan_file_size | BIGINT | |
| last_workout | TIMESTAMP | |
| total_workouts_completed | INT | |
| created_at, updated_at | TIMESTAMP | |

Key computed methods: `getBMI()`, `hasCurrentPlan()`, `updateCurrentPlan(...)`, `incrementWorkoutCount()`.

---

### 4.3 DietProfile

**Table**: `diet_profiles`

| Field | Type | Notes |
|---|---|---|
| id | BIGINT | PK |
| user_id | UUID | FK → users(id) CASCADE |
| diet_type | ENUM | OMNIVORE, VEGETARIAN, VEGAN, KETO, PALEO, + 8 more |
| daily_calorie_goal | INT | |
| meals_per_day | INT | |
| dietary_restrictions | ARRAY | |
| disliked_foods | ARRAY | |
| preferred_cuisines | ARRAY | |
| protein_goal_grams, carb_goal_grams, fat_goal_grams, fiber_goal_grams | DECIMAL | |
| height_cm, weight_kg, age, gender, activity_level | (same as WorkoutProfile) | |
| weight_goal | ENUM | LOSE, MAINTAIN, GAIN |
| current_plan_storage_key, etc. | (same pattern as WorkoutProfile) | |
| last_meal_logged | TIMESTAMP | |
| total_meals_logged | INT | |
| created_at, updated_at | TIMESTAMP | |

Key computed methods: `getBMI()`, `calculateBMR()` (Mifflin-St Jeor), `calculateTDEE()`.

---

### 4.4 PlanGenerationQueue

**Table**: `plan_generation_queue`
**Added by**: `V008__create_plan_generation_queue.sql` (TASK-BE-014)

| Field | Type | Notes |
|---|---|---|
| id | VARCHAR(36) | UUID, PK |
| user_id | UUID | FK → users(id) |
| status | VARCHAR | PENDING, CLAIMED, COMPLETED, FAILED |
| attempt_count | INT | Default 0 |
| max_attempts | INT | Default 3 |
| locked_by | VARCHAR | Worker ID holding the lock |
| locked_at | TIMESTAMP | Lock acquisition time |
| scheduled_at | TIMESTAMP | Process no earlier than this time |
| completed_at | TIMESTAMP | Set on COMPLETED |
| failed_at | TIMESTAMP | Set on FAILED |
| error_message | TEXT | Failure reason |
| workout_storage_key | VARCHAR(1000) | Populated on success |
| diet_storage_key | VARCHAR(1000) | Populated on success |
| created_at, updated_at | TIMESTAMP | Auto-managed |

**Indexes**: `status`, `user_id`, `scheduled_at`, `(status, attempt_count)`.

**QueueStatus enum**: `PENDING` → `CLAIMED` → `COMPLETED` or `FAILED` (terminal). Failed rows with remaining attempts are reset back to `PENDING` with a backoff `scheduled_at`.

---

### 4.5 BillingEntitlement

**Table**: `billing_entitlements`

| Field | Type | Notes |
|---|---|---|
| id | UUID | PK |
| user_id | UUID | UNIQUE FK → users(id) CASCADE |
| provider_customer_id | VARCHAR | RevenueCat customer ID, UNIQUE |
| provider_subscription_id | VARCHAR | RevenueCat subscription ID, UNIQUE |
| plan_tier | ENUM | FREE, PREMIUM, PRO |
| subscription_status | ENUM | ACTIVE, CANCELLED, EXPIRED, GRACE_PERIOD, BILLING_ISSUE, PAUSED, PENDING |
| current_period_end | TIMESTAMP | Subscription expiry |
| entitlement_active | BOOLEAN | Cached fast-access flag |
| last_webhook_event | VARCHAR | Latest RevenueCat event type |
| webhook_event_timestamp | TIMESTAMP | |

**Indexes**: `entitlement_active`, `subscription_status`, `current_period_end`, `updated_at`.

---

### 4.6 WorkoutLog & MealLog

**Tables**: `workout_log`, `meal_log`
**Added by**: `V002__create_workout_log_and_meal_log_tables.sql`

Simple audit-trail records. Both have `user_id` FK with CASCADE delete and indexes on `(user_id, date)`.

**WorkoutLog**: `exercise`, `weight` (DECIMAL), `date`.
**MealLog**: `meal_name`, `calories`, `proteins`, `fats`, `carbs`, `date`.

---

## 5. API Endpoints

### 5.1 Auth — `/api/v1/auth/**` (public)

| Method | Path | Description |
|---|---|---|
| POST | `/auth/register` | Register new user, returns JWT |
| POST | `/auth/login` | Login, returns access + refresh tokens |
| POST | `/auth/mobile-login` | Mobile-specific auth flow |
| POST | `/auth/social-login` | SSO / social auth |

---

### 5.2 Plan — `/api/v1/plan/**`

| Method | Path | Auth | Description |
|---|---|---|---|
| POST | `/plan/generate` | (currently permitAll) | Generate combined workout + diet plans |
| GET | `/plan/current-week` | (currently permitAll) | Extract current week from stored plan |
| GET | `/plan/diet-foods` | (currently permitAll) | Diet food catalog |

---

### 5.3 Workout — `/api/v1/workout-plans/**`

| Method | Path | Auth | Description |
|---|---|---|---|
| GET | `/workout-plans/health` | Public | Health check |
| POST | `/workout-plans/generate` | Authenticated | Generate workout plan |
| POST | `/workout-plans/save` | Authenticated | Save workout plan |
| POST | `/workout-plans/generate-and-save` | Authenticated | Generate + save |
| POST | `/workout/profile` | Authenticated | Create/update workout profile |

---

### 5.4 Diet — `/api/v1/diet-plans/**`

| Method | Path | Auth | Description |
|---|---|---|---|
| GET | `/diet-plans/health` | Public | Health check |
| POST | `/diet-plans/generate` | Authenticated | Generate diet plan |
| POST | `/diet-plans/save` | Authenticated | Save diet plan |
| POST | `/diet-plans/generate-and-save` | Authenticated | Generate + save |
| POST | `/diet/profile` | Authenticated | Create/update diet profile |

---

### 5.5 Users — `/api/v1/users/**`

| Method | Path | Auth | Description |
|---|---|---|---|
| GET/PUT | `/users/**` | Authenticated | User profile management |
| ANY | `/users/admin/**` | ADMIN role | Admin-only user operations |

---

### 5.6 Billing — `/api/v1/billing/**`

| Method | Path | Auth | Description |
|---|---|---|---|
| POST | `/billing/webhook` | Public (webhook secret verified) | RevenueCat event ingestion |

---

### 5.7 Operations & Diagnostics

| Method | Path | Auth | Description |
|---|---|---|---|
| GET | `/api/v1/queue/metrics` | Public | Queue stats (pending, claimed, completed, failed counts) |
| GET | `/actuator/health` | Public | Spring Boot health |
| GET | `/actuator/info` | Public | App info |
| GET | `/actuator/metrics` | Public | Micrometer metrics |
| GET | `/api/v1/admin/**` | Public (temp) | Admin ops |
| GET | `/api/v1/config/**` | Varies | Config info |
| GET | `/api/v1/diagnostics/**` | Varies | System diagnostics |
| GET | `/api/v1/storage/**` | Varies | Object storage operations |

---

## 6. Services Layer

### 6.1 UserService

Implements `UserDetailsService` for Spring Security.

- `loadUserByUsername(username)` — Spring Security hook
- `registerUser(user)` — Mono-wrapped async: validate, BCrypt-encode password, generate verification token, save
- `findByUsername/Email/Id()` — Mono-wrapped lookups
- `updateUser(user)`, `updateLastLogin(username)` — Mono-wrapped updates
- `existsByUsername/Email()` — Mono-wrapped checks

---

### 6.2 JwtService

Token generation and validation (HMAC-SHA256).

- `generateToken(userDetails)` — access token (24 h default)
- `generateRefreshToken(userDetails)` — refresh token (7 days)
- `generateTokenWithExpiration(userDetails, ms)` — custom expiry
- `isTokenValid(token, userDetails)` — validates signature + username + expiry
- `extractUsername/Expiration/Claim(token)` — claims extraction
- Key: derived from `jwt.secret` via `Keys.hmacShaKeyFor`

---

### 6.3 OpenAIService

ChatGPT integration. Builds a system + user prompt from profile data, calls the completions endpoint, and parses the raw JSON response.

- `generateCombinedPlans(workoutProfile, dietProfile)` → `CombinedPlanResult`
  - Model: `gpt-3.5-turbo-1106` (test/beta), `gpt-4` (prod)
  - Temperature: 0.7, MaxTokens: 6500
  - Response must contain `WORKOUT_PLAN_JSON` and `DIET_PLAN_JSON` delimiters
  - Throws `RuntimeException` on API error or empty response

System prompt instructs GPT to:
- Return exactly two JSON objects (workout + diet)
- Produce a concise 1-week plan (7 days)
- Use short descriptions (5–10 words)
- No markdown code blocks, no placeholder arrays

---

### 6.4 StorageService

Strategy router for dual storage backends.

- Delegates to `LocalFileStorageService` when `storage.use-local=true`
- Delegates to `ObjectStorageService` when `storage.use-local=false`
- Methods: `storeWorkoutPlan`, `storeDietPlan`, `retrieveWorkoutPlan`, `retrieveDietPlan`, `isUsingLocalStorage`

---

### 6.5 LocalFileStorageService

Active only when `storage.use-local=true` (test profile).

Path structure:
```
{baseStoragePath}/
  workout/{userId}/weeklyplan/week{N}/plan.json
  diet/{userId}/weeklyplan/week{N}/plan.json
```

Uses Jackson with `JavaTimeModule` and `INDENT_OUTPUT` for pretty-printed JSON.

---

### 6.6 ObjectStorageService

Active only when `storage.use-local=false` (beta/prod).

Uses AWS SDK `S3Client` configured for MinIO (path-style access, custom endpoint, static credentials). Calls `ensureBucketExists()` on startup if `minio.auto-create-bucket=true`.

Object key pattern: `/{userId}/month-{n}/workout-plan.json`

---

### 6.7 PlanGenerationExecutorService (TASK-BE-016B)

Executes plan generation for a single claimed queue entry.

- Loads `WorkoutProfile` and `DietProfile` by `userId`
  - Missing profile → `PlanGenerationException(fatal=true)` (non-retryable)
- Calls `OpenAIService.generateCombinedPlans()` → `GenerationResult`
  - OpenAI failure → `PlanGenerationException(fatal=false)` (retryable)
- Returns `GenerationResult` containing raw workout + diet plan maps

---

### 6.8 PlanPersistenceService (TASK-BE-016C)

Persists generated plans to storage and updates profile records. `@Transactional`.

- `persist(entry, workoutPlan, dietPlan)` → `PersistenceResult`
  1. Store both plans via `StorageService`
  2. Update `WorkoutProfile.currentPlanStorageKey`, `currentPlanTitle`, `currentPlanCreatedAt`
  3. Update `DietProfile` with same fields
  4. Return storage keys for both plans
- Bucket selection: `betaMode` flag → workout-plans-beta / workout-plans

---

### 6.9 QueueClaimService (TASK-BE-016A)

Atomic queue claiming with SERIALIZABLE isolation.

- `claimEntry(entryId)` — single-row atomic claim
  - `@Transactional(isolation=SERIALIZABLE, propagation=REQUIRES_NEW)`
  - Increments `attemptCount`, sets `status=CLAIMED`, stamps `lockedBy`/`lockedAt`
- `claimBatch(ids, batchSize)` — attempts up to `batchSize` independent claims
- `recoverStaleLock(entry)` — resets expired `CLAIMED` → `PENDING`
- `markCompleted(entry, workoutKey, dietKey)` — terminal success state
- `markFailed(entry, errorMessage)` — terminal failure state
- `resetForRetry(entry, nextScheduledAt)` — reschedule with backoff

Worker identity: `${queue.worker.id}` (UUID fallback).

---

### 6.10 QueueRetryService (TASK-BE-016D)

Retry policy and exponential backoff.

- `handleFailure(entry, exception)`
  - If `exception.isFatal()` → `markFailed` immediately
  - Else if `attemptCount >= maxAttempts` → `markFailed` (exhausted)
  - Else → `resetForRetry` with computed backoff

Backoff formula:
```
delay = min(baseDelay × 2^(attemptCount - 1), maxDelay)

Attempt 1 → 60 s
Attempt 2 → 120 s
Attempt 3 → 240 s
Attempt 4+ → 3600 s (cap)
```

Config: `queue.retry.base-delay-seconds` (60), `queue.retry.max-delay-seconds` (3600).

---

### 6.11 BillingEntitlementService & PremiumAccessService

- `BillingEntitlementService` — syncs subscription state from RevenueCat webhook events
- `PremiumAccessService` — feature gate checks:
  - `hasFeatureAccess(userId, feature)` — queries active entitlements; fail-closed on error
  - `hasPremiumAccess(userId)` — checks `PREMIUM_WORKOUTS` feature
  - `hasProAccess(userId)` — checks `UNLIMITED_PLANS` feature

---

### 6.12 CombinedPlanService

Orchestrates synchronous plan generation (direct HTTP path, not queue). Calls `OpenAIService`, then `StorageService`, then returns `CombinedPlanResponseDto`.

---

## 7. Background Jobs

### 7.1 QueueScannerJob (TASK-BE-015)

**Schedule**: `@Scheduled(fixedDelayString = "${queue.scanner.fixed-delay-ms:30000}")` → every 30 s

Responsibilities:
1. Query PENDING rows where `scheduledAt <= now` AND `attemptCount < maxAttempts`, ordered by `scheduledAt ASC`, limited to `batchSize` (default 5)
2. Query CLAIMED rows where `lockedAt < (now - lockTimeoutMinutes)` (default 10 min) — stale locks
3. Log counts and emit metrics

Exposes results to `QueueOrchestrator` (shares the same schedule tick).

Config properties:
```properties
queue.scanner.fixed-delay-ms=30000
queue.scanner.batch-size=5
queue.scanner.lock-timeout-minutes=10
queue.scanner.enabled=true
```

---

### 7.2 QueueOrchestrator (TASK-BE-016A–D)

**Schedule**: Same 30 s fixed delay as QueueScannerJob.

Per tick:
1. **Recover stale locks** — for each stale CLAIMED row: `QueueClaimService.recoverStaleLock()`
2. **Find claimable rows** — PENDING batch from scanner
3. **Claim batch** — `QueueClaimService.claimBatch(ids, batchSize)`
4. **Execute** each claimed entry:
   - Call `PlanGenerationExecutorService.execute(entry)` → `GenerationResult`
   - **Success** → `PlanPersistenceService.persist()` → `QueueClaimService.markCompleted()`
   - **Failure** → `QueueRetryService.handleFailure()` → mark failed or reschedule

Errors are caught per entry; one failure does not interrupt the batch.

---

### 7.3 QueueCleanupJob (TASK-BE-016E)

**Schedule**: `@Scheduled(cron = "${queue.cleanup.cron:0 0 0 * * *}")` → daily at midnight

Responsibilities:
- Find COMPLETED + FAILED rows where `updatedAt < (now - retentionDays)` (default 30 days)
- `deleteAll(expired)` — hard delete terminal rows
- Log deletion count

Config properties:
```properties
queue.cleanup.cron=0 0 0 * * *
queue.cleanup.retention-days=30
queue.cleanup.enabled=true
```

---

## 8. Plan Generation Pipeline

### Full Flow

```
HTTP POST /plan/generate
        │
        ▼
PlanController
  └─ (creates PlanGenerationQueue row: status=PENDING, scheduledAt=now)
        │
        ▼  [async — up to 30 s later]
QueueScannerJob (every 30 s)
  └─ finds PENDING rows where scheduledAt <= now, attemptCount < maxAttempts
        │
        ▼
QueueOrchestrator
  ├─ 1. recoverStaleLock() for any expired CLAIMED rows
  ├─ 2. claimBatch() → SERIALIZABLE transaction per entry
  │       status: PENDING → CLAIMED
  │       lockedBy = workerId, lockedAt = now, attemptCount++
  │
  ├─ 3. PlanGenerationExecutorService.execute(entry)
  │       ├─ load WorkoutProfile + DietProfile
  │       └─ OpenAIService.generateCombinedPlans(profiles)
  │               └─ POST https://api.openai.com/v1/chat/completions
  │                   model: gpt-3.5-turbo-1106 / gpt-4
  │                   maxTokens: 6500
  │                   └─ parse WORKOUT_PLAN_JSON + DIET_PLAN_JSON from response
  │
  ├─ SUCCESS path:
  │   PlanPersistenceService.persist(entry, workoutMap, dietMap)
  │     ├─ StorageService.storeWorkoutPlan() → MinIO/local disk
  │     ├─ StorageService.storeDietPlan()    → MinIO/local disk
  │     ├─ WorkoutProfile.currentPlanStorageKey = storageKey
  │     └─ DietProfile.currentPlanStorageKey = storageKey
  │   QueueClaimService.markCompleted(entry, keys)
  │     └─ status: CLAIMED → COMPLETED, completedAt=now
  │
  └─ FAILURE path:
      QueueRetryService.handleFailure(entry, exception)
        ├─ fatal=true  → markFailed (missing profile, no retry)
        ├─ exhausted   → markFailed (attemptCount >= maxAttempts)
        └─ retryable   → resetForRetry(entry, now + backoff)
                          status: CLAIMED → PENDING, scheduledAt=future
                          [loop back to scanner on next tick]

QueueCleanupJob (nightly at midnight)
  └─ delete COMPLETED + FAILED rows older than 30 days
```

### Lock Timeout Recovery

If a worker crashes mid-processing, its row stays `CLAIMED` with a stale `lockedAt`. On the next scan tick, `QueueScannerJob` surfaces rows where `lockedAt < now - 10 min`. `QueueOrchestrator` calls `recoverStaleLock()` → row resets to `PENDING` and re-enters the queue.

---

## 9. Authentication & Security

### JWT Flow

1. Client sends `POST /auth/login` with credentials
2. `AuthController` validates via `AuthenticationManager` (DaoAuthenticationProvider + BCrypt)
3. `JwtService.generateToken()` produces HS256 signed token (24 h)
4. `JwtService.generateRefreshToken()` produces long-lived token (7 days)
5. Client includes `Authorization: Bearer <token>` on subsequent requests
6. `JwtAuthenticationFilter` (OncePerRequestFilter) intercepts each request:
   - Extracts token from header
   - Calls `JwtService.isTokenValid(token, userDetails)`
   - Sets `SecurityContextHolder` if valid

### Security Config

**Beta Mode** (`beta.mode=true`): ALL endpoints are `permitAll()`. Used in all three current profiles (test, beta, prod). This is a known tech-debt item.

**Secure Mode** (`beta.mode=false`):
- Public: `/auth/**`, health endpoints, actuator
- Authenticated: workout/diet plan generation and save endpoints
- Admin role required: `/users/admin/**`
- Stateless sessions (`SessionCreationPolicy.STATELESS`)

**CORS**: AllowedOrigins `*`, all standard methods and headers.

**Headers**: X-Frame-Options DENY (clickjacking protection).

---

## 10. External Integrations

### 10.1 OpenAI

- Endpoint: `https://api.openai.com/v1/chat/completions`
- Auth: Bearer `${OPENAI_API_KEY}`
- Model: `gpt-3.5-turbo-1106` (test/beta), `gpt-4` (prod)
- Called synchronously via RestTemplate
- Max tokens: 6500 (8K context workaround)
- On failure: RuntimeException propagates to queue retry logic

### 10.2 MinIO / S3

- Endpoint: `${MINIO_ENDPOINT}` (e.g., `https://minio-server-c5z5.onrender.com`)
- Auth: `MINIO_ROOT_USER` / `MINIO_ROOT_PASSWORD`
- Region: `${MINIO_REGION:us-east-1}`
- Access style: path-style (required for MinIO)
- Buckets: `workout-plans` (prod), `workout-plans-beta` (beta)
- Auto-create bucket: configurable via `minio.auto-create-bucket`

### 10.3 RevenueCat

- Webhook ingestion at `POST /api/v1/billing/webhook`
- Config: `revenuecat.api-key`, `revenuecat.webhook-secret`
- Environments: `sandbox` (beta), `production` (prod)
- Events update `BillingEntitlement` rows and sync `entitlementActive` flag

### 10.4 Render (Deployment Platform)

- `render.yaml` defines service configuration
- `DatabaseConfig.java` parses `DATABASE_URL` (Render's standard postgres URL format) into individual JDBC properties
- Port from `PORT` environment variable

---

## 11. Database Migrations

Flyway runs automatically on startup in beta and prod profiles. Disabled in test profile (H2 auto-creates from Hibernate DDL).

| Version | File | Description |
|---|---|---|
| V001 | `V001__create_billing_entitlements_table.sql` | `billing_entitlements` table with RevenueCat fields, indexes, FK to users |
| V002 | `V002__create_workout_log_and_meal_log_tables.sql` | `workout_log` + `meal_log` tables with FK + composite indexes |
| V6 | `V6__create_test_user.sql` | Insert default test user |
| V7 | `V7__cleanup_test_environment.sql` | Remove test environment artefacts |
| V008 | `V008__create_plan_generation_queue.sql` | `plan_generation_queue` table (TASK-BE-014) |

**Note**: Version numbering is non-sequential (V001, V002, V6, V7, V008). Flyway handles this but it may cause ordering issues if V003–V005 are ever added. The next safe version is V009.

---

## 12. Configuration

### 12.1 Profiles

| Profile | DB | Storage | Auth | Port | OpenAI model |
|---|---|---|---|---|---|
| `test` | H2 in-memory | Local FS (`./test-storage`) | Disabled (beta.mode=true) | 8083 | gpt-3.5-turbo-1106 |
| `beta` | PostgreSQL | MinIO | Disabled (beta.mode=true) | 8080 | gpt-3.5-turbo-1106 |
| `prod` | PostgreSQL | MinIO | Disabled (beta.mode=true) ⚠️ | 8080 | gpt-4 |

### 12.2 Key Properties

```yaml
# Base (application.yml / application.properties)
server.port: 8080
spring.jackson.property-naming-strategy: SNAKE_CASE
spring.jackson.default-property-inclusion: NON_NULL
management.endpoints.web.exposure.include: health,info,metrics

# JWT
jwt.secret: <env: JWT_SECRET>
jwt.expiration: 86400000       # 24 hours
jwt.refresh-expiration: 604800000  # 7 days

# OpenAI
openai.api.key: <env: OPENAI_API_KEY>
openai.model: gpt-3.5-turbo-1106
openai.api.url: https://api.openai.com/v1/chat/completions

# Storage
storage.use-local: false
storage.local.path: ./test-storage

# MinIO
minio.endpoint: <env: MINIO_ENDPOINT>
minio.bucket-name: workout-plans
minio.region: us-east-1
minio.auto-create-bucket: true

# Queue
queue.scanner.fixed-delay-ms: 30000
queue.scanner.batch-size: 5
queue.scanner.lock-timeout-minutes: 10
queue.retry.base-delay-seconds: 60
queue.retry.max-delay-seconds: 3600
queue.cleanup.cron: 0 0 0 * * *
queue.cleanup.retention-days: 30
queue.worker.id: <UUID auto-generated>

# RevenueCat
revenuecat.api-key: <env: REVENUECAT_API_KEY>
revenuecat.webhook-secret: <env: REVENUECAT_WEBHOOK_SECRET>
revenuecat.environment: production

# Mode
beta.mode: false
```

### 12.3 Required Environment Variables

| Variable | Required for | Notes |
|---|---|---|
| `OPENAI_API_KEY` | All | Plan generation |
| `DATABASE_URL` | beta, prod | PostgreSQL connection string |
| `MINIO_ENDPOINT` | beta, prod | MinIO server URL |
| `MINIO_ROOT_USER` | beta, prod | MinIO access key |
| `MINIO_ROOT_PASSWORD` | beta, prod | MinIO secret key |
| `JWT_SECRET` | All (secure mode) | HMAC signing key |
| `REVENUECAT_API_KEY` | Billing features | RevenueCat API |
| `REVENUECAT_WEBHOOK_SECRET` | Billing webhooks | Webhook verification |
| `SPRING_PROFILES_ACTIVE` | Render/CI | `beta` or `prod` |
| `PORT` | Render | Auto-set by Render |

---

## 13. Known Issues / Tech Debt

### 13.1 `beta.mode=true` in Production Profile

**File**: `application-prod.properties`

`beta.mode=true` disables all authentication in the production profile. This was set to ease testing and has not been removed. **All production endpoints are currently unauthenticated.** Must be set to `false` before a real production launch.

---

### 13.2 `/api/v1/plan/**` Endpoints Are Publicly Accessible

**File**: `SecurityConfig.java`

The plan endpoints (`/api/v1/plan/**`) are listed under `permitAll()` with a comment indicating this is temporary for testing. These should require `authenticated()`.

---

### 13.3 Excluded Tests

Eight test classes are excluded from compilation and execution in `pom.xml` (both `maven-compiler-plugin` and `maven-surefire-plugin`):

- `ExerciseTest`
- `WorkoutPlanTest`
- `UserProfileTest`
- `DietPlanTest`
- `WorkoutPlanControllerTest`
- `OpenAIServiceTest`
- `DietProfileTest`
- `DatabaseConfigTest`

These are excluded because they contain compile errors or depend on removed/renamed code. They should be fixed or deleted.

---

### 13.4 Non-Sequential Flyway Version Numbers

Migrations jump from V002 → V6 → V7 → V008. If migrations V003–V005 are ever needed, they will be applied _before_ V6 by Flyway, which could cause issues if V6/V7 depend on state from the missing versions. The next safe version is **V009**.

---

### 13.5 OpenAI MaxTokens Workaround

**File**: `OpenAIService.java`

`maxTokens` is hardcoded to 6500 as a workaround for the 8K context limit. The proper fix is to use a model with a larger context window (e.g., gpt-4-turbo with 128K context) or to split the prompt.

---

### 13.6 Array Field Queries Disabled

**Files**: `WorkoutProfileRepository.java`, `DietProfileRepository.java`

Repository methods that query array fields (e.g., `preferred_workout_types`, `dietary_restrictions`) use PostgreSQL `ANY()` syntax that requires additional configuration or type casting. These queries are currently commented out.

---

### 13.7 Circular References Permitted

**File**: `application.yml`

`spring.main.allow-circular-references: true` is enabled. This suppresses Spring Boot's default circular dependency detection. The actual circular dependency should be refactored.

---

### 13.8 `beta.mode` Inconsistency — All Profiles Have Auth Disabled

All three profiles (test, beta, prod) currently set `beta.mode=true`. The intended production security posture (`beta.mode=false`) has never been exercised. The JWT filter, `SecurityConfig` secure branch, and `@PreAuthorize` annotations need integration testing before toggling this off.

---

### 13.9 User Entity Lazy Profile Relationships Commented Out

**File**: `User.java` (lines 105–114)

`@OneToOne` lazy fetch relationships between `User` and `WorkoutProfile`/`DietProfile` are commented out. Profile lookups fall back to manual ID-based queries in repositories.

---

*End of document.*
