-- ============================================================
-- Krewzo — Migration 004: Row Level Security (RLS) Policies
-- ============================================================
-- Strategy:
--   • Enable RLS on every table (deny-by-default)
--   • Use helper functions (current_user_role, current_worker_id,
--     current_client_id) so policy code is DRY
--   • Service Role (Edge Functions) bypasses RLS by design —
--     all business logic runs server-side, never from the client
-- ============================================================


-- ── Enable RLS on all tables ─────────────────────────────────
ALTER TABLE public.users                ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.clients              ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.client_locations     ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.workers              ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.worker_documents     ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.worker_skills        ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.worker_availability  ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.fcm_tokens           ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.bookings             ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.booking_offers       ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.booking_assignments  ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.notifications_log    ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.audit_log            ENABLE ROW LEVEL SECURITY;


-- ============================================================
-- TABLE: users
-- ============================================================

-- Everyone can read their own row
DROP POLICY IF EXISTS "users: self read" ON public.users;
CREATE POLICY "users: self read"
    ON public.users FOR SELECT
    USING (id = auth.uid());

-- Clients can read basic info of workers assigned to their confirmed bookings
DROP POLICY IF EXISTS "users: client reads assigned workers" ON public.users;
CREATE POLICY "users: client reads assigned workers"
    ON public.users FOR SELECT
    USING (
        current_user_role() = 'client'
        AND role = 'worker'
        AND id IN (
            SELECT w.user_id
            FROM public.workers w
            JOIN public.booking_assignments ba ON ba.worker_id = w.id
            JOIN public.bookings b ON b.id = ba.booking_id
            WHERE b.client_id = current_client_id()
              AND b.status IN ('confirmed', 'in_progress', 'completed')
              AND ba.removed_at IS NULL
        )
    );

-- Admins read all
DROP POLICY IF EXISTS "users: admin read all" ON public.users;
CREATE POLICY "users: admin read all"
    ON public.users FOR SELECT
    USING (current_user_role() = 'admin');

-- Users update only their own row (name, avatar)
DROP POLICY IF EXISTS "users: self update" ON public.users;
CREATE POLICY "users: self update"
    ON public.users FOR UPDATE
    USING (id = auth.uid())
    WITH CHECK (id = auth.uid());

-- Admins can update any user (deactivate, role change)
DROP POLICY IF EXISTS "users: admin update" ON public.users;
CREATE POLICY "users: admin update"
    ON public.users FOR UPDATE
    USING (current_user_role() = 'admin');

-- No direct deletes — use soft delete (deleted_at)
-- Service role handles hard deletes only


-- ============================================================
-- TABLE: clients
-- ============================================================

DROP POLICY IF EXISTS "clients: self read" ON public.clients;
CREATE POLICY "clients: self read"
    ON public.clients FOR SELECT
    USING (user_id = auth.uid());

DROP POLICY IF EXISTS "clients: admin read all" ON public.clients;
CREATE POLICY "clients: admin read all"
    ON public.clients FOR SELECT
    USING (current_user_role() = 'admin');

DROP POLICY IF EXISTS "clients: self insert" ON public.clients;
CREATE POLICY "clients: self insert"
    ON public.clients FOR INSERT
    WITH CHECK (user_id = auth.uid() AND current_user_role() = 'client');

DROP POLICY IF EXISTS "clients: self update" ON public.clients;
CREATE POLICY "clients: self update"
    ON public.clients FOR UPDATE
    USING (user_id = auth.uid())
    WITH CHECK (user_id = auth.uid());

DROP POLICY IF EXISTS "clients: admin update" ON public.clients;
CREATE POLICY "clients: admin update"
    ON public.clients FOR UPDATE
    USING (current_user_role() = 'admin');


-- ============================================================
-- TABLE: client_locations
-- ============================================================

DROP POLICY IF EXISTS "client_locations: self read" ON public.client_locations;
CREATE POLICY "client_locations: self read"
    ON public.client_locations FOR SELECT
    USING (client_id = current_client_id());

DROP POLICY IF EXISTS "client_locations: admin read all" ON public.client_locations;
CREATE POLICY "client_locations: admin read all"
    ON public.client_locations FOR SELECT
    USING (current_user_role() = 'admin');

DROP POLICY IF EXISTS "client_locations: self insert" ON public.client_locations;
CREATE POLICY "client_locations: self insert"
    ON public.client_locations FOR INSERT
    WITH CHECK (client_id = current_client_id());

DROP POLICY IF EXISTS "client_locations: self update" ON public.client_locations;
CREATE POLICY "client_locations: self update"
    ON public.client_locations FOR UPDATE
    USING (client_id = current_client_id());

DROP POLICY IF EXISTS "client_locations: self delete" ON public.client_locations;
CREATE POLICY "client_locations: self delete"
    ON public.client_locations FOR DELETE
    USING (client_id = current_client_id());


-- ============================================================
-- TABLE: workers
-- ============================================================

-- Workers read their own record
DROP POLICY IF EXISTS "workers: self read" ON public.workers;
CREATE POLICY "workers: self read"
    ON public.workers FOR SELECT
    USING (user_id = auth.uid());

-- Clients read basic info of workers on their confirmed bookings
DROP POLICY IF EXISTS "workers: client reads assigned" ON public.workers;
CREATE POLICY "workers: client reads assigned"
    ON public.workers FOR SELECT
    USING (
        current_user_role() = 'client'
        AND id IN (
            SELECT ba.worker_id
            FROM public.booking_assignments ba
            JOIN public.bookings b ON b.id = ba.booking_id
            WHERE b.client_id = current_client_id()
              AND b.status IN ('confirmed', 'in_progress', 'completed')
              AND ba.removed_at IS NULL
        )
    );

-- Admins read all workers
DROP POLICY IF EXISTS "workers: admin read all" ON public.workers;
CREATE POLICY "workers: admin read all"
    ON public.workers FOR SELECT
    USING (current_user_role() = 'admin');

-- Workers update their own availability toggle only
DROP POLICY IF EXISTS "workers: self update availability" ON public.workers;
CREATE POLICY "workers: self update availability"
    ON public.workers FOR UPDATE
    USING (user_id = auth.uid())
    WITH CHECK (user_id = auth.uid());

-- Admins insert (create) workers
DROP POLICY IF EXISTS "workers: admin insert" ON public.workers;
CREATE POLICY "workers: admin insert"
    ON public.workers FOR INSERT
    WITH CHECK (current_user_role() = 'admin');

-- Admins update all worker fields
DROP POLICY IF EXISTS "workers: admin update" ON public.workers;
CREATE POLICY "workers: admin update"
    ON public.workers FOR UPDATE
    USING (current_user_role() = 'admin');


-- ============================================================
-- TABLE: worker_documents
-- ============================================================

-- Workers read their own documents
DROP POLICY IF EXISTS "worker_documents: self read" ON public.worker_documents;
CREATE POLICY "worker_documents: self read"
    ON public.worker_documents FOR SELECT
    USING (worker_id = current_worker_id());

-- Admins read and manage all documents
DROP POLICY IF EXISTS "worker_documents: admin read" ON public.worker_documents;
CREATE POLICY "worker_documents: admin read"
    ON public.worker_documents FOR SELECT
    USING (current_user_role() = 'admin');

DROP POLICY IF EXISTS "worker_documents: admin insert" ON public.worker_documents;
CREATE POLICY "worker_documents: admin insert"
    ON public.worker_documents FOR INSERT
    WITH CHECK (current_user_role() = 'admin');

DROP POLICY IF EXISTS "worker_documents: admin update" ON public.worker_documents;
CREATE POLICY "worker_documents: admin update"
    ON public.worker_documents FOR UPDATE
    USING (current_user_role() = 'admin');


-- ============================================================
-- TABLE: worker_skills
-- ============================================================

-- Workers read own skills
DROP POLICY IF EXISTS "worker_skills: self read" ON public.worker_skills;
CREATE POLICY "worker_skills: self read"
    ON public.worker_skills FOR SELECT
    USING (worker_id = current_worker_id());

-- Clients can see skills of assigned workers
DROP POLICY IF EXISTS "worker_skills: client reads assigned" ON public.worker_skills;
CREATE POLICY "worker_skills: client reads assigned"
    ON public.worker_skills FOR SELECT
    USING (
        current_user_role() = 'client'
        AND worker_id IN (
            SELECT ba.worker_id
            FROM public.booking_assignments ba
            JOIN public.bookings b ON b.id = ba.booking_id
            WHERE b.client_id = current_client_id()
              AND ba.removed_at IS NULL
        )
    );

DROP POLICY IF EXISTS "worker_skills: admin all" ON public.worker_skills;
CREATE POLICY "worker_skills: admin all"
    ON public.worker_skills FOR ALL
    USING (current_user_role() = 'admin');


-- ============================================================
-- TABLE: worker_availability
-- ============================================================

DROP POLICY IF EXISTS "worker_availability: self read" ON public.worker_availability;
CREATE POLICY "worker_availability: self read"
    ON public.worker_availability FOR SELECT
    USING (worker_id = current_worker_id());

DROP POLICY IF EXISTS "worker_availability: admin read" ON public.worker_availability;
CREATE POLICY "worker_availability: admin read"
    ON public.worker_availability FOR SELECT
    USING (current_user_role() = 'admin');

DROP POLICY IF EXISTS "worker_availability: admin insert" ON public.worker_availability;
CREATE POLICY "worker_availability: admin insert"
    ON public.worker_availability FOR INSERT
    WITH CHECK (current_user_role() = 'admin');


-- ============================================================
-- TABLE: fcm_tokens
-- ============================================================

-- Users manage only their own FCM token
DROP POLICY IF EXISTS "fcm_tokens: self read" ON public.fcm_tokens;
CREATE POLICY "fcm_tokens: self read"
    ON public.fcm_tokens FOR SELECT
    USING (user_id = auth.uid());

DROP POLICY IF EXISTS "fcm_tokens: self insert" ON public.fcm_tokens;
CREATE POLICY "fcm_tokens: self insert"
    ON public.fcm_tokens FOR INSERT
    WITH CHECK (user_id = auth.uid());

DROP POLICY IF EXISTS "fcm_tokens: self update" ON public.fcm_tokens;
CREATE POLICY "fcm_tokens: self update"
    ON public.fcm_tokens FOR UPDATE
    USING (user_id = auth.uid());

DROP POLICY IF EXISTS "fcm_tokens: self delete" ON public.fcm_tokens;
CREATE POLICY "fcm_tokens: self delete"
    ON public.fcm_tokens FOR DELETE
    USING (user_id = auth.uid());

-- Admins can read all (for debugging notification issues)
DROP POLICY IF EXISTS "fcm_tokens: admin read" ON public.fcm_tokens;
CREATE POLICY "fcm_tokens: admin read"
    ON public.fcm_tokens FOR SELECT
    USING (current_user_role() = 'admin');


-- ============================================================
-- TABLE: bookings
-- ============================================================

-- Clients read their own bookings
DROP POLICY IF EXISTS "bookings: client self read" ON public.bookings;
CREATE POLICY "bookings: client self read"
    ON public.bookings FOR SELECT
    USING (client_id = current_client_id());

-- Workers read bookings they have an offer or assignment for
DROP POLICY IF EXISTS "bookings: worker reads offered" ON public.bookings;
CREATE POLICY "bookings: worker reads offered"
    ON public.bookings FOR SELECT
    USING (
        current_user_role() = 'worker'
        AND id IN (
            SELECT booking_id FROM public.booking_offers
            WHERE worker_id = current_worker_id()
        )
    );

-- Admins read all bookings
DROP POLICY IF EXISTS "bookings: admin read all" ON public.bookings;
CREATE POLICY "bookings: admin read all"
    ON public.bookings FOR SELECT
    USING (current_user_role() = 'admin');

-- Clients create bookings for themselves
DROP POLICY IF EXISTS "bookings: client insert" ON public.bookings;
CREATE POLICY "bookings: client insert"
    ON public.bookings FOR INSERT
    WITH CHECK (
        client_id = current_client_id()
        AND current_user_role() = 'client'
    );

-- Clients can cancel their own bookings (status update only via Edge Function)
-- Direct UPDATE is blocked; use Edge Function for state transitions
-- Admins can update any booking
DROP POLICY IF EXISTS "bookings: admin update" ON public.bookings;
CREATE POLICY "bookings: admin update"
    ON public.bookings FOR UPDATE
    USING (current_user_role() = 'admin');

-- No direct deletes on bookings (use cancelled status)


-- ============================================================
-- TABLE: booking_offers
-- ============================================================

-- Workers read offers sent to them
DROP POLICY IF EXISTS "booking_offers: worker self read" ON public.booking_offers;
CREATE POLICY "booking_offers: worker self read"
    ON public.booking_offers FOR SELECT
    USING (worker_id = current_worker_id());

-- Clients read offers on their bookings (to see worker pipeline)
DROP POLICY IF EXISTS "booking_offers: client reads own booking offers" ON public.booking_offers;
CREATE POLICY "booking_offers: client reads own booking offers"
    ON public.booking_offers FOR SELECT
    USING (
        current_user_role() = 'client'
        AND booking_id IN (
            SELECT id FROM public.bookings
            WHERE client_id = current_client_id()
        )
    );

-- Admins read all
DROP POLICY IF EXISTS "booking_offers: admin read all" ON public.booking_offers;
CREATE POLICY "booking_offers: admin read all"
    ON public.booking_offers FOR SELECT
    USING (current_user_role() = 'admin');

-- Workers can update ONLY their own offer status (accept/decline)
-- and ONLY if currently pending
DROP POLICY IF EXISTS "booking_offers: worker respond" ON public.booking_offers;
CREATE POLICY "booking_offers: worker respond"
    ON public.booking_offers FOR UPDATE
    USING (
        worker_id = current_worker_id()
        AND status = 'pending'
    )
    WITH CHECK (
        worker_id = current_worker_id()
        AND status IN ('accepted', 'declined')
    );

-- Admins can update any offer
DROP POLICY IF EXISTS "booking_offers: admin update" ON public.booking_offers;
CREATE POLICY "booking_offers: admin update"
    ON public.booking_offers FOR UPDATE
    USING (current_user_role() = 'admin');


-- ============================================================
-- TABLE: booking_assignments
-- ============================================================

-- Workers read their own assignments
DROP POLICY IF EXISTS "booking_assignments: worker self read" ON public.booking_assignments;
CREATE POLICY "booking_assignments: worker self read"
    ON public.booking_assignments FOR SELECT
    USING (worker_id = current_worker_id());

-- Clients read assignments on their confirmed bookings
DROP POLICY IF EXISTS "booking_assignments: client reads own" ON public.booking_assignments;
CREATE POLICY "booking_assignments: client reads own"
    ON public.booking_assignments FOR SELECT
    USING (
        current_user_role() = 'client'
        AND booking_id IN (
            SELECT id FROM public.bookings
            WHERE client_id = current_client_id()
        )
    );

-- Admins read and manage all
DROP POLICY IF EXISTS "booking_assignments: admin all" ON public.booking_assignments;
CREATE POLICY "booking_assignments: admin all"
    ON public.booking_assignments FOR ALL
    USING (current_user_role() = 'admin');


-- ============================================================
-- TABLE: notifications_log
-- ============================================================

-- Users read their own notifications
DROP POLICY IF EXISTS "notifications_log: self read" ON public.notifications_log;
CREATE POLICY "notifications_log: self read"
    ON public.notifications_log FOR SELECT
    USING (user_id = auth.uid());

-- Admins read all
DROP POLICY IF EXISTS "notifications_log: admin read all" ON public.notifications_log;
CREATE POLICY "notifications_log: admin read all"
    ON public.notifications_log FOR SELECT
    USING (current_user_role() = 'admin');

-- No inserts/updates from client — Edge Functions use service role


-- ============================================================
-- TABLE: audit_log
-- ============================================================

-- Admins read all audit logs
DROP POLICY IF EXISTS "audit_log: admin read" ON public.audit_log;
CREATE POLICY "audit_log: admin read"
    ON public.audit_log FOR SELECT
    USING (current_user_role() = 'admin');

-- Append-only: NO update or delete allowed for anyone
-- (Service role can still delete for data retention, but that's
--  done outside RLS via scheduled maintenance jobs)
