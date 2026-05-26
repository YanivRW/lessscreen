# BlockViewModel
**Requirements:** R18, R19, R20, R21, R22, R24

ViewModel for the Block screen. Exposes schedule state to the UI and routes
mutations through BlockRepository.

## Knows
- blockRepository: BlockRepository
- uiState: StateFlow<BlockUiState> (schedules grouped by app, loading flag, error)

## Does
- loadSchedules() — triggers BlockRepository.loadSchedules(); updates uiState
- addSchedule(draft: BlockScheduleDraft) — validates draft, calls
  BlockRepository.saveSchedule()
- toggleSchedule(id: String, enabled: Boolean) — calls saveSchedule with flipped
  isEnabled
- deleteSchedule(id: String) — calls BlockRepository.deleteSchedule()

## Collaborators
- BlockRepository: all data operations
- BlockScreen (Composable): observes uiState

## Sequences
- seq-schedule-sync.md
