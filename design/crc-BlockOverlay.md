# BlockOverlay
**Requirements:** R13, R14, R15, R30, R31, R42

Manages the `TYPE_APPLICATION_OVERLAY` window drawn above blocked apps.

## Knows
- windowManager: WindowManager (system service)
- overlayView: View? (null when not shown)
- blockedAppLabel: String (display name shown in the overlay)

## Does
- show(appLabel: String, onGoHome: () -> Unit) — normal overlay (no lock); no-op if same app already shown
- showLocked(appLabel: String, lockPartnerName: String, onGoHome: () -> Unit, onVerifyPin: suspend (String) -> Boolean, onUnlock: (Int) -> Unit) — locked overlay with PIN entry and duration picker
- hide() — removes overlayView from WindowManager; sets to null

## Collaborators
- BlockService: calls show/showLocked/hide
- LockRepository: onVerifyPin callback invokes verifyPinAndUnlock
- WindowManager: Android system service for drawing the overlay

## Sequences
- seq-block-detection.md
- seq-friend-lock-overlay.md
