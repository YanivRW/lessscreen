# Sequence: Schedule Sync

Covers R7, R9, R21, R22, R23.

## Diagram 1 — Save schedule (online)

```
BlockScreen      BlockViewModel       BlockRepository      Supabase
     |                |                    |                  |
1.   | user saves draft schedule           |                  |
1.1  |--addSchedule(draft)-->|             |                  |
1.2  |                       |--saveSchedule(schedule)-->|    |
1.3  |                       |             |--upsert---------->|
1.4  |                       |             |<--success---------|
1.5  |                       |             | update cache      |
1.6  |                       |             | emit StateFlow    |
1.7  |                       |<--updated uiState------------|  |
1.8  |<--recompose-----------|             |                  |
1.9  | BlockService observes StateFlow     |                  |
1.10 | reloads activeSchedules             |                  |
```

## Diagram 2 — Save schedule (offline queue)

```
BlockScreen      BlockViewModel       BlockRepository      SharedPreferences   Supabase
     |                |                    |                     |               |
2.   | user saves draft schedule           |                     |               |
2.1  |--addSchedule(draft)-->|             |                     |               |
2.2  |                       |--saveSchedule(schedule)-->|       |               |
2.3  |                       |             |--upsert attempt-------------------------->|
2.4  |                       |             |<--IOException/timeout---------------------|
2.5  |                       |             |--enqueue PendingOp-->|               |
2.6  |                       |             | update cache locally  |               |
2.7  |                       |             | emit StateFlow        |               |
2.8  |                       |<--updated uiState              |               |
2.9  |<--recompose-----------|             |                     |               |
     |                       |             |                     |               |
2.10 | connectivity restored |             |                     |               |
2.11 |                       |             |--flushQueue()------->|               |
2.12 |                       |             |<--pending ops--------|               |
2.13 |                       |             |--upsert/delete each------------------------->|
2.14 |                       |             |<--success each------------------------------|
2.15 |                       |             |--clearQueue()------->|               |
```

## Diagram 3 — Toggle / delete schedule

```
BlockScreen      BlockViewModel       BlockRepository      Supabase
     |                |                    |                  |
3.   | user toggles schedule               |                  |
3.1  |--toggleSchedule(id, enabled)-->|   |                  |
3.2  |                  (follows Diagram 1 from step 1.2)    |
     |                |                    |                  |
3.3  | user deletes schedule               |                  |
3.4  |--deleteSchedule(id)-->|             |                  |
3.5  |                       |--deleteSchedule(id)------>|    |
3.6  |                       |             |--delete---------->|
3.7  |                       |             |<--success---------|
3.8  |                       |             | update cache      |
3.9  |                       |             | emit StateFlow    |
3.10 |<--recompose-----------|             |                  |
```
