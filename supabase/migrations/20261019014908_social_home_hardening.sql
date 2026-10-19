-- Harden social publishing so community posts can only be created by members.
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
    IF p_community_id IS NOT NULL AND NOT EXISTS (
        SELECT 1 FROM public.community_members
        WHERE community_id = p_community_id AND user_id = v_user
    ) THEN RAISE EXCEPTION 'Join the community before posting'; END IF;
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

CREATE OR REPLACE FUNCTION public.equip_avatar_frame(p_frame_id uuid)
RETURNS void
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path TO 'public'
AS $$
DECLARE
    me uuid := auth.uid();
    current_level integer;
    target public.avatar_frames%rowtype;
BEGIN
    IF me IS NULL THEN RAISE EXCEPTION 'Authentication required'; END IF;
    SELECT level INTO current_level FROM public.user_gamification WHERE user_id = me;
    SELECT * INTO target FROM public.avatar_frames WHERE id = p_frame_id;
    IF target.id IS NULL THEN RAISE EXCEPTION 'Frame not found'; END IF;
    IF NOT (
        target.unlock_type = 'default'
        OR EXISTS (SELECT 1 FROM public.user_frames uf WHERE uf.user_id = me AND uf.frame_id = p_frame_id)
        OR (target.unlock_type = 'level' AND current_level >= coalesce(target.unlock_value, 999999))
    ) THEN RAISE EXCEPTION 'Frame is locked'; END IF;
    UPDATE public.user_gamification SET active_frame_id = p_frame_id WHERE user_id = me;
END;
$$;
