# PermissionGuide
**Requirements:** R16, R17

Checks whether the two blocking permissions are granted and navigates the user
to the correct system settings screen to grant them.

## Knows
- context: Context

## Does
- hasOverlayPermission(): Boolean — checks Settings.canDrawOverlays(context)
- hasAccessibilityEnabled(): Boolean — checks if BlockService is in the enabled
  accessibility services list
- openOverlaySettings(context) — launches ACTION_MANAGE_OVERLAY_PERMISSION intent
- openAccessibilitySettings(context) — launches
  ACTION_ACCESSIBILITY_SETTINGS intent

## Collaborators
- BlockScreen (Composable): calls checks and opens settings on user tap
- BlockService: calls hasOverlayPermission() before attempting to show overlay

## Sequences
- seq-block-detection.md
