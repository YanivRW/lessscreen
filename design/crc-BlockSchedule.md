# BlockSchedule
**Requirements:** R1, R2, R3, R4, R5, R6, R8

Immutable data class representing one blocking rule for one app.

## Knows
- id: UUID string (Supabase primary key)
- userId: UUID string of the owning user
- packageName: app package to block
- isAllDay: true → block the full calendar day
- startTime: LocalTime? (null when isAllDay)
- endTime: LocalTime? (null when isAllDay)
- recurrenceDays: Set<DayOfWeek> (empty = one-time)
- isEnabled: whether this schedule is active
- lockedByUserId: UUID? (null now; reserved for future friend-lock — R8)

## Does
- isActiveNow(now: LocalDateTime): Boolean — returns true if the schedule is
  enabled and the current time falls within the block window on the current day
- matchesDay(day: DayOfWeek): Boolean — true if recurrenceDays contains day, or
  recurrenceDays is empty and today equals the one-time date

## Collaborators
- BlockRepository: persists and retrieves schedules
- BlockService: calls isActiveNow() to decide whether to show overlay

## Sequences
- seq-block-detection.md
- seq-schedule-sync.md
