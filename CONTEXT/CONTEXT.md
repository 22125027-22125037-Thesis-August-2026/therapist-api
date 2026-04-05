# Therapist API - Master System Prompt

## 1. Purpose and Scope
This document is the canonical architecture and business-rules reference for AI-assisted development in this repository.

Primary goals:
- Keep implementations aligned to the target microservice architecture.
- Prevent regressions in booking concurrency, scheduling logic, and appointment state transitions.
- Ensure consistent API and domain modeling decisions across future phases.

## 2. Runtime and Infrastructure Baseline

### 2.1 Service Runtime
- Language and framework: Java 17 + Spring Boot.
- Build tool: Gradle.
- Local app port: `8080`.
- Database: PostgreSQL 15 (containerized).
- Broker: RabbitMQ 3 (containerized).

### 2.2 Container Ports (Local Dev)
- PostgreSQL container `5432` -> host `5434`.
- RabbitMQ AMQP `5672` -> host `5673`.
- RabbitMQ management UI `15672` -> host `15673`.

## 3. Domain Boundary Separation (Strict)
The platform is split into independent bounded contexts. Do not collapse these domains into one shared model.

- Auth Domain:
    Owns identity lifecycle, login, refresh token rotation, and role/permission issuance.
- AI and Context Domain:
    Owns AI prompt context, recommendations, semantic history, and contextual reasoning assets.
- Booking Domain:
    Owns therapist profiles for booking use, schedule generation, availability querying, appointment lifecycle, and clinical sign-off linkage.
- Tracking Domain:
    Owns progress tracking, treatment milestones, session adherence, and analytics timelines.
- Social Domain:
    Owns patient community features, interactions, posts, moderation hooks, and social engagement records.

Rules:
- Cross-domain communication should use APIs or events, not direct table coupling.
- Booking Domain must remain independently deployable and evolvable.

## 4. Booking Database and ERD Contract

### 4.1 Booking Tables (Target Schema)
The Booking Domain schema consists of these core tables:

- `therapists`
- `schedule_slots`
- `appointments`
- `weekly_templates`
- `clinical_notes`
- `reviews`

### 4.2 Table Responsibilities
- `therapists`:
    Stores therapist identity for booking context, professional metadata, and availability ownership.
- `weekly_templates`:
    Stores recurring weekly availability definitions per therapist (day-of-week, time windows, activation flags, timezone intent).
- `schedule_slots`:
    Stores generated, bookable slot instances with exact timestamps and booking lock state.
- `appointments`:
    Stores patient-to-slot booking records, lifecycle status, and meeting/session metadata.
- `clinical_notes`:
    Stores therapist-submitted session outcomes, diagnosis notes, recommendations, and completion evidence.
- `reviews`:
    Stores post-appointment patient feedback linked to appointments, including rating and comment.

### 4.3 Core Relationships (Conceptual)
- One therapist -> many weekly templates.
- One therapist -> many schedule slots.
- One schedule slot -> zero or one appointment.
- One appointment -> zero or one clinical note.
- One appointment -> zero or one review.

Strict cross-microservice modeling rule:
- Cross-database references (like `account_id` in `therapists` and `profile_id` in `appointments` pointing to the Auth DB) MUST be modeled as simple UUID fields in Java.
- Do NOT use JPA `@ManyToOne` or `@JoinColumn` for cross-microservice relationships.

### 4.4 Concurrency-Critical Booking Lock
Double-booking prevention is mandatory.

Booking must rely on a native atomic SQL update against `schedule_slots`:

```sql
UPDATE schedule_slots
SET is_booked = true
WHERE slot_id = :slotId
    AND is_booked = false;
```

Interpretation rule:
- If affected row count = `1`, lock succeeded and booking flow may continue.
- If affected row count = `0`, slot was already locked/booked and flow must fail with conflict semantics.

## 5. High-Level Architecture

### 5.1 Event-Driven Integration (RabbitMQ)
Booking operations publish domain events for downstream services.

- Required pattern:
    Command succeeds locally -> persist state -> publish event.
- Primary event example:
    `appointment.booked`
- Event purpose:
    Notify decoupled consumers (notifications, analytics, tracking, etc.) without blocking booking response latency.

Guidelines:
- Producer must not depend on consumer availability.
- Failure handling should be explicit (retry/outbox policy in future hardening).

### 5.2 Hybrid Security Model (Stateful and Stateless)
Security architecture combines stateless API access with stateful session continuity.

- Stateless layer:
    Short-lived JWT access tokens authenticate API calls.
- Stateful layer:
    Long-lived refresh tokens stored in Redis enable silent token rotation and session management.

Expected behavior:
- Access tokens expire quickly and are never treated as durable sessions.
- Refresh token rotation should invalidate old refresh artifacts to reduce replay risk.
- Redis acts as the revocation and session continuity control plane.

### 5.3 WebRTC P2P Video Flow (Backend as Gatekeeper)
The backend does not proxy media streams.

- Media path:
    Client-to-client P2P via WebRTC or provider-managed transport.
- Backend role:
    Validate appointment/time policy, then mint/return temporary auth token for the Video Cloud provider.
- Security intent:
    Only authorized, time-valid participants receive temporary join credentials.

## 6. Strict Business Rules (Non-Negotiable)

### 6.1 Automated Slot Generation
- A scheduled job runs every Sunday at `02:00`.
- Spring mechanism: `@Scheduled` cron-based task.
- Function:
    Read active `weekly_templates` and generate `30` days of future `schedule_slots`.
- Idempotency expectation:
    Generation must avoid duplicate slots for the same therapist/time window.
- Secondary cleanup rule:
    A monthly cron job deletes unbooked `schedule_slots` older than `30` days to keep the database lightweight.

### 6.2 Time and Policy Rules
- Booking lead-time rule:
    A slot must be booked at least `12` hours before `start_datetime`.
- Cancellation rule:
    Appointment cancellation is only allowed at least `24` hours before `start_datetime`.
- Time storage rule:
    All timestamps persisted in DB are UTC.
- Validation zone rule:
    Business cutoffs are validated against `Asia/Ho_Chi_Minh` (`UTC+7`) semantics.

### 6.3 Appointment State Machine
Allowed transition flow:

`UPCOMING` -> `IN_PROGRESS` -> `COMPLETED`

Transition triggers:
- `UPCOMING` -> `IN_PROGRESS`:
    Triggered by successful video handshake/join authorization.
- `IN_PROGRESS` -> `COMPLETED`:
    Triggered manually when therapist submits `clinical_notes`.

No implicit shortcuts:
- Do not transition directly from `UPCOMING` to `COMPLETED`.

### 6.4 Video Handshake Time Lock
- Join endpoint returns a temporary video token only when current time is within `10` minutes of `start_datetime`.
- Requests outside that window must be rejected with a policy error.

## 7. Booking Domain API Specifications
All endpoints below are Booking Domain contracts to preserve during implementation and refactoring.

### 7.1 Therapist Profile Management
- `GET /therapists/{id}`
    Returns therapist profile details for booking context.

Expected concerns:
- Existence validation (`404` for missing therapist).
- Read-only exposure of profile fields needed by client booking flows.

### 7.2 Schedule Availability Management
- `GET /therapists/{id}/slots`
    Returns therapist slot availability (future and filterable).
- `POST /therapists/{id}/templates`
    Creates or updates recurring availability templates used by scheduled generation.

Expected concerns:
- Timezone-safe request validation.
- Pagination/filtering for slot queries.
- Template conflict checks and deterministic generation behavior.

### 7.3 Appointment Management
- `GET /appointments`
    Lists appointments for authorized principal scope (patient or therapist according to role).
- `PATCH /status`
    Performs controlled appointment status updates according to state-machine rules.

Expected concerns:
- Role-aware data filtering.
- Strict enforcement of allowed state transitions.
- Auditability of status changes.

### 7.4 Clinical Notes Management
- `POST /notes`
    Submits therapist clinical notes for an appointment and triggers completion transition.

Expected concerns:
- Therapist-only authorization.
- Appointment must be in `IN_PROGRESS` before note submission.
- Successful note creation must transition appointment to `COMPLETED`.

## 8. Error and Contract Enforcement Principles
- Use explicit domain exceptions for policy violations (booking window, join window, transition violations).
- Return stable HTTP semantics (`400`, `403`, `404`, `409`) mapped to clear machine-readable payloads.
- Validate DTOs at controller boundary; enforce state and policy rules at service boundary.

## 9. Phase 10 Implementation Focus
Current phase objective: complete Booking Domain model parity with ERD.

Deliverables:
- Implement JPA entities for `Therapist`, `WeeklyTemplate`, and `ClinicalNote`.
- Wire relationships with existing `ScheduleSlot` and `Appointment` entities.
- Preserve atomic booking lock behavior in repository/service paths.
- Keep API behavior and state-machine rules aligned with this document.

Required `Therapist` fields:
- `therapist_id`
- `account_id`
- `full_name`
- `specialization`
- `country`
- `years_experience`
- `about_me`
- `rating_avg`
- `license_url`

Required `WeeklyTemplate` fields:
- `template_id`
- `therapist_id`
- `day_of_week`
- `start_time`
- `end_time`
- `is_active`

Required `ClinicalNote` fields:
- `note_id` (PK)
- `appt_id` (FK)
- `diagnosis`
- `recommendations`
- `created_at`

Completion definition for Phase 10:
- Schema reflects all six Booking tables.
- Entity relationships support upcoming APIs without breaking current booking critical path.

## 10. Post-Phase 10 Roadmap

### 10.1 Phase 11: The Clinical Sign-Off
- Build the `POST /notes` endpoint.
- Implement service logic to save the note and execute the final state transition: `IN_PROGRESS` -> `COMPLETED`.
- Lock it down using `@PreAuthorize("hasRole('ROLE_THERAPIST')")`.

### 10.2 Phase 12: Automated Scheduling
- Build the `@Scheduled` cron job service.
- Implement logic that reads `WeeklyTemplate` and generates `ScheduleSlot` records for the next `30` days, respecting UTC boundaries.

### 10.3 Phase 13: Querying Availability
- Build the `GET /slots` endpoint.
- Implement pagination and filtering so frontend clients receive only future, unbooked slots.

