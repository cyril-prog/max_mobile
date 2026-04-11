package com.max.aiassistant.ui.tasks

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.max.aiassistant.model.Event
import com.max.aiassistant.model.Note
import com.max.aiassistant.model.SubTask
import com.max.aiassistant.model.Task
import com.max.aiassistant.model.TaskPriority
import com.max.aiassistant.model.TaskStatus
import com.max.aiassistant.ui.theme.AccentBlue
import com.max.aiassistant.ui.common.BannerTone
import com.max.aiassistant.ui.common.EmptyStateView
import com.max.aiassistant.ui.common.ErrorStateView
import com.max.aiassistant.ui.common.InlineStatusBanner
import com.max.aiassistant.ui.common.LoadingStateView
import com.max.aiassistant.ui.common.NavigationScreen
import com.max.aiassistant.ui.common.NavigationSidebarScaffold
import com.max.aiassistant.ui.common.rememberNavigationSidebarState
import com.max.aiassistant.ui.notes.NotesScreen
import com.max.aiassistant.ui.planning.PlanningScreen
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

private val Ink = Color(0xFF0A1018)
private val OrganiserBackdrop = Brush.verticalGradient(
    listOf(
        Color(0xFF0B1420),
        Color(0xFF101927),
        Color(0xFF0D1520),
        Color(0xFF091019)
    )
)
private val Panel = Color(0xFF121827)
private val PanelSoft = Color(0xFF192235)
private val Blue = Color(0xFF77AFFF)
private val Mint = Color(0xFF78E4BC)
private val Gold = Color(0xFFFFC36A)
private val Rose = Color(0xFFFF728D)
private val Dim = Color(0xFF97A3BC)
private val Lilac = Color(0xFFC8A8FF)

private enum class DeckTab { FOCUS, PIPELINE, DONE }
enum class OrgaMode { TASK, PLANNING, NOTE }
private enum class TaskSheetTab { TASK, PLANNING, NOTE }

private fun OrgaMode.toSheetTab(): TaskSheetTab = when (this) {
    OrgaMode.TASK -> TaskSheetTab.TASK
    OrgaMode.PLANNING -> TaskSheetTab.PLANNING
    OrgaMode.NOTE -> TaskSheetTab.NOTE
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasksScreen(
    tasks: List<Task>,
    events: List<Event>,
    notes: List<Note>,
    isRefreshing: Boolean,
    isEventsRefreshing: Boolean,
    errorMessage: String? = null,
    eventsErrorMessage: String? = null,
    isOffline: Boolean = false,
    onRefresh: () -> Unit,
    onRefreshPlanning: () -> Unit,
    onTaskStatusChange: (String, TaskStatus) -> Unit,
    onTaskPriorityChange: (String, TaskPriority) -> Unit,
    onTaskDurationChange: (String, String) -> Unit,
    onTaskDeadlineChange: (String, String) -> Unit,
    onTaskCategoryChange: (String, String) -> Unit,
    onTaskTitleChange: (String, String) -> Unit,
    onTaskDescriptionChange: (String, String) -> Unit,
    onTaskNoteChange: (String, String) -> Unit,
    onTaskDelete: (String) -> Unit,
    onTaskCreate: (String, String, String, TaskPriority, String, String) -> Unit,
    onAddNote: (String, String) -> Unit,
    onUpdateNote: (String, String, String) -> Unit,
    onDeleteNote: (String) -> Unit,
    onSubTaskCreate: (String, String) -> Unit,
    onSubTaskUpdate: (String, String, Boolean) -> Unit,
    onSubTaskDelete: (String) -> Unit,
    onTaskScheduleRequested: (Task) -> Unit,
    onTaskCreateNoteRequested: (Task) -> Unit,
    initialMode: OrgaMode = OrgaMode.TASK,
    onModeChange: (OrgaMode) -> Unit = {},
    onNavigateToHome: () -> Unit,
    onNavigateToChat: () -> Unit = {},
    onNavigateToPlanning: () -> Unit = {},
    onNavigateToWeather: () -> Unit = {},
    onNavigateToNotes: () -> Unit = {},
    onNavigateToActu: () -> Unit = {},
    showChrome: Boolean = true,
    modifier: Modifier = Modifier
) {
    val content: @Composable () -> Unit = {
        TasksContent(
            tasks, events, notes, isRefreshing, isEventsRefreshing, errorMessage, eventsErrorMessage, isOffline, onRefresh, onRefreshPlanning,
            onTaskStatusChange, onTaskPriorityChange, onTaskDurationChange, onTaskDeadlineChange,
            onTaskCategoryChange, onTaskTitleChange, onTaskDescriptionChange, onTaskNoteChange,
            onTaskDelete, onTaskCreate, onAddNote, onUpdateNote, onDeleteNote, onSubTaskCreate, onSubTaskUpdate, onSubTaskDelete,
            onTaskScheduleRequested, onTaskCreateNoteRequested,
            initialMode, onModeChange,
            showChrome, modifier
        )
    }
    if (showChrome) {
        val sidebarState = rememberNavigationSidebarState()
        NavigationSidebarScaffold(
            currentScreen = NavigationScreen.TASKS,
            onNavigateToScreen = { screen -> when (screen) {
                NavigationScreen.HOME -> onNavigateToHome()
                NavigationScreen.VOICE -> onNavigateToHome()
                NavigationScreen.CHAT -> onNavigateToChat()
                NavigationScreen.TASKS -> Unit
                NavigationScreen.PLANNING -> onNavigateToPlanning()
                NavigationScreen.WEATHER -> onNavigateToWeather()
                NavigationScreen.NOTES -> onNavigateToNotes()
                NavigationScreen.ACTU -> onNavigateToActu()
            } },
            sidebarState = sidebarState,
            content = content
        )
    } else content()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TasksContent(
    tasks: List<Task>,
    events: List<Event>,
    notes: List<Note>,
    isRefreshing: Boolean,
    isEventsRefreshing: Boolean,
    errorMessage: String?,
    eventsErrorMessage: String?,
    isOffline: Boolean,
    onRefresh: () -> Unit,
    onRefreshPlanning: () -> Unit,
    onTaskStatusChange: (String, TaskStatus) -> Unit,
    onTaskPriorityChange: (String, TaskPriority) -> Unit,
    onTaskDurationChange: (String, String) -> Unit,
    onTaskDeadlineChange: (String, String) -> Unit,
    onTaskCategoryChange: (String, String) -> Unit,
    onTaskTitleChange: (String, String) -> Unit,
    onTaskDescriptionChange: (String, String) -> Unit,
    onTaskNoteChange: (String, String) -> Unit,
    onTaskDelete: (String) -> Unit,
    onTaskCreate: (String, String, String, TaskPriority, String, String) -> Unit,
    onAddNote: (String, String) -> Unit,
    onUpdateNote: (String, String, String) -> Unit,
    onDeleteNote: (String) -> Unit,
    onSubTaskCreate: (String, String) -> Unit,
    onSubTaskUpdate: (String, String, Boolean) -> Unit,
    onSubTaskDelete: (String) -> Unit,
    onTaskScheduleRequested: (Task) -> Unit,
    onTaskCreateNoteRequested: (Task) -> Unit,
    initialMode: OrgaMode,
    onModeChange: (OrgaMode) -> Unit,
    showChrome: Boolean,
    modifier: Modifier
) {
    var tab by remember { mutableStateOf(DeckTab.FOCUS) }
    var mode by remember(initialMode) { mutableStateOf(initialMode) }
    var selectedTaskId by remember { mutableStateOf<String?>(null) }
    var selectedSheetTab by remember { mutableStateOf(TaskSheetTab.TASK) }
    var showCreate by remember { mutableStateOf(false) }
    val selectedTask = tasks.firstOrNull { it.id == selectedTaskId }
    val focus = remember(tasks) { tasks.filter { it.status == TaskStatus.IN_PROGRESS || it.deadlineDate == todayIso() || overdue(it) } }
    val pipeline = remember(tasks) { tasks.filter { it.status != TaskStatus.COMPLETED && it !in focus } }
    val done = remember(tasks) { tasks.filter { it.status == TaskStatus.COMPLETED } }
    val visible = when (tab) { DeckTab.FOCUS -> focus; DeckTab.PIPELINE -> pipeline; DeckTab.DONE -> done }
    val categories = remember(tasks) { tasks.map { it.category }.filter { it.isNotBlank() }.distinct().sorted() }
    LaunchedEffect(initialMode) {
        mode = initialMode
    }
    Box(modifier = modifier.fillMaxSize().background(OrganiserBackdrop)) {
        Column(Modifier.fillMaxSize()) {
            if (showChrome) Header("ORGANISER", "Execution")
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 20.dp)
                    .padding(top = 8.dp)
            ) {
                OrgaModeSwitcher(mode = mode, onSelect = {
                    mode = it
                    onModeChange(it)
                })
                Spacer(Modifier.height(16.dp))
                when (mode) {
                    OrgaMode.TASK -> PullToRefreshBox(isRefreshing = isRefreshing, onRefresh = onRefresh, modifier = Modifier.weight(1f)) {
                        when {
                            isRefreshing && tasks.isEmpty() -> LoadingStateView("Chargement des taches", "Le tableau se prepare.", Modifier.fillMaxSize())
                            errorMessage != null && tasks.isEmpty() -> ErrorStateView("Impossible de charger les taches", errorMessage, onRefresh, Modifier.fillMaxSize())
                            else -> LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(bottom = 120.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                if (errorMessage != null && tasks.isNotEmpty()) item {
                                    InlineStatusBanner("Sync en retrait", errorMessage, BannerTone.Error)
                                } else if (isOffline) item {
                                    InlineStatusBanner("Mode local", "Les actions repartiront a la reconnexion.", BannerTone.Offline)
                                }
                                if (visible.isEmpty()) item {
                                    EmptyStateView(Icons.Default.CheckCircle, Blue, when (tab) {
                                        DeckTab.FOCUS -> "Aucune action urgente"
                                        DeckTab.PIPELINE -> "Pipeline vide"
                                        DeckTab.DONE -> "Rien de termine"
                                    }, "La zone reste propre et compacte tant qu'il n'y a rien a montrer.")
                                }
                                items(visible, key = { it.id }) { task ->
                                    TaskCard(
                                        task = task,
                                        mode = mode,
                                        onClick = {
                                            selectedSheetTab = mode.toSheetTab()
                                            selectedTaskId = task.id
                                        },
                                        onToggle = { onTaskStatusChange(task.id, if (task.status == TaskStatus.COMPLETED) TaskStatus.TODO else TaskStatus.COMPLETED) },
                                        onAdvance = { onTaskStatusChange(task.id, when (task.status) {
                                            TaskStatus.TODO -> TaskStatus.IN_PROGRESS
                                            TaskStatus.IN_PROGRESS -> TaskStatus.COMPLETED
                                            TaskStatus.COMPLETED -> TaskStatus.COMPLETED
                                        }) },
                                        onSchedule = { onTaskScheduleRequested(task) },
                                        onCreateNote = { onTaskCreateNoteRequested(task) }
                                    )
                                }
                            }
                        }
                    }
                    OrgaMode.PLANNING -> Box(Modifier.weight(1f)) {
                        PlanningScreen(
                            events = events,
                            isRefreshing = isEventsRefreshing,
                            errorMessage = eventsErrorMessage,
                            isOffline = isOffline,
                            onRefresh = onRefreshPlanning,
                            onNavigateToHome = {},
                            showChrome = false,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    OrgaMode.NOTE -> Box(Modifier.weight(1f)) {
                        NotesScreen(
                            notes = notes,
                            onAddNote = onAddNote,
                            onUpdateNote = onUpdateNote,
                            onDeleteNote = onDeleteNote,
                            onNavigateBack = {},
                            showChrome = false,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }
        if (mode == OrgaMode.TASK) {
            FloatingActionButton(
                onClick = { showCreate = true },
                containerColor = Blue,
                contentColor = Color(0xFF081120),
                modifier = Modifier.align(Alignment.BottomEnd).padding(end = 20.dp, bottom = 24.dp)
            ) { Icon(Icons.Default.Add, contentDescription = "Nouvelle tache") }
        }
    }
    if (showCreate) CreateSheet(categories, { showCreate = false }) { a, b, c, d, e, f -> onTaskCreate(a, b, c, d, e, f); showCreate = false }
    selectedTask?.let { task ->
        TaskSheet(
            task = task,
            initialTab = selectedSheetTab,
            categories = categories,
            onDismiss = { selectedTaskId = null },
            onDelete = { onTaskDelete(task.id); selectedTaskId = null },
            onSave = { title, desc, note, category, date, duration, priority, status ->
                if (title != task.title) onTaskTitleChange(task.id, title)
                if (desc != task.description) onTaskDescriptionChange(task.id, desc)
                if (note != task.note) onTaskNoteChange(task.id, note)
                if (category != task.category) onTaskCategoryChange(task.id, category)
                if (date != task.deadlineDate) onTaskDeadlineChange(task.id, date)
                if (duration != task.estimatedDuration) onTaskDurationChange(task.id, duration)
                if (priority != task.priority) onTaskPriorityChange(task.id, priority)
                if (status != task.status) onTaskStatusChange(task.id, status)
                selectedTaskId = null
            },
            onSchedule = { onTaskScheduleRequested(task) },
            onCreateNote = { onTaskCreateNoteRequested(task) },
            onSubTaskCreate = { onSubTaskCreate(task.id, it) },
            onSubTaskUpdate = onSubTaskUpdate,
            onSubTaskDelete = onSubTaskDelete
        )
    }
}

@Composable private fun Header(eyebrow: String, title: String) {
    Column(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 20.dp, vertical = 14.dp)) {
        Text(eyebrow, color = Blue, style = MaterialTheme.typography.labelMedium)
        Spacer(Modifier.height(4.dp))
        Text(title, color = Color.White, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
    }
}

@Composable private fun OrgaModeSwitcher(mode: OrgaMode, onSelect: (OrgaMode) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        FilterChip(selected = mode == OrgaMode.TASK, onClick = { onSelect(OrgaMode.TASK) }, label = { Text("Taches") }, colors = orgaModeChipColors())
        FilterChip(selected = mode == OrgaMode.PLANNING, onClick = { onSelect(OrgaMode.PLANNING) }, label = { Text("Planning") }, colors = orgaModeChipColors())
        FilterChip(selected = mode == OrgaMode.NOTE, onClick = { onSelect(OrgaMode.NOTE) }, label = { Text("Notes") }, colors = orgaModeChipColors())
    }
}

@Composable private fun TaskCard(task: Task, mode: OrgaMode, onClick: () -> Unit, onToggle: () -> Unit, onAdvance: () -> Unit, onSchedule: () -> Unit, onCreateNote: () -> Unit) {
    val accent = priorityColor(task.priority)
    Card(Modifier.fillMaxWidth().clickable(onClick = onClick), shape = RoundedCornerShape(26.dp), colors = CardDefaults.cardColors(containerColor = Panel)) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Box(Modifier.width(6.dp).height(64.dp).clip(RoundedCornerShape(999.dp)).background(accent))
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(task.title, color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                        Pill(if (task.deadlineDate.isNotBlank()) task.deadlineDate else if (task.deadline.isNotBlank()) task.deadline else "Sans date", when {
                            task.status == TaskStatus.COMPLETED -> Mint
                            overdue(task) -> Rose
                            task.deadlineDate == todayIso() -> Gold
                            else -> Blue
                        })
                    }
                    if (task.description.isNotBlank()) {
                        Spacer(Modifier.height(8.dp))
                        Text(task.description, color = Dim, style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Pill(statusLabel(task.status), statusColor(task.status))
                Pill(priorityLabel(task.priority), accent)
                if (task.category.isNotBlank()) Pill(task.category, Color(0xFF7D8FB6))
            }
            if (task.subTasks.isNotEmpty()) {
                Spacer(Modifier.height(14.dp))
                Text("${task.subTasks.count { it.isCompleted }}/${task.subTasks.size} sous-actions fermees", color = Dim, style = MaterialTheme.typography.bodySmall)
            }
            Spacer(Modifier.height(16.dp))
            when (mode) {
                OrgaMode.TASK -> Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ActionPill(if (task.status == TaskStatus.COMPLETED) "Rouvrir" else "Terminer", Mint, Modifier.weight(1f), Icons.Default.Check, onClick = onToggle)
                    ActionPill(when (task.status) { TaskStatus.TODO -> "Lancer"; TaskStatus.IN_PROGRESS -> "Clore"; TaskStatus.COMPLETED -> "Fini" }, Blue, Modifier.weight(1f), if (task.status == TaskStatus.COMPLETED) Icons.Default.CheckCircle else Icons.Default.PlayArrow, task.status != TaskStatus.COMPLETED, onAdvance)
                }
                OrgaMode.PLANNING -> DestinationButton("Planifier cette tache", "Basculer vers l'agenda", Mint, Icons.Default.CalendarMonth, Modifier.fillMaxWidth(), onSchedule)
                OrgaMode.NOTE -> DestinationButton("Creer une note", "Basculer vers notes", Lilac, Icons.Default.Description, Modifier.fillMaxWidth(), onCreateNote)
            }
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable private fun CreateSheet(categories: List<String>, onDismiss: () -> Unit, onCreate: (String, String, String, TaskPriority, String, String) -> Unit) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(categories.firstOrNull().orEmpty()) }
    var date by remember { mutableStateOf("") }
    var duration by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf(TaskPriority.P3) }
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Panel, dragHandle = null) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 12.dp).padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("Nouvelle tache", color = Color.White, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
            Text("Une feuille propre, sans chrome superflu.", color = Dim, style = MaterialTheme.typography.bodyMedium)
            Field(title, { title = it }, "Titre")
            Field(description, { description = it }, "Description", false, 4)
            Field(category, { category = it }, "Categorie")
            Field(date, { date = it }, "Date ISO", true, 1, "2026-04-11")
            Field(duration, { duration = it }, "Duree", true, 1, "45 min")
            PriorityRow(priority) { priority = it }
            Button(onClick = { onCreate(title.trim(), category.trim(), description.trim(), priority, date.trim(), duration.trim()) }, enabled = title.isNotBlank(), modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), colors = ButtonDefaults.buttonColors(containerColor = Blue, contentColor = Color(0xFF081120))) { Text("Ajouter") }
        }
    }
}
@Composable private fun DestinationButton(label: String, caption: String, tint: Color, icon: ImageVector, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(modifier.clip(RoundedCornerShape(20.dp)).clickable(onClick = onClick), color = tint.copy(alpha = 0.14f), shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
                Text(label, color = Color.White, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            }
            Text(caption, color = tint, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable private fun TaskSheet(task: Task, initialTab: TaskSheetTab, categories: List<String>, onDismiss: () -> Unit, onDelete: () -> Unit, onSave: (String, String, String, String, String, String, TaskPriority, TaskStatus) -> Unit, onSchedule: () -> Unit, onCreateNote: () -> Unit, onSubTaskCreate: (String) -> Unit, onSubTaskUpdate: (String, String, Boolean) -> Unit, onSubTaskDelete: (String) -> Unit) {
    var title by remember(task.id) { mutableStateOf(task.title) }
    var description by remember(task.id) { mutableStateOf(task.description) }
    var note by remember(task.id) { mutableStateOf(task.note) }
    var category by remember(task.id) { mutableStateOf(task.category) }
    var date by remember(task.id) { mutableStateOf(task.deadlineDate) }
    var duration by remember(task.id) { mutableStateOf(task.estimatedDuration) }
    var priority by remember(task.id) { mutableStateOf(task.priority) }
    var status by remember(task.id) { mutableStateOf(task.status) }
    var newSubTask by remember(task.id) { mutableStateOf("") }
    var tab by remember(task.id, initialTab) { mutableStateOf(initialTab) }
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Panel, dragHandle = null) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 12.dp).padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Fiche tache", color = Color.White, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, contentDescription = "Supprimer", tint = Rose) }
            }
            Text("Chaque onglet prepare une sortie claire: execution, planning ou notes.", color = Dim, style = MaterialTheme.typography.bodyMedium)
            SheetTabRow(tab = tab, onSelect = { tab = it })
            when (tab) {
                TaskSheetTab.TASK -> {
                    Field(title, { title = it }, "Titre")
                    Field(description, { description = it }, "Description", false, 4)
                    Field(note, { note = it }, "Notes", false, 4)
                    Field(category, { category = it }, "Categorie", true, 1, categories.firstOrNull().orEmpty())
                    Field(date, { date = it }, "Date ISO", true, 1, "2026-04-11")
                    Field(duration, { duration = it }, "Duree", true, 1, "45 min")
                    PriorityRow(priority) { priority = it }
                    StatusRow(status) { status = it }
                    if (task.subTasks.isNotEmpty()) Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Sous-actions", color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
                        task.subTasks.forEach { subTask ->
                            Surface(color = PanelSoft, shape = RoundedCornerShape(18.dp)) {
                                Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(checked = subTask.isCompleted, onCheckedChange = { onSubTaskUpdate(subTask.id, subTask.text, !subTask.isCompleted) })
                                    Spacer(Modifier.width(10.dp))
                                    Text(subTask.text, color = if (subTask.isCompleted) Dim else Color.White, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                                    IconButton(onClick = { onSubTaskDelete(subTask.id) }) { Icon(Icons.Default.Close, contentDescription = "Supprimer", tint = Dim) }
                                }
                            }
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Field(newSubTask, { newSubTask = it }, "Nouvelle sous-action", modifier = Modifier.weight(1f))
                        Spacer(Modifier.width(10.dp))
                        IconButton(onClick = { if (newSubTask.isNotBlank()) { onSubTaskCreate(newSubTask.trim()); newSubTask = "" } }) { Icon(Icons.Default.Add, contentDescription = "Ajouter", tint = Blue) }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("Fermer", color = Dim) }
                        Button(onClick = { onSave(title.trim(), description.trim(), note.trim(), category.trim(), date.trim(), duration.trim(), priority, status) }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(18.dp), colors = ButtonDefaults.buttonColors(containerColor = Blue, contentColor = Color(0xFF081120))) { Text("Enregistrer") }
                    }
                }
                TaskSheetTab.PLANNING -> PlanningPanel(task = task, date = date, duration = duration, onOpenPlanning = onSchedule)
                TaskSheetTab.NOTE -> NotePanel(task = task, onCreateNote = onCreateNote)
            }
        }
    }
}

@Composable private fun Field(value: String, onChange: (String) -> Unit, label: String, singleLine: Boolean = true, minLines: Int = 1, hint: String = "", modifier: Modifier = Modifier) {
    OutlinedTextField(value = value, onValueChange = onChange, modifier = modifier.fillMaxWidth(), label = { Text(label) }, placeholder = if (hint.isBlank()) null else ({ Text(hint) }), singleLine = singleLine, minLines = minLines, keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences), shape = RoundedCornerShape(18.dp))
}

@Composable private fun SheetTabRow(tab: TaskSheetTab, onSelect: (TaskSheetTab) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Choice("Tache", tab == TaskSheetTab.TASK, Blue, Modifier.weight(1f)) { onSelect(TaskSheetTab.TASK) }
        Choice("Planning", tab == TaskSheetTab.PLANNING, Mint, Modifier.weight(1f)) { onSelect(TaskSheetTab.PLANNING) }
        Choice("Note", tab == TaskSheetTab.NOTE, Lilac, Modifier.weight(1f)) { onSelect(TaskSheetTab.NOTE) }
    }
}

@Composable private fun PlanningPanel(task: Task, date: String, duration: String, onOpenPlanning: () -> Unit) {
    Surface(color = PanelSoft, shape = RoundedCornerShape(22.dp)) {
        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Envoyer au planning", color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text("Preparer cette tache pour l'agenda avec les infos deja disponibles.", color = Dim, style = MaterialTheme.typography.bodyMedium)
            Text(task.title, color = Color.White, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            if (date.isNotBlank()) Pill(date, Mint)
            if (duration.isNotBlank()) Pill(duration, Blue)
            Button(onClick = onOpenPlanning, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), colors = ButtonDefaults.buttonColors(containerColor = Mint, contentColor = Color(0xFF081120))) { Text("Ouvrir le planning") }
        }
    }
}

@Composable private fun NotePanel(task: Task, onCreateNote: () -> Unit) {
    Surface(color = PanelSoft, shape = RoundedCornerShape(22.dp)) {
        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Basculer en note", color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text("Transformer la tache en note exploitable sans perdre son contexte.", color = Dim, style = MaterialTheme.typography.bodyMedium)
            Text(task.title, color = Color.White, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            if (task.description.isNotBlank()) Text(task.description, color = Dim, style = MaterialTheme.typography.bodyMedium, maxLines = 3, overflow = TextOverflow.Ellipsis)
            Button(onClick = onCreateNote, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), colors = ButtonDefaults.buttonColors(containerColor = Lilac, contentColor = Color(0xFF1A112A))) { Text("Creer la note") }
        }
    }
}

@Composable private fun PriorityRow(selected: TaskPriority, onSelect: (TaskPriority) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Priorite", color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { TaskPriority.values().forEach { Choice(priorityLabel(it), selected == it, priorityColor(it), Modifier.weight(1f)) { onSelect(it) } } }
    }
}

@Composable private fun StatusRow(selected: TaskStatus, onSelect: (TaskStatus) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Statut", color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { TaskStatus.values().forEach { Choice(statusLabel(it), selected == it, statusColor(it), Modifier.weight(1f)) { onSelect(it) } } }
    }
}

@Composable private fun Choice(label: String, selected: Boolean, tint: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(modifier.clip(RoundedCornerShape(16.dp)).clickable(onClick = onClick), color = if (selected) tint.copy(alpha = 0.18f) else PanelSoft, shape = RoundedCornerShape(16.dp)) {
        Text(label, color = if (selected) Color.White else Dim, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(horizontal = 10.dp, vertical = 12.dp))
    }
}

@Composable private fun orgaModeChipColors() = FilterChipDefaults.filterChipColors(
    selectedContainerColor = AccentBlue.copy(alpha = 0.18f),
    selectedLabelColor = AccentBlue,
    containerColor = Panel,
    labelColor = Dim
)

@Composable private fun Pill(label: String, tint: Color) {
    Surface(color = tint.copy(alpha = 0.14f), shape = RoundedCornerShape(999.dp)) { Text(label, color = tint, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) }
}

@Composable private fun ActionPill(label: String, tint: Color, modifier: Modifier = Modifier, icon: ImageVector, enabled: Boolean = true, onClick: () -> Unit) {
    Surface(modifier.clip(RoundedCornerShape(18.dp)).clickable(enabled = enabled, onClick = onClick), color = if (enabled) tint.copy(alpha = 0.16f) else PanelSoft, shape = RoundedCornerShape(18.dp)) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            Icon(icon, contentDescription = null, tint = if (enabled) tint else Dim, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(label, color = if (enabled) Color.White else Dim, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
private fun priorityLabel(priority: TaskPriority): String = when (priority) {
    TaskPriority.P1 -> "P1"
    TaskPriority.P2 -> "P2"
    TaskPriority.P3 -> "P3"
    TaskPriority.P4 -> "P4"
    TaskPriority.P5 -> "P5"
}

private fun priorityColor(priority: TaskPriority): Color = when (priority) {
    TaskPriority.P1 -> Rose
    TaskPriority.P2 -> Color(0xFFFF9966)
    TaskPriority.P3 -> Gold
    TaskPriority.P4 -> Blue
    TaskPriority.P5 -> Mint
}

private fun statusLabel(status: TaskStatus): String = when (status) {
    TaskStatus.TODO -> "A traiter"
    TaskStatus.IN_PROGRESS -> "En cours"
    TaskStatus.COMPLETED -> "Fermee"
}

private fun statusColor(status: TaskStatus): Color = when (status) {
    TaskStatus.TODO -> Gold
    TaskStatus.IN_PROGRESS -> Blue
    TaskStatus.COMPLETED -> Mint
}

private fun overdue(task: Task): Boolean = task.deadlineDate.isNotBlank() && task.deadlineDate < todayIso() && task.status != TaskStatus.COMPLETED

fun buildTaskNoteContent(task: Task): String {
    val parts = buildList {
        if (task.description.isNotBlank()) add(task.description.trim())
        if (task.note.isNotBlank()) add("Notes:\n${task.note.trim()}")
        if (task.category.isNotBlank()) add("Categorie: ${task.category}")
        if (task.deadlineDate.isNotBlank()) add("Date: ${task.deadlineDate}")
        if (task.estimatedDuration.isNotBlank()) add("Duree: ${task.estimatedDuration}")
        if (task.subTasks.isNotEmpty()) add("Sous-actions:\n" + task.subTasks.joinToString("\n") { "- ${it.text}" })
    }
    return parts.joinToString("\n\n").ifBlank { task.title }
}

private fun todayIso(): String {
    val calendar = Calendar.getInstance()
    return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(calendar.time)
}
