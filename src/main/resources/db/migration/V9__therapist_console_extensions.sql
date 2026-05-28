-- Schema extensions backing the therapist console:
--   * appointments: end_datetime, reason, cancellation_reason, cancelled_at, patient_name
--   * clinical_notes: SOAP fields, summary, risk flags, status, updated_at
--   * patient_tags: per-patient tags + risk level set by the assigned therapist
-- Migration is idempotent (IF NOT EXISTS, conditional ALTERs).

------------------------------------------------------------------------------------------
-- Appointments
------------------------------------------------------------------------------------------
ALTER TABLE appointments
    ADD COLUMN IF NOT EXISTS end_datetime        TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS reason              TEXT,
    ADD COLUMN IF NOT EXISTS cancellation_reason TEXT,
    ADD COLUMN IF NOT EXISTS cancelled_at        TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS patient_name        VARCHAR(255);

-- Backfill end_datetime from the originating slot when missing.
UPDATE appointments a
SET end_datetime = s.end_datetime
FROM schedule_slots s
WHERE a.slot_id = s.slot_id
  AND a.end_datetime IS NULL;

-- Note: status column is VARCHAR(50) and not enforced by a CHECK constraint,
-- so the new REQUESTED value does not require a constraint change.

------------------------------------------------------------------------------------------
-- Clinical notes
------------------------------------------------------------------------------------------
ALTER TABLE clinical_notes
    ADD COLUMN IF NOT EXISTS subjective           TEXT,
    ADD COLUMN IF NOT EXISTS objective            TEXT,
    ADD COLUMN IF NOT EXISTS assessment           TEXT,
    ADD COLUMN IF NOT EXISTS plan                 TEXT,
    ADD COLUMN IF NOT EXISTS summary              VARCHAR(500),
    ADD COLUMN IF NOT EXISTS risk_suicidal        BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS risk_self_harm       BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS risk_substance_use   BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS risk_abuse           BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS status               VARCHAR(20) NOT NULL DEFAULT 'FINALIZED',
    ADD COLUMN IF NOT EXISTS updated_at           TIMESTAMPTZ;

UPDATE clinical_notes
SET updated_at = created_at
WHERE updated_at IS NULL;

ALTER TABLE clinical_notes
    ALTER COLUMN updated_at SET NOT NULL;

-- Diagnosis / recommendations were NOT NULL via @NotBlank validation only.
-- For DRAFT notes we want them nullable in the DB to allow partial saves.
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'clinical_notes'
          AND column_name = 'diagnosis'
          AND is_nullable = 'NO'
    ) THEN
        ALTER TABLE clinical_notes ALTER COLUMN diagnosis DROP NOT NULL;
    END IF;

    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'clinical_notes'
          AND column_name = 'recommendations'
          AND is_nullable = 'NO'
    ) THEN
        ALTER TABLE clinical_notes ALTER COLUMN recommendations DROP NOT NULL;
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE table_schema = 'public'
          AND table_name = 'clinical_notes'
          AND constraint_name = 'ck_clinical_notes_status'
    ) THEN
        ALTER TABLE clinical_notes
            ADD CONSTRAINT ck_clinical_notes_status
            CHECK (status IN ('DRAFT', 'FINALIZED'));
    END IF;
END $$;

------------------------------------------------------------------------------------------
-- Patient tags + risk level (therapist-set, per-patient)
------------------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS patient_tags (
    profile_id      UUID         PRIMARY KEY,
    tags            VARCHAR[],
    risk_level      VARCHAR(20),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_by      UUID
);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE table_schema = 'public'
          AND table_name = 'patient_tags'
          AND constraint_name = 'ck_patient_tags_risk_level'
    ) THEN
        ALTER TABLE patient_tags
            ADD CONSTRAINT ck_patient_tags_risk_level
            CHECK (risk_level IS NULL OR risk_level IN ('LOW', 'MEDIUM', 'HIGH'));
    END IF;
END $$;

------------------------------------------------------------------------------------------
-- Useful indexes
------------------------------------------------------------------------------------------
CREATE INDEX IF NOT EXISTS idx_appointments_therapist_status
    ON appointments(therapist_id, status, start_datetime);

CREATE INDEX IF NOT EXISTS idx_clinical_notes_status
    ON clinical_notes(status);
