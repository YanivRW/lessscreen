# Friend-Lock Feature

**Language/environment:** Kotlin, Android (min SDK 26), Jetpack Compose, Supabase
(Postgres + GoTrue + Realtime).

## Overview

A user can assign a trusted friend as the "lock partner" for one of their
blocking schedules. Once the friend sets a secret PIN via their own device,
the locked user's overlay cannot be dismissed without the friend physically
typing the correct PIN on the device. The friend also has remote control:
they can unlock for a timed window or indefinitely, and can re-lock at any
time from their own app.

## Setting up a friend lock

- When creating or editing a blocking schedule the user sees an "Add friend
  lock" option.
- Tapping it lets the user pick one of their mutual friends as the lock partner.
- The schedule is saved with the chosen friend as `locked_by_user_id`. It is in
  a "pending" state until the friend sets a PIN.
- The lock partner sees a pending request ("Yaniv wants you to lock their
  Instagram") in a new "Locks I manage" section of the Friends screen.
- The lock partner taps the request and enters a 6-digit numeric PIN. The PIN
  is stored server-side as a bcrypt hash. The locked user never sees the hash.
- Once the PIN is set the schedule is "active-locked."

## The overlay for a locked schedule

- The block overlay shows the lock partner's display name: "Locked by [Friend]."
- There is a PIN-entry field on the overlay.
- The user asks the friend to come over or call and read out the PIN. The
  friend (or anyone who knows the PIN) types it on the device.
- After the correct PIN is entered a duration picker appears:
  - 30 minutes
  - 1 hour
  - 2 hours
  - Until re-locked (indefinite)
- The friend selects a duration. The overlay dismisses and the app is
  accessible for that window. The unlock state is written to Supabase so it
  survives app restarts.
- When the timed window expires the overlay returns automatically the next
  time the app is foregrounded.

## Remote control by the lock partner

The "Locks I manage" section of the Friends screen also shows active locks:
- Each lock card shows the locked user's name, the blocked app, and the current
  unlock state (locked / unlocked until <time> / unlocked indefinitely).
- The lock partner can tap "Re-lock" to immediately cancel an active unlock
  (sets `unlocked_until` back to null).
- The lock partner can tap "Remove lock" to fully remove themselves as the
  lock partner (clears `locked_by_user_id` and deletes the stored PIN).
- Changes propagate to the locked user's device via Supabase Realtime.

## Security constraints

- The PIN hash must never be readable by the locked user, not even via the
  Supabase client. It lives in a separate `block_lock_pins` table with no
  select RLS policy.
- PIN verification and unlock-state writes happen inside a `security definer`
  Supabase RPC (`verify_lock_pin`) so the hash is never transmitted to the client.
- Setting a PIN is also a `security definer` RPC (`set_lock_pin`) callable only
  by the user whose id matches `locked_by_user_id` on the schedule.
- A wrong PIN returns a generic "Incorrect code" message with no hint.

## Data model additions

- `blocking_schedules` gains two columns:
  - `unlocked_until timestamptz` — null = locked, future timestamp = timed
    unlock, `9999-12-31T23:59:59Z` = indefinitely unlocked until re-locked.
  - `unlock_requested_at timestamptz` — optional, set when user taps a
    "Notify friend" button; used by the lock partner to see pending requests.
- New table `block_lock_pins`:
  - `schedule_id uuid` (FK → `blocking_schedules.id`, on delete cascade)
  - `passcode_hash text`
  - `set_at timestamptz`
  - No select RLS policy (access only via RPCs).

## Non-goals for this phase

- Push notifications when an unlock is requested. The lock partner must open
  the app to see pending requests.
- Allowing the locked user to request a remote unlock (no in-app messaging).
- More than one lock partner per schedule.
- Changing the PIN without removing and re-adding the lock.
