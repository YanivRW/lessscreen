# UI: Locked Overlay

**Requirements:** R30, R31, R42
**CRC:** crc-BlockOverlay.md

Replaces the normal "← Go Home" overlay when a schedule has an active friend lock.

```
┌──────────────────────────────────────┐
│                                      │
│                                      │
│               📵                     │
│                                      │
│      Instagram is blocked.           │
│                                      │
│    Locked by Yaniv                   │  ← lockPartnerName from schedule
│                                      │
│  ┌────────────────────────────────┐  │
│  │  Enter unlock code             │  │  ← 6-digit numeric PIN field
│  │  [  ] [  ] [  ] [  ] [  ] [  ]│  │    auto-advances digit by digit
│  └────────────────────────────────┘  │
│                                      │
│  [  Incorrect code  ]                │  ← error, shown only on wrong PIN
│                                      │
│         ← Go Home                   │  ← always visible; GLOBAL_ACTION_HOME
│                                      │
└──────────────────────────────────────┘
```

After correct PIN → duration picker replaces PIN area:

```
┌──────────────────────────────────────┐
│                                      │
│      Unlock for how long?            │
│                                      │
│   [ 30 minutes ]                     │
│   [ 1 hour     ]                     │
│   [ 2 hours    ]                     │
│   [ Until re-locked ]                │
│                                      │
└──────────────────────────────────────┘
```

Tapping a duration writes `unlocked_until` and hides the overlay.
