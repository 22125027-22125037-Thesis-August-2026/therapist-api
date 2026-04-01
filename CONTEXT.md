# Project Context: Therapist Telehealth Platform - Booking Service

## 1. System Overview
This is the **Booking Domain** backend for a microservices-based telehealth platform. It manages doctor schedules, appointments, and secure video telehealth handshakes. It prioritizes data integrity, asynchronous event-driven processing, and strict clinical compliance.

## 2. Tech Stack & Standards
* **Framework:** Java 17+, Spring Boot
* **Database:** PostgreSQL (Spring Data JPA)
* **Message Broker:** RabbitMQ (Spring AMQP)
* **Security:** Stateless JWT for APIs, Stateful Refresh Tokens in Redis.
* **Architecture:** Domain-Driven Design, Controller-Service-Repository pattern.
* **Strict Standards:**
  * ALL timestamps must be handled, saved, and processed in **UTC** using `java.time.Instant`.
  * ALL Primary Keys are auto-generated **UUIDs**.
  * NEVER expose JPA Entities directly to the web layer; always use DTOs (Java Records preferred).

## 3. Database Schema (Core Entities)
* **Therapists:** Professional details (ID, name, specialization).
* **Weekly_Templates:** Defines recurring availability (day of week, start/end time).
* **Schedule_Slots:** Generated instances of availability.
  * Fields: `id` (UUID), `therapist_id`, `start_datetime`, `end_datetime`, `is_booked` (boolean).
* **Appointments:** The transactional booking.
  * Fields: `id` (UUID), `profile_id` (patient), `therapist_id`, `slot_id` (Unique FK), `mode` (VIDEO/TEXT), `status` (UPCOMING, IN_PROGRESS, COMPLETED, CANCELLED), `meeting_link`.
* **Clinical_Notes:** Post-session medical records linked to an appointment.

## 4. Core Business Logic & Constraints
* **Slot Generation:** Slots are generated asynchronously via cron jobs based on weekly templates.
* **Double-Booking Prevention:** Handled purely via atomic database locks. When booking, the system MUST execute a native/JPQL update: `UPDATE schedule_slots SET is_booked = true WHERE id = :id AND is_booked = false`.
* **Lead Time:** Patients can only book slots where `start_datetime` is at least 12 hours in the future.
* **Event-Driven Offloading:** Upon successful booking, the critical path immediately returns `201 Created`. The system publishes an `appointment.booked` event to RabbitMQ for background workers (emails, analytics) to consume.
* **Video Handshake Security:** The endpoint to fetch the Video SDK meeting token (`GET /appointments/{id}/join`) will ONLY return a valid token if the current UTC time is exactly within 10 minutes of the appointment's `start_datetime`.

## 5. State Machine (Appointments)
* `UPCOMING` (Default on creation)
* `IN_PROGRESS` (Triggered when a user successfully requests the video join token)
* `COMPLETED` (Triggered manually when the Therapist submits `Clinical_Notes`)
* `CANCELLED` (Triggers the `Schedule_Slot.is_booked` flag to reset to `false`)