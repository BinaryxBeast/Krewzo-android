-- ============================================================
-- Krewzo — Migration 007: Storage Buckets & Policies
-- ============================================================

-- 1. Create the 'avatars' storage bucket (Public)
INSERT INTO storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
VALUES (
    'avatars',
    'avatars',
    TRUE,
    5242880, -- 5 MB limit
    ARRAY['image/jpeg', 'image/png', 'image/webp']
)
ON CONFLICT (id) DO UPDATE SET
    public = TRUE,
    file_size_limit = 5242880,
    allowed_mime_types = ARRAY['image/jpeg', 'image/png', 'image/webp'];

-- 2. Allow anyone to view avatar images (Public Read)
DROP POLICY IF EXISTS "avatars_public_select" ON storage.objects;
CREATE POLICY "avatars_public_select" ON storage.objects
    FOR SELECT
    USING (bucket_id = 'avatars');

-- 3. Allow uploads into the 'avatars' bucket
DROP POLICY IF EXISTS "avatars_allow_insert" ON storage.objects;
CREATE POLICY "avatars_allow_insert" ON storage.objects
    FOR INSERT
    WITH CHECK (bucket_id = 'avatars');

-- 4. Allow updates / overwrites in the 'avatars' bucket
DROP POLICY IF EXISTS "avatars_allow_update" ON storage.objects;
CREATE POLICY "avatars_allow_update" ON storage.objects
    FOR UPDATE
    USING (bucket_id = 'avatars');

-- 5. Allow deletes in the 'avatars' bucket
DROP POLICY IF EXISTS "avatars_allow_delete" ON storage.objects;
CREATE POLICY "avatars_allow_delete" ON storage.objects
    FOR DELETE
    USING (bucket_id = 'avatars');
