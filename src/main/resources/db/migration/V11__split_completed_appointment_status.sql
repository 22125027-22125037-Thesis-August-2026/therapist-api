-- Splits the single appointment status COMPLETED into three states:
--   PATIENT_COMPLETE      - patient reviewed, therapist has not finalized a note yet
--   PROFESSIONAL_COMPLETE - therapist finalized a note, patient has not reviewed yet
--   OVERALL_COMPLETE      - both sides are done
--
-- appointments.status is VARCHAR(50) with no CHECK constraint (see V9), so the
-- new values themselves need no DDL change. This migration only backfills
-- existing COMPLETED rows into the correct new value based on which of the
-- 1:1 reviews / clinical_notes rows already exist for that appointment.

UPDATE appointments a
SET status = 'OVERALL_COMPLETE'
WHERE a.status = 'COMPLETED'
  AND EXISTS (SELECT 1 FROM reviews r WHERE r.appt_id = a.appt_id)
  AND EXISTS (
      SELECT 1 FROM clinical_notes cn
      WHERE cn.appt_id = a.appt_id AND cn.status = 'FINALIZED'
  );

UPDATE appointments a
SET status = 'PATIENT_COMPLETE'
WHERE a.status = 'COMPLETED'
  AND EXISTS (SELECT 1 FROM reviews r WHERE r.appt_id = a.appt_id)
  AND NOT EXISTS (
      SELECT 1 FROM clinical_notes cn
      WHERE cn.appt_id = a.appt_id AND cn.status = 'FINALIZED'
  );

UPDATE appointments a
SET status = 'PROFESSIONAL_COMPLETE'
WHERE a.status = 'COMPLETED'
  AND NOT EXISTS (SELECT 1 FROM reviews r WHERE r.appt_id = a.appt_id)
  AND EXISTS (
      SELECT 1 FROM clinical_notes cn
      WHERE cn.appt_id = a.appt_id AND cn.status = 'FINALIZED'
  );

-- Any leftover COMPLETED row (neither a review nor a finalized note) should
-- not occur under the pre-split invariants, but resolve conservatively to
-- OVERALL_COMPLETE rather than leaving an unrecognized enum value behind.
UPDATE appointments
SET status = 'OVERALL_COMPLETE'
WHERE status = 'COMPLETED';
