-- Allow members to submit quote suggestions without allowing them to self-approve.
DROP POLICY IF EXISTS quotes_insert ON public.quotes;
CREATE POLICY quotes_insert
ON public.quotes FOR INSERT TO authenticated
WITH CHECK (
    (auth.uid() = submitted_by AND status = 'pending')
    OR public.is_admin()
);

DROP POLICY IF EXISTS quotes_update ON public.quotes;
CREATE POLICY quotes_update
ON public.quotes FOR UPDATE TO authenticated
USING (
    public.is_admin()
    OR (auth.uid() = submitted_by AND status = 'pending')
)
WITH CHECK (
    public.is_admin()
    OR (auth.uid() = submitted_by AND status = 'pending')
);

CREATE OR REPLACE FUNCTION public.review_quote_submission(
    p_quote_id uuid,
    p_status text
)
RETURNS void
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path TO 'pg_catalog', 'public'
AS $$
BEGIN
    IF auth.uid() IS NULL OR NOT public.is_admin() THEN
        RAISE EXCEPTION 'Admin permission required';
    END IF;
    IF p_status NOT IN ('approved', 'rejected') THEN
        RAISE EXCEPTION 'Invalid quote status';
    END IF;

    UPDATE public.quotes
       SET status = p_status
     WHERE id = p_quote_id AND status = 'pending';
    IF NOT FOUND THEN
        RAISE EXCEPTION 'Pending quote not found';
    END IF;

    INSERT INTO public.admin_audit_log(actor_id, action, target_type, target_id, details)
    VALUES (
        auth.uid(),
        CASE WHEN p_status = 'approved' THEN 'quote_approved' ELSE 'quote_rejected' END,
        'quote', p_quote_id,
        jsonb_build_object('status', p_status)
    );
END;
$$;

REVOKE ALL ON FUNCTION public.review_quote_submission(uuid, text) FROM PUBLIC, anon, authenticated;
GRANT EXECUTE ON FUNCTION public.review_quote_submission(uuid, text) TO authenticated;
