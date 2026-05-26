# UI Layout: Block Screen

**Requirements:** R18, R19, R20, R21, R22
**CRC:** crc-BlockViewModel.md, crc-PermissionGuide.md

## Permission Banner (shown when permissions not granted)

```
┌────────────────────────────────────────────┐
│  ⚠  Blocking requires two permissions      │
│  [Enable Overlay]  [Enable Accessibility]  │
└────────────────────────────────────────────┘
```

Each button opens the relevant system settings page (PermissionGuide).
Banner disappears once both permissions are granted.

## Main Schedule List (permissions granted, schedules exist)

```
┌────────────────────────────────────────────┐
│  Block                                     │  ← top bar title
├────────────────────────────────────────────┤
│  Instagram                                 │  ← app section header
│  ┌──────────────────────────────────────┐  │
│  │ 🔴  Evenings  9 PM – 11 PM  Mon–Fri  │  │
│  │     [toggle ●]          [delete 🗑]   │  │
│  └──────────────────────────────────────┘  │
│  ┌──────────────────────────────────────┐  │
│  │ ⚪  All day  Sunday                  │  │  ← disabled schedule
│  │     [toggle ○]          [delete 🗑]   │  │
│  └──────────────────────────────────────┘  │
│                                            │
│  TikTok                                    │
│  ┌──────────────────────────────────────┐  │
│  │ 🔴  All day  Every day               │  │
│  │     [toggle ●]          [delete 🗑]   │  │
│  └──────────────────────────────────────┘  │
│                                            │
│                          [+ Add Schedule]  │  ← FAB bottom-right
└────────────────────────────────────────────┘
```

## Empty State (no schedules)

```
┌────────────────────────────────────────────┐
│  Block                                     │
├────────────────────────────────────────────┤
│                                            │
│          📵                                │
│   No blocking schedules yet.               │
│   Tap + to add one.                        │
│                                            │
│                          [+ Add Schedule]  │
└────────────────────────────────────────────┘
```

## Add Schedule Bottom Sheet

```
┌────────────────────────────────────────────┐
│  New Schedule                        [✕]   │
├────────────────────────────────────────────┤
│  Apps                                      │
│  [ ] Instagram  [ ] TikTok  [ ] YouTube    │
│  [ ] Facebook   [ ] X       [ ] LinkedIn   │
│                                            │
│  Block type                                │
│  ( ) All day                               │
│  (●) Time window                           │
│      From: [ 9:00 PM ]  To: [ 11:00 PM ]  │
│                                            │
│  Repeat                                    │
│  [Mo] [Tu] [We] [Th] [Fr] [Sa] [Su]       │
│  (tap to toggle; none selected = one-time) │
│                                            │
│  [        Save Schedule        ]           │
└────────────────────────────────────────────┘
```

## Block Overlay (drawn above blocked app)

```
┌────────────────────────────────────────────┐
│                                            │
│                                            │
│              📵                            │
│                                            │
│     Instagram is blocked right now.        │
│                                            │
│   You're doing great — stay off Instagram! │
│                                            │
│                                            │
│         [ ← Go Back ]                      │
│                                            │
└────────────────────────────────────────────┘
```
