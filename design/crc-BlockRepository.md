# BlockRepository
**Requirements:** R1, R5, R6, R7, R8, R9, R10, R23

Single source of truth for blocking schedules. Bridges Supabase and local
in-memory cache; queues writes when offline.

## Knows
- supabaseClient: SupabaseClient (existing singleton)
- cache: List<BlockSchedule> (in-memory, refreshed on load/change)
- pendingQueue: List<PendingOp> (offline write queue, survived in SharedPreferences)

## Does
- loadSchedules(): List<BlockSchedule> — fetch from Supabase, update cache
- saveSchedule(schedule: BlockSchedule) — upsert to Supabase; queue if offline
- deleteSchedule(id: String) — delete from Supabase; queue if offline
- flushQueue() — replay queued ops against Supabase when connectivity returns
- schedules: StateFlow<List<BlockSchedule>> — observable for UI and service

## Collaborators
- SupabaseClient: all Supabase I/O
- BlockSchedule: the data shape
- BlockService: observes schedules StateFlow to stay up-to-date (R23)
- BlockViewModel: calls save/delete, observes StateFlow

## Sequences
- seq-schedule-sync.md
