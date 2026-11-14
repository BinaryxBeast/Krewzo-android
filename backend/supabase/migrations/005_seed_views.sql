-- ============================================================
-- Krewzo — Migration 005: Seed Data & Storage Buckets
-- ============================================================

-- ── Supabase Storage Buckets ─────────────────────────────────
-- Run these in Supabase Dashboard > Storage, or via CLI.
-- Listed here as documentation.

-- Public bucket: client logos, worker profile photos
-- Private bucket: worker ID documents

-- INSERT INTO storage.buckets (id, name, public) VALUES
--     ('avatars',           'avatars',           TRUE),
--     ('client-logos',      'client-logos',      TRUE),
--     ('worker-documents',  'worker-documents',  FALSE);  -- Private!


-- ── Storage RLS ───────────────────────────────────────────────
-- (Apply in Supabase Dashboard > Storage > Policies)

-- avatars bucket: anyone can read, authenticated users upload their own
-- CREATE POLICY "avatars public read" ON storage.objects FOR SELECT
--     USING (bucket_id = 'avatars');
-- CREATE POLICY "avatars self upload" ON storage.objects FOR INSERT
--     WITH CHECK (bucket_id = 'avatars' AND auth.uid()::TEXT = (storage.foldername(name))[1]);

-- worker-documents bucket: only worker themselves + admins
-- CREATE POLICY "worker-docs self read" ON storage.objects FOR SELECT
--     USING (
--         bucket_id = 'worker-documents'
--         AND (
--             auth.uid()::TEXT = (storage.foldername(name))[1]
--             OR (SELECT role FROM public.users WHERE id = auth.uid()) = 'admin'
--         )
--     );


-- ── Default Admin User ────────────────────────────────────────
-- Create the first admin via Supabase Auth phone OTP,
-- then run this to promote them:

-- UPDATE public.users
-- SET role = 'admin'
-- WHERE phone = '+919XXXXXXXXX';


-- ── Useful Views ──────────────────────────────────────────────

-- Active bookings with client name, fill progress
DROP VIEW IF EXISTS public.v_active_bookings;
CREATE OR REPLACE VIEW public.v_active_bookings AS
SELECT
    b.id,
    b.status,
    c.business_name AS client_name,
    b.work_address,
    b.work_date,
    b.work_start_time,
    b.workers_needed,
    b.workers_accepted,
    b.pay_per_worker,
    b.workers_needed * b.pay_per_worker AS total_booking_value,
    b.created_at
FROM public.bookings b
JOIN public.clients c ON c.id = b.client_id
WHERE b.status NOT IN ('completed', 'cancelled', 'expired')
  AND c.deleted_at IS NULL
ORDER BY b.work_date ASC, b.created_at DESC;


-- Worker with their current status
DROP VIEW IF EXISTS public.v_workers_summary;
CREATE OR REPLACE VIEW public.v_workers_summary AS
SELECT
    w.id,
    w.full_name,
    w.phone,
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


-- Dashboard stats for admin
DROP VIEW IF EXISTS public.v_dashboard_stats;
CREATE OR REPLACE VIEW public.v_dashboard_stats AS
SELECT
    COUNT(*) FILTER (WHERE status = 'searching')        AS searching_bookings,
    COUNT(*) FILTER (WHERE status = 'confirmed')        AS confirmed_bookings,
    COUNT(*) FILTER (WHERE status = 'in_progress')      AS active_bookings,
    COUNT(*) FILTER (WHERE DATE(created_at) = CURRENT_DATE) AS bookings_today,
    COUNT(*) FILTER (WHERE DATE(created_at) >= CURRENT_DATE - 7) AS bookings_this_week,
    SUM(workers_needed * pay_per_worker)
        FILTER (WHERE status IN ('confirmed', 'completed')) AS total_booking_value
FROM public.bookings;
