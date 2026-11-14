# Krewzo — Agent Context

## Project Overview

**Krewzo** is an on-demand gig-labour marketplace for Pune, India. It connects **businesses** (clients) that need short-notice blue-collar workers (loaders, cleaners, security guards, event staff, etc.) with a pool of verified **workers** who can accept or decline job offers in real-time.

The system operates across three user roles:

| Role | Description |
|------|-------------|
| **Admin** | Internal operator — creates/manages clients & workers, monitors bookings |
| **Client** | Business owner — creates bookings, tracks crew fill progress live |
| **Worker** | Gig laborer — receives push/SMS job offers, accepts or declines |

---

## Repository Structure

```
Krewzo/
├── app/                          # Android application (Kotlin + Jetpack Compose)
│   ├── build.gradle.kts
│   └── src/main/java/com/binaryxbeast/krewzo/
│       ├── MainActivity.kt
│       ├── navigation/           # NavGraph + Screen sealed class
│       ├── core/
│       │   ├── common/           # Result.kt (sealed Result<T, E> wrapper)
│       │   ├── model/            # Domain models (Booking, Worker, Client, Offer, User)
│       │   ├── network/          # Supabase client + DTOs
│       │   ├── database/         # Room DAOs & entities (local cache)
│       │   ├── di/               # Dependency injection
│       │   └── util/             # GeocoderHelper, ImageCompressor
│       ├── data/repository/      # AuthRepository, BookingRepository, WorkerRepository, AdminRepository
│       ├── feature/
│       │   ├── auth/             # PhoneInputScreen, OtpVerificationScreen, ClientOnboardingScreen
│       │   ├── admin/            # AdminDashboard, Bookings, Workers, Clients, AddEdit screens
│       │   ├── client/           # ClientHome, CreateBooking, BookingDetailLive
│       │   └── worker/           # WorkerHome
│       └── ui/
│           ├── components/       # Shared Compose components (KrewzoButton, KrewzoCard, etc.)
│           └── theme/            # Material3 theme, colors, typography, dimensions
├── backend/
│   └── supabase/
│       ├── functions/            # Deno/TypeScript Edge Functions
│       │   ├── create-booking/   # Creates booking + dispatches Wave 1 offers
│       │   ├── wave-dispatch/    # Triggered for subsequent dispatch waves
│       │   ├── respond-offer/    # Worker accept/decline handler
│       │   ├── auth-role-routing/# Post-auth role detection & redirect
│       │   └── _shared/          # fcm.ts, msg91.ts, geocoding.ts, supabaseClient.ts
│       └── migrations/           # Ordered SQL migrations (001–008)
├── gradle/libs.versions.toml     # Version catalog
└── local.properties              # API keys (not committed)
```

---

## Tech Stack

### Android App

| Layer | Technology |
|-------|-----------|
| Language | Kotlin 2.2.10 |
| UI | Jetpack Compose (BOM 2026.02.01) + Material3 |
| Navigation | Navigation Compose 2.8.5 |
| Backend Client | Supabase Kotlin SDK 3.1.2 (PostgREST, Auth, Realtime, Storage) |
| Networking | Ktor Client Android 3.1.0 |
| Maps | Google Maps Compose 6.4.4 + Play Services Maps 19.1.0 |
| Location | Play Services Location 21.3.0 |
| Image Loading | Coil 2.7.0 |
| Serialization | kotlinx.serialization 1.8.0 |
| Min SDK | 24 (Android 7.0) |
| Target SDK | 37 |

### Backend (Supabase)

| Component | Technology |
|-----------|-----------|
| Database | PostgreSQL with PostGIS extension |
| Auth | Supabase Auth (phone OTP via MSG91) |
| Edge Functions | Deno / TypeScript |
| Push Notifications | Firebase Cloud Messaging (FCM) |
| SMS | MSG91 |
| Geocoding | Google Geocoding API |
| Storage | Supabase Storage (worker photos) |

---

## Domain Model

### Core Entities

```
User (auth.users)
 ├── role: admin | client | worker
 ├── Client (1:1) — business entity
 └── Worker (1:1) — gig laborer profile

Booking
 ├── created by Client
 ├── status: searching → partially_filled → confirmed → in_progress → completed | cancelled | expired
 ├── workers_needed / workers_accepted
 ├── work_coordinates: PostGIS POINT (for spatial queries)
 └── current_wave: wave number for dispatch iterations

BookingOffer  (booking ↔ worker, UNIQUE)
 └── status: pending → accepted | declined | expired | cancelled

BookingAssignment  (confirmed worker-to-booking link)
 └── payment_status: pending | paid
```

### Skill Categories (Worker)

`LOADING_UNLOADING`, `CLEANING`, `SECURITY`, `HOSPITALITY`, `CONSTRUCTION`, `DELIVERY`, `PACKAGING`, `EVENT_STAFF`, `OTHER`

---

## Wave Dispatch System

The core business logic. When a client creates a booking:

1. **Wave 1** (`create-booking` edge function):
   - Runs `find_eligible_workers` PostGIS RPC — finds nearest available, skill-matched workers within **20 km**, up to `max(4, workers_needed × 2)` workers.
   - Inserts `booking_offers` with 10-minute expiry via `create_booking_wave_offers` RPC.
   - Dispatches notifications in parallel:
     - **FCM push** (preferred) — if worker has a registered token.
     - **SMS fallback** via MSG91 — only if push fails or no token.

2. **Subsequent Waves** (`wave-dispatch` edge function):
   - Triggered when offers expire and the booking is still not filled.
   - Expands radius or recruits from a new pool.

3. **Worker Response** (`respond-offer` edge function):
   - Accept: creates `booking_assignments` row, increments `workers_accepted`, updates booking status.
   - Decline/expire: marks offer accordingly; booking remains open for next wave.

---

## Authentication Flow

1. User enters phone number → `PhoneInputScreen`
2. OTP sent via MSG91 → verified by Supabase Auth → `OtpVerificationScreen`
3. `auth-role-routing` edge function reads `users.role` → returns role in JWT claims
4. App navigates to role-appropriate home screen:
   - `admin` → `AdminDashboardScreen`
   - `client` → `ClientHomeScreen`
   - `worker` → `WorkerHomeScreen`
5. New client users complete onboarding → `ClientOnboardingScreen`

---

## Key Configuration

### `local.properties` (required, not committed)

```properties
supabase.url=https://<project-ref>.supabase.co
supabase.anon.key=<anon-key>
google.maps.api.key=<maps-key>
```

### BuildConfig Fields

| Field | Source |
|-------|--------|
| `SUPABASE_URL` | `local.properties` → `supabase.url` |
| `SUPABASE_ANON_KEY` | `local.properties` → `supabase.anon.key` |
| `MAPS_API_KEY` | `local.properties` → `google.maps.api.key` |

---

## Database Migrations (ordered)

| File | Purpose |
|------|---------|
| `001_extensions_enums.sql` | PostGIS, pgcrypto; enums: `user_role`, `booking_status`, `offer_status`, `notification_channel` |
| `002_tables.sql` | All core tables: `users`, `clients`, `workers`, `client_locations`, `bookings`, `booking_offers`, `booking_assignments`, `fcm_tokens`, `notifications_log`, `audit_log`, `worker_availability` |
| `003_functions_triggers.sql` | RPC functions (`find_eligible_workers`, `create_booking_wave_offers`) + triggers |
| `004_rls_policies.sql` | Row-Level Security policies per role |
| `005_fix_rls_and_admin_direct.sql` | RLS patches |
| `005_seed_views.sql` | Convenience views for admin dashboard |
| `006_booking_logic_rpc.sql` | Additional booking RPCs |
| `007_storage_buckets.sql` | Supabase Storage bucket config |
| `008_client_map_coordinates.sql` | Adds lat/lng columns to `client_locations` |
| `009_worker_date_of_birth.sql` | Adds date_of_birth column, index, and age sync trigger on workers |

---

## Shared UI Components

| Component | File |
|-----------|------|
| `KrewzoButton` | Primary/Secondary CTA button |
| `KrewzoCard` | Elevated surface card |
| `KrewzoTextField` | Styled input field |
| `KrewzoTopBar` | App bar with back navigation |
| `KrewzoBottomNav` | Role-aware bottom navigation |
| `KrewzoStatusBadge` | Coloured booking/offer status chip |
| `KrewzoMapWidget` | Embedded Google Map with marker |

---

## Coding Conventions

- **Architecture**: MVVM with Repository pattern. ViewModels use `StateFlow` for UI state; repositories return `Result<T>`.
- **Package structure**: Feature-first under `feature/`, shared code under `core/`.
- **Compose**: Stateless composables receive state + lambdas; `ViewModel` holds all logic.
- **Supabase**: All DB access goes through `KrewzoSupabaseClient` singleton; heavy mutations go via Edge Functions.
- **Error handling**: Repositories wrap responses in `core.common.Result` (Success / Error).
- **Realtime**: `BookingDetailLiveScreen` subscribes to Supabase Realtime channels for live status updates.
- **Image compression**: Worker photos are compressed via `ImageCompressor` before upload to Supabase Storage.
- **Geography**: Work locations use PostGIS `GEOGRAPHY(POINT, 4326)`; spatial queries use `ST_DWithin` for radius matching.

---

## Development Notes

- The app currently targets **Pune** as its sole city of operation. City-specific defaults (`city = "Pune"`, `pincode = "411001"`, seeded lat/lng) are hardcoded in several places.
- Worker availability is tracked both by an `is_available` flag on the `workers` table and by time-ranged blocks in `worker_availability`.
- Offer expiry is enforced at the DB level via `expires_at` timestamps; the `wave-dispatch` function handles escalation.
- SMS is treated as a cost-optimized fallback — it is only sent when FCM push is unavailable or fails.
- `audit_log` captures actor, role, action, entity, and before/after JSONB snapshots for all sensitive mutations.
