# LockRepository
**Requirements:** R27, R28, R29, R32, R33, R36, R37, R38, R39, R40, R25, R26, R35

Handles all friend-lock Supabase operations: RPC calls for PIN set/verify,
unlock-state writes, Realtime subscription for `unlocked_until` changes, and
lock-management queries for the lock partner's Friends screen.

## Knows
- supabaseClient: SupabaseClient (existing singleton)
- realtimeChannel: RealtimeChannel? (active subscription per locked schedule)

## Does
- setLockPartner(scheduleId: String, friendUserId: String) — writes `locked_by_user_id` on schedule
- removeLock(scheduleId: String) — clears `locked_by_user_id` + deletes PIN row via `remove_lock` RPC
- setPin(scheduleId: String, rawPin: String) — calls `set_lock_pin(schedule_id, raw_pin)` RPC; only succeeds if caller == `locked_by_user_id`
- verifyPinAndUnlock(scheduleId: String, rawPin: String, durationMinutes: Int) — calls `verify_lock_pin(schedule_id, raw_pin, duration_minutes)` RPC; -1 = indefinite; returns Boolean
- relock(scheduleId: String) — sets `unlocked_until = null` on schedule (direct update, caller must be `locked_by_user_id`)
- loadManagedLocks(): List<LockInfo> — fetch schedules where `locked_by_user_id = me` with joined profile for the locked user's display name
- subscribeUnlockState(scheduleId: String, onChanged: (unlockedUntil: String?) -> Unit) — Realtime channel on `blocking_schedules` row; fires callback on `unlocked_until` change
- unsubscribe() — cancel active Realtime channel

## Collaborators
- SupabaseClient: all Supabase I/O
- BlockRepository: calls subscribeUnlockState when a locked schedule is active
- FriendsScreen: calls loadManagedLocks, relock, removeLock, setPin
- BlockScreen: calls setLockPartner, removeLock

## Sequences
- seq-friend-lock-setup.md
- seq-friend-lock-overlay.md
