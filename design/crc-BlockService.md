# BlockService
**Requirements:** R11, R12, R13, R23

`AccessibilityService` subclass. Monitors foreground app changes and triggers
the overlay when the current app is blocked.

## Knows
- blockRepository: BlockRepository (injected via companion-object singleton)
- blockOverlay: BlockOverlay
- currentForegroundPackage: String? (last seen)
- activeSchedules: List<BlockSchedule> (kept in sync via StateFlow)

## Does
- onAccessibilityEvent(event): extracts package from TYPE_WINDOW_STATE_CHANGED;
  calls checkAndBlock()
- checkAndBlock(packageName: String) — finds an active schedule for packageName;
  shows overlay if found, hides if not
- onServiceConnected() — loads schedules from BlockRepository; subscribes to
  schedules StateFlow
- onUnbind() — hides overlay, cleans up

## Collaborators
- BlockRepository: reads activeSchedules (R23)
- BlockOverlay: show/hide
- BlockSchedule: calls isActiveNow()

## Sequences
- seq-block-detection.md
