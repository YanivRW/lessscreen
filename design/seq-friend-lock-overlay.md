# Sequence: Friend-Lock Overlay

Covers R30, R31, R32, R33, R34, R36, R38, R42.

## Diagram 1 — Locked overlay: PIN entry and unlock

```
BlockService      BlockRepository   LockRepository    BlockOverlay      Supabase
     |                  |                |                 |                |
1.1  | TYPE_WINDOW_STATE_CHANGED (blocked app)             |                |
1.2  | schedule.isActiveNow() = true      |                |                |
1.3  | schedule has locked_by_user_id     |                |                |
1.4  | check unlocked_until: null (locked)|                |                |
1.5  |------------------------------------------------showLocked(label,    |
1.6  |                                               lockPartnerName)       |
1.7  | subscribe Realtime for unlocked_until changes       |                |
1.8  |------------subscribeUnlockState(scheduleId,cb)-->   |                |
     |                                                     |                |
1.9  | user enters 6-digit PIN on overlay                  |                |
1.10 |------------verifyPinAndUnlock(id,pin,mins)------>   |                |
1.11 |                  |                |--RPC verify_lock_pin------------>|
1.12 |                  |                |  (hash+compare, set unlocked_until)
1.13 |                  |                |<--{matched: true}----------------|
1.14 |                  |                | update local schedule cache      |
1.15 |<--true-----------|                |                 |                |
1.16 | show duration picker on overlay   |                 |                |
1.17 | user/friend selects duration      |                 |                |
1.18 |------------------------------------------hide()    |                |
     |                                                     |                |
1.19 | wrong PIN entered:                                  |                |
1.20 |------------verifyPinAndUnlock(id,pin,mins)------>   |                |
1.21 |                  |                |--RPC verify_lock_pin------------>|
1.22 |                  |                |<--{matched: false}---------------|
1.23 |<--false----------|                |                 |                |
1.24 | overlay shows "Incorrect code"    |                 |                |
```

## Diagram 2 — Lock partner re-locks remotely; Realtime propagation

```
FriendsScreen (partner)  LockRepository              Supabase        BlockService (locked user)
          |                    |                         |                     |
2.1       | partner taps "Re-lock"                       |                     |
2.2       |---relock(scheduleId)->|                      |                     |
2.3       |                   |--UPDATE blocking_schedules SET unlocked_until=null-->|
2.4       |                   |<--ok---------------------|                     |
2.5       |                   |                          |--Realtime event----->|
2.6       |                   |                          |  (unlocked_until changed)
2.7       |                   |<--subscribeUnlockState callback fires-----------|
2.8       |                   | update cache, StateFlow emits                   |
2.9       |                   |                          |  BlockService checks |
2.10      |                   |                          |  next foreground event
2.11      |                   |                          |  overlay returns     |
```

## Diagram 3 — Timed unlock expiry

```
BlockService           BlockRepository            BlockOverlay
     |                       |                         |
3.1  | TYPE_WINDOW_STATE_CHANGED (blocked app returns) |
3.2  | schedule.isActiveNow() = true                   |
3.3  | check unlocked_until: timestamp in PAST         |
3.4  |-----------------------------------------------showLocked(label, name)
3.5  | overlay shown again — timed window expired      |
```
