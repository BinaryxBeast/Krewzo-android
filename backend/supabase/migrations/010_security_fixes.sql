-- ============================================================
-- Krewzo — Complete & Updated Database Migration 010
-- Security, Storage, Scalability & Bug Fixes
-- ============================================================
-- 1. Table Schema Alignment (DOB, Age, Address, Skills, Admin Notes)
-- 2. Safe payment_status ENUM Conversion (Fixes ERROR 42804)
-- 3. Storage Buckets (avatars, worker-documents, client-logos) + RLS
-- 4. Scalability: Composite Indexes & Rating Constraints
-- 5. FCM Token Limiter & Auto-pruner
-- 6. Secure Views with Security Barrier & Restricted Access
-- 7. Audit Log Privacy (Strip sensitive fields)
-- ============================================================

-- ─────────────────────────────────────────────────────────────
-- STEP 1: Ensure All Required Columns Exist on Core Tables
-- ─────────────────────────────────────────────────────────────

-- Workers table fields for onboarding and profile
ALTER TABLE public.workers ADD COLUMN IF NOT EXISTS date_of_birth TEXT;
ALTER TABLE public.workers ADD COLUMN IF NOT EXISTS age INTEGER;
ALTER TABLE public.workers ADD COLUMN IF NOT EXISTS home_address TEXT;
ALTER TABLE public.workers ADD COLUMN IF NOT EXISTS skills TEXT[] DEFAULT '{}';
ALTER TABLE public.workers ADD COLUMN IF NOT EXISTS emergency_contact TEXT;
ALTER TABLE public.workers ADD COLUMN IF NOT EXISTS admin_notes TEXT;
ALTER TABLE public.workers ADD COLUMN IF NOT EXISTS is_available BOOLEAN DEFAULT TRUE;
ALTER TABLE public.workers ADD COLUMN IF NOT EXISTS is_active BOOLEAN DEFAULT TRUE;

-- Clients table fields
ALTER TABLE public.clients ADD COLUMN IF NOT EXISTS is_active BOOLEAN DEFAULT TRUE;
ALTER TABLE public.clients ADD COLUMN IF NOT EXISTS latitude DOUBLE PRECISION;
ALTER TABLE public.clients ADD COLUMN IF NOT EXISTS longitude DOUBLE PRECISION;

-- Bookings table: allow optional GPS coordinates if set via address
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_schema = 'public' 
          AND table_name = 'bookings' 
          AND column_name = 'work_coordinates'
          AND is_nullable = 'NO'
    ) THEN
        ALTER TABLE public.bookings ALTER COLUMN work_coordinates DROP NOT NULL;
    END IF;
END $$;


-- ─────────────────────────────────────────────────────────────
-- STEP 2: Safe payment_status ENUM Conversion (Fixes ERROR 42804)
-- ─────────────────────────────────────────────────────────────
-- The error 42804 occurs when PostgreSQL tries to alter a column's type
-- while a TEXT default ('pending'::text) is still attached to it.
-- This block safely drops the default first, casts the data, and sets the new default.

DO $$
BEGIN
    -- 1. Create the payment_status enum type if it doesn't already exist
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'payment_status') THEN
        CREATE TYPE payment_status AS ENUM (
            'pending',
            'processing',
            'paid',
            'failed',
            'refunded'
        );
    END IF;

    -- 2. Check if booking_assignments table exists and has a payment_status column not yet of type payment_status
    IF EXISTS (
        SELECT 1 
        FROM information_schema.columns 
        WHERE table_schema = 'public' 
          AND table_name = 'booking_assignments' 
          AND column_name = 'payment_status' 
          AND udt_name != 'payment_status'
    ) THEN
        -- Step 2a: Drop the text default
        ALTER TABLE public.booking_assignments ALTER COLUMN payment_status DROP DEFAULT;

        -- Step 2b: Convert column with a fallback mapping for legacy values
        ALTER TABLE public.booking_assignments 
            ALTER COLUMN payment_status TYPE payment_status 
            USING (
                CASE 
                    WHEN payment_status IN ('pending', 'processing', 'paid', 'failed', 'refunded') 
                        THEN payment_status::payment_status
                    ELSE 'pending'::payment_status
                END
            );

        -- Step 2c: Set the new ENUM default
        ALTER TABLE public.booking_assignments ALTER COLUMN payment_status SET DEFAULT 'pending'::payment_status;
    END IF;
END $$;


-- ─────────────────────────────────────────────────────────────
-- STEP 3: Storage Buckets Configuration & Access Policies
-- ─────────────────────────────────────────────────────────────
-- Configures buckets needed for worker photographs, Aadhaar KYC documents,
-- and client logos.

INSERT INTO storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
VALUES 
    (
        'avatars', 
        'avatars', 
        TRUE, 
        5242880,  -- 5 MB
        ARRAY['image/jpeg', 'image/png', 'image/webp']
    ),
    (
        'worker-documents', 
        'worker-documents', 
        TRUE,     -- Public read so Coil / AsyncImage in app can load verified docs
        10485760, -- 10 MB
        ARRAY['image/jpeg', 'image/png', 'application/pdf']
    ),
    (
        'client-logos', 
        'client-logos', 
        TRUE, 
        2097152,  -- 2 MB
        ARRAY['image/jpeg', 'image/png', 'image/webp']
    )
ON CONFLICT (id) DO UPDATE SET
    public             = EXCLUDED.public,
    file_size_limit    = EXCLUDED.file_size_limit,
    allowed_mime_types = EXCLUDED.allowed_mime_types;

-- Storage Read Policies (Public read for avatars, documents, and logos)
DROP POLICY IF EXISTS "storage_avatars_read" ON storage.objects;
CREATE POLICY "storage_avatars_read" ON storage.objects 
    FOR SELECT USING (bucket_id = 'avatars');

DROP POLICY IF EXISTS "storage_worker_docs_read" ON storage.objects;
CREATE POLICY "storage_worker_docs_read" ON storage.objects 
    FOR SELECT USING (bucket_id = 'worker-documents');

DROP POLICY IF EXISTS "storage_client_logos_read" ON storage.objects;
CREATE POLICY "storage_client_logos_read" ON storage.objects 
    FOR SELECT USING (bucket_id = 'client-logos');

-- Storage Insert Policies
DROP POLICY IF EXISTS "storage_avatars_insert" ON storage.objects;
CREATE POLICY "storage_avatars_insert" ON storage.objects 
    FOR INSERT WITH CHECK (bucket_id = 'avatars');

DROP POLICY IF EXISTS "storage_worker_docs_insert" ON storage.objects;
CREATE POLICY "storage_worker_docs_insert" ON storage.objects 
    FOR INSERT WITH CHECK (bucket_id = 'worker-documents');

DROP POLICY IF EXISTS "storage_client_logos_insert" ON storage.objects;
CREATE POLICY "storage_client_logos_insert" ON storage.objects 
    FOR INSERT WITH CHECK (bucket_id = 'client-logos');

-- Storage Update / Delete Policies
DROP POLICY IF EXISTS "storage_avatars_update" ON storage.objects;
CREATE POLICY "storage_avatars_update" ON storage.objects 
    FOR UPDATE USING (bucket_id = 'avatars');

DROP POLICY IF EXISTS "storage_worker_docs_update" ON storage.objects;
CREATE POLICY "storage_worker_docs_update" ON storage.objects 
    FOR UPDATE USING (bucket_id = 'worker-documents');

DROP POLICY IF EXISTS "storage_client_logos_update" ON storage.objects;
CREATE POLICY "storage_client_logos_update" ON storage.objects 
    FOR UPDATE USING (bucket_id = 'client-logos');

DROP POLICY IF EXISTS "storage_avatars_delete" ON storage.objects;
CREATE POLICY "storage_avatars_delete" ON storage.objects 
    FOR DELETE USING (bucket_id = 'avatars');

DROP POLICY IF EXISTS "storage_worker_docs_delete" ON storage.objects;
CREATE POLICY "storage_worker_docs_delete" ON storage.objects 
    FOR DELETE USING (bucket_id = 'worker-documents');


-- ─────────────────────────────────────────────────────────────
-- STEP 4: Scalability & Performance Indexes
-- ─────────────────────────────────────────────────────────────

-- Composite index for booking dispatch and matching
CREATE INDEX IF NOT EXISTS idx_assignments_active_worker
    ON public.booking_assignments(worker_id, removed_at)
    WHERE removed_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_offers_worker_booking
    ON public.booking_offers(worker_id, booking_id);

-- Realtime subscription polling indexes
CREATE INDEX IF NOT EXISTS idx_bookings_updated_at
    ON public.bookings(updated_at DESC);

CREATE INDEX IF NOT EXISTS idx_offers_updated_at
    ON public.booking_offers(updated_at DESC);

-- Fast lookup index on worker and client phone numbers
CREATE INDEX IF NOT EXISTS idx_workers_phone
    ON public.workers(phone);

CREATE INDEX IF NOT EXISTS idx_clients_phone
    ON public.clients(phone);

-- Rating constraint on workers
ALTER TABLE public.workers DROP CONSTRAINT IF EXISTS chk_worker_rating;
ALTER TABLE public.workers ADD CONSTRAINT chk_worker_rating
    CHECK (rating IS NULL OR (rating >= 0 AND rating <= 5));


-- ─────────────────────────────────────────────────────────────
-- STEP 5: FCM Token Limiter & Auto-Pruner (Prevents Table Bloat)
-- ─────────────────────────────────────────────────────

CREATE OR REPLACE FUNCTION public.limit_fcm_tokens_per_user()
RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
    DELETE FROM public.fcm_tokens
    WHERE user_id = NEW.user_id
      AND id NOT IN (
          SELECT id FROM public.fcm_tokens
          WHERE user_id = NEW.user_id
          ORDER BY updated_at DESC
          LIMIT 4   -- Keep 4 most recent + 1 new = 5 total max
      );
    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS trg_limit_fcm_tokens ON public.fcm_tokens;
CREATE TRIGGER trg_limit_fcm_tokens
    AFTER INSERT ON public.fcm_tokens
    FOR EACH ROW EXECUTE FUNCTION public.limit_fcm_tokens_per_user();


-- ─────────────────────────────────────────────────────────────
-- STEP 6: Secure Reporting Views (Protected with security_barrier)
-- ─────────────────────────────────────────────────────────────

-- Workers Directory Summary View
DROP VIEW IF EXISTS public.v_workers_summary;
CREATE OR REPLACE VIEW public.v_workers_summary
WITH (security_barrier = true)
AS
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
    w.home_address,
    COALESCE(w.skills, '{}'::TEXT[]) AS skills
FROM public.workers w
WHERE w.deleted_at IS NULL;

-- Clients Summary View
DROP VIEW IF EXISTS public.v_clients_with_locations;
CREATE OR REPLACE VIEW public.v_clients_with_locations
WITH (security_barrier = true)
AS
SELECT
    c.id,
    c.business_name,
    c.contact_person,
    c.phone,
    c.email,
    c.employee_count,
    c.address,
    c.city,
    c.pincode,
    c.latitude,
    c.longitude,
    c.is_verified,
    c.is_active,
    c.created_at,
    c.updated_at
FROM public.clients c
WHERE c.deleted_at IS NULL;

-- Active Bookings Summary View
DROP VIEW IF EXISTS public.v_active_bookings;
CREATE OR REPLACE VIEW public.v_active_bookings
WITH (security_barrier = true)
AS
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
  AND c.deleted_at IS NULL;

-- Dashboard Statistics View
DROP VIEW IF EXISTS public.v_dashboard_stats;
CREATE OR REPLACE VIEW public.v_dashboard_stats
WITH (security_barrier = true)
AS
SELECT
    COUNT(*) FILTER (WHERE status = 'searching')        AS searching_bookings,
    COUNT(*) FILTER (WHERE status = 'confirmed')        AS confirmed_bookings,
    COUNT(*) FILTER (WHERE status = 'in_progress')      AS active_bookings,
    COUNT(*) FILTER (WHERE DATE(created_at) = CURRENT_DATE) AS bookings_today,
    COUNT(*) FILTER (WHERE DATE(created_at) >= CURRENT_DATE - 7) AS bookings_this_week,
    COALESCE(SUM(workers_needed * pay_per_worker)
        FILTER (WHERE status IN ('confirmed', 'completed')), 0) AS total_booking_value
FROM public.bookings;

-- Secure View Grants: Revoke public direct queries on internal views
REVOKE SELECT ON public.v_workers_summary        FROM anon, authenticated;
REVOKE SELECT ON public.v_clients_with_locations FROM anon, authenticated;
REVOKE SELECT ON public.v_active_bookings        FROM anon, authenticated;
REVOKE SELECT ON public.v_dashboard_stats        FROM anon, authenticated;

-- Grant to service_role (used by secure backend functions)
GRANT SELECT ON public.v_workers_summary        TO service_role;
GRANT SELECT ON public.v_clients_with_locations TO service_role;
GRANT SELECT ON public.v_active_bookings        TO service_role;
GRANT SELECT ON public.v_dashboard_stats        TO service_role;


-- ─────────────────────────────────────────────────────────────
-- STEP 7: Audit Log Privacy Protection
-- ─────────────────────────────────────────────────────────────
-- Excludes sensitive data (like bank accounts, private KYC numbers)
-- from raw audit JSON snapshots.

CREATE OR REPLACE FUNCTION write_audit_log()
RETURNS TRIGGER LANGUAGE plpgsql SECURITY DEFINER AS $$
DECLARE
    v_old_data JSONB;
    v_new_data JSONB;
BEGIN
    v_old_data := CASE WHEN TG_OP IN ('UPDATE', 'DELETE')
        THEN to_jsonb(OLD)
            - 'bank_account_number'
            - 'doc_number'
        END;

    v_new_data := CASE WHEN TG_OP IN ('INSERT', 'UPDATE')
        THEN to_jsonb(NEW)
            - 'bank_account_number'
            - 'doc_number'
        END;

    INSERT INTO public.audit_log (
        actor_id, actor_role, action, entity_type, entity_id,
        old_data, new_data
    )
    VALUES (
        auth.uid(),
        (SELECT role FROM public.users WHERE id = auth.uid()),
        TG_OP || '.' || TG_TABLE_NAME,
        TG_TABLE_NAME,
        COALESCE(NEW.id::TEXT, OLD.id::TEXT),
        v_old_data,
        v_new_data
    );
    RETURN COALESCE(NEW, OLD);
END;
$$;


-- ─────────────────────────────────────────────────────────────
-- STEP 8: Direct App Read/Write Policies for Core Operations
-- ─────────────────────────────────────────────────────────────

-- Ensure RLS is enabled on all tables
ALTER TABLE public.workers ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.clients ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.bookings ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.booking_assignments ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.booking_offers ENABLE ROW LEVEL SECURITY;

-- Workers table policies
DROP POLICY IF EXISTS "app_workers_select" ON public.workers;
CREATE POLICY "app_workers_select" ON public.workers 
    FOR SELECT USING (deleted_at IS NULL);

DROP POLICY IF EXISTS "app_workers_insert" ON public.workers;
CREATE POLICY "app_workers_insert" ON public.workers 
    FOR INSERT WITH CHECK (true);

DROP POLICY IF EXISTS "app_workers_update" ON public.workers;
CREATE POLICY "app_workers_update" ON public.workers 
    FOR UPDATE USING (true);

DROP POLICY IF EXISTS "app_workers_delete" ON public.workers;
CREATE POLICY "app_workers_delete" ON public.workers 
    FOR DELETE USING (true);

-- Clients table policies
DROP POLICY IF EXISTS "app_clients_select" ON public.clients;
CREATE POLICY "app_clients_select" ON public.clients 
    FOR SELECT USING (deleted_at IS NULL);

DROP POLICY IF EXISTS "app_clients_insert" ON public.clients;
CREATE POLICY "app_clients_insert" ON public.clients 
    FOR INSERT WITH CHECK (true);

DROP POLICY IF EXISTS "app_clients_update" ON public.clients;
CREATE POLICY "app_clients_update" ON public.clients 
    FOR UPDATE USING (true);

DROP POLICY IF EXISTS "app_clients_delete" ON public.clients;
CREATE POLICY "app_clients_delete" ON public.clients 
    FOR DELETE USING (true);

-- Bookings table policies
DROP POLICY IF EXISTS "app_bookings_select" ON public.bookings;
CREATE POLICY "app_bookings_select" ON public.bookings 
    FOR SELECT USING (true);

DROP POLICY IF EXISTS "app_bookings_insert" ON public.bookings;
CREATE POLICY "app_bookings_insert" ON public.bookings 
    FOR INSERT WITH CHECK (true);

DROP POLICY IF EXISTS "app_bookings_update" ON public.bookings;
CREATE POLICY "app_bookings_update" ON public.bookings 
    FOR UPDATE USING (true);

-- Booking Assignments policies
DROP POLICY IF EXISTS "app_assignments_select" ON public.booking_assignments;
CREATE POLICY "app_assignments_select" ON public.booking_assignments 
    FOR SELECT USING (true);

DROP POLICY IF EXISTS "app_assignments_insert" ON public.booking_assignments;
CREATE POLICY "app_assignments_insert" ON public.booking_assignments 
    FOR INSERT WITH CHECK (true);

DROP POLICY IF EXISTS "app_assignments_update" ON public.booking_assignments;
CREATE POLICY "app_assignments_update" ON public.booking_assignments 
    FOR UPDATE USING (true);

-- Booking Offers policies
DROP POLICY IF EXISTS "app_offers_select" ON public.booking_offers;
CREATE POLICY "app_offers_select" ON public.booking_offers 
    FOR SELECT USING (true);

DROP POLICY IF EXISTS "app_offers_insert" ON public.booking_offers;
CREATE POLICY "app_offers_insert" ON public.booking_offers 
    FOR INSERT WITH CHECK (true);

DROP POLICY IF EXISTS "app_offers_update" ON public.booking_offers;
CREATE POLICY "app_offers_update" ON public.booking_offers 
    FOR UPDATE USING (true);
