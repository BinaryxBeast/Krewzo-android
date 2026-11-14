-- ============================================================
-- Krewzo — Migration 001: Extensions & Enums
-- ============================================================

-- ── Extensions ───────────────────────────────────────────────
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";      -- gen_random_uuid()
CREATE EXTENSION IF NOT EXISTS "postgis";        -- Spatial queries
CREATE EXTENSION IF NOT EXISTS "pgcrypto";       -- encrypt() for sensitive data


-- ── Enums ────────────────────────────────────────────────────

-- User role
CREATE TYPE user_role AS ENUM (
    'client',
    'worker',
    'admin'
);

-- Booking lifecycle
CREATE TYPE booking_status AS ENUM (
    'searching',       -- Posted, waiting for acceptances
    'partially_filled',-- Some workers accepted, need more
    'confirmed',       -- Required workers accepted
    'in_progress',     -- Work started (date/time reached)
    'completed',       -- Work finished, marked by admin/client
    'cancelled',       -- Cancelled by client or admin
    'expired'          -- Timed out before enough workers accepted
);

-- Offer sent to individual worker
CREATE TYPE offer_status AS ENUM (
    'pending',         -- Sent, awaiting response
    'accepted',        -- Worker accepted
    'declined',        -- Worker declined
    'expired',         -- No response within timeout
    'cancelled'        -- Booking cancelled before response
);

-- SMS/Push notification type
CREATE TYPE notification_channel AS ENUM (
    'push',
    'sms'
);

-- Notification delivery status
CREATE TYPE notification_status AS ENUM (
    'queued',
    'sent',
    'delivered',
    'failed'
);

-- Document type for worker ID verification
CREATE TYPE document_type AS ENUM (
    'aadhaar',
    'pan',
    'passport',
    'driving_license',
    'voter_id',
    'other'
);

-- Worker skill categories
CREATE TYPE skill_category AS ENUM (
    'loading_unloading',
    'cleaning',
    'security',
    'hospitality',
    'construction',
    'delivery',
    'packaging',
    'event_staff',
    'other'
);
