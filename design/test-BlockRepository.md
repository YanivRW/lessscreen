# Test Design: BlockRepository
**Source:** crc-BlockRepository.md

## Test: load schedules returns all user schedules
**Purpose:** Verify loadSchedules fetches only the current user's schedules from Supabase
**Input:** Supabase contains 2 schedules for user A and 1 for user B; current user is A
**Expected:** Returns exactly 2 schedules belonging to user A
**Refs:** crc-BlockRepository.md, seq-schedule-sync.md

## Test: save schedule — online upserts to Supabase and updates cache
**Purpose:** Verify saveSchedule reaches Supabase and updates the in-memory cache
**Input:** A valid BlockSchedule draft; Supabase available
**Expected:** Supabase upsert called once; cache contains the new schedule; StateFlow emits updated list
**Refs:** crc-BlockRepository.md, seq-schedule-sync.md#1.3

## Test: save schedule — offline enqueues and updates cache locally
**Purpose:** Verify offline writes don't lose data
**Input:** A valid BlockSchedule draft; Supabase unreachable (IOException)
**Expected:** PendingOp written to SharedPreferences; cache updated locally; StateFlow emits
**Refs:** crc-BlockRepository.md, seq-schedule-sync.md#2.5

## Test: flushQueue — replays queued ops on reconnect
**Purpose:** Verify pending ops reach Supabase when connectivity is restored
**Input:** 2 pending ops in SharedPreferences; Supabase now available
**Expected:** Both ops sent to Supabase; queue cleared from SharedPreferences
**Refs:** crc-BlockRepository.md, seq-schedule-sync.md#2.13

## Test: delete schedule removes from Supabase and cache
**Purpose:** Verify deleteSchedule removes the record and emits updated list
**Input:** Existing schedule id; Supabase available
**Expected:** Supabase delete called; cache no longer contains the schedule; StateFlow emits
**Refs:** crc-BlockRepository.md, seq-schedule-sync.md#3.6
