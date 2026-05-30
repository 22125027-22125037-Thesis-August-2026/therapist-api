# API Controller Reference

Swagger-style, human-readable API reference for `therapist-api` controllers.

## Base URL

- Local default: `http://localhost:8082`
- Production/staging: use the deployed host for that environment.

## Authentication

Most endpoints require JWT bearer authentication.

- Header: `Authorization: Bearer <access_token>`
- Security rule:
- `/api/v1/test/**` is public (no auth required)
- `/api/v1/**` requires authentication
- Role checks:
- Some endpoints additionally require `ROLE_PATIENT` or `ROLE_THERAPIST`

JWT claim usage in this API:

- Principal ID comes from `profileId` claim, or falls back to JWT `sub`
- Role comes from `role` claim and is normalized to `ROLE_*`

Profile-scoped authorization pattern used in controllers:

- `@PreAuthorize("#profileId.toString() == authentication.name or hasRole('ROLE_ADMIN')")`
- Meaning: allow when caller is the same profile (`self`) or has `ROLE_ADMIN`.

## Common Error Response Format

Errors are returned as RFC 7807 `ProblemDetail` JSON.

```json
{
  "type": "about:blank",
  "title": "Validation Error",
  "status": 400,
  "detail": "Validation failed for one or more request fields",
  "instance": "/api/v1/reviews",
  "errors": {
    "rating": ["rating must be between 1 and 5"]
  }
}
```

Known titles:

- `Booking Conflict` (`409`)
- `Resource Not Found` (`404`)
- `Room Not Open` (`403`)
- `Invalid Appointment State` (`409`)
- `Slot Conflict` (`409`)
- `Clinical Note Conflict` (`409`)
- `Clinical Note Forbidden` (`403`)
- `Review Conflict` (`409`)
- `Review Forbidden` (`403`)
- `Validation Error` (`400`)
- `Internal Server Error` (`500`)

## Conventions

- All IDs are UUIDs.
- Date/time fields are ISO-8601 timestamps in UTC.
- Successful creation endpoints return `201 Created`.
- No-content success endpoints return `204 No Content`.

## Endpoint Summary

| Method | Path | Auth | Role | Description |
| --- | --- | --- | --- | --- |
| POST | `/api/v1/bookings` | Yes | Any authenticated user | Create a booking from an available slot (optional `reason`, `mode`) |
| GET | `/api/v1/bookings/{appointmentId}` | Yes | Owning patient, owning therapist, or `ROLE_ADMIN` | Get full appointment detail |
| GET | `/api/v1/bookings/{appointmentId}/join` | Yes | Any authenticated user | Join a video session |
| POST | `/api/v1/bookings/{appointmentId}/cancel` | Yes | Owning patient, owning therapist, or `ROLE_ADMIN` | Cancel an appointment with a reason and release the slot |
| POST | `/api/v1/bookings/{appointmentId}/confirm` | Yes | Owning therapist or `ROLE_ADMIN` | Confirm a `REQUESTED` booking → `UPCOMING` |
| POST | `/api/v1/bookings/{appointmentId}/reject` | Yes | Owning therapist or `ROLE_ADMIN` | Reject a `REQUESTED` booking → `CANCELLED` and release the slot |
| GET | `/api/v1/profiles/{profileId}/appointments/upcoming` | Yes | `self` or `ROLE_ADMIN` | Get the closest upcoming appointment for a profile |
| GET | `/api/v1/profiles/{profileId}/appointments/history` | Yes | `self` or `ROLE_ADMIN` | Get completed/cancelled appointment history for a profile |
| GET | `/api/v1/profiles/{profileId}/appointments/unreviewed` | Yes | `self` or `ROLE_ADMIN` | Get completed appointments for a profile that have not yet been reviewed |
| GET | `/api/v1/therapists/{id}` | Yes | Any authenticated user | Get therapist detail profile payload |
| GET | `/api/v1/therapists/{id}/slots` | Yes | Any authenticated user | Get pageable future available slots (patient-facing) |
| GET | `/api/v1/therapists/{id}/slots/manage` | Yes | `self` or `ROLE_ADMIN` | Get therapist's own slots with optional `?includeBooked=true` and booking metadata |
| POST | `/api/v1/therapists/{id}/slots` | Yes | `self` or `ROLE_ADMIN` | Create a slot |
| POST | `/api/v1/therapists/{id}/slots:bulk` | Yes | `self` or `ROLE_ADMIN` | Create slots in bulk |
| PUT | `/api/v1/therapists/{id}/slots/{slotId}` | Yes | `self` or `ROLE_ADMIN` | Update an unbooked slot |
| DELETE | `/api/v1/therapists/{id}/slots/{slotId}` | Yes | `self` or `ROLE_ADMIN` | Delete an unbooked slot |
| GET | `/api/v1/therapists/{id}/availability-templates` | Yes | `self` or `ROLE_ADMIN` | List weekly templates |
| POST | `/api/v1/therapists/{id}/availability-templates` | Yes | `self` or `ROLE_ADMIN` | Create a weekly template |
| PUT | `/api/v1/therapists/{id}/availability-templates/{templateId}` | Yes | `self` or `ROLE_ADMIN` | Update a weekly template |
| DELETE | `/api/v1/therapists/{id}/availability-templates/{templateId}` | Yes | `self` or `ROLE_ADMIN` | Delete a weekly template |
| GET | `/api/v1/therapists/{id}/reviews` | Yes | Any authenticated user | Get all reviews for a therapist (newest first) |
| GET | `/api/v1/therapists/{id}/appointments` | Yes | `self` or `ROLE_ADMIN` | Pageable list of the therapist's own appointments, filterable by `status`/`from`/`to` |
| GET | `/api/v1/therapists/{id}/patients` | Yes | `self` or `ROLE_ADMIN` | List patients ever assigned to this therapist (active + historical) |
| GET | `/api/v1/therapists/{id}/dashboard/summary` | Yes | `self` or `ROLE_ADMIN` | KPIs for the therapist dashboard |
| GET | `/api/v1/patients/{profileId}/matching-preferences` | Yes | Active assigned therapist or `ROLE_ADMIN` | Read the patient's matching-form responses |
| GET | `/api/v1/patients/{profileId}/tags` | Yes | Active assigned therapist or `ROLE_ADMIN` | Read patient tags |
| PUT | `/api/v1/patients/{profileId}/tags` | Yes | Active assigned therapist or `ROLE_ADMIN` | Upsert patient tags |
| GET | `/api/v1/patients/{profileId}/risk-level` | Yes | Active assigned therapist or `ROLE_ADMIN` | Read patient risk level |
| PUT | `/api/v1/patients/{profileId}/risk-level` | Yes | Active assigned therapist or `ROLE_ADMIN` | Upsert patient risk level |
| POST | `/api/v1/notes` | Yes | `ROLE_THERAPIST`, `ROLE_ADMIN` | Submit a clinical note (supports `status=DRAFT` or `FINALIZED`) |
| GET | `/api/v1/notes` | Yes | `ROLE_THERAPIST`, `ROLE_ADMIN` | List clinical notes (filterable by `therapistId`, `patientId`, `status`) |
| GET | `/api/v1/notes/{noteId}` | Yes | Owning patient, owning therapist, or `ROLE_ADMIN` | Get a clinical note by id |
| PUT | `/api/v1/notes/{noteId}` | Yes | Owning therapist or `ROLE_ADMIN` | Amend a draft note (cannot edit FINALIZED) |
| POST | `/api/v1/notes/{noteId}/finalize` | Yes | Owning therapist or `ROLE_ADMIN` | Finalize a draft note |
| GET | `/api/v1/notes/appointments/{appointmentId}` | Yes | `ROLE_PATIENT`, `ROLE_THERAPIST`, `ROLE_ADMIN` | Get a clinical note for an appointment |
| POST | `/api/v1/reviews` | Yes | `ROLE_PATIENT` | Submit a therapist review |
| POST | `/api/v1/matching/preferences` | Yes | Any authenticated user | Save profile matching preferences |
| GET | `/api/v1/matching/therapists` | Yes | Any authenticated user | Find therapist matches by preferences |
| POST | `/api/v1/matching/assign/{therapistId}` | Yes | Any authenticated user | Assign therapist to profile |
| GET | `/api/v1/profiles/{profileId}/assigned-therapist` | Yes | `self` or `ROLE_ADMIN` | Get active therapist assignment details for a profile |
| POST | `/api/v1/test/trigger-generation` | No | Public | Trigger schedule generation job |
| POST | `/api/v1/test/trigger-cleanup` | No | Public | Trigger schedule cleanup job |

## Detailed Endpoints

### 1. Create Booking

- Method/Path: `POST /api/v1/bookings`
- Auth: Required
- Description: Books a slot and creates an appointment. New optional fields:
  `reason` (free-text patient-stated reason, max 1000 chars) and `mode`
  (`VIDEO` default, `TEXT` allowed).

Request body:

```json
{
  "slotId": "9b3ea8e0-7eaf-4b9f-a72e-932c9ce0e0d6",
  "reason": "Recurring anxiety around exams",
  "mode": "VIDEO"
}
```

Response `201`:

```json
{
  "appointmentId": "76d7800a-ae23-4f65-9d3d-c9536e2bdf5a",
  "slotId": "9b3ea8e0-7eaf-4b9f-a72e-932c9ce0e0d6",
  "status": "REQUESTED",
  "message": "Booking created successfully"
}
```

Booking lifecycle:

- New bookings start in `REQUESTED`. The slot is immediately locked so it
  cannot be double-booked while the therapist decides.
- The therapist confirms with `POST /api/v1/bookings/{id}/confirm`
  (→ `UPCOMING`) or rejects with `POST /api/v1/bookings/{id}/reject`
  (→ `CANCELLED`, slot released).
- Either party can cancel via `POST /api/v1/bookings/{id}/cancel` subject to
  the cancel-window rule (see below).

Booking time-window rules:

- **Bookings**: `slotId.startDatetime` must be at least **2 hours** in the
  future. Earlier slots are rejected with `409 Invalid Appointment State`
  because they would be auto-rejected by the sweep below within 60 seconds.
- **Confirm / reject**: the therapist can only act on a `REQUESTED` booking
  until **2 hours** before `startDatetime`. Past that cutoff the endpoint
  returns `409 Invalid Appointment State` and the sweep cancels the request.
- **Auto-reject sweep**: a `@Scheduled` job runs every minute (configurable
  via `booking.auto-reject.interval-ms`). It cancels every `REQUESTED`
  appointment whose `startDatetime <= now + 2h`, sets
  `cancellationReason = "Auto-rejected: therapist did not respond before the required decision window."`,
  and releases the slot.
- **Cancel**: while the appointment is `UPCOMING`, neither the patient nor
  the therapist can cancel within **1 hour** of `startDatetime`. `REQUESTED`
  appointments may be cancelled at any time (their own 2-hour deadline already
  applies via the sweep).

Possible errors:

- `400` validation failure (`slotId` missing)
- `404` slot not found
- `409` slot already booked **or** slot starts within the 2-hour booking window
- `401` unauthenticated

### 2. Join Video Session

- Method/Path: `GET /api/v1/bookings/{appointmentId}/join`
- Auth: Required
- Description: Returns meeting URL and token. If appointment status is `UPCOMING`, this call updates it to `IN_PROGRESS`.

Path params:

- `appointmentId` (UUID)

Response `200`:

```json
{
  "meetingUrl": "https://video.example.com/room/abc123",
  "sdkToken": "sdk-token-placeholder"
}
```

Possible errors:

- `403` room not open yet (opens 10 minutes before scheduled time)
- `404` appointment not found
- `401` unauthenticated

### 3. Get Closest Upcoming Appointment

- Method/Path: `GET /api/v1/profiles/{profileId}/appointments/upcoming`
- Auth: Required
- Description: Returns the closest future appointment for the requested profile where status is `UPCOMING`.

Path params:

- `profileId` (UUID profile ID)

Authorization:

- Allowed when JWT principal ID equals path `profileId`.
- Allowed when caller has `ROLE_ADMIN`.
- Otherwise returns `403`.

Response `200` (example):

```json
{
  "appointmentId": "76d7800a-ae23-4f65-9d3d-c9536e2bdf5a",
  "profileId": "76d7800a-ae23-4f65-9d3d-c9536e2bdf5a",
  "therapistId": "5f2afc57-d6e4-4dd4-a2f2-34b2520ff31f",
  "slotId": "9b3ea8e0-7eaf-4b9f-a72e-932c9ce0e0d6",
  "mode": "VIDEO",
  "status": "UPCOMING",
  "startDatetime": "2026-04-20T08:00:00Z",
  "endDatetime": "2026-04-20T08:50:00Z",
  "reason": "Recurring anxiety around exams"
}
```

Possible errors:

- `403` caller is not the same profile and not admin
- `404` no upcoming appointment found for requested profile
- `401` unauthenticated

### 4. Get Completed/Cancelled Appointment History

- Method/Path: `GET /api/v1/profiles/{profileId}/appointments/history`
- Auth: Required
- Description: Returns all appointments for a profile where status is `COMPLETED` or `CANCELLED`, ordered by `startDatetime` descending.

Path params:

- `profileId` (UUID profile ID)

Authorization:

- Allowed when JWT principal ID equals path `profileId`.
- Allowed when caller has `ROLE_ADMIN`.
- Otherwise returns `403`.

Response `200` (example):

```json
[
  {
    "appointmentId": "1d7e6402-f91d-4a8f-8178-ec2ccb281d1f",
    "profileId": "76d7800a-ae23-4f65-9d3d-c9536e2bdf5a",
    "therapistId": "5f2afc57-d6e4-4dd4-a2f2-34b2520ff31f",
    "therapistName": "Dr. Sarah Johnson",
    "therapistSpecialization": "Anxiety & Panic Disorders",
    "location": "United States",
    "slotId": "9b3ea8e0-7eaf-4b9f-a72e-932c9ce0e0d6",
    "mode": "VIDEO",
    "status": "COMPLETED",
    "startDatetime": "2026-03-20T08:00:00Z"
  },
  {
    "appointmentId": "7c4d6a4f-e4d1-41d1-b951-d718fc68f943",
    "profileId": "76d7800a-ae23-4f65-9d3d-c9536e2bdf5a",
    "therapistId": "5f2afc57-d6e4-4dd4-a2f2-34b2520ff31f",
    "therapistName": "Dr. Sarah Johnson",
    "therapistSpecialization": "Anxiety & Panic Disorders",
    "location": "United States",
    "slotId": "f2482f84-85a6-4b7f-9e36-9f4e0f93b689",
    "mode": "VIDEO",
    "status": "CANCELLED",
    "startDatetime": "2026-03-12T08:00:00Z"
  }
]
```

Possible errors:

- `403` caller is not the same profile and not admin
- `401` unauthenticated

### 5. Get Completed Unreviewed Appointments

- Method/Path: `GET /api/v1/profiles/{profileId}/appointments/unreviewed`
- Auth: Required
- Description: Returns all `COMPLETED` appointments for a profile that have no associated review yet, ordered by `startDatetime` descending. Useful for prompting a patient to leave reviews for their past sessions.

Path params:

- `profileId` (UUID profile ID)

Authorization:

- Allowed when JWT principal ID equals path `profileId`.
- Allowed when caller has `ROLE_ADMIN`.
- Otherwise returns `403`.

Response `200` (example):

```json
[
  {
    "appointmentId": "1d7e6402-f91d-4a8f-8178-ec2ccb281d1f",
    "profileId": "76d7800a-ae23-4f65-9d3d-c9536e2bdf5a",
    "therapistId": "5f2afc57-d6e4-4dd4-a2f2-34b2520ff31f",
    "therapistName": "Dr. Sarah Johnson",
    "therapistSpecialization": "Anxiety & Panic Disorders",
    "location": "United States",
    "slotId": "9b3ea8e0-7eaf-4b9f-a72e-932c9ce0e0d6",
    "mode": "VIDEO",
    "status": "COMPLETED",
    "startDatetime": "2026-03-20T08:00:00Z"
  }
]
```

Notes:

- Returns an empty array when the profile has no completed appointments awaiting a review.
- Only `COMPLETED` appointments are considered; `CANCELLED` appointments are excluded.

Possible errors:

- `403` caller is not the same profile and not admin
- `401` unauthenticated

### 6. Get Therapist Detail

- Method/Path: `GET /api/v1/therapists/{id}`
- Auth: Required
- Description: Returns therapist profile details with stats, active weekly working hours, and review list.

Path params:

- `id` (UUID therapist ID)

Response `200` (example):

```json
{
  "id": "5f2afc57-d6e4-4dd4-a2f2-34b2520ff31f",
  "fullName": "Dr. Sarah Johnson",
  "avatarUrl": "",
  "specialty": "Anxiety & Panic Disorders",
  "location": "United States",
  "bio": "Specialized in cognitive behavioral therapy techniques.",
  "stats": {
    "patientCount": 42,
    "yearsOfExperience": 12,
    "averageRating": 4.85,
    "reviewCount": 10
  },
  "workingHours": [
    {
      "dayLabel": "Monday",
      "startTime": "08:00",
      "endTime": "16:00"
    }
  ],
  "reviews": [
    {
      "id": "f4a1ad89-b6df-4867-9fe8-0d01fd3265f5",
      "reviewerName": "Anonymous Patient",
      "reviewerAvatarUrl": null,
      "rating": 5,
      "comment": "Very helpful session",
      "createdAt": "2026-04-15T07:40:03.011Z"
    }
  ]
}
```

Notes:

- `avatarUrl` is currently returned as an empty string because therapist avatar storage is not yet modeled in schema.
- Reviewer identity fields are anonymized in this API (`reviewerName` defaults to `Anonymous Patient`, `reviewerAvatarUrl` is `null`).

Possible errors:

- `404` therapist not found
- `401` unauthenticated

### 7. Get Therapist Available Slots

- Method/Path: `GET /api/v1/therapists/{id}/slots`
- Auth: Required
- Description: Returns future unbooked slots for one therapist, paginated.

Path params:

- `id` (UUID therapist ID)

Query params (Spring Pageable):

- `page` (default `0`)
- `size` (default framework value)
- `sort` (for example `sort=startDatetime,asc`)

Response `200` (example):

```json
{
  "content": [
    {
      "slotId": "7d99dc64-9374-4647-9f06-abf346f074ef",
      "startDatetime": "2026-04-20T08:00:00Z",
      "endDatetime": "2026-04-20T08:50:00Z"
    }
  ],
  "pageable": {},
  "totalPages": 1,
  "totalElements": 1,
  "last": true,
  "size": 20,
  "number": 0,
  "sort": {},
  "numberOfElements": 1,
  "first": true,
  "empty": false
}
```

Possible errors:

- `404` therapist not found
- `401` unauthenticated

### 8. Get Therapist Reviews

- Method/Path: `GET /api/v1/therapists/{id}/reviews`
- Auth: Required
- Description: Returns all reviews for a therapist (across all of their appointments), ordered by `createdAt` descending. Reviewer identity is anonymized.

Path params:

- `id` (UUID therapist ID)

Response `200` (example):

```json
[
  {
    "id": "f4a1ad89-b6df-4867-9fe8-0d01fd3265f5",
    "reviewerName": "Anonymous Patient",
    "reviewerAvatarUrl": null,
    "rating": 5,
    "comment": "Very helpful session",
    "createdAt": "2026-04-15T07:40:03.011Z"
  },
  {
    "id": "8c1ed9b3-7c5f-4e6f-9a01-2b6f4eaa0bf2",
    "reviewerName": "Anonymous Patient",
    "reviewerAvatarUrl": null,
    "rating": 4,
    "comment": "Great listener and very patient.",
    "createdAt": "2026-04-10T09:12:44.512Z"
  }
]
```

Notes:

- Returns an empty array (not `404`) when the therapist exists but has no reviews.
- Reviewer identity fields are anonymized (`reviewerName` defaults to `Anonymous Patient`, `reviewerAvatarUrl` is `null`).

Possible errors:

- `404` therapist not found
- `401` unauthenticated

### 9. Submit Clinical Note

- Method/Path: `POST /api/v1/notes`
- Auth: Required
- Role: `ROLE_THERAPIST`, `ROLE_ADMIN`
- Description: Creates one clinical note for an in-progress appointment and marks appointment as `COMPLETED`. Only the assigned therapist or an admin can submit a note.

Request body:

```json
{
  "appointmentId": "76d7800a-ae23-4f65-9d3d-c9536e2bdf5a",
  "diagnosis": "Moderate anxiety symptoms",
  "recommendations": "Weekly CBT sessions for 8 weeks"
}
```

Response `201`:

```json
{
  "noteId": "2eb65f39-7da4-4ca4-9820-e9c412084d45",
  "appointmentId": "76d7800a-ae23-4f65-9d3d-c9536e2bdf5a",
  "appointmentStatus": "COMPLETED",
  "createdAt": "2026-04-15T07:35:21.913Z",
  "message": "Clinical note submitted successfully"
}
```

Possible errors:

- `400` validation failure
- `403` appointment does not belong to caller
- `404` appointment not found
- `409` appointment not `IN_PROGRESS`
- `409` note already exists for appointment
- `401` unauthenticated

### 10. Get Clinical Note By Appointment

- Method/Path: `GET /api/v1/notes/appointments/{appointmentId}`
- Auth: Required
- Role: `ROLE_PATIENT`, `ROLE_THERAPIST`, `ROLE_ADMIN`
- Description: Returns the clinical note for the appointment if the caller is the appointment patient, therapist, or an admin.

Path params:

- `appointmentId` (UUID)

Response `200`:

```json
{
  "noteId": "2eb65f39-7da4-4ca4-9820-e9c412084d45",
  "appointmentId": "76d7800a-ae23-4f65-9d3d-c9536e2bdf5a",
  "appointmentStatus": "COMPLETED",
  "diagnosis": "Moderate anxiety symptoms",
  "recommendations": "Weekly CBT sessions for 8 weeks",
  "createdAt": "2026-04-15T07:35:21.913Z"
}
```

Possible errors:

- `403` appointment does not belong to caller
- `404` appointment not found
- `404` clinical note not found for appointment
- `401` unauthenticated

### 11. Submit Review

- Method/Path: `POST /api/v1/reviews`
- Auth: Required
- Role: `ROLE_PATIENT`
- Description: Submits one review for a completed appointment owned by the caller.

Request body:

```json
{
  "appointmentId": "76d7800a-ae23-4f65-9d3d-c9536e2bdf5a",
  "rating": 5,
  "comment": "Very helpful session"
}
```

Response `201`:

```json
{
  "reviewId": "f4a1ad89-b6df-4867-9fe8-0d01fd3265f5",
  "appointmentId": "76d7800a-ae23-4f65-9d3d-c9536e2bdf5a",
  "therapistId": "5f2afc57-d6e4-4dd4-a2f2-34b2520ff31f",
  "rating": 5,
  "therapistRatingAvg": 4.67,
  "createdAt": "2026-04-15T07:40:03.011Z",
  "message": "Review submitted successfully"
}
```

Possible errors:

- `400` validation failure (`rating` 1-5, comment <= 1000 chars)
- `403` forbidden role
- `403` appointment does not belong to caller
- `404` appointment not found
- `409` appointment not `COMPLETED`
- `409` review already exists
- `401` unauthenticated

### 12. Save Matching Preferences

- Method/Path: `POST /api/v1/matching/preferences`
- Auth: Required
- Description: Upserts matching preferences for authenticated profile.

Request body:

```json
{
  "has_prior_counseling": "No",
  "gender": "female",
  "age": "22",
  "sexual_orientation": "heterosexual",
  "is_lgbtq_priority": false,
  "self_harm_thought": "No",
  "reasons": ["stress", "sleep"],
  "mood_levels": {
    "anxiety": 3,
    "lossInterest": 2,
    "fatigue": 4
  },
  "communication_style": "empathetic"
}
```

Validation notes:

- `mood_levels` must include exactly `anxiety`, `lossInterest`, `fatigue`

Response `204`:

- No body

Possible errors:

- `400` validation failure
- `401` unauthenticated

### 13. Find Matching Therapists

- Method/Path: `GET /api/v1/matching/therapists`
- Auth: Required
- Description: Returns therapist candidates computed from saved preferences.

Response `200`:

```json
[
  {
    "id": "5f2afc57-d6e4-4dd4-a2f2-34b2520ff31f",
    "full_name": "Nguyen Thi A",
    "specialization": "Anxiety & Stress",
    "match_score": 2,
    "matching_reasons": ["stress", "sleep"],
    "communication_style": "empathetic"
  }
]
```

Possible errors:

- `404` matching preferences not found for caller
- `401` unauthenticated

### 14. Assign Therapist

- Method/Path: `POST /api/v1/matching/assign/{therapistId}`
- Auth: Required
- Description: Assigns selected therapist to caller profile; previous active assignment is closed.

Path params:

- `therapistId` (UUID)

Response `204`:

- No body

Possible errors:

- `404` therapist not found
- `401` unauthenticated

### 15. Get Active Assigned Therapist

- Method/Path: `GET /api/v1/profiles/{profileId}/assigned-therapist`
- Auth: Required
- Description: Returns the `ACTIVE` therapist assignment for the requested profile.

Path params:

- `profileId` (UUID profile ID)

Authorization:

- Allowed when JWT principal ID equals path `profileId`.
- Allowed when caller has `ROLE_ADMIN`.
- Otherwise returns `403`.

Response `200` (example):

```json
{
  "assignmentId": "a6120b6f-0d8a-47b8-b9a8-8df84f4f347d",
  "profileId": "76d7800a-ae23-4f65-9d3d-c9536e2bdf5a",
  "status": "ACTIVE",
  "assignedAt": "2026-04-15T08:10:19.251Z",
  "therapist": {
    "id": "5f2afc57-d6e4-4dd4-a2f2-34b2520ff31f",
    "full_name": "Nguyen Thi A",
    "specialization": "Anxiety & Stress",
    "communication_style": "empathetic"
  }
}
```

Possible errors:

- `403` caller is not the same profile and not admin
- `404` no active assignment found for requested profile
- `401` unauthenticated

Response `403` (example):

```json
{
  "type": "about:blank",
  "title": "Forbidden",
  "status": 403,
  "detail": "Access Denied",
  "instance": "/api/v1/profiles/76d7800a-ae23-4f65-9d3d-c9536e2bdf5a/assigned-therapist"
}
```

### 16. Trigger Slot Generation (Test Endpoint)

- Method/Path: `POST /api/v1/test/trigger-generation`
- Auth: Not required
- Description: Triggers internal schedule generation job.

Response `200`:

```json
{
  "message": "Schedule slot generation triggered"
}
```

### 17. Trigger Slot Cleanup (Test Endpoint)

- Method/Path: `POST /api/v1/test/trigger-cleanup`
- Auth: Not required
- Description: Triggers cleanup of old unbooked slots.

Response `200`:

```json
{
  "message": "Schedule slot cleanup triggered"
}
```

### 18. Get Appointment Detail

- Method/Path: `GET /api/v1/bookings/{appointmentId}`
- Auth: Required
- Authorization: Owning patient, owning therapist, or `ROLE_ADMIN`.

Response `200`:

```json
{
  "appointmentId": "76d7800a-ae23-4f65-9d3d-c9536e2bdf5a",
  "profileId": "76d7800a-ae23-4f65-9d3d-c9536e2bdf5a",
  "patientName": "Alice Nguyen",
  "therapistId": "5f2afc57-d6e4-4dd4-a2f2-34b2520ff31f",
  "therapistName": "Dr. Sarah Johnson",
  "therapistSpecialization": "Anxiety & Panic Disorders",
  "slotId": "9b3ea8e0-7eaf-4b9f-a72e-932c9ce0e0d6",
  "mode": "VIDEO",
  "status": "UPCOMING",
  "startDatetime": "2026-04-20T08:00:00Z",
  "endDatetime": "2026-04-20T08:50:00Z",
  "reason": "Recurring anxiety around exams",
  "createdAt": "2026-04-15T07:20:11.913Z"
}
```

Possible errors: `403`, `404`, `401`.

### 19. Cancel Appointment

- Method/Path: `POST /api/v1/bookings/{appointmentId}/cancel`
- Auth: Required
- Authorization: Owning patient, owning therapist, or `ROLE_ADMIN`.
- Description: Marks the appointment `CANCELLED`, captures `cancellationReason`,
  and releases the slot so it can be re-booked. Allowed in `REQUESTED`,
  `UPCOMING`, or `IN_PROGRESS` states.
- Window rule: `UPCOMING` appointments cannot be cancelled within **1 hour**
  of `startDatetime`. `REQUESTED` appointments have no cancel window because
  the 2-hour auto-reject deadline already applies.

Request body:

```json
{ "reason": "Therapist unavailable due to family emergency" }
```

Response `200`: same shape as #18, with `status: "CANCELLED"`,
`cancellationReason` populated, and `cancelledAt` set.

Possible errors:

- `400` validation failure (reason missing or too long)
- `409` appointment already `COMPLETED` or `CANCELLED`
- `403` caller is not the owning therapist and not admin
- `404` appointment not found

### 20. Confirm / Reject Booking

- Method/Path: `POST /api/v1/bookings/{appointmentId}/confirm`
- Method/Path: `POST /api/v1/bookings/{appointmentId}/reject`
- Auth: Required
- Authorization: Owning therapist or `ROLE_ADMIN`.
- Description: Confirm transitions `REQUESTED → UPCOMING`. Reject transitions
  `REQUESTED → CANCELLED`, captures the optional `reason`, and releases the slot.
- Window rule: only allowed while `startDatetime - now > 2 hours`. Past that
  cutoff the auto-reject sweep cancels the request and these endpoints return
  `409`.

Reject body (optional):

```json
{ "reason": "Outside my clinical specialty" }
```

Response `200`: same shape as #18.

Possible errors:

- `409` appointment is not in `REQUESTED` state
- `403` / `404` / `401`

### 21. List Therapist Appointments

- Method/Path: `GET /api/v1/therapists/{therapistId}/appointments`
- Auth: Required
- Authorization: `self` or `ROLE_ADMIN`.
- Query params: `status` (repeatable), `from` (ISO-8601), `to` (ISO-8601),
  Spring `Pageable` (`page`, `size`, `sort`).

Response `200` returns a Spring `Page<TherapistAppointmentItemDto>`. Each item:

```json
{
  "appointmentId": "76d7800a-ae23-4f65-9d3d-c9536e2bdf5a",
  "profileId": "76d7800a-ae23-4f65-9d3d-c9536e2bdf5a",
  "patientName": "Alice Nguyen",
  "therapistId": "5f2afc57-d6e4-4dd4-a2f2-34b2520ff31f",
  "slotId": "9b3ea8e0-7eaf-4b9f-a72e-932c9ce0e0d6",
  "mode": "VIDEO",
  "status": "UPCOMING",
  "startDatetime": "2026-04-20T08:00:00Z",
  "endDatetime": "2026-04-20T08:50:00Z",
  "reason": "Exam-related anxiety"
}
```

### 22. Therapist-managed Slots

- `GET /api/v1/therapists/{id}/slots/manage?includeBooked=true|false` —
  Spring `Pageable`. When `includeBooked=true`, returns booked slots with
  patient metadata.
- `POST /api/v1/therapists/{id}/slots` — `{ startDatetime, endDatetime }`.
  Validation: `end > start`, `start` in the future, no overlap.
- `POST /api/v1/therapists/{id}/slots:bulk` — `{ slots: [...] }`.
- `PUT /api/v1/therapists/{id}/slots/{slotId}` — update; rejected if the slot
  is booked.
- `DELETE /api/v1/therapists/{id}/slots/{slotId}` — delete; rejected if booked.

Slot response:

```json
{
  "slotId": "7d99dc64-9374-4647-9f06-abf346f074ef",
  "startDatetime": "2026-04-20T08:00:00Z",
  "endDatetime": "2026-04-20T08:50:00Z",
  "isBooked": true,
  "bookedByPatientId": "76d7800a-ae23-4f65-9d3d-c9536e2bdf5a",
  "bookedByPatientName": "Alice Nguyen",
  "appointmentId": "76d7800a-ae23-4f65-9d3d-c9536e2bdf5a"
}
```

Possible errors: `409 Slot Conflict` on overlap or invalid window;
`409 Invalid Appointment State` when mutating a booked slot.

### 23. Weekly Availability Templates

- `GET    /api/v1/therapists/{id}/availability-templates`
- `POST   /api/v1/therapists/{id}/availability-templates`
- `PUT    /api/v1/therapists/{id}/availability-templates/{templateId}`
- `DELETE /api/v1/therapists/{id}/availability-templates/{templateId}`

Request / response body:

```json
{
  "templateId": "5e6f51c4-9eaf-4e1c-8c4e-bda3d2e3fae6",
  "therapistId": "5f2afc57-d6e4-4dd4-a2f2-34b2520ff31f",
  "dayOfWeek": "MONDAY",
  "startTime": "08:00",
  "endTime": "16:00",
  "isActive": true
}
```

### 24. Therapist Patient Roster

- Method/Path: `GET /api/v1/therapists/{therapistId}/patients`
- Auth: Required
- Authorization: `self` or `ROLE_ADMIN`.
- Description: All profiles ever assigned (ACTIVE + historical), newest first.

Response `200`:

```json
[
  {
    "profileId": "76d7800a-ae23-4f65-9d3d-c9536e2bdf5a",
    "patientName": "Alice Nguyen",
    "assignmentStatus": "ACTIVE",
    "assignedAt": "2026-04-15T08:10:19.251Z",
    "riskLevel": "LOW",
    "tags": ["cbt", "exam-anxiety"]
  }
]
```

### 25. Patient Matching Preferences (therapist read)

- Method/Path: `GET /api/v1/patients/{profileId}/matching-preferences`
- Auth: Required
- Authorization: Therapist currently `ACTIVE`-assigned to the patient, or `ROLE_ADMIN`.

Response `200`:

```json
{
  "profileId": "76d7800a-ae23-4f65-9d3d-c9536e2bdf5a",
  "has_prior_counseling": "No",
  "sexual_orientation": "heterosexual",
  "is_lgbtq_priority": false,
  "reasons": ["stress", "sleep"],
  "communication_style": "empathetic",
  "last_updated_at": "2026-04-15T07:40:03.011Z"
}
```

Possible errors: `403` (no active assignment), `404` (no preferences saved).

### 26. Patient Tags + Risk Level

- `GET /api/v1/patients/{profileId}/tags`
- `PUT /api/v1/patients/{profileId}/tags` — body `{ "tags": ["cbt", "exam-anxiety"] }`
- `GET /api/v1/patients/{profileId}/risk-level`
- `PUT /api/v1/patients/{profileId}/risk-level` — body `{ "riskLevel": "LOW" | "MEDIUM" | "HIGH" }`

Authorization: Therapist currently `ACTIVE`-assigned to the patient, or `ROLE_ADMIN`.

Tags response:

```json
{
  "profileId": "76d7800a-ae23-4f65-9d3d-c9536e2bdf5a",
  "tags": ["cbt", "exam-anxiety"],
  "updatedAt": "2026-04-20T09:11:32.014Z",
  "updatedBy": "5f2afc57-d6e4-4dd4-a2f2-34b2520ff31f"
}
```

Risk-level response is the same shape with `riskLevel` instead of `tags`.

### 27. Rich Clinical Notes (SOAP + risk flags + drafts)

`POST /api/v1/notes` now accepts the full SOAP shape plus risk flags and an
optional `status`. Default `status` is `FINALIZED` for back-compat.

Request body:

```json
{
  "appointmentId": "76d7800a-ae23-4f65-9d3d-c9536e2bdf5a",
  "diagnosis": "Moderate anxiety",
  "recommendations": "Weekly CBT for 8 weeks",
  "subjective": "Patient reports trouble sleeping...",
  "objective": "Anxious affect, restlessness observed",
  "assessment": "F41.1 Generalized Anxiety Disorder",
  "plan": "CBT sessions, sleep hygiene exercises",
  "summary": "Anxiety w/ sleep impact — CBT plan",
  "riskSuicidalIdeation": false,
  "riskSelfHarm": false,
  "riskSubstanceUse": false,
  "riskAbuse": false,
  "status": "DRAFT"
}
```

- `status: "DRAFT"` does NOT flip the appointment to `COMPLETED`. Drafts only
  allowed when the appointment is `UPCOMING` or `IN_PROGRESS`.
- `status: "FINALIZED"` keeps the original semantics: appointment must be
  `IN_PROGRESS` and is transitioned to `COMPLETED`.

Lifecycle endpoints:

- `PUT  /api/v1/notes/{noteId}` — amend a draft (FINALIZED notes are read-only).
- `POST /api/v1/notes/{noteId}/finalize` — transition draft → FINALIZED and
  flip the appointment to `COMPLETED` if it is still `IN_PROGRESS`.
- `GET  /api/v1/notes/{noteId}` — fetch by note id.
- `GET  /api/v1/notes/appointments/{appointmentId}` — fetch by appointment.
- `GET  /api/v1/notes?therapistId=&patientId=&status=&page=&size=` — list
  (therapist callers are always scoped to their own notes regardless of the
  `therapistId` query param).

Note detail response:

```json
{
  "noteId": "2eb65f39-7da4-4ca4-9820-e9c412084d45",
  "appointmentId": "76d7800a-ae23-4f65-9d3d-c9536e2bdf5a",
  "profileId": "76d7800a-ae23-4f65-9d3d-c9536e2bdf5a",
  "therapistId": "5f2afc57-d6e4-4dd4-a2f2-34b2520ff31f",
  "appointmentStatus": "COMPLETED",
  "status": "FINALIZED",
  "diagnosis": "Moderate anxiety",
  "recommendations": "Weekly CBT for 8 weeks",
  "subjective": "...",
  "objective": "...",
  "assessment": "...",
  "plan": "...",
  "summary": "Anxiety w/ sleep impact — CBT plan",
  "riskFlags": {
    "suicidalIdeation": false,
    "selfHarm": false,
    "substanceUse": false,
    "abuse": false
  },
  "createdAt": "2026-04-15T07:35:21.913Z",
  "updatedAt": "2026-04-15T07:40:03.011Z"
}
```

### 28. Therapist Dashboard Summary

- Method/Path: `GET /api/v1/therapists/{therapistId}/dashboard/summary`
- Auth: Required
- Authorization: `self` or `ROLE_ADMIN`.

Response `200`:

```json
{
  "activePatientCount": 12,
  "completedThisMonth": 23,
  "averageRating": 4.85,
  "pendingBookingCount": 2,
  "draftNoteCount": 1,
  "moodAlertCount": 0
}
```

`moodAlertCount` is owned by the Tracking Service and is currently returned
as `0` from the therapist-api until the cross-service `/alerts` endpoint is
wired in.

## Appointment status lifecycle

```
REQUESTED ──confirm──▶ UPCOMING ──join──▶ IN_PROGRESS ──submit note (FINALIZED)──▶ COMPLETED
    │                     │                      │
    └──reject──▶ CANCELLED └──cancel──▶ CANCELLED └──cancel──▶ CANCELLED
```

- New patient bookings via `POST /api/v1/bookings` start in `REQUESTED`. The
  therapist must call `/confirm` (→ `UPCOMING`) or `/reject` (→ `CANCELLED`).
- The `/profiles/{id}/appointments/upcoming` endpoint includes `REQUESTED`,
  `UPCOMING`, and `IN_PROGRESS` so the patient's "next appointment" view
  surfaces pending requests too.
- Cancellations (patient or therapist) always release the originating slot so
  it can be re-booked.

Time-window summary (all values configurable via `BookingService` constants):

| Action | Window |
| --- | --- |
| Book a slot (`POST /bookings`) | slot must start `> 2h` from now |
| Therapist confirm / reject | only while `start - now > 2h` |
| Auto-reject sweep | every 60s; cancels any `REQUESTED` whose `start <= now + 2h` |
| Cancel `UPCOMING` (patient or therapist) | only while `start - now > 1h` |
| Cancel `REQUESTED` | any time before auto-reject fires |

## Quick cURL Examples

Create booking:

```bash
curl -X POST "http://localhost:8082/api/v1/bookings" \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"slotId":"9b3ea8e0-7eaf-4b9f-a72e-932c9ce0e0d6"}'
```

Get matching therapists:

```bash
curl "http://localhost:8082/api/v1/matching/therapists" \
  -H "Authorization: Bearer <token>"
```

Get active assigned therapist:

```bash
curl "http://localhost:8082/api/v1/profiles/<profile-id>/assigned-therapist" \
  -H "Authorization: Bearer <token>"
```

Get closest upcoming appointment:

```bash
curl "http://localhost:8082/api/v1/profiles/<profile-id>/appointments/upcoming" \
  -H "Authorization: Bearer <token>"
```

Get completed/cancelled appointment history:

```bash
curl "http://localhost:8082/api/v1/profiles/<profile-id>/appointments/history" \
  -H "Authorization: Bearer <token>"
```

Get completed unreviewed appointments:

```bash
curl "http://localhost:8082/api/v1/profiles/<profile-id>/appointments/unreviewed" \
  -H "Authorization: Bearer <token>"
```

Submit review:

```bash
curl -X POST "http://localhost:8082/api/v1/reviews" \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"appointmentId":"76d7800a-ae23-4f65-9d3d-c9536e2bdf5a","rating":5,"comment":"Very helpful session"}'
```

Get therapist reviews:

```bash
curl "http://localhost:8082/api/v1/therapists/<therapist-id>/reviews" \
  -H "Authorization: Bearer <token>"
```
