-- Step 1: Create dedicated table for therapist Zoom Personal Meeting Room credentials.
CREATE TABLE IF NOT EXISTS therapist_zoom_credentials (
    therapist_id          UUID          PRIMARY KEY,
    zoom_email            VARCHAR(255)  NOT NULL UNIQUE,
    zoom_meeting_number   VARCHAR(50)   NOT NULL,
    zoom_meeting_password VARCHAR(50)   NOT NULL,
    updated_at            TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_zoom_creds_therapist
        FOREIGN KEY (therapist_id) REFERENCES therapists(therapist_id)
        ON DELETE CASCADE
);

-- Step 2: Rename meeting_link -> meeting_number and resize to VARCHAR(50).
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name   = 'appointments'
          AND column_name  = 'meeting_link'
    ) AND NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name   = 'appointments'
          AND column_name  = 'meeting_number'
    ) THEN
        ALTER TABLE appointments RENAME COLUMN meeting_link TO meeting_number;
        ALTER TABLE appointments ALTER COLUMN meeting_number TYPE VARCHAR(50)
            USING LEFT(meeting_number, 50);
    END IF;
END $$;

-- Step 3: Add meeting_password column.
ALTER TABLE appointments
    ADD COLUMN IF NOT EXISTS meeting_password VARCHAR(50);

-- Step 4: Seed Zoom credentials for the first two mock therapists.
INSERT INTO therapist_zoom_credentials (therapist_id, zoom_email, zoom_meeting_number, zoom_meeting_password)
VALUES
    ('550e8400-e29b-41d4-a716-446655440001'::uuid, 'brukeduong@gmail.com',      '7075120473', 'N212sP'),
    ('550e8400-e29b-41d4-a716-446655440002'::uuid, 'khiemduong0938@gmail.com',  '2582871621', 'vc7SPn')
ON CONFLICT DO NOTHING;
