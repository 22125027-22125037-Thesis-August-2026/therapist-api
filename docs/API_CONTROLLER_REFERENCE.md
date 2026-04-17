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
- `Clinical Note Conflict` (`409`)
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
| POST | `/api/v1/bookings` | Yes | Any authenticated user | Create a booking from an available slot |
| GET | `/api/v1/bookings/{appointmentId}/join` | Yes | Any authenticated user | Join a video session |
| GET | `/api/v1/profiles/{profileId}/appointments/upcoming` | Yes | `self` or `ROLE_ADMIN` | Get the closest upcoming appointment for a profile |
| GET | `/api/v1/therapists/{id}` | Yes | Any authenticated user | Get therapist detail profile payload |
| GET | `/api/v1/therapists/{id}/slots` | Yes | Any authenticated user | Get pageable future available slots |
| POST | `/api/v1/notes` | Yes | `ROLE_THERAPIST` | Submit a clinical note |
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
- Description: Books a slot and creates a video appointment.

Request body:

```json
{
  "slotId": "9b3ea8e0-7eaf-4b9f-a72e-932c9ce0e0d6"
}
```

Response `201`:

```json
{
  "appointmentId": "76d7800a-ae23-4f65-9d3d-c9536e2bdf5a",
  "slotId": "9b3ea8e0-7eaf-4b9f-a72e-932c9ce0e0d6",
  "status": "UPCOMING",
  "message": "Booking created successfully"
}
```

Possible errors:

- `400` validation failure (`slotId` missing)
- `404` slot not found
- `409` slot already booked
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
  "startDatetime": "2026-04-20T08:00:00Z"
}
```

Possible errors:

- `403` caller is not the same profile and not admin
- `404` no upcoming appointment found for requested profile
- `401` unauthenticated

### 4. Get Therapist Detail

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

### 5. Get Therapist Available Slots

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

### 6. Submit Clinical Note

- Method/Path: `POST /api/v1/notes`
- Auth: Required
- Role: `ROLE_THERAPIST`
- Description: Creates one clinical note for an in-progress appointment and marks appointment as `COMPLETED`.

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
- `403` forbidden role
- `404` appointment not found
- `409` appointment not `IN_PROGRESS`
- `409` note already exists for appointment
- `401` unauthenticated

### 7. Submit Review

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

### 8. Save Matching Preferences

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

### 9. Find Matching Therapists

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

### 10. Assign Therapist

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

### 11. Get Active Assigned Therapist

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

### 12. Trigger Slot Generation (Test Endpoint)

- Method/Path: `POST /api/v1/test/trigger-generation`
- Auth: Not required
- Description: Triggers internal schedule generation job.

Response `200`:

```json
{
  "message": "Schedule slot generation triggered"
}
```

### 13. Trigger Slot Cleanup (Test Endpoint)

- Method/Path: `POST /api/v1/test/trigger-cleanup`
- Auth: Not required
- Description: Triggers cleanup of old unbooked slots.

Response `200`:

```json
{
  "message": "Schedule slot cleanup triggered"
}
```

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

Submit review:

```bash
curl -X POST "http://localhost:8082/api/v1/reviews" \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"appointmentId":"76d7800a-ae23-4f65-9d3d-c9536e2bdf5a","rating":5,"comment":"Very helpful session"}'
```
