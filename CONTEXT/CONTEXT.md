# Therapist API - Implementation Context

Last updated: 2026-04-15

## 0. Context Maintenance Rule
Rule: Whenever major changes, new features, or architectural adjustments are made to the codebase, this CONTEXT.md file MUST be updated accordingly to reflect the current implemented reality.

## 1. Purpose
This file describes the current implemented behavior of `therapist-api` (not a future target architecture). Use this as the source of truth when adding features, fixing bugs, or generating code.

## 2. Runtime and Infrastructure

### 2.1 Stack
- Java 17 (Gradle toolchain).
- Spring Boot `4.0.5`.
- Spring modules: Web MVC, Data JPA, Validation, Security, AMQP.
- Flyway for SQL migrations.
- JWT via `jjwt`.

### 2.2 Data and Messaging
- PostgreSQL 15 (via Docker Compose).
- RabbitMQ 3 management image (via Docker Compose).
- Default local ports from current `docker-compose.yml`:
  - Postgres: host `5433` -> container `5432`
  - RabbitMQ AMQP: host `5672` -> container `5672`
  - RabbitMQ management UI: host `15672` -> container `15672`

### 2.3 Spring Config Defaults
- Datasource URL defaults to localhost Postgres with env fallbacks.
- JPA `ddl-auto: validate`.
- Flyway enabled (`baseline-on-migrate: true`, `baseline-version: 0`).
- JWT verification uses RSA public key (`JWT_PUBLIC_KEY` / `jwt.public-key`).
- JWT validation checks `iss`, `aud`, and optional header `kid` from config/env.

## 3. Implemented Domain Scope
This service currently implements Booking-domain capabilities around:
- therapist availability slot querying
- appointment booking
- appointment video join state transition
- clinical notes submission
- reviews and therapist average rating updates
- profile matching preference upsert (service layer)
- therapist compatibility lookup using preference-based filtering (service layer)
- therapist reassignment flow with ACTIVE to INACTIVE transition (service layer)
- active therapist assignment detail lookup by profile ID path with self-or-admin authorization
- scheduled slot generation and cleanup
- booking event publication to RabbitMQ

Cross-domain IDs are UUID primitives where appropriate:
- `therapists.account_id` is treated as Auth-domain reference UUID.
- `appointments.profile_id` is treated as patient profile UUID reference.

## 3.1 Non-Negotiable Domain Boundaries (System-Wide)
Independent bounded contexts in the wider system are:
- Auth Domain
- AI and Context Domain
- Booking Domain
- Tracking Domain
- Social Domain

Rule:
- Cross-domain communication should use APIs or events, not direct table coupling.

Cross-microservice ERD rule:
- Cross-database references MUST be modeled as simple UUID fields in Java.
- Do NOT use JPA `@ManyToOne` or `@JoinColumn` for cross-microservice relationships.
- Current examples in this service: `account_id`, `profile_id` are UUID scalar references, not entity joins.

## 4. Database Model (Implemented)

### 4.1 Core Booking DB Schema Details

#### `therapists` (detailed)
Primary key:
- `therapist_id` UUID PK

Core columns:
- `account_id` UUID NOT NULL (Auth-domain reference UUID; scalar field)
- `full_name` VARCHAR(255) NOT NULL
- `specialization` VARCHAR(255)
- `country` VARCHAR(255)
- `years_experience` INTEGER
- `about_me` TEXT
- `rating_avg` NUMERIC(3,2)
- `license_url` VARCHAR(1024)

Matching-related columns:
- `gender` VARCHAR(50)
- `is_lgbtq_allied` BOOLEAN
- `communication_style` VARCHAR(100)
- `treated_challenges` VARCHAR[]

Referenced by foreign keys:
- `weekly_templates.therapist_id -> therapists.therapist_id`
- `schedule_slots.therapist_id -> therapists.therapist_id`
- `appointments.therapist_id -> therapists.therapist_id`
- `therapist_assignments.therapist_id -> therapists.therapist_id`

#### `profiles_matching_preferences` (logical) / `profiles_preferences` (current physical table)
Note:
- Historical/architectural docs may refer to this table as `profiles_matching_preferences`.
- Current implemented migrations/entities use table name `profiles_preferences`.

Primary key:
- `profile_id` UUID PK (cross-domain Profile/Auth reference UUID)

Columns currently persisted in Booking DB:
- `has_prior_counseling` VARCHAR(50)
- `sexual_orientation` VARCHAR(100)
- `is_lgbtq_priority` BOOLEAN
- `reasons` VARCHAR[] (converted from JSONB in `V4__matching_schema_alignment.sql`)
- `communication_style` VARCHAR(100)
- `last_updated_at` TIMESTAMPTZ NOT NULL DEFAULT NOW()

DDD boundary note:
- Request fields like `age`, `gender`, `mood_levels`, and `self_harm_thought` are consumed by API/service flow but are not stored in this Booking DB table; they are propagated via RabbitMQ integration events for other domains.

#### `therapist_assignments` (detailed)
Primary key:
- `assignment_id` UUID PK

Columns:
- `profile_id` UUID NOT NULL (cross-domain UUID scalar; FK intentionally removed in `V4`)
- `therapist_id` UUID NOT NULL
- `status` VARCHAR(20) NOT NULL
- `assigned_at` TIMESTAMPTZ NOT NULL DEFAULT NOW()
- `unassigned_at` TIMESTAMPTZ

Constraints and indexes:
- FK: `therapist_assignments.therapist_id -> therapists.therapist_id` (ON DELETE RESTRICT)
- CHECK: status in `ACTIVE`, `INACTIVE`, `CHANGED_BY_REQUEST`
- indexes on `profile_id` and `therapist_id`

#### `appointments` (core columns summary)
Primary key:
- `appt_id` UUID PK

Core columns:
- `profile_id` UUID NOT NULL (cross-domain profile reference UUID)
- `therapist_id` UUID NOT NULL (FK -> `therapists.therapist_id`)
- `slot_id` UUID NOT NULL UNIQUE (FK -> `schedule_slots.slot_id`)
- `mode` VARCHAR(50) NOT NULL
- `status` VARCHAR(50) NOT NULL
- `meeting_link` VARCHAR(1024)
- `start_datetime` TIMESTAMPTZ NOT NULL
- `created_at` TIMESTAMPTZ NOT NULL

#### `schedule_slots` (core columns summary)
Primary key:
- `slot_id` UUID PK

Core columns:
- `therapist_id` UUID NOT NULL (FK -> `therapists.therapist_id`)
- `start_datetime` TIMESTAMPTZ NOT NULL
- `end_datetime` TIMESTAMPTZ NOT NULL
- `is_booked` BOOLEAN NOT NULL DEFAULT FALSE

### 4.2 Key Relationships
- One therapist -> many weekly templates.
- One therapist -> many schedule slots.
- One therapist -> many appointments.
- One schedule slot -> zero/one appointment (`appointments.slot_id` unique).
- One appointment -> zero/one clinical note (`clinical_notes.appt_id` unique).
- One appointment -> zero/one review (`reviews.appt_id` unique).
- One profile -> one matching preference (`profiles_matching_preferences`).
- One profile -> many therapist assignment records (`therapist_assignments`).
- One therapist -> many therapist assignment records (`therapist_assignments`).

### 4.3 Concurrency-Safe Slot Lock
Booking uses atomic native SQL in `ScheduleSlotRepository.lockAndBookSlot`:

```sql
UPDATE schedule_slots
SET is_booked = true
WHERE slot_id = :slotId AND is_booked = false;
```

Contract:
- affected rows `1` -> lock success.
- affected rows `0` -> already booked -> conflict error.

## 5. Implemented Business Rules

### 5.1 Booking
- `POST /api/v1/bookings` requires authenticated principal UUID from JWT (`profileId` claim, fallback `sub`).
- Booking loads slot, atomically marks it booked, creates appointment, and publishes `appointment.booked` event.
- Default appointment mode used by booking flow: `VIDEO`.
- New appointments start with status `UPCOMING`.

### 5.2 Video Join
- `GET /api/v1/bookings/{appointmentId}/join`:
  - rejects join if current time is earlier than `start_datetime - 10 minutes` (`403`).
  - transitions `UPCOMING` -> `IN_PROGRESS` on successful join.
  - returns `meetingUrl` and currently mocked `sdkToken`.

### 5.3 Clinical Notes
- `POST /api/v1/notes` is therapist-only (`ROLE_THERAPIST`).
- Appointment must be `IN_PROGRESS`.
- Only one note per appointment.
- On success, note is saved and appointment transitions `IN_PROGRESS` -> `COMPLETED`.

### 5.4 Reviews
- `POST /api/v1/reviews` is patient-only (`ROLE_PATIENT`).
- Patient can review only own appointment (`appointments.profile_id` must match authenticated principal ID from JWT).
- Appointment must be `COMPLETED`.
- Only one review per appointment.
- `therapists.rating_avg` is recalculated from average of all therapist reviews, rounded to 2 decimals.

### 5.5 Scheduled Jobs
- Generate slots: `@Scheduled(cron = "0 0 2 * * SUN", zone = "Asia/Ho_Chi_Minh")`.
  - reads active weekly templates.
  - generates up to next 30 days.
  - converts local template time (`Asia/Ho_Chi_Minh`) to UTC `Instant`.
  - idempotent via existence check on therapist/start/end.
- Cleanup slots: `@Scheduled(cron = "0 0 3 1 * *", zone = "Asia/Ho_Chi_Minh")`.
  - deletes unbooked slots with `end_datetime` older than 30 days.

### 5.6 Matching Service Logic
- `savePreferences(profileId, request)` upserts `profiles_preferences` by `profile_id`.
- After preference upsert, the service computes therapist matches and auto-assigns the top-ranked therapist when at least one match exists.
- `savePreferences(profileId, request)` also publishes cross-domain integration events via RabbitMQ topic exchange `booking.exchange`:
  - `profile.demographics.updated` with demographics payload.
  - `tracking.mood.logged` with mood levels and timestamp.
  - `ai.crisis.alerted` when `self_harm_thought` indicates positive risk ("Có"/"Yes" intent), with `source=INTAKE_FORM`.
- `findMatches(profileId)` loads saved preference, applies therapist filtering by communication style and optional strict LGBTQ allied requirement, then returns therapists ordered by overlap of requested reasons and therapist treated challenges.
- If no therapist is found for the exact requested `communication_style`, matching automatically falls back to style-agnostic filtering while keeping the LGBTQ and reason-overlap ordering logic.
- `assignTherapist(profileId, therapistId)` deactivates any existing `ACTIVE` assignment (`status -> INACTIVE`, `unassigned_at` set) and creates a new `ACTIVE` assignment for the selected therapist.

### 5.7 Active Assignment Lookup
- `GET /api/v1/profiles/{profileId}/assigned-therapist` returns `ACTIVE` therapist assignment details for a requested profile.
- Authorization policy is `self-or-admin`:
  - allowed when JWT principal (`profileId` claim, fallback `sub`) equals path `profileId`
  - allowed when caller has `ROLE_ADMIN`
  - otherwise request is denied with `403 Forbidden`
- If no `ACTIVE` assignment exists for the requested profile, endpoint returns `404 Not Found`.

## 6. API Surface (Current)

### 6.1 Production APIs under `/api/v1`
- `POST /api/v1/bookings`
- `GET /api/v1/bookings/{appointmentId}/join`
- `GET /api/v1/therapists/{id}/slots` (paginated `Pageable`)
- `POST /api/v1/notes`
- `POST /api/v1/reviews`
- `POST /api/v1/matching/preferences`
- `GET /api/v1/matching/therapists`
- `POST /api/v1/matching/assign/{therapistId}`
- `GET /api/v1/profiles/{profileId}/assigned-therapist`

### 6.2 Test/Utility APIs
- `POST /api/v1/test/trigger-generation`
- `POST /api/v1/test/trigger-cleanup`

## 7. Security Model (Current)
- Stateless security (`SessionCreationPolicy.STATELESS`).
- JWT filter verifies RS256 signature with configured RSA public key.
- JWT filter validates configured issuer (`iss`) and audience (`aud`); optional `kid` check is supported.
- JWT filter extracts principal as:
  - `profileId` claim when present (used by booking/matching/review flows)
  - fallback: `sub` claim.
- JWT `role` claim is normalized to Spring authorities (for example `TEEN` -> `ROLE_PATIENT`, `THERAPIST` -> `ROLE_THERAPIST`).
- HTTP rules:
  - `/api/v1/test/**` permit all.
  - `/api/v1/**` authenticated.
- Method security enabled (`@EnableMethodSecurity`) for role checks in controllers.
- `GrantedAuthorityDefaults("")` is used so `hasRole('ROLE_X')` matches literal `ROLE_X` authorities.
- Matching endpoints derive `profileId` from JWT authenticated principal and do not accept `profileId` in request payloads.
- Assignment read endpoint accepts `profileId` in path and enforces `self-or-admin` via method security.

## 7.1 Hybrid Security Model (Required Architecture)
System-wide security intent is hybrid:
- Stateless layer: short-lived JWT access tokens on API calls.
- Stateful layer: long-lived refresh tokens stored in Redis for silent rotation and revocation.

Current implementation status in this repository:
- Stateless JWT access-token handling is implemented.
- Redis-backed refresh-token lifecycle is not yet implemented in this service.

## 7.2 WebRTC Video Flow Intent (Required Architecture)
For video sessions, the backend acts only as an authorization gatekeeper:
- validates appointment state/time-window policies
- maps allowed session limits into provider SDK token claims

Media path intent:
- actual audio/video is client-to-client P2P via WebRTC
- backend does not proxy media streams

## 8. Validation and Error Contracts

### 8.1 Request Validation
- Booking request: `slotId` required.
- Clinical note request:
  - `appointmentId` required
  - `diagnosis` required (non-blank)
  - `recommendations` required (non-blank)
- Review request:
  - `appointmentId` required
  - `rating` required, min 1, max 5
  - `comment` max 1000 chars

### 8.2 Error Mapping
`GlobalExceptionHandler` returns RFC `ProblemDetail` with these status mappings:
- `SlotAlreadyBookedException` -> `409 Conflict`
- `ResourceNotFoundException` -> `404 Not Found`
- `MeetingNotOpenException` -> `403 Forbidden`
- `InvalidAppointmentStateException` -> `409 Conflict`
- `ClinicalNoteAlreadyExistsException` -> `409 Conflict`
- `ReviewAlreadyExistsException` -> `409 Conflict`
- `ReviewNotAllowedException` -> `403 Forbidden`
- validation failures -> `400 Bad Request` with `errors` map
- uncaught exceptions -> `500 Internal Server Error`

## 9. Eventing (RabbitMQ)
- Exchange: `booking.exchange` (topic).
- Queue: `notification.booking.queue`.
- Routing key: `appointment.booked`.
- Event payload: `AppointmentBookedEvent(appointmentId, timestamp)`.
- Additional integration routing keys used by matching workflow:
  - `profile.demographics.updated`
  - `tracking.mood.logged`
  - `ai.crisis.alerted`

### 9.1 Event-Driven Intent (Required Architecture)
- Command succeeds locally -> persist state -> publish event.
- Producer must not depend on consumer availability.
- Failure handling should be explicit (retry/outbox policy in future hardening).

## 10. Test Coverage Snapshot
Implemented integration tests currently cover:
- `ScheduleGenerationServiceIntegrationTest`
  - timezone conversion from `Asia/Ho_Chi_Minh` to UTC
  - idempotent generation behavior
- `ReviewServiceIntegrationTest`
  - review creation
  - duplicate review prevention
  - non-completed appointment rejection
  - therapist average rating update logic
- context startup test

## 11. Known Gaps vs Earlier Target Specs
These items were described in older planning docs but are not currently implemented in code:
- no booking lead-time enforcement (for example 12-hour advance booking rule)
- no cancellation endpoint/rule enforcement
- no `GET /api/v1/therapists/{id}` profile endpoint
- no template create/update endpoint (for example `POST /therapists/{id}/templates`)
- no appointment list endpoint (for example `GET /appointments`)
- no appointment status patch endpoint (for example `PATCH /status`)
- join endpoint does not yet mint a real provider SDK token (returns placeholder token)

## 12. Strict Business Policies (Required Constraints)
The following are required system constraints even where enforcement is still pending in code:
- Booking lead-time rule: A slot must be booked at least 12 hours before `start_datetime`.
- Cancellation rule: Appointment cancellation is only allowed at least 24 hours before `start_datetime`.

When implementing new features, prefer updating this section first so planned behavior and implemented behavior remain clearly separated.
