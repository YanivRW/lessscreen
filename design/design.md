# Design: App Blocking Feature

## Intent

Allow users to block tracked social media apps during configurable time windows.
An `AccessibilityService` detects when a blocked app is foregrounded and shows a
full-screen `TYPE_APPLICATION_OVERLAY`. Schedules are stored in Supabase so the
architecture can later accommodate a friend-lock feature without a schema rewrite.

## Cross-cutting Concerns

- **Auth:** all Supabase calls use the existing authenticated `SupabaseClient`
  singleton; `BlockRepository` scopes all queries to the current user's id.
- **Offline:** write ops that fail with IOException are queued in SharedPreferences
  and replayed by `flushQueue()` on reconnect.
- **Permissions:** both `SYSTEM_ALERT_WINDOW` and `BIND_ACCESSIBILITY_SERVICE`
  require explicit user grants; `PermissionGuide` handles checks and deep-links
  to system settings. Neither the overlay nor the service will attempt to run
  without their respective permissions.
- **No regressions:** the Block tab is appended to the existing bottom nav;
  no existing screen logic is changed.

## Artifacts

### CRC Cards
- [ ] crc-BlockSchedule.md → `app/src/main/kotlin/com/yanivrw/lessscreen/data/models/Models.kt`
- [ ] crc-BlockRepository.md → `app/src/main/kotlin/com/yanivrw/lessscreen/data/BlockRepository.kt`
- [ ] crc-BlockService.md → `app/src/main/kotlin/com/yanivrw/lessscreen/blocking/BlockAccessibilityService.kt`
- [ ] crc-BlockOverlay.md → `app/src/main/kotlin/com/yanivrw/lessscreen/blocking/BlockOverlay.kt`
- [ ] crc-BlockViewModel.md → `app/src/main/kotlin/com/yanivrw/lessscreen/ui/screens/BlockScreen.kt`
- [ ] crc-PermissionGuide.md → `app/src/main/kotlin/com/yanivrw/lessscreen/permission/BlockPermission.kt`

### Sequences
- [ ] seq-block-detection.md → `app/src/main/kotlin/com/yanivrw/lessscreen/blocking/BlockAccessibilityService.kt`, `app/src/main/kotlin/com/yanivrw/lessscreen/blocking/BlockOverlay.kt`
- [ ] seq-schedule-sync.md → `app/src/main/kotlin/com/yanivrw/lessscreen/data/BlockRepository.kt`, `app/src/main/kotlin/com/yanivrw/lessscreen/ui/screens/BlockScreen.kt`

### UI Layouts
- [ ] ui-block-screen.md → `app/src/main/kotlin/com/yanivrw/lessscreen/ui/screens/BlockScreen.kt`

### Test Designs
- [ ] test-BlockRepository.md → `app/src/test/kotlin/com/yanivrw/lessscreen/BlockRepositoryTest.kt`
- [ ] test-BlockService.md → `app/src/test/kotlin/com/yanivrw/lessscreen/BlockServiceTest.kt`

## Gaps

- [ ] O1: No unit tests written — test design files exist (test-BlockRepository.md, test-BlockService.md) but no Kotlin test files yet
  - [ ] BlockRepository: 5 test cases (load, save online, save offline, flush queue, delete)
  - [ ] BlockService: 5 test cases (blocked triggers overlay, unblocked hides overlay, disabled schedule, outside window, StateFlow reload)
- [ ] O2: No local build possible (no gradlew in repo) — first compile validation happens via CI on push to main
- [ ] O3: Supabase schema.sql needs to be manually run in the Supabase SQL Editor to create blocking_schedules table and RLS policies
