-- Run this in Supabase SQL Editor (Project → SQL Editor → + New query → paste → Run).
-- Safe to run more than once (uses IF NOT EXISTS / CREATE OR REPLACE where possible).

create extension if not exists pgcrypto;

-- 1. Profiles -----------------------------------------------------------------
create table if not exists public.profiles (
  id           uuid primary key references auth.users(id) on delete cascade,
  email        text unique not null,
  display_name text,
  invite_code  text unique not null,
  created_at   timestamptz not null default now()
);

-- Auto-create a profile + invite code on every new auth user.
create or replace function public.handle_new_user()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
begin
  insert into public.profiles (id, email, invite_code)
  values (
    new.id,
    new.email,
    upper(substring(encode(gen_random_bytes(6), 'base64') from 1 for 6))
  )
  on conflict (id) do nothing;
  return new;
end;
$$;

drop trigger if exists on_auth_user_created on auth.users;
create trigger on_auth_user_created
  after insert on auth.users
  for each row execute procedure public.handle_new_user();

-- 2. Friends (mutual, two rows per friendship) --------------------------------
create table if not exists public.friends (
  user_id    uuid not null references public.profiles(id) on delete cascade,
  friend_id  uuid not null references public.profiles(id) on delete cascade,
  created_at timestamptz not null default now(),
  primary key (user_id, friend_id),
  check (user_id <> friend_id)
);

-- 3. Daily usage (one row per user per day) -----------------------------------
create table if not exists public.daily_usage (
  user_id       uuid not null references public.profiles(id) on delete cascade,
  date          date not null,
  total_minutes integer not null default 0,
  updated_at    timestamptz not null default now(),
  primary key (user_id, date)
);

-- 4. Blocking schedules -------------------------------------------------------
-- R7: per-user, per-app blocking rules with time windows and recurrence.
-- R8: locked_by_user_id reserved for future friend-lock feature (null for now).
create table if not exists public.blocking_schedules (
  id                  uuid primary key default gen_random_uuid(),
  user_id             uuid not null references public.profiles(id) on delete cascade,
  package_name        text not null,
  is_all_day          boolean not null default false,
  start_time          time,                      -- null when is_all_day = true
  end_time            time,                      -- null when is_all_day = true
  recurrence_days     int[] not null default '{}', -- 1=Mon..7=Sun; empty = every day
  is_enabled          boolean not null default true,
  locked_by_user_id   uuid references public.profiles(id), -- future friend-lock, null now
  created_at          timestamptz not null default now(),
  updated_at          timestamptz not null default now()
);

-- 5. Row-Level Security -------------------------------------------------------
alter table public.profiles           enable row level security;
alter table public.friends            enable row level security;
alter table public.daily_usage        enable row level security;
alter table public.blocking_schedules enable row level security;

-- profiles: anyone signed-in can read (needed for invite-code lookup);
--           only the owner can update/insert their row.
drop policy if exists profiles_select on public.profiles;
create policy profiles_select on public.profiles
  for select using (auth.role() = 'authenticated');

drop policy if exists profiles_insert on public.profiles;
create policy profiles_insert on public.profiles
  for insert with check (auth.uid() = id);

drop policy if exists profiles_update on public.profiles;
create policy profiles_update on public.profiles
  for update using (auth.uid() = id);

-- friends: you can only see/insert/delete rows where user_id = you.
drop policy if exists friends_select on public.friends;
create policy friends_select on public.friends
  for select using (auth.uid() = user_id);

drop policy if exists friends_insert on public.friends;
create policy friends_insert on public.friends
  for insert with check (
    auth.uid() = user_id
    or auth.uid() = friend_id     -- allow the inverse row inserted by the requester
  );

drop policy if exists friends_delete on public.friends;
create policy friends_delete on public.friends
  for delete using (auth.uid() = user_id);

-- daily_usage: owner can read/write own rows.
drop policy if exists usage_own on public.daily_usage;
create policy usage_own on public.daily_usage
  for all using (auth.uid() = user_id)
  with check (auth.uid() = user_id);

-- blocking_schedules: owner can read/write/delete own rows only.
drop policy if exists blocking_schedules_all on public.blocking_schedules;
create policy blocking_schedules_all on public.blocking_schedules
  for all using (auth.uid() = user_id)
  with check (auth.uid() = user_id);

-- daily_usage: also read rows of users who are in your friends list.
drop policy if exists usage_friends_read on public.daily_usage;
create policy usage_friends_read on public.daily_usage
  for select using (
    exists (
      select 1 from public.friends f
      where f.user_id = auth.uid()
        and f.friend_id = public.daily_usage.user_id
    )
  );
