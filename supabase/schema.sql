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
    upper(substring(encode(extensions.gen_random_bytes(6), 'base64') from 1 for 6))
  )
  on conflict do nothing;
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
-- R7, R39: per-user, per-app blocking rules with time windows and recurrence.
create table if not exists public.blocking_schedules (
  id                    uuid primary key default gen_random_uuid(),
  user_id               uuid not null references public.profiles(id) on delete cascade,
  package_name          text not null,
  is_all_day            boolean not null default false,
  start_time            time,
  end_time              time,
  recurrence_days       int[] not null default '{}',
  is_enabled            boolean not null default true,
  locked_by_user_id     uuid references public.profiles(id),
  unlocked_until        timestamptz,        -- R39: null=locked, future=timed unlock, 9999=indefinite
  unlock_requested_at   timestamptz,        -- R39: set when user wants friend to see request
  created_at            timestamptz not null default now(),
  updated_at            timestamptz not null default now()
);

-- 4b. Lock PINs — R40: no select policy; hash never readable by client --------
create table if not exists public.block_lock_pins (
  schedule_id   uuid primary key references public.blocking_schedules(id) on delete cascade,
  passcode_hash text not null,
  set_at        timestamptz not null default now()
);
alter table public.block_lock_pins enable row level security;
-- intentionally no select policy: all access via security definer RPCs only

-- RPC: set_lock_pin — R29: only callable by locked_by_user_id ----------------
create or replace function public.set_lock_pin(p_schedule_id uuid, p_raw_pin text)
returns void
language plpgsql
security definer
set search_path = public
as $$
declare
  v_locked_by uuid;
begin
  select locked_by_user_id into v_locked_by
  from blocking_schedules where id = p_schedule_id;
  if v_locked_by is null or v_locked_by <> auth.uid() then
    raise exception 'not authorized';
  end if;
  insert into block_lock_pins (schedule_id, passcode_hash)
  values (p_schedule_id, extensions.crypt(p_raw_pin, extensions.gen_salt('bf')))
  on conflict (schedule_id) do update set passcode_hash = excluded.passcode_hash, set_at = now();
end;
$$;

-- RPC: check_lock_pin — R28: verify PIN only (no state change) ---------------
create or replace function public.check_lock_pin(p_schedule_id uuid, p_raw_pin text)
returns boolean
language plpgsql
security definer
set search_path = public
as $$
declare
  v_hash text;
  v_owner uuid;
begin
  select p.passcode_hash, s.user_id
  into v_hash, v_owner
  from block_lock_pins p
  join blocking_schedules s on s.id = p.schedule_id
  where p.schedule_id = p_schedule_id;
  if v_hash is null then return false; end if;
  if auth.uid() <> v_owner then return false; end if;
  return extensions.crypt(p_raw_pin, v_hash) = v_hash;
end;
$$;

-- RPC: verify_lock_pin — R28: verify PIN and write unlocked_until atomically --
-- duration_minutes: -1 = indefinite (9999-12-31), else now + duration
create or replace function public.verify_lock_pin(
  p_schedule_id    uuid,
  p_raw_pin        text,
  p_duration_mins  int
) returns boolean
language plpgsql
security definer
set search_path = public
as $$
declare
  v_hash text;
  v_owner uuid;
begin
  select p.passcode_hash, s.user_id
  into v_hash, v_owner
  from block_lock_pins p
  join blocking_schedules s on s.id = p.schedule_id
  where p.schedule_id = p_schedule_id;

  if v_hash is null then return false; end if;
  if extensions.crypt(p_raw_pin, v_hash) <> v_hash then return false; end if;
  if auth.uid() <> v_owner then return false; end if;

  update blocking_schedules set
    unlocked_until = case
      when p_duration_mins = -1 then '9999-12-31 23:59:59+00'::timestamptz
      else now() + (p_duration_mins || ' minutes')::interval
    end,
    updated_at = now()
  where id = p_schedule_id;

  return true;
end;
$$;

-- RPC: remove_lock — R37: clear partner + delete PIN in one transaction -------
create or replace function public.remove_lock(p_schedule_id uuid)
returns void
language plpgsql
security definer
set search_path = public
as $$
declare
  v_locked_by uuid;
  v_owner     uuid;
begin
  select locked_by_user_id, user_id into v_locked_by, v_owner
  from blocking_schedules where id = p_schedule_id;
  -- only the lock partner or the schedule owner may remove
  if auth.uid() <> v_locked_by and auth.uid() <> v_owner then
    raise exception 'not authorized';
  end if;
  delete from block_lock_pins where schedule_id = p_schedule_id;
  update blocking_schedules set
    locked_by_user_id = null,
    unlocked_until    = null,
    updated_at        = now()
  where id = p_schedule_id;
end;
$$;

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

-- blocking_schedules: owner full access; lock partner can read + update unlocked_until/relock.
drop policy if exists blocking_schedules_all on public.blocking_schedules;
create policy blocking_schedules_all on public.blocking_schedules
  for all using (auth.uid() = user_id)
  with check (auth.uid() = user_id);

-- R35, R36: lock partner can read schedules they manage and relock (update unlocked_until).
drop policy if exists blocking_schedules_partner_select on public.blocking_schedules;
create policy blocking_schedules_partner_select on public.blocking_schedules
  for select using (auth.uid() = locked_by_user_id);

drop policy if exists blocking_schedules_partner_update on public.blocking_schedules;
create policy blocking_schedules_partner_update on public.blocking_schedules
  for update using (auth.uid() = locked_by_user_id)
  with check (auth.uid() = locked_by_user_id);

-- Enable Realtime on blocking_schedules for unlocked_until changes (R38).
alter publication supabase_realtime add table public.blocking_schedules;

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
