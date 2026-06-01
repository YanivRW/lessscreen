# Sequence: Friend-Lock Setup

Covers R25, R26, R27, R28, R29, R35, R36, R37, R40, R41.

## Diagram 1 — Locked user assigns a friend lock

```
BlockScreen           BlockRepository       LockRepository        Supabase
     |                      |                     |                   |
1.1  | user taps "Add friend lock" on schedule     |                   |
1.2  | friend picker shown (from FriendsRepository)|                   |
1.3  | user selects a friend |                     |                   |
1.4  |                       |                     |--setLockPartner()->|
1.5  |                       |                     |  UPDATE blocking_schedules
1.6  |                       |                     |  SET locked_by_user_id=friendId
1.7  |                       |                     |<--ok--------------|
1.8  | schedule shows lock badge (pending)         |                   |
1.9  | BlockRepository.loadSchedules() sees locked_by_user_id set      |
```

## Diagram 2 — Lock partner accepts and sets PIN

```
FriendsScreen         LockRepository                            Supabase
     |                      |                                       |
2.1  | load "Locks I manage"  |                                     |
2.2  |---loadManagedLocks()-->|                                     |
2.3  |                       |--SELECT blocking_schedules WHERE---->|
2.4  |                       |   locked_by_user_id = me             |
2.5  |<--List<LockInfo>-------|                                     |
2.6  | shows pending card: "[User] wants you to lock [App]"         |
2.7  | partner taps card, enters 6-digit PIN                        |
2.8  |---setPin(scheduleId, rawPin)-->|                             |
2.9  |                       |--RPC set_lock_pin(sid, pin)--------->|
2.10 |                       |  (security definer: hash+store)      |
2.11 |                       |<--ok----------------------------------|
2.12 | card updates to "Active lock"                                 |
```

## Diagram 3 — Lock partner removes a lock

```
FriendsScreen         LockRepository                            Supabase
     |                      |                                       |
3.1  | partner taps "Remove lock"  |                                |
3.2  |---removeLock(scheduleId)--->|                                |
3.3  |                       |--RPC remove_lock(scheduleId)-------->|
3.4  |                       |  (clears locked_by_user_id,          |
3.5  |                       |   deletes block_lock_pins row)       |
3.6  |                       |<--ok----------------------------------|
3.7  | card removed from list |                                     |
```
