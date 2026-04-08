CREATE TABLE IF NOT EXISTS reviews (
    review_id UUID PRIMARY KEY,
    appt_id UUID NOT NULL UNIQUE,
    rating INTEGER NOT NULL CHECK (rating BETWEEN 1 AND 5),
    comment TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.table_constraints tc
        WHERE tc.table_schema = 'public'
          AND tc.table_name = 'reviews'
          AND tc.constraint_name = 'fk_reviews_appointment'
    ) THEN
        ALTER TABLE reviews
            ADD CONSTRAINT fk_reviews_appointment
            FOREIGN KEY (appt_id) REFERENCES appointments(appt_id)
            ON DELETE CASCADE;
    END IF;
END $$;
