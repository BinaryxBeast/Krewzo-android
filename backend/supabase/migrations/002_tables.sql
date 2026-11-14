-- ============================================================
-- Krewzo — Migration 002: Core User & Role Tables
-- ============================================================

-- ── users ────────────────────────────────────────────────────
-- Mirrors auth.users. Created by trigger on signup.
-- Owns the role and phone number.
CREATE TABLE IF NOT EXISTS public.users (
    id              UUID PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
    phone           TEXT NOT NULL UNIQUE,
    role            user_role NOT NULL,
    full_name       TEXT,
    avatar_url      TEXT,                        -- Supabase Storage URL
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    deleted_at      TIMESTAMPTZ,                 -- Soft delete
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Index for phone lookup (used every login)
CREATE INDEX IF NOT EXISTS idx_users_phone ON public.users(phone);
CREATE INDEX IF NOT EXISTS idx_users_role  ON public.users(role);
-- Partial index: only active, non-deleted users
CREATE INDEX IF NOT EXISTS idx_users_active ON public.users(id) WHERE is_active = TRUE AND deleted_at IS NULL;


-- ── clients ──────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS public.clients (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id             UUID NOT NULL UNIQUE REFERENCES public.users(id) ON DELETE CASCADE,
    business_name       TEXT NOT NULL,
    employee_count      INTEGER,
    logo_url            TEXT,                    -- Supabase Storage URL
    gst_number          TEXT,
    billing_address     TEXT,
    is_verified         BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at          TIMESTAMPTZ,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_clients_user_id ON public.clients(user_id);


-- ── client_locations ─────────────────────────────────────────
-- A client can have multiple work sites.
-- The primary/default address is used pre-filled in bookings.
CREATE TABLE IF NOT EXISTS public.client_locations (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    client_id       UUID NOT NULL REFERENCES public.clients(id) ON DELETE CASCADE,
    label           TEXT NOT NULL DEFAULT 'Primary',  -- "HQ", "Warehouse A", etc.
    address_line    TEXT NOT NULL,
    city            TEXT NOT NULL DEFAULT 'Pune',
    state           TEXT NOT NULL DEFAULT 'Maharashtra',
    pincode         TEXT NOT NULL,
    coordinates     GEOGRAPHY(POINT, 4326),      -- PostGIS spatial point
    is_default      BOOLEAN NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Spatial index for proximity searches
CREATE INDEX IF NOT EXISTS idx_client_locations_coords ON public.client_locations USING GIST(coordinates);
CREATE INDEX IF NOT EXISTS idx_client_locations_client ON public.client_locations(client_id);


-- ── workers ──────────────────────────────────────────────────
-- Workers are created by admins only, not self-registered.
CREATE TABLE IF NOT EXISTS public.workers (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id             UUID UNIQUE REFERENCES public.users(id) ON DELETE SET NULL,
    phone               TEXT NOT NULL UNIQUE,
    full_name           TEXT NOT NULL,
    date_of_birth       DATE,
    age                 INTEGER,                 -- Direct integer age (avoiding non-immutable generation)
    photo_url           TEXT,                    -- Supabase Storage URL
    home_address        TEXT,
    home_coordinates    GEOGRAPHY(POINT, 4326),  -- For wave-based matching
    emergency_contact   TEXT,
    bank_account_number BYTEA,                   -- pgcrypto encrypted
    bank_ifsc           TEXT,
    is_available        BOOLEAN NOT NULL DEFAULT FALSE,  -- Worker's online/offline toggle
    is_active           BOOLEAN NOT NULL DEFAULT TRUE,   -- Admin can deactivate
    total_jobs_completed INTEGER NOT NULL DEFAULT 0,
    total_earnings      NUMERIC(12, 2) NOT NULL DEFAULT 0,
    rating              NUMERIC(3, 2),           -- 0.00–5.00, updated after each job
    admin_notes         TEXT,
    deleted_at          TIMESTAMPTZ,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Spatial index on home location (critical for wave-matching queries)
CREATE INDEX IF NOT EXISTS idx_workers_home_coords    ON public.workers USING GIST(home_coordinates);
CREATE INDEX IF NOT EXISTS idx_workers_phone         ON public.workers(phone);
CREATE INDEX IF NOT EXISTS idx_workers_available     ON public.workers(is_available) WHERE is_active = TRUE AND deleted_at IS NULL;


-- ── worker_documents ─────────────────────────────────────────
CREATE TABLE IF NOT EXISTS public.worker_documents (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    worker_id       UUID NOT NULL REFERENCES public.workers(id) ON DELETE CASCADE,
    doc_type        document_type NOT NULL,
    doc_number      BYTEA NOT NULL,              -- pgcrypto encrypted
    doc_url         TEXT,                        -- Supabase Storage (private bucket)
    verified_at     TIMESTAMPTZ,
    verified_by     UUID REFERENCES public.users(id),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE(worker_id, doc_type)
);

CREATE INDEX IF NOT EXISTS idx_worker_docs_worker ON public.worker_documents(worker_id);


-- ── worker_skills ─────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS public.worker_skills (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    worker_id   UUID NOT NULL REFERENCES public.workers(id) ON DELETE CASCADE,
    skill       skill_category NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE(worker_id, skill)
);

CREATE INDEX IF NOT EXISTS idx_worker_skills_worker ON public.worker_skills(worker_id);
CREATE INDEX IF NOT EXISTS idx_worker_skills_skill  ON public.worker_skills(skill);


-- ── worker_availability ──────────────────────────────────────
CREATE TABLE IF NOT EXISTS public.worker_availability (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    worker_id       UUID NOT NULL REFERENCES public.workers(id) ON DELETE CASCADE,
    unavailable_from TIMESTAMPTZ NOT NULL,
    unavailable_to   TIMESTAMPTZ NOT NULL,
    reason          TEXT,                        -- e.g., "Annual leave"
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_availability_order CHECK (unavailable_to > unavailable_from)
);

CREATE INDEX IF NOT EXISTS idx_worker_avail_worker ON public.worker_availability(worker_id);
CREATE INDEX IF NOT EXISTS idx_worker_avail_range  ON public.worker_availability(unavailable_from, unavailable_to);


-- ── fcm_tokens ───────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS public.fcm_tokens (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID NOT NULL REFERENCES public.users(id) ON DELETE CASCADE,
    token       TEXT NOT NULL UNIQUE,
    device_id   TEXT,
    platform    TEXT NOT NULL DEFAULT 'android',
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_fcm_tokens_user ON public.fcm_tokens(user_id);


-- ── bookings ─────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS public.bookings (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    client_id           UUID NOT NULL REFERENCES public.clients(id),
    location_id         UUID REFERENCES public.client_locations(id),
    workers_needed      INTEGER NOT NULL CHECK (workers_needed > 0),
    workers_accepted    INTEGER NOT NULL DEFAULT 0,
    pay_per_worker      NUMERIC(10, 2) NOT NULL CHECK (pay_per_worker > 0),
    work_address        TEXT NOT NULL,
    work_coordinates    GEOGRAPHY(POINT, 4326) NOT NULL,
    work_date           DATE NOT NULL,
    work_start_time     TIME NOT NULL,
    work_end_time       TIME,
    status              booking_status NOT NULL DEFAULT 'searching',
    job_notes           TEXT,
    current_wave        INTEGER NOT NULL DEFAULT 1,
    offer_expires_at    TIMESTAMPTZ,
    confirmed_at        TIMESTAMPTZ,
    completed_at        TIMESTAMPTZ,
    cancelled_at        TIMESTAMPTZ,
    cancelled_by        UUID REFERENCES public.users(id),
    cancellation_reason TEXT,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_workers_accepted CHECK (workers_accepted <= workers_needed)
);

CREATE INDEX IF NOT EXISTS idx_bookings_work_coords ON public.bookings USING GIST(work_coordinates);
CREATE INDEX IF NOT EXISTS idx_bookings_client      ON public.bookings(client_id);
CREATE INDEX IF NOT EXISTS idx_bookings_status      ON public.bookings(status);
CREATE INDEX IF NOT EXISTS idx_bookings_work_date   ON public.bookings(work_date);


-- ── booking_offers ───────────────────────────────────────────
CREATE TABLE IF NOT EXISTS public.booking_offers (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    booking_id      UUID NOT NULL REFERENCES public.bookings(id) ON DELETE CASCADE,
    worker_id       UUID NOT NULL REFERENCES public.workers(id),
    wave_number     INTEGER NOT NULL DEFAULT 1,
    status          offer_status NOT NULL DEFAULT 'pending',
    distance_m      NUMERIC(10, 2),
    offered_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    responded_at    TIMESTAMPTZ,
    expires_at      TIMESTAMPTZ NOT NULL,
    push_sent       BOOLEAN NOT NULL DEFAULT FALSE,
    sms_sent        BOOLEAN NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE(booking_id, worker_id)
);

CREATE INDEX IF NOT EXISTS idx_offers_booking  ON public.booking_offers(booking_id);
CREATE INDEX IF NOT EXISTS idx_offers_worker   ON public.booking_offers(worker_id);
CREATE INDEX IF NOT EXISTS idx_offers_status   ON public.booking_offers(status);
CREATE INDEX IF NOT EXISTS idx_offers_pending  ON public.booking_offers(booking_id, status) WHERE status = 'pending';


-- ── booking_assignments ──────────────────────────────────────
CREATE TABLE IF NOT EXISTS public.booking_assignments (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    booking_id      UUID NOT NULL REFERENCES public.bookings(id) ON DELETE CASCADE,
    worker_id       UUID NOT NULL REFERENCES public.workers(id),
    offer_id        UUID NOT NULL REFERENCES public.booking_offers(id),
    assigned_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    checked_in_at   TIMESTAMPTZ,
    checked_out_at  TIMESTAMPTZ,
    amount_paid     NUMERIC(10, 2),
    payment_status  TEXT NOT NULL DEFAULT 'pending',
    removed_at      TIMESTAMPTZ,
    removed_by      UUID REFERENCES public.users(id),
    removal_reason  TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE(booking_id, worker_id)
);

CREATE INDEX IF NOT EXISTS idx_assignments_booking ON public.booking_assignments(booking_id);
CREATE INDEX IF NOT EXISTS idx_assignments_worker  ON public.booking_assignments(worker_id);


-- ── notifications_log ────────────────────────────────────────
CREATE TABLE IF NOT EXISTS public.notifications_log (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID REFERENCES public.users(id) ON DELETE SET NULL,
    booking_id      UUID REFERENCES public.bookings(id) ON DELETE SET NULL,
    offer_id        UUID REFERENCES public.booking_offers(id) ON DELETE SET NULL,
    channel         notification_channel NOT NULL,
    status          notification_status NOT NULL DEFAULT 'queued',
    title           TEXT,
    body            TEXT NOT NULL,
    recipient_phone TEXT,
    fcm_token       TEXT,
    provider_ref    TEXT,
    retry_count     INTEGER NOT NULL DEFAULT 0,
    last_error      TEXT,
    sent_at         TIMESTAMPTZ,
    delivered_at    TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_notif_user     ON public.notifications_log(user_id);
CREATE INDEX IF NOT EXISTS idx_notif_booking  ON public.notifications_log(booking_id);
CREATE INDEX IF NOT EXISTS idx_notif_status   ON public.notifications_log(status);
CREATE INDEX IF NOT EXISTS idx_notif_created  ON public.notifications_log(created_at DESC);


-- ── audit_log ────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS public.audit_log (
    id              BIGSERIAL PRIMARY KEY,
    actor_id        UUID REFERENCES public.users(id) ON DELETE SET NULL,
    actor_role      user_role,
    action          TEXT NOT NULL,
    entity_type     TEXT NOT NULL,
    entity_id       TEXT NOT NULL,
    old_data        JSONB,
    new_data        JSONB,
    ip_address      INET,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_audit_actor    ON public.audit_log(actor_id);
CREATE INDEX IF NOT EXISTS idx_audit_entity   ON public.audit_log(entity_type, entity_id);
CREATE INDEX IF NOT EXISTS idx_audit_created  ON public.audit_log(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_audit_action   ON public.audit_log(action);
