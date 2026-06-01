# UI: Lock Management (Friends Screen)

**Requirements:** R35, R36, R37
**CRC:** crc-LockRepository.md

New collapsible section at the bottom of the Friends screen.

```
────────────────────────────────────
  Locks I manage
────────────────────────────────────

  [Pending]
  ┌──────────────────────────────────┐
  │ 🔒  Dana → Instagram             │
  │     Waiting for you to set PIN   │
  │     [ Set PIN ]                  │
  └──────────────────────────────────┘

  [Active]
  ┌──────────────────────────────────┐
  │ 🔒  Dana → TikTok                │
  │     Unlocked until 18:30         │  ← or "Locked" / "Unlocked indefinitely"
  │     [ Re-lock ]  [ Remove lock ] │
  └──────────────────────────────────┘
```

**Set PIN sheet** (bottom sheet, shown when partner taps "Set PIN"):

```
  Set unlock code for Dana's Instagram
  ─────────────────────────────────────
  Choose a 6-digit PIN that Dana will
  need to enter to unlock the app.

  [ _ ] [ _ ] [ _ ] [ _ ] [ _ ] [ _ ]

  [ Confirm ]
```

Hidden when no managed locks exist (section not shown at all).
