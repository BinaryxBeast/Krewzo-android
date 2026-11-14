-- ============================================================
-- Krewzo — Migration 006: Stored Procedures & Atomic Business Logic
-- ============================================================

-- ── 1. User Session & Role Routing RPC ───────────────────────
-- Called right after OTP verification. Routes user to worker, admin,
-- existing client, or initiates new client onboarding.
CREATE OR REPLACE FUNCTION public.get_or_route_user_session(
    p_phone TEXT,
    p_auth_user_id UUID
)
RETURNS JSONB
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public, auth, pg_temp
AS $$
DECLARE
    v_worker      public.workers%ROWTYPE;
    v_client      public.clients%ROWTYPE;
    v_existing_u  public.users%ROWTYPE;
    v_clean_phone TEXT;
BEGIN
    -- Normalize phone string (trim whitespace)
    v_clean_phone := TRIM(p_phone);

    -- 1. Check if user already exists in public.users
    SELECT * INTO v_existing_u FROM public.users WHERE id = p_auth_user_id;

    -- 2. Worker check: Is this phone pre-registered as a worker by Admin?
    SELECT * INTO v_worker FROM public.workers 
    WHERE phone = v_clean_phone AND deleted_at IS NULL;

    IF FOUND THEN
        -- Link worker record with auth user if not linked yet
        UPDATE public.workers
        SET user_id = p_auth_user_id,
            updated_at = NOW()
        WHERE id = v_worker.id;

        -- Ensure user role is worker
        INSERT INTO public.users (id, phone, role, full_name, avatar_url, is_active)
        VALUES (p_auth_user_id, v_clean_phone, 'worker', v_worker.full_name, v_worker.photo_url, v_worker.is_active)
        ON CONFLICT (id) DO UPDATE
        SET role = 'worker',
            full_name = EXCLUDED.full_name,
            avatar_url = EXCLUDED.avatar_url,
            is_active = EXCLUDED.is_active,
            updated_at = NOW();

        RETURN jsonb_build_object(
            'role', 'worker',
            'is_onboarded', TRUE,
            'worker_id', v_worker.id,
            'user_id', p_auth_user_id,
            'full_name', v_worker.full_name,
            'is_available', v_worker.is_available
        );
    END IF;

    -- 3. Admin check: Was this user designated as Admin?
    IF v_existing_u.role = 'admin' THEN
        RETURN jsonb_build_object(
            'role', 'admin',
            'is_onboarded', TRUE,
            'user_id', p_auth_user_id,
            'full_name', v_existing_u.full_name
        );
    END IF;

    -- 4. Existing Client check
    SELECT * INTO v_client FROM public.clients 
    WHERE user_id = p_auth_user_id AND deleted_at IS NULL;

    IF FOUND THEN
        UPDATE public.users
        SET role = 'client', updated_at = NOW()
        WHERE id = p_auth_user_id;

        RETURN jsonb_build_object(
            'role', 'client',
            'is_onboarded', TRUE,
            'client_id', v_client.id,
            'user_id', p_auth_user_id,
            'business_name', v_client.business_name
        );
    END IF;

    -- 5. No match -> Brand new client requiring onboarding
    INSERT INTO public.users (id, phone, role)
    VALUES (p_auth_user_id, v_clean_phone, 'client')
    ON CONFLICT (id) DO UPDATE
    SET role = 'client', updated_at = NOW();

    RETURN jsonb_build_object(
        'role', 'client',
        'is_onboarded', FALSE,
        'user_id', p_auth_user_id,
        'phone', v_clean_phone
    );
END;
$$;


-- ── 2. Complete Client Onboarding RPC ────────────────────────
CREATE OR REPLACE FUNCTION public.complete_client_onboarding(
    p_user_id        UUID,
    p_business_name  TEXT,
    p_employee_count INTEGER,
    p_address_line   TEXT,
    p_city           TEXT DEFAULT 'Pune',
    p_state          TEXT DEFAULT 'Maharashtra',
    p_pincode        TEXT DEFAULT '411001',
    p_latitude       DOUBLE PRECISION DEFAULT NULL,
    p_longitude      DOUBLE PRECISION DEFAULT NULL,
    p_logo_url       TEXT DEFAULT NULL
)
RETURNS JSONB
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public, auth, pg_temp
AS $$
DECLARE
    v_client_id UUID;
    v_coords    GEOGRAPHY(POINT, 4326) := NULL;
BEGIN
    IF p_latitude IS NOT NULL AND p_longitude IS NOT NULL THEN
        v_coords := ST_SetSRID(ST_MakePoint(p_longitude, p_latitude), 4326)::GEOGRAPHY;
    END IF;

    -- Insert or update client profile
    INSERT INTO public.clients (
        user_id, business_name, employee_count, logo_url
    )
    VALUES (
        p_user_id, p_business_name, p_employee_count, p_logo_url
    )
    ON CONFLICT (user_id) DO UPDATE
    SET business_name = EXCLUDED.business_name,
        employee_count = EXCLUDED.employee_count,
        logo_url = COALESCE(EXCLUDED.logo_url, clients.logo_url),
        deleted_at = NULL,
        updated_at = NOW()
    RETURNING id INTO v_client_id;

    -- Insert primary default location
    INSERT INTO public.client_locations (
        client_id, label, address_line, city, state, pincode, coordinates, is_default
    )
    VALUES (
        v_client_id, 'Primary HQ', p_address_line, p_city, p_state, p_pincode, v_coords, TRUE
    );

    RETURN jsonb_build_object(
        'success', TRUE,
        'client_id', v_client_id,
        'role', 'client',
        'is_onboarded', TRUE
    );
END;
$$;


-- ── 3. Find Eligible Workers Spatial Matching RPC ────────────
-- Queries PostGIS for available workers nearest to work_coordinates
-- with zero time conflicts and wave deduplication.
CREATE OR REPLACE FUNCTION public.find_eligible_workers(
    p_booking_id         UUID,
    p_limit              INTEGER DEFAULT 5,
    p_max_radius_meters  DOUBLE PRECISION DEFAULT 30000 -- 30 km default
)
RETURNS TABLE (
    worker_id       UUID,
    user_id         UUID,
    full_name       TEXT,
    phone           TEXT,
    photo_url       TEXT,
    distance_meters NUMERIC(10, 2),
    fcm_token       TEXT
)
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public, auth, pg_temp
AS $$
DECLARE
    v_booking public.bookings%ROWTYPE;
    v_start_ts TIMESTAMPTZ;
    v_end_ts   TIMESTAMPTZ;
BEGIN
    -- Fetch booking details
    SELECT * INTO v_booking FROM public.bookings WHERE id = p_booking_id;
    IF NOT FOUND THEN
        RAISE EXCEPTION 'Booking % not found', p_booking_id;
    END IF;

    -- Calculate timestamp range for conflict checking
    v_start_ts := (v_booking.work_date + v_booking.work_start_time)::TIMESTAMPTZ;
    v_end_ts   := (v_booking.work_date + COALESCE(v_booking.work_end_time, (v_booking.work_start_time + INTERVAL '8 hours')::TIME))::TIMESTAMPTZ;

    RETURN QUERY
    SELECT
        w.id AS worker_id,
        w.user_id,
        w.full_name,
        w.phone,
        w.photo_url,
        ROUND(ST_Distance(w.home_coordinates, v_booking.work_coordinates)::NUMERIC, 2) AS distance_meters,
        ft.token AS fcm_token
    FROM public.workers w
    LEFT JOIN LATERAL (
        -- Get the latest registered FCM token for the worker's user
        SELECT token FROM public.fcm_tokens
        WHERE user_id = w.user_id
        ORDER BY updated_at DESC
        LIMIT 1
    ) ft ON TRUE
    WHERE
        w.is_active = TRUE
        AND w.is_available = TRUE
        AND w.deleted_at IS NULL
        AND w.home_coordinates IS NOT NULL
        -- Distance limit (spatial GiST acceleration)
        AND ST_DWithin(w.home_coordinates, v_booking.work_coordinates, p_max_radius_meters)
        -- Exclude workers who have already received an offer for this booking
        AND NOT EXISTS (
            SELECT 1 FROM public.booking_offers bo
            WHERE bo.booking_id = p_booking_id
              AND bo.worker_id = w.id
        )
        -- Exclude workers with overlapping confirmed/in_progress assignments
        AND NOT EXISTS (
            SELECT 1 FROM public.booking_assignments ba
            JOIN public.bookings ob ON ob.id = ba.booking_id
            WHERE ba.worker_id = w.id
              AND ba.removed_at IS NULL
              AND ob.status IN ('confirmed', 'in_progress')
              AND ob.work_date = v_booking.work_date
              -- Check time overlap
              AND (
                  (ob.work_date + ob.work_start_time)::TIMESTAMPTZ < v_end_ts
                  AND
                  (ob.work_date + COALESCE(ob.work_end_time, (ob.work_start_time + INTERVAL '8 hours')::TIME))::TIMESTAMPTZ > v_start_ts
              )
        )
        -- Exclude workers with scheduled blackout unavailability
        AND NOT EXISTS (
            SELECT 1 FROM public.worker_availability wa
            WHERE wa.worker_id = w.id
              AND v_start_ts < wa.unavailable_to
              AND v_end_ts > wa.unavailable_from
        )
    ORDER BY ST_Distance(w.home_coordinates, v_booking.work_coordinates) ASC
    LIMIT p_limit;
END;
$$;


-- ── 4. Atomic Concurrency-Safe Accept Offer RPC ──────────────
-- Uses SELECT FOR UPDATE on bookings to prevent overbooking races.
CREATE OR REPLACE FUNCTION public.accept_booking_offer(
    p_offer_id       UUID,
    p_worker_user_id UUID
)
RETURNS JSONB
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public, auth, pg_temp
AS $$
DECLARE
    v_offer            public.booking_offers%ROWTYPE;
    v_booking          public.bookings%ROWTYPE;
    v_worker           public.workers%ROWTYPE;
    v_assignment_id    UUID;
    v_new_accepted     INTEGER;
BEGIN
    -- 1. Verify worker identity from caller auth ID
    SELECT * INTO v_worker FROM public.workers
    WHERE user_id = p_worker_user_id AND deleted_at IS NULL;

    IF NOT FOUND THEN
        RETURN jsonb_build_object('success', FALSE, 'error_code', 'NOT_A_WORKER', 'message', 'Worker record not found');
    END IF;

    -- 2. Lock the offer row
    SELECT * INTO v_offer
    FROM public.booking_offers
    WHERE id = p_offer_id
    FOR UPDATE;

    IF NOT FOUND THEN
        RETURN jsonb_build_object('success', FALSE, 'error_code', 'OFFER_NOT_FOUND', 'message', 'Offer does not exist');
    END IF;

    IF v_offer.worker_id != v_worker.id THEN
        RETURN jsonb_build_object('success', FALSE, 'error_code', 'FORBIDDEN', 'message', 'This offer does not belong to you');
    END IF;

    IF v_offer.status != 'pending' THEN
        RETURN jsonb_build_object('success', FALSE, 'error_code', 'OFFER_INACTIVE', 'message', 'Offer is already ' || v_offer.status::TEXT);
    END IF;

    IF v_offer.expires_at < NOW() THEN
        UPDATE public.booking_offers SET status = 'expired', updated_at = NOW() WHERE id = p_offer_id;
        RETURN jsonb_build_object('success', FALSE, 'error_code', 'OFFER_EXPIRED', 'message', 'This job offer has expired');
    END IF;

    -- 3. Lock the booking row (CRITICAL for concurrency safety)
    SELECT * INTO v_booking
    FROM public.bookings
    WHERE id = v_offer.booking_id
    FOR UPDATE;

    IF v_booking.status NOT IN ('searching', 'partially_filled') THEN
        UPDATE public.booking_offers SET status = 'cancelled', updated_at = NOW() WHERE id = p_offer_id;
        RETURN jsonb_build_object('success', FALSE, 'error_code', 'BOOKING_CLOSED', 'message', 'This booking has already been filled or cancelled');
    END IF;

    IF v_booking.workers_accepted >= v_booking.workers_needed THEN
        UPDATE public.booking_offers SET status = 'cancelled', updated_at = NOW() WHERE id = p_offer_id;
        RETURN jsonb_build_object('success', FALSE, 'error_code', 'SLOT_FULL', 'message', 'All requested slots have just been filled');
    END IF;

    -- 4. Mark offer as accepted
    UPDATE public.booking_offers
    SET status = 'accepted',
        responded_at = NOW(),
        updated_at = NOW()
    WHERE id = p_offer_id;

    -- 5. Create assignment (Trigger trg_sync_booking_status handles auto-increment & confirmation)
    INSERT INTO public.booking_assignments (
        booking_id, worker_id, offer_id, amount_paid, payment_status
    )
    VALUES (
        v_booking.id, v_worker.id, v_offer.id, v_booking.pay_per_worker, 'pending'
    )
    RETURNING id INTO v_assignment_id;

    -- Read refreshed booking state
    SELECT workers_accepted, status INTO v_new_accepted, v_booking.status
    FROM public.bookings
    WHERE id = v_booking.id;

    RETURN jsonb_build_object(
        'success', TRUE,
        'assignment_id', v_assignment_id,
        'booking_id', v_booking.id,
        'workers_accepted', v_new_accepted,
        'workers_needed', v_booking.workers_needed,
        'is_confirmed', (v_booking.status = 'confirmed'),
        'message', 'Job accepted successfully'
    );
END;
$$;


-- ── 5. Decline Offer RPC ─────────────────────────────────────
CREATE OR REPLACE FUNCTION public.decline_booking_offer(
    p_offer_id       UUID,
    p_worker_user_id UUID
)
RETURNS JSONB
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public, auth, pg_temp
AS $$
DECLARE
    v_offer  public.booking_offers%ROWTYPE;
    v_worker public.workers%ROWTYPE;
BEGIN
    SELECT * INTO v_worker FROM public.workers WHERE user_id = p_worker_user_id;
    IF NOT FOUND THEN
        RETURN jsonb_build_object('success', FALSE, 'error_code', 'NOT_A_WORKER');
    END IF;

    SELECT * INTO v_offer FROM public.booking_offers WHERE id = p_offer_id FOR UPDATE;
    IF NOT FOUND OR v_offer.worker_id != v_worker.id THEN
        RETURN jsonb_build_object('success', FALSE, 'error_code', 'INVALID_OFFER');
    END IF;

    IF v_offer.status != 'pending' THEN
        RETURN jsonb_build_object('success', FALSE, 'error_code', 'OFFER_NOT_PENDING');
    END IF;

    UPDATE public.booking_offers
    SET status = 'declined',
        responded_at = NOW(),
        updated_at = NOW()
    WHERE id = p_offer_id;

    RETURN jsonb_build_object(
        'success', TRUE,
        'booking_id', v_offer.booking_id,
        'message', 'Offer declined'
    );
END;
$$;


-- ── 6. Create Wave Offers Batch RPC ──────────────────────────
CREATE OR REPLACE FUNCTION public.create_booking_wave_offers(
    p_booking_id       UUID,
    p_wave_number      INTEGER,
    p_worker_ids       UUID[],
    p_timeout_minutes  INTEGER DEFAULT 10
)
RETURNS TABLE (
    offer_id    UUID,
    worker_id   UUID,
    distance_m  NUMERIC(10, 2),
    expires_at  TIMESTAMPTZ
)
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public, auth, pg_temp
AS $$
DECLARE
    v_booking    public.bookings%ROWTYPE;
    v_expires_at TIMESTAMPTZ;
BEGIN
    SELECT * INTO v_booking FROM public.bookings WHERE id = p_booking_id;
    v_expires_at := NOW() + (p_timeout_minutes || ' minutes')::INTERVAL;

    -- Update booking current wave & expiration
    UPDATE public.bookings
    SET current_wave = p_wave_number,
        offer_expires_at = v_expires_at,
        updated_at = NOW()
    WHERE id = p_booking_id;

    RETURN QUERY
    INSERT INTO public.booking_offers (
        booking_id,
        worker_id,
        wave_number,
        status,
        distance_m,
        expires_at
    )
    SELECT
        p_booking_id,
        w_id,
        p_wave_number,
        'pending'::offer_status,
        ROUND(ST_Distance(w.home_coordinates, v_booking.work_coordinates)::NUMERIC, 2),
        v_expires_at
    FROM UNNEST(p_worker_ids) AS w_id
    JOIN public.workers w ON w.id = w_id
    ON CONFLICT (booking_id, worker_id) DO NOTHING
    RETURNING id AS offer_id, booking_offers.worker_id, booking_offers.distance_m, booking_offers.expires_at;
END;
$$;
