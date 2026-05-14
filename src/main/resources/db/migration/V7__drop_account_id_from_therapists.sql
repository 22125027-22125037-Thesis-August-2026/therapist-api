-- Drop account_id now that the therapists table no longer needs it.
ALTER TABLE therapists
    DROP COLUMN IF EXISTS account_id;
