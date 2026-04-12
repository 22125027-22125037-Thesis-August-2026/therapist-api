-- Add matching-related fields and tables for therapist assignment.

ALTER TABLE therapists
    ADD COLUMN IF NOT EXISTS gender VARCHAR(50),
    ADD COLUMN IF NOT EXISTS is_lgbtq_allied BOOLEAN,
    ADD COLUMN IF NOT EXISTS communication_style VARCHAR(100),
    ADD COLUMN IF NOT EXISTS treated_challenges VARCHAR[];

CREATE TABLE IF NOT EXISTS profiles_preferences (
    profile_id UUID PRIMARY KEY,
    has_prior_counseling VARCHAR(50),
    sexual_orientation VARCHAR(100),
    is_lgbtq_priority BOOLEAN,
    reasons JSONB,
    communication_style VARCHAR(100),
    last_updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS therapist_assignments (
    assignment_id UUID PRIMARY KEY,
    profile_id UUID NOT NULL,
    therapist_id UUID NOT NULL,
    status VARCHAR(20) NOT NULL,
    assigned_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    unassigned_at TIMESTAMPTZ
);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.table_constraints tc
        WHERE tc.table_schema = 'public'
          AND tc.table_name = 'therapist_assignments'
          AND tc.constraint_name = 'fk_therapist_assignments_profile'
    ) THEN
        ALTER TABLE therapist_assignments
            ADD CONSTRAINT fk_therapist_assignments_profile
            FOREIGN KEY (profile_id) REFERENCES profiles_preferences(profile_id)
            ON DELETE CASCADE;
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.table_constraints tc
        WHERE tc.table_schema = 'public'
          AND tc.table_name = 'therapist_assignments'
          AND tc.constraint_name = 'fk_therapist_assignments_therapist'
    ) THEN
        ALTER TABLE therapist_assignments
            ADD CONSTRAINT fk_therapist_assignments_therapist
            FOREIGN KEY (therapist_id) REFERENCES therapists(therapist_id)
            ON DELETE RESTRICT;
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.table_constraints tc
        WHERE tc.table_schema = 'public'
          AND tc.table_name = 'therapist_assignments'
          AND tc.constraint_name = 'ck_therapist_assignments_status'
    ) THEN
        ALTER TABLE therapist_assignments
            ADD CONSTRAINT ck_therapist_assignments_status
            CHECK (status IN ('ACTIVE', 'INACTIVE', 'CHANGED_BY_REQUEST'));
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_therapist_assignments_profile_id
    ON therapist_assignments(profile_id);

CREATE INDEX IF NOT EXISTS idx_therapist_assignments_therapist_id
    ON therapist_assignments(therapist_id);
