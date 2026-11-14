-- ============================================================
-- Krewzo — Migration 009: Add Date of Birth for Workers
-- ============================================================

-- 1. Ensure date_of_birth column exists on public.workers
ALTER TABLE public.workers
ADD COLUMN IF NOT EXISTS date_of_birth DATE;

-- 2. Create index on date_of_birth for filtering and queries
CREATE INDEX IF NOT EXISTS idx_workers_dob ON public.workers(date_of_birth);

-- 3. Trigger function to automatically calculate and sync age when date_of_birth is set or updated
CREATE OR REPLACE FUNCTION public.sync_worker_age_from_dob()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.date_of_birth IS NOT NULL THEN
        NEW.age := DATE_PART('year', AGE(CURRENT_DATE, NEW.date_of_birth))::INTEGER;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_sync_worker_age ON public.workers;
CREATE TRIGGER trg_sync_worker_age
BEFORE INSERT OR UPDATE OF date_of_birth ON public.workers
FOR EACH ROW
EXECUTE FUNCTION public.sync_worker_age_from_dob();

-- 4. Backfill / sync age for any existing workers who already have a date_of_birth
UPDATE public.workers
SET age = DATE_PART('year', AGE(CURRENT_DATE, date_of_birth))::INTEGER
WHERE date_of_birth IS NOT NULL;

-- 5. Refresh v_workers_summary view to include date_of_birth and age
DROP VIEW IF EXISTS public.v_workers_summary;
CREATE OR REPLACE VIEW public.v_workers_summary AS
SELECT
    w.id,
    w.full_name,
    w.phone,
    w.date_of_birth,
    w.age,
    w.is_available,
    w.is_active,
    w.total_jobs_completed,
    w.total_earnings,
    w.rating,
    ST_X(w.home_coordinates::GEOMETRY) AS lng,
    ST_Y(w.home_coordinates::GEOMETRY) AS lat,
    array_agg(ws.skill) FILTER (WHERE ws.skill IS NOT NULL) AS skills
FROM public.workers w
LEFT JOIN public.worker_skills ws ON ws.worker_id = w.id
WHERE w.deleted_at IS NULL
GROUP BY w.id;
