-- ============================================================
-- Krewzo — Migration 008: Client Map Coordinates & Spatial Support
-- ============================================================
-- Enables full PostGIS spatial support for client addresses,
-- including direct latitude/longitude columns, spatial GiST indexing,
-- automated coordinate synchronization triggers, updated onboarding RPC,
-- and proximity matching functions for workers & clients.

-- ── 1. Add Coordinate Columns to public.clients ─────────────
ALTER TABLE public.clients 
    ADD COLUMN IF NOT EXISTS latitude DOUBLE PRECISION,
    ADD COLUMN IF NOT EXISTS longitude DOUBLE PRECISION,
    ADD COLUMN IF NOT EXISTS coordinates GEOGRAPHY(POINT, 4326);

-- Also add explicit lat/lng to client_locations for quick scalar querying
ALTER TABLE public.client_locations
    ADD COLUMN IF NOT EXISTS latitude DOUBLE PRECISION,
    ADD COLUMN IF NOT EXISTS longitude DOUBLE PRECISION;

-- ── 2. Spatial Indexes (GiST) & Coordinate B-Tree Indexes ───
CREATE INDEX IF NOT EXISTS idx_clients_coords 
    ON public.clients USING GIST(coordinates);

CREATE INDEX IF NOT EXISTS idx_clients_lat_lng 
    ON public.clients(latitude, longitude);

CREATE INDEX IF NOT EXISTS idx_client_locations_coords 
    ON public.client_locations USING GIST(coordinates);

-- ── 3. Backfill Existing Client Coordinates from client_locations ─
UPDATE public.clients c
SET 
    coordinates = cl.coordinates,
    latitude = ST_Y(cl.coordinates::GEOMETRY),
    longitude = ST_X(cl.coordinates::GEOMETRY),
    address = COALESCE(c.address, cl.address_line),
    city = COALESCE(c.city, cl.city),
    pincode = COALESCE(c.pincode, cl.pincode)
FROM public.client_locations cl
WHERE cl.client_id = c.id 
  AND cl.is_default = TRUE 
  AND cl.coordinates IS NOT NULL
  AND c.coordinates IS NULL;

-- If neither is present, backfill default Central Pune coordinates
UPDATE public.clients
SET 
    latitude = 18.520430,
    longitude = 73.856744,
    coordinates = ST_SetSRID(ST_MakePoint(73.856744, 18.520430), 4326)::GEOGRAPHY
WHERE coordinates IS NULL AND latitude IS NULL;


-- ── 4. Trigger: Bi-directional Coordinate Auto-Sync on clients ─
-- Ensures that if latitude/longitude are set, coordinates is populated;
-- and if coordinates is set, latitude/longitude are automatically populated.
CREATE OR REPLACE FUNCTION public.sync_client_coordinates()
RETURNS TRIGGER 
LANGUAGE plpgsql
SECURITY DEFINER
AS $$
BEGIN
    -- If lat & lng provided, generate PostGIS GEOGRAPHY point
    IF NEW.latitude IS NOT NULL AND NEW.longitude IS NOT NULL THEN
        NEW.coordinates := ST_SetSRID(ST_MakePoint(NEW.longitude, NEW.latitude), 4326)::GEOGRAPHY;
    -- If coordinates provided without lat/lng, extract scalar coordinates
    ELSIF NEW.coordinates IS NOT NULL AND (NEW.latitude IS NULL OR NEW.longitude IS NULL) THEN
        NEW.latitude  := ST_Y(NEW.coordinates::GEOMETRY);
        NEW.longitude := ST_X(NEW.coordinates::GEOMETRY);
    END IF;

    NEW.updated_at := NOW();
    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS trg_sync_client_coordinates ON public.clients;
CREATE TRIGGER trg_sync_client_coordinates
    BEFORE INSERT OR UPDATE ON public.clients
    FOR EACH ROW EXECUTE FUNCTION public.sync_client_coordinates();


-- ── 5. Trigger: Auto-sync Primary client_locations Row ───────
-- Keeps the client_locations table synchronized whenever a client's
-- address or map coordinates are created or modified.
CREATE OR REPLACE FUNCTION public.sync_client_primary_location()
RETURNS TRIGGER
LANGUAGE plpgsql
SECURITY DEFINER
AS $$
BEGIN
    -- Only sync if address or coordinates exist
    IF NEW.address IS NOT NULL OR NEW.coordinates IS NOT NULL THEN
        INSERT INTO public.client_locations (
            client_id,
            label,
            address_line,
            city,
            state,
            pincode,
            coordinates,
            latitude,
            longitude,
            is_default,
            updated_at
        )
        VALUES (
            NEW.id,
            'Primary HQ',
            COALESCE(NEW.address, 'Primary Location'),
            COALESCE(NEW.city, 'Pune'),
            'Maharashtra',
            COALESCE(NEW.pincode, '411001'),
            NEW.coordinates,
            NEW.latitude,
            NEW.longitude,
            TRUE,
            NOW()
        )
        ON CONFLICT (id) DO NOTHING;

        -- Update existing default location if present
        UPDATE public.client_locations
        SET 
            address_line = COALESCE(NEW.address, address_line),
            city         = COALESCE(NEW.city, city),
            pincode      = COALESCE(NEW.pincode, pincode),
            coordinates  = NEW.coordinates,
            latitude     = NEW.latitude,
            longitude    = NEW.longitude,
            updated_at   = NOW()
        WHERE client_id = NEW.id AND is_default = TRUE;
    END IF;

    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS trg_sync_client_primary_location ON public.clients;
CREATE TRIGGER trg_sync_client_primary_location
    AFTER INSERT OR UPDATE OF address, city, pincode, latitude, longitude, coordinates ON public.clients
    FOR EACH ROW EXECUTE FUNCTION public.sync_client_primary_location();


-- ── 6. Updated Complete Client Onboarding RPC ────────────────
CREATE OR REPLACE FUNCTION public.complete_client_onboarding(
    p_user_id        UUID,
    p_business_name  TEXT,
    p_employee_count INTEGER,
    p_address_line   TEXT,
    p_city           TEXT DEFAULT 'Pune',
    p_state          TEXT DEFAULT 'Maharashtra',
    p_pincode        TEXT DEFAULT '411001',
    p_latitude       DOUBLE PRECISION DEFAULT 18.520430,
    p_longitude      DOUBLE PRECISION DEFAULT 73.856744,
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

    -- Insert or update client profile with coordinates
    INSERT INTO public.clients (
        user_id,
        business_name,
        employee_count,
        logo_url,
        address,
        city,
        pincode,
        latitude,
        longitude,
        coordinates,
        is_active,
        is_verified
    )
    VALUES (
        p_user_id,
        p_business_name,
        p_employee_count,
        p_logo_url,
        p_address_line,
        p_city,
        p_pincode,
        p_latitude,
        p_longitude,
        v_coords,
        TRUE,
        TRUE
    )
    ON CONFLICT (user_id) DO UPDATE
    SET business_name  = EXCLUDED.business_name,
        employee_count = EXCLUDED.employee_count,
        logo_url       = COALESCE(EXCLUDED.logo_url, clients.logo_url),
        address        = EXCLUDED.address,
        city           = EXCLUDED.city,
        pincode        = EXCLUDED.pincode,
        latitude       = EXCLUDED.latitude,
        longitude      = EXCLUDED.longitude,
        coordinates    = EXCLUDED.coordinates,
        deleted_at     = NULL,
        updated_at     = NOW()
    RETURNING id INTO v_client_id;

    -- Upsert primary location in client_locations
    INSERT INTO public.client_locations (
        client_id, label, address_line, city, state, pincode, coordinates, latitude, longitude, is_default
    )
    VALUES (
        v_client_id, 'Primary HQ', p_address_line, p_city, p_state, p_pincode, v_coords, p_latitude, p_longitude, TRUE
    )
    ON CONFLICT DO NOTHING;

    RETURN jsonb_build_object(
        'success', TRUE,
        'client_id', v_client_id,
        'role', 'client',
        'is_onboarded', TRUE,
        'latitude', p_latitude,
        'longitude', p_longitude
    );
END;
$$;


-- ── 7. Spatial Query RPC: Find Clients Near Given Coordinates ─
CREATE OR REPLACE FUNCTION public.find_clients_within_radius(
    p_latitude      DOUBLE PRECISION,
    p_longitude     DOUBLE PRECISION,
    p_radius_meters DOUBLE PRECISION DEFAULT 15000 -- 15 km default
)
RETURNS TABLE (
    client_id       UUID,
    business_name   TEXT,
    contact_person  TEXT,
    phone           TEXT,
    address         TEXT,
    city            TEXT,
    pincode         TEXT,
    latitude        DOUBLE PRECISION,
    longitude       DOUBLE PRECISION,
    distance_meters NUMERIC(10, 2)
)
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public, auth, pg_temp
AS $$
DECLARE
    v_center GEOGRAPHY(POINT, 4326);
BEGIN
    v_center := ST_SetSRID(ST_MakePoint(p_longitude, p_latitude), 4326)::GEOGRAPHY;

    RETURN QUERY
    SELECT 
        c.id AS client_id,
        c.business_name,
        c.contact_person,
        c.phone,
        c.address,
        c.city,
        c.pincode,
        c.latitude,
        c.longitude,
        ROUND(ST_Distance(c.coordinates, v_center)::NUMERIC, 2) AS distance_meters
    FROM public.clients c
    WHERE c.deleted_at IS NULL
      AND c.coordinates IS NOT NULL
      AND ST_DWithin(c.coordinates, v_center, p_radius_meters)
    ORDER BY distance_meters ASC;
END;
$$;


-- ── 8. Spatial Matching RPC: Find Nearest Workers For Client ──
CREATE OR REPLACE FUNCTION public.find_nearest_workers_for_client(
    p_client_id         UUID,
    p_limit             INTEGER DEFAULT 10,
    p_max_radius_meters DOUBLE PRECISION DEFAULT 25000 -- 25 km
)
RETURNS TABLE (
    worker_id       UUID,
    full_name       TEXT,
    phone           TEXT,
    distance_meters NUMERIC(10, 2),
    distance_km     NUMERIC(10, 2),
    rating          NUMERIC(3, 2),
    is_available    BOOLEAN
)
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public, auth, pg_temp
AS $$
DECLARE
    v_client_coords GEOGRAPHY(POINT, 4326);
BEGIN
    SELECT coordinates INTO v_client_coords
    FROM public.clients
    WHERE id = p_client_id;

    IF v_client_coords IS NULL THEN
        RAISE EXCEPTION 'Client % does not have configured map coordinates', p_client_id;
    END IF;

    RETURN QUERY
    SELECT 
        w.id AS worker_id,
        w.full_name,
        w.phone,
        ROUND(ST_Distance(w.home_coordinates, v_client_coords)::NUMERIC, 2) AS distance_meters,
        ROUND((ST_Distance(w.home_coordinates, v_client_coords) / 1000.0)::NUMERIC, 2) AS distance_km,
        w.rating,
        w.is_available
    FROM public.workers w
    WHERE w.is_active = TRUE
      AND w.deleted_at IS NULL
      AND w.home_coordinates IS NOT NULL
      AND ST_DWithin(w.home_coordinates, v_client_coords, p_max_radius_meters)
    ORDER BY distance_meters ASC
    LIMIT p_limit;
END;
$$;


-- ── 9. View: Detailed Client Locations with GeoJSON & Distance ─
DROP VIEW IF EXISTS public.v_clients_with_locations;
CREATE OR REPLACE VIEW public.v_clients_with_locations AS
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
    ST_AsGeoJSON(c.coordinates)::JSONB AS location_geojson,
    c.is_verified,
    c.is_active,
    c.created_at,
    c.updated_at
FROM public.clients c
WHERE c.deleted_at IS NULL;
