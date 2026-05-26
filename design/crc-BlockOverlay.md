# BlockOverlay
**Requirements:** R13, R14, R15

Manages the `TYPE_APPLICATION_OVERLAY` window drawn above blocked apps.

## Knows
- windowManager: WindowManager (system service)
- overlayView: View? (null when not shown)
- blockedAppLabel: String (display name shown in the overlay)

## Does
- show(appLabel: String) — inflates overlay layout, adds to WindowManager with
  TYPE_APPLICATION_OVERLAY flags; no-op if already shown for same app
- hide() — removes overlayView from WindowManager; sets to null
- onBackPressed() / onHomePressed() — dispatches GLOBAL_ACTION_BACK or
  GLOBAL_ACTION_HOME via the AccessibilityService; does NOT dismiss the overlay
  (dismissal only happens when block ends)

## Collaborators
- BlockService: calls show/hide
- WindowManager: Android system service for drawing the overlay

## Sequences
- seq-block-detection.md
