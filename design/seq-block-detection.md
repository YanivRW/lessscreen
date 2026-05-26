# Sequence: Block Detection

Covers R11, R12, R13, R14, R15, R16, R17, R23.

## Diagram 1 — Foreground app triggers overlay

```
BlockService          BlockRepository       BlockSchedule         BlockOverlay
     |                      |                     |                    |
1.1  | onServiceConnected()  |                     |                    |
1.2  |---loadSchedules()---->|                     |                    |
1.3  |<--StateFlow<List>-----|                     |                    |
     |                      |                     |                    |
1.4  | onAccessibilityEvent(TYPE_WINDOW_STATE_CHANGED)                  |
1.5  | extract packageName   |                     |                    |
1.6  | for each schedule     |                     |                    |
1.6.1|                       |  isActiveNow(now)-->|                    |
1.6.2|                       |<--Boolean-----------|                    |
1.7  | [blocked=true]        |                     |                    |
1.7.1|--------------------------------------------------------show(label)|
1.8  | [blocked=false]       |                     |                    |
1.8.1|--------------------------------------------------------hide()     |
     |                      |                     |                    |
1.9  | user presses back/home in overlay           |                    |
1.10 |                       |                     | onBackPressed()    |
1.11 |  performGlobalAction(GLOBAL_ACTION_BACK)    |                    |
1.12 | Android returns to home/previous screen     |                    |
1.13 | onAccessibilityEvent fires again (new pkg)  |                    |
1.14 | [new pkg not blocked] |                     |                    |
1.14.1|-------------------------------------------------------hide()    |
```

## Diagram 2 — Permission gate on first use

```
BlockScreen           PermissionGuide       Android Settings
     |                      |                     |
2.1  | user taps "Enable Blocking"                |
2.2  |---hasOverlayPermission()-->|               |
2.3  |<--false--------------------|               |
2.4  |---openOverlaySettings()---->               |
2.5  |                             |--ACTION_MANAGE_OVERLAY_PERMISSION-->|
2.6  | user grants permission      |                                     |
2.7  |---hasAccessibilityEnabled()->               |
2.8  |<--false----------------------               |
2.9  |---openAccessibilitySettings()-------------->|
2.10 | user enables BlockService   |               |
2.11 | BlockService.onServiceConnected() fires     |
```
