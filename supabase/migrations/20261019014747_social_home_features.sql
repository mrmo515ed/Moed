-- Keep the local migration history aligned with the changes applied to anime-black-fresh.
ALTER TABLE public.communities
    ADD COLUMN IF NOT EXISTS category text NOT NULL DEFAULT 'general';

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conrelid = 'public.communities'::regclass
          AND conname = 'communities_category_check'
    ) THEN
        ALTER TABLE public.communities
            ADD CONSTRAINT communities_category_check
            CHECK (category IN ('general', 'anime', 'studio', 'manga', 'shonen', 'other'));
    END IF;
END $$;

ALTER TABLE public.posts
    ADD COLUMN IF NOT EXISTS topic text NOT NULL DEFAULT 'general',
    ADD COLUMN IF NOT EXISTS video_url text;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conrelid = 'public.posts'::regclass
          AND conname = 'posts_topic_check'
    ) THEN
        ALTER TABLE public.posts
            ADD CONSTRAINT posts_topic_check
            CHECK (topic IN ('general', 'theory', 'discussion', 'question', 'announcement'));
    END IF;
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conrelid = 'public.posts'::regclass
          AND conname = 'posts_video_url_http'
    ) THEN
        ALTER TABLE public.posts
            ADD CONSTRAINT posts_video_url_http
            CHECK (video_url IS NULL OR video_url ~* '^https?://');
    END IF;
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conrelid = 'public.posts'::regclass
          AND conname = 'posts_content_or_media'
    ) THEN
        ALTER TABLE public.posts
            ADD CONSTRAINT posts_content_or_media
            CHECK (length(trim(content)) > 0 OR image_url IS NOT NULL OR video_url IS NOT NULL);
    END IF;
END $$;

INSERT INTO storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
VALUES ('reel-videos', 'reel-videos', true, 52428800, ARRAY['video/mp4', 'video/webm', 'video/quicktime'])
ON CONFLICT (id) DO UPDATE SET
    public = EXCLUDED.public,
    file_size_limit = EXCLUDED.file_size_limit,
    allowed_mime_types = EXCLUDED.allowed_mime_types;

DROP POLICY IF EXISTS reel_videos_public_read ON storage.objects;
CREATE POLICY reel_videos_public_read ON storage.objects
    FOR SELECT USING (bucket_id = 'reel-videos');

DROP POLICY IF EXISTS reel_videos_owner_insert ON storage.objects;
CREATE POLICY reel_videos_owner_insert ON storage.objects
    FOR INSERT WITH CHECK (bucket_id = 'reel-videos' AND (storage.foldername(name))[1] = auth.uid()::text);

DROP POLICY IF EXISTS reel_videos_owner_update ON storage.objects;
CREATE POLICY reel_videos_owner_update ON storage.objects
    FOR UPDATE USING (bucket_id = 'reel-videos' AND (storage.foldername(name))[1] = auth.uid()::text)
    WITH CHECK (bucket_id = 'reel-videos' AND (storage.foldername(name))[1] = auth.uid()::text);

DROP POLICY IF EXISTS reel_videos_owner_delete ON storage.objects;
CREATE POLICY reel_videos_owner_delete ON storage.objects
    FOR DELETE USING (bucket_id = 'reel-videos' AND (storage.foldername(name))[1] = auth.uid()::text);

CREATE OR REPLACE FUNCTION public.publish_social_post(
    p_content text,
    p_kind text DEFAULT 'text',
    p_topic text DEFAULT 'general',
    p_image_url text DEFAULT NULL,
    p_video_url text DEFAULT NULL,
    p_contains_spoiler boolean DEFAULT false,
    p_spoiler_label text DEFAULT NULL,
    p_related_anime_id uuid DEFAULT NULL,
    p_rating integer DEFAULT NULL,
    p_poll_options text[] DEFAULT NULL,
    p_community_id uuid DEFAULT NULL
)
RETURNS uuid
LANGUAGE plpgsql
SET search_path TO 'public'
AS $$
DECLARE
    v_user uuid := auth.uid();
    v_post uuid;
    v_poll uuid;
BEGIN
    IF v_user IS NULL THEN RAISE EXCEPTION 'Authentication required'; END IF;
    IF p_kind NOT IN ('text', 'image', 'poll', 'review') THEN RAISE EXCEPTION 'Unsupported post kind'; END IF;
    IF p_topic NOT IN ('general', 'theory', 'discussion', 'question', 'announcement') THEN RAISE EXCEPTION 'Unsupported post topic'; END IF;
    IF length(trim(coalesce(p_content, ''))) = 0 AND p_image_url IS NULL AND p_video_url IS NULL THEN RAISE EXCEPTION 'Post content is required'; END IF;
    IF length(coalesce(p_content, '')) > 2000 THEN RAISE EXCEPTION 'Post is too long'; END IF;
    IF p_kind = 'review' AND (p_related_anime_id IS NULL OR p_rating IS NULL OR p_rating < 1 OR p_rating > 10) THEN RAISE EXCEPTION 'A review needs an anime and rating from one to ten'; END IF;
    IF p_kind = 'poll' AND (coalesce(array_length(p_poll_options, 1), 0) < 2 OR array_length(p_poll_options, 1) > 6 OR (SELECT count(DISTINCT trim(x)) FROM unnest(p_poll_options) x WHERE length(trim(x)) > 0) < 2) THEN RAISE EXCEPTION 'A poll needs two to six unique options'; END IF;

    INSERT INTO public.posts(author_id, community_id, content, image_url, video_url, kind, topic, contains_spoiler, spoiler_label, related_anime_id)
    VALUES (v_user, p_community_id, trim(coalesce(p_content, '')), p_image_url, p_video_url, p_kind, p_topic, coalesce(p_contains_spoiler, false), nullif(trim(coalesce(p_spoiler_label, '')), ''), p_related_anime_id)
    RETURNING id INTO v_post;

    IF p_kind = 'review' THEN
        INSERT INTO public.reviews(user_id, anime_id, rating, title, body, contains_spoiler)
        VALUES (v_user, p_related_anime_id, p_rating, NULL, trim(p_content), coalesce(p_contains_spoiler, false));
    ELSIF p_kind = 'poll' THEN
        INSERT INTO public.polls(post_id, ends_at) VALUES (v_post, now() + interval '7 days') RETURNING id INTO v_poll;
        INSERT INTO public.poll_options(poll_id, text, position)
        SELECT v_poll, trim(option_text), ordinality::integer
        FROM unnest(p_poll_options) WITH ORDINALITY AS options(option_text, ordinality)
        WHERE length(trim(option_text)) > 0;
    END IF;
    RETURN v_post;
END;
$$;
