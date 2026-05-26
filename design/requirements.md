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
