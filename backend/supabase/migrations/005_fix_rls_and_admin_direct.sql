-- ============================================================
-- Krewzo — Migration 005: RLS Recursion Fix & Admin Direct Management
-- ============================================================

-- ── 1. Break RLS Infinite Recursion via SECURITY DEFINER ─────
CREATE OR REPLACE FUNCTION public.get_client_assigned_worker_ids(p_client_id UUID)
RETURNS SETOF UUID
LANGUAGE sql
SECURITY DEFINER
SET search_path = public
STABLE
AS $$
    SELECT ba.worker_id
    FROM public.booking_assignments ba
    JOIN public.bookings b ON b.id = ba.booking_id
    WHERE b.client_id = p_client_id
      AND b.status IN ('confirmed', 'in_progress', 'completed')
      AND ba.removed_at IS NULL;
$$;

CREATE OR REPLACE FUNCTION public.get_worker_offered_booking_ids(p_worker_id UUID)
RETURNS SETOF UUID
LANGUAGE sql
SECURITY DEFINER
SET search_path = public
STABLE
AS $$
    SELECT booking_id
    FROM public.booking_offers
    WHERE worker_id = p_worker_id;
$$;

CREATE OR REPLACE FUNCTION public.get_client_booking_ids(p_client_id UUID)
RETURNS SETOF UUID
LANGUAGE sql
SECURITY DEFINER
SET search_path = public
STABLE
AS $$
    SELECT id
    FROM public.bookings
    WHERE client_id = p_client_id;
$$;

-- Replace recursive policies with non-recursive function calls
DROP POLICY IF EXISTS "workers: client reads assigned" ON public.workers;
CREATE POLICY "workers: client reads assigned" ON public.workers FOR SELECT
    USING (
        current_user_role() = 'client'
        AND id IN (SELECT public.get_client_assigned_worker_ids(current_client_id()))
    );

DROP POLICY IF EXISTS "bookings: worker reads offered" ON public.bookings;
CREATE POLICY "bookings: worker reads offered" ON public.bookings FOR SELECT
    USING (
        current_user_role() = 'worker'
        AND id IN (SELECT public.get_worker_offered_booking_ids(current_worker_id()))
    );

DROP POLICY IF EXISTS "booking_offers: client reads own booking offers" ON public.booking_offers;
CREATE POLICY "booking_offers: client reads own booking offers" ON public.booking_offers FOR SELECT
    USING (
        current_user_role() = 'client'
        AND booking_id IN (SELECT public.get_client_booking_ids(current_client_id()))
    );


-- ── 2. Allow Direct Client Management by Admin ───────────────
-- Allow client creation without pre-existing auth.user row
ALTER TABLE public.clients ALTER COLUMN user_id DROP NOT NULL;

-- Add direct admin-managed fields to clients table
ALTER TABLE public.clients ADD COLUMN IF NOT EXISTS contact_person TEXT;
ALTER TABLE public.clients ADD COLUMN IF NOT EXISTS phone TEXT;
ALTER TABLE public.clients ADD COLUMN IF NOT EXISTS email TEXT;
ALTER TABLE public.clients ADD COLUMN IF NOT EXISTS address TEXT;
ALTER TABLE public.clients ADD COLUMN IF NOT EXISTS city TEXT DEFAULT 'Pune';
ALTER TABLE public.clients ADD COLUMN IF NOT EXISTS pincode TEXT DEFAULT '411001';
ALTER TABLE public.clients ADD COLUMN IF NOT EXISTS notes TEXT;
ALTER TABLE public.clients ADD COLUMN IF NOT EXISTS is_active BOOLEAN DEFAULT TRUE;

-- Add skills array directly to workers table for fast single-row read/write
ALTER TABLE public.workers ADD COLUMN IF NOT EXISTS skills TEXT[] DEFAULT '{}';

-- Allow bookings without strict GPS coordinates
ALTER TABLE public.bookings ALTER COLUMN work_coordinates DROP NOT NULL;


-- ── 3. Dev / Admin Direct Access Policies ────────────────────
-- Enables the Android app to read, add, update and delete
-- workers and clients directly with the Supabase anon/publishable key.

DROP POLICY IF EXISTS "dev_anon_workers_select" ON public.workers;
CREATE POLICY "dev_anon_workers_select" ON public.workers FOR SELECT USING (true);

DROP POLICY IF EXISTS "dev_anon_workers_insert" ON public.workers;
CREATE POLICY "dev_anon_workers_insert" ON public.workers FOR INSERT WITH CHECK (true);

DROP POLICY IF EXISTS "dev_anon_workers_update" ON public.workers;
CREATE POLICY "dev_anon_workers_update" ON public.workers FOR UPDATE USING (true);

DROP POLICY IF EXISTS "dev_anon_workers_delete" ON public.workers;
CREATE POLICY "dev_anon_workers_delete" ON public.workers FOR DELETE USING (true);


DROP POLICY IF EXISTS "dev_anon_clients_select" ON public.clients;
CREATE POLICY "dev_anon_clients_select" ON public.clients FOR SELECT USING (true);

DROP POLICY IF EXISTS "dev_anon_clients_insert" ON public.clients;
CREATE POLICY "dev_anon_clients_insert" ON public.clients FOR INSERT WITH CHECK (true);

DROP POLICY IF EXISTS "dev_anon_clients_update" ON public.clients;
CREATE POLICY "dev_anon_clients_update" ON public.clients FOR UPDATE USING (true);

DROP POLICY IF EXISTS "dev_anon_clients_delete" ON public.clients;
CREATE POLICY "dev_anon_clients_delete" ON public.clients FOR DELETE USING (true);


DROP POLICY IF EXISTS "dev_anon_bookings_select" ON public.bookings;
CREATE POLICY "dev_anon_bookings_select" ON public.bookings FOR SELECT USING (true);

DROP POLICY IF EXISTS "dev_anon_bookings_insert" ON public.bookings;
CREATE POLICY "dev_anon_bookings_insert" ON public.bookings FOR INSERT WITH CHECK (true);

DROP POLICY IF EXISTS "dev_anon_bookings_update" ON public.bookings;
CREATE POLICY "dev_anon_bookings_update" ON public.bookings FOR UPDATE USING (true);
