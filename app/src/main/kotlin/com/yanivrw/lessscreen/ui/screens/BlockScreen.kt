package com.yanivrw.lessscreen.ui.screens

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yanivrw.lessscreen.data.AuthRepository
import com.yanivrw.lessscreen.data.BlockRepository
import com.yanivrw.lessscreen.data.FriendsRepository
import com.yanivrw.lessscreen.data.LockRepository
import com.yanivrw.lessscreen.data.TRACKED_PACKAGES
import com.yanivrw.lessscreen.data.models.BlockSchedule
import com.yanivrw.lessscreen.data.models.Profile
import com.yanivrw.lessscreen.permission.BlockPermission
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

// CRC: crc-BlockViewModel.md | Seq: seq-schedule-sync.md, seq-friend-lock-setup.md | R18, R19, R20, R21, R22, R24, R41
class BlockViewModel : ViewModel() {

    data class UiState(
        val schedulesByApp: Map<String, List<BlockSchedule>> = emptyMap(),
        val isLoading: Boolean = false,
    )

    data class Draft(
        val selectedPackages: Set<String> = emptySet(),
        val isAllDay: Boolean = true,
        val startHour: Int = 21,
        val startMinute: Int = 0,
        val endHour: Int = 23,
        val endMinute: Int = 0,
        val recurrenceDays: Set<Int> = (1..7).toSet(),
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _friends = MutableStateFlow<List<Profile>>(emptyList())
    val friends: StateFlow<List<Profile>> = _friends.asStateFlow()

    init {
        viewModelScope.launch {
            BlockRepository.schedules.collect { schedules ->
                _uiState.update { it.copy(schedulesByApp = schedules.groupBy { s -> s.packageName }) }
            }
        }
        loadSchedules()
        loadFriends()
    }

    fun loadSchedules() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            runCatching { BlockRepository.loadSchedules() }
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun loadFriends() {
        viewModelScope.launch { runCatching { _friends.value = FriendsRepository.listFriends() } }
    }

    fun addSchedule(draft: Draft) {
        viewModelScope.launch {
            val userId = AuthRepository.currentUserId() ?: return@launch
            for (pkg in draft.selectedPackages) {
                runCatching {
                    BlockRepository.saveSchedule(
                        BlockSchedule(
                            id = UUID.randomUUID().toString(),
                            userId = userId,
                            packageName = pkg,
                            isAllDay = draft.isAllDay,
                            startTime = if (draft.isAllDay) null else "%02d:%02d".format(draft.startHour, draft.startMinute),
                            endTime = if (draft.isAllDay) null else "%02d:%02d".format(draft.endHour, draft.endMinute),
                            recurrenceDays = draft.recurrenceDays.sorted(),
                        )
                    )
                }
            }
        }
    }

    fun toggleSchedule(id: String, enabled: Boolean) {
        viewModelScope.launch {
            val s = BlockRepository.schedules.value.find { it.id == id } ?: return@launch
            runCatching { BlockRepository.saveSchedule(s.copy(isEnabled = enabled)) }
        }
    }

    fun deleteSchedule(id: String) {
        viewModelScope.launch { runCatching { BlockRepository.deleteSchedule(id) } }
    }

    // Seq: seq-friend-lock-setup.md#1.4 | R41
    fun addFriendLock(scheduleId: String, friendUserId: String) {
        viewModelScope.launch {
            runCatching {
                LockRepository.setLockPartner(scheduleId, friendUserId)
                BlockRepository.loadSchedules()
            }
        }
    }

    // Seq: seq-friend-lock-setup.md#3.2 | R41
    fun removeFriendLock(scheduleId: String) {
        viewModelScope.launch {
            runCatching {
                LockRepository.removeLock(scheduleId)
                BlockRepository.loadSchedules()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlockScreen(vm: BlockViewModel = viewModel()) {
    val uiState by vm.uiState.collectAsState()
    val friends by vm.friends.collectAsState()
    val context = LocalContext.current
    var showAdd by remember { mutableStateOf(false) }
    var hasOverlay by remember { mutableStateOf(BlockPermission.hasOverlayPermission(context)) }
    var hasAccessibility by remember { mutableStateOf(BlockPermission.hasAccessibilityEnabled(context)) }
    var pendingLockScheduleId by remember { mutableStateOf<String?>(null) }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        hasOverlay = BlockPermission.hasOverlayPermission(context)
        hasAccessibility = BlockPermission.hasAccessibilityEnabled(context)
    }

    Scaffold(
        containerColor = Color(0xFF0E0E10),
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAdd = true },
                containerColor = Color(0xFF2C2C2E),
            ) { Text("+", color = Color.White, fontSize = 24.sp) }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            if (!hasOverlay || !hasAccessibility) {
                PermissionBanner(hasOverlay, hasAccessibility, context)
            }
            when {
                uiState.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color.White)
                }
                uiState.schedulesByApp.isEmpty() -> EmptyState()
                else -> ScheduleList(
                    schedulesByApp = uiState.schedulesByApp,
                    onToggle = vm::toggleSchedule,
                    onDelete = vm::deleteSchedule,
                    onAddLock = { scheduleId -> pendingLockScheduleId = scheduleId },
                    onRemoveLock = vm::removeFriendLock,
                )
            }
        }
    }

    if (showAdd) {
        AddScheduleSheet(
            onDismiss = { showAdd = false },
            onSave = { draft ->
                vm.addSchedule(draft)
                showAdd = false
            },
        )
    }

    if (pendingLockScheduleId != null) {
        FriendPickerDialog(
            friends = friends,
            onSelect = { friendId ->
                vm.addFriendLock(pendingLockScheduleId!!, friendId)
                pendingLockScheduleId = null
            },
            onDismiss = { pendingLockScheduleId = null },
        )
    }
}

@Composable
private fun PermissionBanner(hasOverlay: Boolean, hasAccessibility: Boolean, context: Context) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF3A2A00)),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("⚠ Blocking requires two permissions", color = Color(0xFFFFCC00), fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (!hasOverlay) {
                    Button(
                        onClick = { BlockPermission.openOverlaySettings(context) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2C2C2E)),
                    ) { Text("Enable Overlay", fontSize = 12.sp) }
                }
                if (!hasAccessibility) {
                    Button(
                        onClick = { BlockPermission.openAccessibilitySettings(context) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2C2C2E)),
                    ) { Text("Enable Accessibility", fontSize = 12.sp) }
                }
            }
        }
    }
}

@Composable
private fun EmptyState() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("📵", fontSize = 48.sp)
            Text("No blocking schedules yet.", color = Color(0xFF888888))
            Text("Tap + to add one.", color = Color(0xFF888888))
        }
    }
}

@Composable
private fun ScheduleList(
    schedulesByApp: Map<String, List<BlockSchedule>>,
    onToggle: (String, Boolean) -> Unit,
    onDelete: (String) -> Unit,
    onAddLock: (String) -> Unit,
    onRemoveLock: (String) -> Unit,
) {
    val appLabels = TRACKED_PACKAGES.toMap()
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        schedulesByApp.forEach { (pkg, schedules) ->
            item {
                Text(
                    appLabels[pkg] ?: pkg,
                    color = Color(0xFF888888),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            items(schedules, key = { it.id }) { schedule ->
                ScheduleCard(schedule, onToggle, onDelete, onAddLock, onRemoveLock)
            }
        }
    }
}

@Composable
private fun ScheduleCard(
    schedule: BlockSchedule,
    onToggle: (String, Boolean) -> Unit,
    onDelete: (String) -> Unit,
    onAddLock: (String) -> Unit,
    onRemoveLock: (String) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1C1F)),
    ) {
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    if (schedule.isAllDay) "All day"
                    else "${formatTime(schedule.startTime)} – ${formatTime(schedule.endTime)}",
                    color = Color.White,
                    fontWeight = FontWeight.Medium,
                )
                Text(formatDays(schedule.recurrenceDays), color = Color(0xFF888888), fontSize = 13.sp)
                if (schedule.isFriendLocked) {
                    Text("🔒 Friend locked", color = Color(0xFF6BB8FF), fontSize = 12.sp)
                }
            }
            Switch(
                checked = schedule.isEnabled,
                onCheckedChange = { onToggle(schedule.id, it) },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = Color(0xFF30D158),
                ),
            )
            IconButton(onClick = {
                if (schedule.isFriendLocked) onRemoveLock(schedule.id) else onAddLock(schedule.id)
            }) {
                Text(if (schedule.isFriendLocked) "🔓" else "🔒", fontSize = 18.sp)
            }
            IconButton(onClick = { onDelete(schedule.id) }) {
                Text("🗑", fontSize = 18.sp)
            }
        }
    }
}

private fun formatTime(time: String?): String {
    if (time == null) return ""
    val parts = time.split(":")
    val h = parts.getOrNull(0)?.toIntOrNull() ?: return time
    val m = parts.getOrNull(1)?.toIntOrNull() ?: 0
    val ampm = if (h < 12) "AM" else "PM"
    val h12 = when {
        h == 0 -> 12
        h > 12 -> h - 12
        else -> h
    }
    return "%d:%02d %s".format(h12, m, ampm)
}

private fun formatDays(days: List<Int>): String {
    if (days.isEmpty() || days.size == 7) return "Every day"
    val names = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    return days.sorted().joinToString(", ") { names.getOrElse(it - 1) { "$it" } }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun AddScheduleSheet(onDismiss: () -> Unit, onSave: (BlockViewModel.Draft) -> Unit) {
    var draft by remember { mutableStateOf(BlockViewModel.Draft()) }
    var showStartPicker by remember { mutableStateOf(false) }
    var showEndPicker by remember { mutableStateOf(false) }
    val startState = rememberTimePickerState(initialHour = draft.startHour, initialMinute = draft.startMinute, is24Hour = false)
    val endState = rememberTimePickerState(initialHour = draft.endHour, initialMinute = draft.endMinute, is24Hour = false)

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Color(0xFF1C1C1F)) {
        Column(
            Modifier.padding(horizontal = 16.dp).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("New Schedule", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)

            Text("Apps", color = Color(0xFF888888), fontSize = 13.sp)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                TRACKED_PACKAGES.forEach { (pkg, label) ->
                    val sel = pkg in draft.selectedPackages
                    FilterChip(
                        selected = sel,
                        onClick = { draft = draft.copy(selectedPackages = if (sel) draft.selectedPackages - pkg else draft.selectedPackages + pkg) },
                        label = { Text(label, fontSize = 12.sp) },
                        colors = chipColors(),
                    )
                }
            }

            Text("Block type", color = Color(0xFF888888), fontSize = 13.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = draft.isAllDay, onClick = { draft = draft.copy(isAllDay = true) }, label = { Text("All day") }, colors = chipColors())
                FilterChip(selected = !draft.isAllDay, onClick = { draft = draft.copy(isAllDay = false) }, label = { Text("Time window") }, colors = chipColors())
            }

            if (!draft.isAllDay) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(
                        onClick = { showStartPicker = true },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    ) { Text("From: ${formatTime("%02d:%02d".format(draft.startHour, draft.startMinute))}") }
                    OutlinedButton(
                        onClick = { showEndPicker = true },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    ) { Text("To: ${formatTime("%02d:%02d".format(draft.endHour, draft.endMinute))}") }
                }
            }

            Text("Repeat", color = Color(0xFF888888), fontSize = 13.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("Mo", "Tu", "We", "Th", "Fr", "Sa", "Su").forEachIndexed { idx, label ->
                    val day = idx + 1
                    val sel = day in draft.recurrenceDays
                    FilterChip(
                        selected = sel,
                        onClick = { draft = draft.copy(recurrenceDays = if (sel) draft.recurrenceDays - day else draft.recurrenceDays + day) },
                        label = { Text(label, fontSize = 11.sp) },
                        colors = chipColors(),
                    )
                }
            }

            Button(
                onClick = { if (draft.selectedPackages.isNotEmpty()) onSave(draft) },
                modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp),
                enabled = draft.selectedPackages.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF30D158)),
            ) { Text("Save Schedule", color = Color.White) }
        }
    }

    if (showStartPicker) {
        PickerDialog(startState, onDismiss = { showStartPicker = false }) {
            draft = draft.copy(startHour = startState.hour, startMinute = startState.minute)
            showStartPicker = false
        }
    }
    if (showEndPicker) {
        PickerDialog(endState, onDismiss = { showEndPicker = false }) {
            draft = draft.copy(endHour = endState.hour, endMinute = endState.minute)
            showEndPicker = false
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PickerDialog(state: TimePickerState, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onConfirm) { Text("OK") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        text = { TimePicker(state = state) },
        containerColor = Color(0xFF1C1C1F),
    )
}

// Seq: seq-friend-lock-setup.md#1.2 | R41
@Composable
private fun FriendPickerDialog(
    friends: List<Profile>,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Lock with a friend", color = Color.White, fontWeight = FontWeight.Bold) },
        text = {
            if (friends.isEmpty()) {
                Text("Add friends first to use the friend-lock feature.", color = Color(0xFF888888))
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    friends.forEach { f ->
                        TextButton(
                            onClick = { onSelect(f.id) },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(f.displayName ?: f.email, color = Color.White)
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        containerColor = Color(0xFF1C1C1F),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun chipColors() = FilterChipDefaults.filterChipColors(
    selectedContainerColor = Color(0xFF30D158),
    selectedLabelColor = Color.White,
    containerColor = Color(0xFF2C2C2E),
    labelColor = Color(0xFF888888),
)
