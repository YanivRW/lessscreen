# Test Design: BlockService
**Source:** crc-BlockService.md

## Test: blocked app triggers overlay
**Purpose:** Verify overlay is shown when foreground app has an active schedule
**Input:** TYPE_WINDOW_STATE_CHANGED event with packageName = "com.instagram.android";
  active schedule exists for Instagram covering current time
**Expected:** BlockOverlay.show("Instagram") called once
**Refs:** crc-BlockService.md, crc-BlockOverlay.md, seq-block-detection.md#1.3

## Test: unblocked app hides overlay
**Purpose:** Verify overlay is dismissed when foreground app has no active schedule
**Input:** TYPE_WINDOW_STATE_CHANGED event with packageName = "com.android.chrome";
  no blocking schedule for Chrome; overlay currently visible
**Expected:** BlockOverlay.hide() called once
**Refs:** crc-BlockService.md, crc-BlockOverlay.md, seq-block-detection.md#1.4

## Test: disabled schedule does not block
**Purpose:** Verify a schedule with isEnabled=false does not trigger overlay
**Input:** Foreground app = Instagram; schedule exists but isEnabled=false
**Expected:** BlockOverlay.show() never called
**Refs:** crc-BlockService.md, crc-BlockSchedule.md

## Test: schedule outside time window does not block
**Purpose:** Verify time-window schedule respects current time
**Input:** Foreground app = Instagram; schedule window is 9 PM–11 PM; current time is 3 PM
**Expected:** BlockOverlay.show() never called
**Refs:** crc-BlockService.md, crc-BlockSchedule.md, seq-block-detection.md#2.2.1

## Test: service reloads schedules on StateFlow update
**Purpose:** Verify BlockService picks up new schedules without restarting
**Input:** BlockRepository emits a new schedule after service is connected
**Expected:** activeSchedules updated; subsequent foreground events use new schedule
**Refs:** crc-BlockService.md, crc-BlockRepository.md, seq-block-detection.md#1.2
