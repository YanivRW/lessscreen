# App Blocking Feature

**Language/environment:** Kotlin, Android (min SDK 26), Jetpack Compose, Supabase.

## Overview

LessScreen can already measure social media usage. This feature lets users
actively *block* the tracked apps during configured time windows. When a user
opens a blocked app, a full-screen overlay appears that prevents use until the
block period ends (or is manually lifted).

## Blocking schedules

A schedule is per-user, per-app, with flexible recurrence:

- **All-day block:** block a specific app for an entire calendar day.
- **Time-window block:** block an app between a start time and end time on
  selected days (e.g. Instagram blocked Mon–Fri 9 pm to 9 am).
- **Recurring:** schedules repeat on selected days of the week (any subset of
  Mon–Sun). A schedule with every day selected runs daily.
- **One-time:** a schedule can be set for a single day only (no recurrence).
- Multiple schedules can exist for the same app (e.g. one for evenings and one
  for weekend mornings).
- Schedules are stored in Supabase so they can later be controlled by a trusted
  friend (the future friend-lock feature).

## Which apps can be blocked

All apps in `TRACKED_PACKAGES` (Instagram, TikTok, YouTube, Facebook, X,
LinkedIn) are blockable. The user chooses which apps each schedule applies to.

## Detecting a blocked app

The app uses an `AccessibilityService` to observe foreground app changes. When
the foreground package matches a tracked app that has an active blocking
schedule, the service shows the block overlay.

## Block overlay

- Full-screen overlay drawn with `TYPE_APPLICATION_OVERLAY` (SYSTEM_ALERT_WINDOW
  permission).
- Shows the blocked app's name and a motivational message (e.g. "You're doing
  great — stay off Instagram!").
- Has a back/home button so the user can navigate away.
- No snooze or override — the block holds until the schedule ends.

## Permissions required

- `BIND_ACCESSIBILITY_SERVICE` — detect foreground app.
- `SYSTEM_ALERT_WINDOW` — draw the overlay above other apps.
- Both require explicit user grant via system settings; the app guides the user
  through granting each permission at first use.

## UI for managing schedules

A new "Block" screen (tab in the bottom nav) where the user can:
1. See all existing schedules (grouped by app).
2. Add a new schedule: pick app(s), choose all-day or time window, pick
   recurrence days.
3. Toggle a schedule on/off (pause without deleting).
4. Delete a schedule.

## Sync to Supabase

Schedules are stored in a new `blocking_schedules` table. The local service
reads schedules on start and re-syncs whenever the user makes a change. Offline
changes queue and sync when connectivity returns.

## Non-goals (out of scope for this phase)

- Friend-lock (a friend controlling the schedule). Architecture must accommodate
  it later, but no UI or auth logic for it now.
- Blocking specific in-app features (e.g. YouTube Shorts only). Full-app
  blocking only.
- Root or ADB-based blocking — overlay approach only.
