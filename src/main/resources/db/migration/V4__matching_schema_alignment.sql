-- Align matching schema with cross-domain profile UUID policy.
-- 1) reasons: jsonb -> varchar[]
-- 2) therapist_assignments.profile_id: keep plain UUID (no FK)

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns c
        WHERE c.table_schema = 'public'
          AND c.table_name = 'profiles_preferences'
          AND c.column_name = 'reasons'
          AND c.udt_name = 'jsonb'
    ) THEN
        ALTER TABLE profiles_preferences
            ADD COLUMN reasons_tmp varchar[];

        UPDATE profiles_preferences
        SET reasons_tmp = CASE
            WHEN reasons IS NULL THEN NULL
            ELSE ARRAY(SELECT jsonb_array_elements_text(reasons))
        END;

        ALTER TABLE profiles_preferences
            DROP COLUMN reasons;

        ALTER TABLE profiles_preferences
            RENAME COLUMN reasons_tmp TO reasons;
    END IF;
END $$;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.table_constraints tc
        WHERE tc.table_schema = 'public'
          AND tc.table_name = 'therapist_assignments'
          AND tc.constraint_name = 'fk_therapist_assignments_profile'
    ) THEN
        ALTER TABLE therapist_assignments
            DROP CONSTRAINT fk_therapist_assignments_profile;
    END IF;
END $$;
