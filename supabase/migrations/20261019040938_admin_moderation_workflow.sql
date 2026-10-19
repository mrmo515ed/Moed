ALTER TABLE public.post_reports
    ADD COLUMN status text NOT NULL DEFAULT 'pending',
    ADD COLUMN reviewed_by uuid REFERENCES public.profiles(id) ON DELETE SET NULL,
    ADD COLUMN reviewed_at timestamptz,
    ADD COLUMN moderator_note text;

ALTER TABLE public.post_reports
    ADD CONSTRAINT post_reports_status_check CHECK (status IN ('pending', 'resolved', 'dismissed'));

CREATE INDEX post_reports_status_created_idx ON public.post_reports(status, created_at DESC);

CREATE TABLE public.admin_audit_log (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    actor_id uuid NOT NULL REFERENCES public.profiles(id) ON DELETE RESTRICT,
    action text NOT NULL,
    target_type text NOT NULL,
    target_id uuid,
    details jsonb NOT NULL DEFAULT '{}'::jsonb,
    created_at timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX admin_audit_log_created_idx ON public.admin_audit_log(created_at DESC);
ALTER TABLE public.admin_audit_log ENABLE ROW LEVEL SECURITY;
REVOKE ALL ON public.admin_audit_log FROM anon, authenticated;
GRANT SELECT ON public.admin_audit_log TO authenticated;

CREATE OR REPLACE FUNCTION public.can_moderate_social_content()
RETURNS boolean
LANGUAGE sql
STABLE
SECURITY DEFINER
SET search_path TO 'public'
AS $$
    SELECT EXISTS (
        SELECT 1 FROM public.profiles
        WHERE id = auth.uid() AND role IN ('admin', 'moderator')
    );
$$;

REVOKE ALL ON FUNCTION public.can_moderate_social_content() FROM PUBLIC;
GRANT EXECUTE ON FUNCTION public.can_moderate_social_content() TO authenticated;

CREATE POLICY post_reports_moderator_select
ON public.post_reports FOR SELECT TO authenticated
USING (public.can_moderate_social_content());

CREATE POLICY admin_audit_log_admin_select
ON public.admin_audit_log FOR SELECT TO authenticated
USING (public.is_admin());

CREATE OR REPLACE FUNCTION public.admin_dashboard_metrics()
RETURNS jsonb
LANGUAGE plpgsql
STABLE
SECURITY DEFINER
SET search_path TO 'public'
AS $$
BEGIN
    IF auth.uid() IS NULL OR NOT public.can_moderate_social_content() THEN
        RAISE EXCEPTION 'Moderation permission required';
    END IF;
    RETURN jsonb_build_object(
        'users', (SELECT count(*) FROM public.profiles),
        'posts', (SELECT count(*) FROM public.posts),
        'communities', (SELECT count(*) FROM public.communities),
        'reels', (SELECT count(*) FROM public.reels),
        'pending_reports', (SELECT count(*) FROM public.post_reports WHERE status = 'pending')
    );
END;
$$;
REVOKE ALL ON FUNCTION public.admin_dashboard_metrics() FROM PUBLIC;
GRANT EXECUTE ON FUNCTION public.admin_dashboard_metrics() TO authenticated;

CREATE OR REPLACE FUNCTION public.admin_list_reports(p_pending_only boolean DEFAULT true)
RETURNS TABLE (
    report_id uuid,
    post_id uuid,
    reason text,
    reported_at timestamptz,
    status text,
    moderator_note text,
    post_content text,
    post_author_id uuid,
    post_author_name text
)
LANGUAGE plpgsql
STABLE
SECURITY DEFINER
SET search_path TO 'public'
AS $$
BEGIN
    IF auth.uid() IS NULL OR NOT public.can_moderate_social_content() THEN
        RAISE EXCEPTION 'Moderation permission required';
    END IF;
    RETURN QUERY
    SELECT r.id, r.post_id, r.reason, r.created_at, r.status, r.moderator_note,
           p.content, p.author_id, author.display_name
    FROM public.post_reports r
    LEFT JOIN public.posts p ON p.id = r.post_id
    LEFT JOIN public.profiles author ON author.id = p.author_id
    WHERE (NOT coalesce(p_pending_only, true) OR r.status = 'pending')
    ORDER BY r.created_at ASC
    LIMIT 200;
END;
$$;
REVOKE ALL ON FUNCTION public.admin_list_reports(boolean) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION public.admin_list_reports(boolean) TO authenticated;

CREATE OR REPLACE FUNCTION public.review_post_report(
    p_report_id uuid,
    p_status text,
    p_note text DEFAULT NULL
)
RETURNS void
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path TO 'public'
AS $$
DECLARE
    v_post_id uuid;
BEGIN
    IF auth.uid() IS NULL OR NOT public.can_moderate_social_content() THEN
        RAISE EXCEPTION 'Moderation permission required';
    END IF;
    IF p_status NOT IN ('resolved', 'dismissed') THEN
        RAISE EXCEPTION 'Invalid report status';
    END IF;
    IF length(coalesce(p_note, '')) > 1000 THEN
        RAISE EXCEPTION 'Moderator note is too long';
    END IF;

    UPDATE public.post_reports
       SET status = p_status,
           reviewed_by = auth.uid(),
           reviewed_at = now(),
           moderator_note = nullif(trim(coalesce(p_note, '')), '')
     WHERE id = p_report_id
     RETURNING post_id INTO v_post_id;
    IF NOT FOUND THEN RAISE EXCEPTION 'Report not found'; END IF;

    INSERT INTO public.admin_audit_log(actor_id, action, target_type, target_id, details)
    VALUES (
        auth.uid(),
        CASE WHEN p_status = 'resolved' THEN 'post_report_resolved' ELSE 'post_report_dismissed' END,
        'post_report', p_report_id,
        jsonb_build_object('post_id', v_post_id, 'status', p_status, 'note', nullif(trim(coalesce(p_note, '')), ''))
    );
END;
$$;
REVOKE ALL ON FUNCTION public.review_post_report(uuid, text, text) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION public.review_post_report(uuid, text, text) TO authenticated;
