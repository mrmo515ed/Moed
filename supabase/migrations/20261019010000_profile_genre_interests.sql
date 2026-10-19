create table if not exists public.profile_genre_interests (
  user_id uuid not null references auth.users(id) on delete cascade,
  genre_id uuid not null references public.genres(id) on delete cascade,
  created_at timestamptz not null default now(),
  primary key (user_id, genre_id)
);

alter table public.profile_genre_interests enable row level security;

create policy profile_genre_interests_select_own
  on public.profile_genre_interests for select to authenticated
  using (auth.uid() = user_id);

create policy profile_genre_interests_insert_own
  on public.profile_genre_interests for insert to authenticated
  with check (auth.uid() = user_id);

create policy profile_genre_interests_delete_own
  on public.profile_genre_interests for delete to authenticated
  using (auth.uid() = user_id);

grant select, insert, delete on public.profile_genre_interests to authenticated;

create or replace function public.replace_my_genre_interests(p_genre_ids uuid[])
returns void
language plpgsql
security invoker
set search_path = public
as $function$
declare
  current_user_id uuid := auth.uid();
begin
  if current_user_id is null then
    raise exception 'Authentication required';
  end if;
  if coalesce(array_length(p_genre_ids, 1), 0) < 3 then
    raise exception 'Select at least three genres';
  end if;
  if coalesce(array_length(p_genre_ids, 1), 0) > 10 then
    raise exception 'Select no more than ten genres';
  end if;

  delete from public.profile_genre_interests
   where user_id = current_user_id;

  insert into public.profile_genre_interests (user_id, genre_id)
  select current_user_id, selected.genre_id
    from (select distinct unnest(p_genre_ids) as genre_id) as selected
    join public.genres g on g.id = selected.genre_id;
end;
$function$;

grant execute on function public.replace_my_genre_interests(uuid[]) to authenticated;
