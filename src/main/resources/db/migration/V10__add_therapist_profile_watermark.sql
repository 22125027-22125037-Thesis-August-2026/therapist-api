-- Watermark for mirroring auth-service therapist-profile edits into the therapists
-- read replica. Stores the occurredAt of the last applied therapist.profile.updated
-- event so out-of-order / replayed events can be skipped idempotently.
ALTER TABLE therapists
    ADD COLUMN IF NOT EXISTS last_profile_event_at TIMESTAMPTZ;
