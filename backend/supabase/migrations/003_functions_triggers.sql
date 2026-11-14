-- ============================================================
-- Krewzo — Migration 003: Functions & Triggers
-- ============================================================

-- ── updated_at auto-maintenance ──────────────────────────────
CREATE OR REPLACE FUNCTION set_updated_at()
RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$;

-- Apply to all tables with updated_at
DO $$
DECLARE
    t TEXT;
BEGIN
    FOREACH t IN ARRAY ARRAY[
        'users', 'clients', 'client_locations', 'workers',
        'worker_documents', 'worker_availability', 'fcm_tokens',
        'bookings', 'booking_offers', 'booking_assignments', 'notifications_log'
    ]
    LOOP
        EXECUTE FORMAT(
            'DROP TRIGGER IF EXISTS trg_%I_updated_at ON public.%I;
             CREATE TRIGGER trg_%I_updated_at
             BEFORE UPDATE ON public.%I
             FOR EACH ROW EXECUTE FUNCTION set_updated_at()',
            t, t, t, t
        );
    END LOOP;
END;
$$;


-- ── on_auth_user_created ─────────────────────────────────────
-- When Supabase Auth creates a user via phone OTP,
-- this trigger fires. The Edge Function for role routing
-- will then populate the role in public.users.
CREATE OR REPLACE FUNCTION handle_new_auth_user()
RETURNS TRIGGER LANGUAGE plpgsql SECURITY DEFINER AS $$
BEGIN
    INSERT INTO public.users (id, phone, role)
    VALUES (
        NEW.id,
        NEW.phone,
        'client'
    )
    ON CONFLICT (id) DO NOTHING;
    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS trg_on_auth_user_created ON auth.users;
CREATE TRIGGER trg_on_auth_user_created
    AFTER INSERT ON auth.users
    FOR EACH ROW EXECUTE FUNCTION handle_new_auth_user();


-- ── booking_status auto-transition ───────────────────────────
CREATE OR REPLACE FUNCTION sync_booking_status()
RETURNS TRIGGER LANGUAGE plpgsql SECURITY DEFINER AS $$
DECLARE
    v_booking          public.bookings%ROWTYPE;
    v_accepted_count   INTEGER;
BEGIN
    -- Lock the booking row for this transaction
    SELECT * INTO v_booking
    FROM public.bookings
    WHERE id = NEW.booking_id
    FOR UPDATE;

    -- Count confirmed assignments (not removed)
    SELECT COUNT(*) INTO v_accepted_count
    FROM public.booking_assignments
    WHERE booking_id = NEW.booking_id
      AND removed_at IS NULL;

    -- Update accepted count
    UPDATE public.bookings
    SET workers_accepted = v_accepted_count
    WHERE id = NEW.booking_id;

    -- Promote to confirmed if threshold met
    IF v_accepted_count >= v_booking.workers_needed
       AND v_booking.status IN ('searching', 'partially_filled') THEN

        UPDATE public.bookings
        SET status       = 'confirmed',
            confirmed_at = NOW()
        WHERE id = NEW.booking_id;

        -- Cancel all remaining pending offers
        UPDATE public.booking_offers
        SET status = 'cancelled',
            updated_at = NOW()
        WHERE booking_id = NEW.booking_id
          AND status = 'pending';

    -- Partial fill
    ELSIF v_accepted_count > 0
       AND v_accepted_count < v_booking.workers_needed
       AND v_booking.status = 'searching' THEN

        UPDATE public.bookings
        SET status = 'partially_filled'
        WHERE id = NEW.booking_id;
    END IF;

    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS trg_sync_booking_status ON public.booking_assignments;
CREATE TRIGGER trg_sync_booking_status
    AFTER INSERT OR UPDATE ON public.booking_assignments
    FOR EACH ROW EXECUTE FUNCTION sync_booking_status();


-- ── worker stats update ──────────────────────────────────────
CREATE OR REPLACE FUNCTION update_worker_stats_on_completion()
RETURNS TRIGGER LANGUAGE plpgsql SECURITY DEFINER AS $$
BEGIN
    IF NEW.status = 'completed' AND OLD.status != 'completed' THEN
        UPDATE public.workers w
        SET
            total_jobs_completed = total_jobs_completed + 1,
            total_earnings = total_earnings + b.pay_per_worker,
            updated_at = NOW()
        FROM public.booking_assignments ba
        JOIN public.bookings b ON b.id = ba.booking_id
        WHERE ba.booking_id = NEW.id
          AND ba.worker_id = w.id
          AND ba.removed_at IS NULL
          AND b.id = NEW.id;
    END IF;
    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS trg_update_worker_stats ON public.bookings;
CREATE TRIGGER trg_update_worker_stats
    AFTER UPDATE ON public.bookings
    FOR EACH ROW EXECUTE FUNCTION update_worker_stats_on_completion();


-- ── Audit log trigger ────────────────────────────────────────
CREATE OR REPLACE FUNCTION write_audit_log()
RETURNS TRIGGER LANGUAGE plpgsql SECURITY DEFINER AS $$
BEGIN
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
        CASE WHEN TG_OP IN ('UPDATE', 'DELETE') THEN to_jsonb(OLD) END,
        CASE WHEN TG_OP IN ('INSERT', 'UPDATE') THEN to_jsonb(NEW) END
    );
    RETURN COALESCE(NEW, OLD);
END;
$$;

-- Apply audit to sensitive tables only
DO $$
DECLARE
    t TEXT;
BEGIN
    FOREACH t IN ARRAY ARRAY[
        'bookings', 'booking_offers', 'booking_assignments',
        'workers', 'clients'
    ]
    LOOP
        EXECUTE FORMAT(
            'DROP TRIGGER IF EXISTS trg_%I_audit ON public.%I;
             CREATE TRIGGER trg_%I_audit
             AFTER INSERT OR UPDATE OR DELETE ON public.%I
             FOR EACH ROW EXECUTE FUNCTION write_audit_log()',
            t, t, t, t
        );
    END LOOP;
END;
$$;


-- ── Helper: get current user role ────────────────────────────
CREATE OR REPLACE FUNCTION current_user_role()
RETURNS user_role LANGUAGE sql STABLE SECURITY DEFINER AS $$
    SELECT role FROM public.users WHERE id = auth.uid();
$$;

-- ── Helper: get current worker id ────────────────────────────
CREATE OR REPLACE FUNCTION current_worker_id()
RETURNS UUID LANGUAGE sql STABLE SECURITY DEFINER AS $$
    SELECT id FROM public.workers WHERE user_id = auth.uid();
$$;

-- ── Helper: get current client id ────────────────────────────
CREATE OR REPLACE FUNCTION current_client_id()
RETURNS UUID LANGUAGE sql STABLE SECURITY DEFINER AS $$
    SELECT id FROM public.clients WHERE user_id = auth.uid();
$$;
