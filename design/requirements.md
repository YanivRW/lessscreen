# Requirements

## Feature: app-blocking
**Source:** specs/app-blocking.md

- **R1:** A blocking schedule is associated with exactly one user and one app package.
- **R2:** A schedule supports an all-day mode that blocks the app for the entire calendar day.
- **R3:** A schedule supports a time-window mode with a start time and end time.
- **R4:** A schedule has a set of recurrence days (any subset of Mon–Sun); an empty set means one-time (no recurrence).
- **R5:** Multiple schedules may exist for the same app.
- **R6:** A schedule can be toggled enabled/disabled without deleting it.
- **R7:** Schedules are stored in Supabase in a `blocking_schedules` table.
- **R8:** (inferred) The `blocking_schedules` table schema must leave room for a future `locked_by_user_id` column (friend-lock) without a breaking migration.
- **R9:** Local changes to schedules queue offline and sync to Supabase when connectivity returns.
- **R10:** All six apps in `TRACKED_PACKAGES` are eligible to be blocked.
- **R11:** An `AccessibilityService` monitors foreground app changes continuously while running.
- **R12:** When the foreground app matches a tracked package with an active schedule, the block overlay is shown.
- **R13:** The block overlay is a full-screen window drawn with `TYPE_APPLICATION_OVERLAY`.
- **R14:** The overlay displays the blocked app's name and a motivational message.
- **R15:** The overlay provides a way for the user to navigate away (back/home), but no snooze or override.
- **R16:** The app requests `SYSTEM_ALERT_WINDOW` permission and guides the user to grant it via system settings on first use.
- **R17:** The app requests `BIND_ACCESSIBILITY_SERVICE` permission and guides the user to grant it via system settings on first use.
- **R18:** A new "Block" screen is added as a tab in the bottom navigation bar.
- **R19:** The Block screen lists all existing schedules grouped by app.
- **R20:** The Block screen allows the user to add a new schedule: select app(s), choose all-day or time-window, pick recurrence days.
- **R21:** The Block screen allows the user to toggle a schedule on/off.
- **R22:** The Block screen allows the user to delete a schedule.
- **R23:** (inferred) The `AccessibilityService` loads schedules on start and refreshes them whenever a schedule change is made.
- **R24:** (inferred) No regressions to existing screens (today, scoreboard, friends) when the Block tab is added.

## Feature: friend-lock
**Source:** specs/friend-lock.md

- **R25:** A blocking schedule may optionally have a lock partner (`locked_by_user_id`) who is one of the user's mutual friends.
- **R26:** A schedule with a lock partner but no PIN set is in "pending" state; the overlay behaves as a normal (unlocked) block until the PIN is set.
- **R27:** The lock partner sets a 6-digit numeric PIN via their own device; the PIN is stored as a bcrypt hash in a separate `block_lock_pins` table with no client-readable select policy.
- **R28:** PIN verification and unlock-state writes are performed by a `security definer` RPC (`verify_lock_pin`) so the hash is never transmitted to the client.
- **R29:** Setting a PIN is performed by a `security definer` RPC (`set_lock_pin`) callable only by the user whose id matches `locked_by_user_id` on the schedule.
- **R30:** When a locked schedule is active, the overlay shows "Locked by [Friend]" and a 6-digit PIN-entry field instead of the normal "Go Home" button only.
- **R31:** After correct PIN entry a duration picker appears with options: 30 min, 1 hour, 2 hours, Until re-locked.
- **R32:** The selected unlock duration is written to `blocking_schedules.unlocked_until` (`9999-12-31T23:59:59Z` = indefinite).
- **R33:** The unlock state persists across app restarts; the overlay checks `unlocked_until` before showing.
- **R34:** When `unlocked_until` passes, the overlay returns on the next foreground event for that app.
- **R35:** The lock partner sees pending lock requests ("X wants you to lock their Y") in a "Locks I manage" section of the Friends screen.
- **R36:** The lock partner can re-lock (set `unlocked_until = null`) from the Friends screen at any time.
- **R37:** The lock partner can remove themselves as lock partner (clears `locked_by_user_id`, deletes the PIN row) from the Friends screen.
- **R38:** Changes to `unlocked_until` propagate to the locked user's device via Supabase Realtime within a few seconds.
- **R39:** `blocking_schedules` gains `unlocked_until timestamptz` and `unlock_requested_at timestamptz` columns.
- **R40:** New table `block_lock_pins` with columns `schedule_id` (FK), `passcode_hash`, `set_at`; no select RLS policy.
- **R41:** The Block screen shows a lock-partner badge on locked schedules and an "Add friend lock" / "Remove lock" action.
- **R42:** A wrong PIN shows "Incorrect code" with no additional hint; no lockout after N attempts (out of scope).
