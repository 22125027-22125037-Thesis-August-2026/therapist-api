-- Phase 10 + Phase 11 schema alignment for Booking Domain

CREATE TABLE IF NOT EXISTS therapists (
    therapist_id UUID PRIMARY KEY,
    account_id UUID NOT NULL,
    full_name VARCHAR(255) NOT NULL,
    specialization VARCHAR(255),
    country VARCHAR(255),
    years_experience INTEGER,
    about_me TEXT,
    rating_avg NUMERIC(3,2),
    license_url VARCHAR(1024)
);

CREATE TABLE IF NOT EXISTS weekly_templates (
    template_id UUID PRIMARY KEY,
    therapist_id UUID NOT NULL,
    day_of_week VARCHAR(20) NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS clinical_notes (
    note_id UUID PRIMARY KEY,
    appt_id UUID NOT NULL UNIQUE,
    diagnosis TEXT,
    recommendations TEXT,
    created_at TIMESTAMPTZ NOT NULL
);

-- Fresh environments do not have legacy phase-10 tables yet.
-- Create canonical shells first so alignment ALTER statements below are always valid.
CREATE TABLE IF NOT EXISTS schedule_slots (
    slot_id UUID PRIMARY KEY,
    therapist_id UUID NOT NULL,
    start_datetime TIMESTAMPTZ NOT NULL,
    end_datetime TIMESTAMPTZ NOT NULL,
    is_booked BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE IF NOT EXISTS appointments (
    appt_id UUID PRIMARY KEY,
    profile_id UUID NOT NULL,
    therapist_id UUID NOT NULL,
    slot_id UUID NOT NULL,
    mode VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL,
    meeting_link VARCHAR(1024),
    start_datetime TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);

-- Align legacy PK column names to domain contract where needed.
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public' AND table_name = 'schedule_slots' AND column_name = 'id'
    ) AND NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public' AND table_name = 'schedule_slots' AND column_name = 'slot_id'
    ) THEN
        ALTER TABLE schedule_slots RENAME COLUMN id TO slot_id;
    END IF;
END $$;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public' AND table_name = 'appointments' AND column_name = 'id'
    ) AND NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public' AND table_name = 'appointments' AND column_name = 'appt_id'
    ) THEN
        ALTER TABLE appointments RENAME COLUMN id TO appt_id;
    END IF;
END $$;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public' AND table_name = 'appointments' AND column_name = 'slotid'
    ) AND NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public' AND table_name = 'appointments' AND column_name = 'slot_id'
    ) THEN
        EXECUTE 'ALTER TABLE appointments RENAME COLUMN slotid TO slot_id';
    END IF;
END $$;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public' AND table_name = 'schedule_slots' AND column_name = 'therapistid'
    ) AND NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public' AND table_name = 'schedule_slots' AND column_name = 'therapist_id'
    ) THEN
        EXECUTE 'ALTER TABLE schedule_slots RENAME COLUMN therapistid TO therapist_id';
    END IF;
END $$;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public' AND table_name = 'appointments' AND column_name = 'therapistid'
    ) AND NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public' AND table_name = 'appointments' AND column_name = 'therapist_id'
    ) THEN
        EXECUTE 'ALTER TABLE appointments RENAME COLUMN therapistid TO therapist_id';
    END IF;
END $$;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public' AND table_name = 'appointments' AND column_name = 'profileid'
    ) AND NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public' AND table_name = 'appointments' AND column_name = 'profile_id'
    ) THEN
        EXECUTE 'ALTER TABLE appointments RENAME COLUMN profileid TO profile_id';
    END IF;
END $$;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public' AND table_name = 'appointments' AND column_name = 'startdatetime'
    ) AND NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public' AND table_name = 'appointments' AND column_name = 'start_datetime'
    ) THEN
        EXECUTE 'ALTER TABLE appointments RENAME COLUMN startdatetime TO start_datetime';
    END IF;
END $$;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public' AND table_name = 'appointments' AND column_name = 'createdat'
    ) AND NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public' AND table_name = 'appointments' AND column_name = 'created_at'
    ) THEN
        EXECUTE 'ALTER TABLE appointments RENAME COLUMN createdat TO created_at';
    END IF;
END $$;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public' AND table_name = 'schedule_slots' AND column_name = 'startdatetime'
    ) AND NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public' AND table_name = 'schedule_slots' AND column_name = 'start_datetime'
    ) THEN
        EXECUTE 'ALTER TABLE schedule_slots RENAME COLUMN startdatetime TO start_datetime';
    END IF;
END $$;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public' AND table_name = 'schedule_slots' AND column_name = 'enddatetime'
    ) AND NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public' AND table_name = 'schedule_slots' AND column_name = 'end_datetime'
    ) THEN
        EXECUTE 'ALTER TABLE schedule_slots RENAME COLUMN enddatetime TO end_datetime';
    END IF;
END $$;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public' AND table_name = 'schedule_slots' AND column_name = 'booked'
    ) AND NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public' AND table_name = 'schedule_slots' AND column_name = 'is_booked'
    ) THEN
        EXECUTE 'ALTER TABLE schedule_slots RENAME COLUMN booked TO is_booked';
    END IF;
END $$;

ALTER TABLE schedule_slots
    ADD COLUMN IF NOT EXISTS therapist_id UUID,
    ADD COLUMN IF NOT EXISTS start_datetime TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS end_datetime TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS is_booked BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE appointments
    ADD COLUMN IF NOT EXISTS appt_id UUID,
    ADD COLUMN IF NOT EXISTS profile_id UUID,
    ADD COLUMN IF NOT EXISTS therapist_id UUID,
    ADD COLUMN IF NOT EXISTS slot_id UUID,
    ADD COLUMN IF NOT EXISTS mode VARCHAR(50),
    ADD COLUMN IF NOT EXISTS status VARCHAR(50),
    ADD COLUMN IF NOT EXISTS meeting_link VARCHAR(1024),
    ADD COLUMN IF NOT EXISTS start_datetime TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS created_at TIMESTAMPTZ;

-- Normalize appointments PK if table existed without appt_id PK.
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public' AND table_name = 'appointments' AND column_name = 'appt_id'
    ) AND NOT EXISTS (
        SELECT 1
        FROM information_schema.table_constraints tc
        WHERE tc.table_schema = 'public'
          AND tc.table_name = 'appointments'
          AND tc.constraint_type = 'PRIMARY KEY'
    ) THEN
        EXECUTE 'ALTER TABLE appointments ADD PRIMARY KEY (appt_id)';
    END IF;
END $$;

ALTER TABLE schedule_slots
    ALTER COLUMN therapist_id SET NOT NULL,
    ALTER COLUMN start_datetime SET NOT NULL,
    ALTER COLUMN end_datetime SET NOT NULL;

ALTER TABLE appointments
    ALTER COLUMN profile_id SET NOT NULL,
    ALTER COLUMN therapist_id SET NOT NULL,
    ALTER COLUMN slot_id SET NOT NULL,
    ALTER COLUMN mode SET NOT NULL,
    ALTER COLUMN status SET NOT NULL,
    ALTER COLUMN start_datetime SET NOT NULL,
    ALTER COLUMN created_at SET NOT NULL;

-- Backfill legacy therapist references before FK creation.
INSERT INTO therapists (
    therapist_id,
    account_id,
    full_name,
    specialization,
    country,
    years_experience,
    about_me,
    rating_avg,
    license_url
)
SELECT DISTINCT
    s.therapist_id,
    s.therapist_id,
    'Legacy Therapist ' || LEFT(s.therapist_id::text, 8),
    NULL::VARCHAR(255),
    NULL::VARCHAR(255),
    NULL::INTEGER,
    'Auto-generated during Flyway migration to satisfy FK for legacy slots.',
    NULL::NUMERIC(3,2),
    NULL::VARCHAR(1024)
FROM schedule_slots s
LEFT JOIN therapists t ON t.therapist_id = s.therapist_id
WHERE s.therapist_id IS NOT NULL
  AND t.therapist_id IS NULL;

INSERT INTO therapists (
    therapist_id,
    account_id,
    full_name,
    specialization,
    country,
    years_experience,
    about_me,
    rating_avg,
    license_url
)
SELECT DISTINCT
    a.therapist_id,
    a.therapist_id,
    'Legacy Therapist ' || LEFT(a.therapist_id::text, 8),
    NULL::VARCHAR(255),
    NULL::VARCHAR(255),
    NULL::INTEGER,
    'Auto-generated during Flyway migration to satisfy FK for legacy appointments.',
    NULL::NUMERIC(3,2),
    NULL::VARCHAR(1024)
FROM appointments a
LEFT JOIN therapists t ON t.therapist_id = a.therapist_id
WHERE a.therapist_id IS NOT NULL
  AND t.therapist_id IS NULL;

ALTER TABLE weekly_templates
    ADD CONSTRAINT fk_weekly_templates_therapist
        FOREIGN KEY (therapist_id) REFERENCES therapists(therapist_id)
        ON DELETE CASCADE;

ALTER TABLE schedule_slots
    ADD CONSTRAINT fk_schedule_slots_therapist
        FOREIGN KEY (therapist_id) REFERENCES therapists(therapist_id)
        ON DELETE RESTRICT;

ALTER TABLE appointments
    ADD CONSTRAINT uq_appointments_slot_id UNIQUE (slot_id);

ALTER TABLE appointments
    ADD CONSTRAINT fk_appointments_therapist
        FOREIGN KEY (therapist_id) REFERENCES therapists(therapist_id)
        ON DELETE RESTRICT;

ALTER TABLE appointments
    ADD CONSTRAINT fk_appointments_slot
        FOREIGN KEY (slot_id) REFERENCES schedule_slots(slot_id)
        ON DELETE RESTRICT;

ALTER TABLE clinical_notes
    ADD CONSTRAINT fk_clinical_notes_appointment
        FOREIGN KEY (appt_id) REFERENCES appointments(appt_id)
        ON DELETE CASCADE;
