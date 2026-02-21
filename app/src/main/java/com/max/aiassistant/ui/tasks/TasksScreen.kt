package com.max.aiassistant.ui.tasks

import android.os.Build
import android.text.Html
import android.widget.TextView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material.icons.outlined.HourglassBottom
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardCapitalization
import com.max.aiassistant.model.*
import com.max.aiassistant.ui.common.EmptyStateView
import com.max.aiassistant.ui.common.MiniFluidOrb
import com.max.aiassistant.ui.common.NavigationSidebarScaffold
import com.max.aiassistant.ui.common.NavigationScreen
import com.max.aiassistant.ui.common.rememberNavigationSidebarState
import com.max.aiassistant.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

/**
 * ÉCRAN : Tâches
 *
 * Affiche :
 * - Liste des tâches avec filtres et tri
 * - Création de nouvelles tâches
 * - Sidebar de navigation accessible par swipe vers la gauche depuis le bord droit
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasksScreen(
    tasks: List<Task>,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    onTaskStatusChange: (String, TaskStatus) -> Unit,
    onTaskPriorityChange: (String, TaskPriority) -> Unit,
    onTaskDurationChange: (String, String) -> Unit,
    onTaskDeadlineChange: (String, String) -> Unit,
    onTaskCategoryChange: (String, String) -> Unit,
    onTaskTitleChange: (String, String) -> Unit,
    onTaskDescriptionChange: (String, String) -> Unit,
    onTaskNoteChange: (String, String) -> Unit,
    onTaskDelete: (String) -> Unit,
    onTaskCreate: (titre: String, categorie: String, description: String, priorite: TaskPriority, dateLimite: String, dureeEstimee: String) -> Unit,
    onSubTaskCreate: (taskId: String, text: String) -> Unit,
    onSubTaskUpdate: (subTaskId: String, text: String, isCompleted: Boolean) -> Unit,
    onSubTaskDelete: (subTaskId: String) -> Unit,
    onNavigateToHome: () -> Unit,
    onNavigateToChat: () -> Unit = {},
    onNavigateToPlanning: () -> Unit = {},
    onNavigateToWeather: () -> Unit = {},
    onNavigateToNotes: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val sidebarState = rememberNavigationSidebarState()
    
    NavigationSidebarScaffold(
        currentScreen = NavigationScreen.TASKS,
        onNavigateToScreen = { screen ->
            when (screen) {
                NavigationScreen.HOME -> onNavigateToHome()
                NavigationScreen.VOICE -> onNavigateToHome() // Retourne à Home pour accéder à Voice
                NavigationScreen.CHAT -> onNavigateToChat()
                NavigationScreen.TASKS -> { /* Déjà sur cet écran */ }
                NavigationScreen.PLANNING -> onNavigateToPlanning()
                NavigationScreen.WEATHER -> onNavigateToWeather()
                NavigationScreen.NOTES -> onNavigateToNotes()
            }
        },
        sidebarState = sidebarState
    ) {
        TasksScreenContent(
            tasks = tasks,
            isRefreshing = isRefreshing,
            onRefresh = onRefresh,
            onTaskStatusChange = onTaskStatusChange,
            onTaskPriorityChange = onTaskPriorityChange,
            onTaskDurationChange = onTaskDurationChange,
            onTaskDeadlineChange = onTaskDeadlineChange,
            onTaskCategoryChange = onTaskCategoryChange,
            onTaskTitleChange = onTaskTitleChange,
            onTaskDescriptionChange = onTaskDescriptionChange,
            onTaskNoteChange = onTaskNoteChange,
            onTaskDelete = onTaskDelete,
            onTaskCreate = onTaskCreate,
            onSubTaskCreate = onSubTaskCreate,
            onSubTaskUpdate = onSubTaskUpdate,
            onSubTaskDelete = onSubTaskDelete,
            modifier = modifier
        )
    }
}

/**
 * Contenu de l'écran Tâches
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TasksScreenContent(
    tasks: List<Task>,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    onTaskStatusChange: (String, TaskStatus) -> Unit,
    onTaskPriorityChange: (String, TaskPriority) -> Unit,
    onTaskDurationChange: (String, String) -> Unit,
    onTaskDeadlineChange: (String, String) -> Unit,
    onTaskCategoryChange: (String, String) -> Unit,
    onTaskTitleChange: (String, String) -> Unit,
    onTaskDescriptionChange: (String, String) -> Unit,
    onTaskNoteChange: (String, String) -> Unit,
    onTaskDelete: (String) -> Unit,
    onTaskCreate: (titre: String, categorie: String, description: String, priorite: TaskPriority, dateLimite: String, dureeEstimee: String) -> Unit,
    onSubTaskCreate: (taskId: String, text: String) -> Unit,
    onSubTaskUpdate: (subTaskId: String, text: String, isCompleted: Boolean) -> Unit,
    onSubTaskDelete: (subTaskId: String) -> Unit,
    modifier: Modifier = Modifier
) {
    // Stocker l'ID de la tâche sélectionnée au lieu de la tâche elle-même
    // pour que le dialog se mette à jour quand la liste tasks change
    var selectedTaskId by remember { mutableStateOf<String?>(null) }
    
    // États pour les filtres
    var showFilterDialog by remember { mutableStateOf(false) }
    var filterCategory by remember { mutableStateOf<String?>(null) }
    var filterPriority by remember { mutableStateOf<TaskPriority?>(null) }
    var filterDeadlineDate by remember { mutableStateOf<String?>(null) } // Date ISO ou null
    
    // État pour le tri
    var showSortDialog by remember { mutableStateOf(false) }
    var sortOrder by remember { mutableStateOf<String?>("priority") } // "priority" ou "deadline" ou null (défaut: priority)
    
    // État pour le dialog de création
    var showCreateTaskDialog by remember { mutableStateOf(false) }
    
    // Récupérer la tâche sélectionnée depuis la liste actuelle
    val selectedTask = selectedTaskId?.let { id -> tasks.find { it.id == id } }
    
    // Appliquer les filtres et le tri sur les tâches
    val filteredTasks = remember(tasks, filterCategory, filterPriority, filterDeadlineDate, sortOrder) {
        val filtered = tasks.filter { task ->
            val categoryMatch = filterCategory == null || task.category == filterCategory
            val priorityMatch = filterPriority == null || task.priority == filterPriority
            val deadlineMatch = if (filterDeadlineDate == null) {
                true
            } else {
                // Comparer les dates : afficher les tâches dont la deadline <= filterDeadlineDate
                try {
                    if (task.deadlineDate.isEmpty()) {
                        false // Pas de deadline = ne pas afficher si on filtre par date
                    } else {
                        task.deadlineDate <= filterDeadlineDate!!
                    }
                } catch (e: Exception) {
                    true
                }
            }
            categoryMatch && priorityMatch && deadlineMatch
        }
        
        // Appliquer le tri
        when (sortOrder) {
            "priority" -> filtered.sortedBy { it.priority.ordinal }
            "deadline" -> filtered.sortedBy { it.deadlineDate.ifEmpty { "9999-12-31" } }
            else -> filtered
        }
    }
    
    // Extraire les catégories uniques des tâches
    val categories = remember(tasks) {
        tasks.map { it.category }.filter { it.isNotEmpty() }.distinct().sorted()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .windowInsetsPadding(WindowInsets.navigationBars)
    ) {
        // Espace en haut pour éviter que le calendrier soit coupé
        Spacer(modifier = Modifier.height(32.dp))

        // En-tête avec titre et barre d'actions
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Tâches",
                style = MaterialTheme.typography.headlineMedium,
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Barre de filtres et création
            TaskActionBar(
                hasActiveFilters = filterCategory != null || filterPriority != null || filterDeadlineDate != null,
                currentSortOrder = sortOrder,
                onFilterClick = { showFilterDialog = true },
                onSortClick = { showSortDialog = true },
                onCreateClick = { showCreateTaskDialog = true }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Liste des tâches
        Box(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp)
        ) {
            TasksContent(
                tasks = filteredTasks,
                isRefreshing = isRefreshing,
                onRefresh = onRefresh,
                onTaskClick = { selectedTaskId = it.id },
                onTaskStatusChange = onTaskStatusChange,
                onSubTaskToggle = onSubTaskUpdate,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
    
    // Dialog de filtre
    if (showFilterDialog) {
        TaskFilterDialog(
            categories = categories,
            selectedCategory = filterCategory,
            selectedPriority = filterPriority,
            selectedDeadlineDate = filterDeadlineDate,
            onCategorySelected = { filterCategory = it },
            onPrioritySelected = { filterPriority = it },
            onDeadlineDateSelected = { filterDeadlineDate = it },
            onClearFilters = {
                filterCategory = null
                filterPriority = null
                filterDeadlineDate = null
            },
            onDismiss = { showFilterDialog = false }
        )
    }
    
    // Dialog de tri
    if (showSortDialog) {
        TaskSortDialog(
            currentSortOrder = sortOrder,
            onSortSelected = { sortOrder = it },
            onDismiss = { showSortDialog = false }
        )
    }
    
    // Dialog de création de tâche
    if (showCreateTaskDialog) {
        CreateTaskDialog(
            categories = categories,
            onDismiss = { showCreateTaskDialog = false },
            onCreateTask = { title, description, priority, category, deadline, estimatedDuration ->
                onTaskCreate(title, category, description, priority, deadline, estimatedDuration)
                showCreateTaskDialog = false
            }
        )
    }

    // Dialog des détails de la tâche
    selectedTask?.let { task ->
        TaskDetailsDialog(
            task = task,
            categories = categories,
            onDismiss = { selectedTaskId = null },
            onStatusChange = { newStatus ->
                onTaskStatusChange(task.id, newStatus)
                // Ne pas fermer le dialog pour permettre plusieurs modifications
            },
            onPriorityChange = { newPriority ->
                onTaskPriorityChange(task.id, newPriority)
                // Ne pas fermer le dialog pour permettre plusieurs modifications
            },
            onDurationChange = { newDuration ->
                onTaskDurationChange(task.id, newDuration)
                // Ne pas fermer le dialog pour permettre plusieurs modifications
            },
            onDeadlineChange = { newDeadlineDate ->
                onTaskDeadlineChange(task.id, newDeadlineDate)
                // Ne pas fermer le dialog pour permettre plusieurs modifications
            },
            onCategoryChange = { newCategory ->
                onTaskCategoryChange(task.id, newCategory)
                // Ne pas fermer le dialog pour permettre plusieurs modifications
            },
            onTitleChange = { newTitle ->
                onTaskTitleChange(task.id, newTitle)
            },
            onDescriptionChange = { newDescription ->
                onTaskDescriptionChange(task.id, newDescription)
            },
            onNoteChange = { newNote ->
                onTaskNoteChange(task.id, newNote)
            },
            onSubTaskCreate = { text ->
                onSubTaskCreate(task.id, text)
            },
            onSubTaskUpdate = onSubTaskUpdate,
            onSubTaskDelete = onSubTaskDelete,
            onDelete = {
                onTaskDelete(task.id)
                selectedTaskId = null
            }
        )
    }
}

/**
 * Sélecteur d'onglets (Tâches / Planning)
 * Style similaire à l'écran météo avec TabRow Material3
 */
@Composable
fun TabSelector(
    selectedIndex: Int,
    onTabSelected: (Int) -> Unit
) {
    val tabs = listOf("Tâches", "Planning")
    
    TabRow(
        selectedTabIndex = selectedIndex,
        containerColor = DarkBackground,
        contentColor = AccentBlue,
        modifier = Modifier.fillMaxWidth()
    ) {
        tabs.forEachIndexed { index, title ->
            Tab(
                selected = selectedIndex == index,
                onClick = { onTabSelected(index) },
                text = {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        color = if (selectedIndex == index) AccentBlue else Color.White.copy(alpha = 0.6f)
                    )
                }
            )
        }
    }
}

/**
 * Barre d'actions pour les tâches (filtre + tri + création)
 */
@Composable
fun TaskActionBar(
    hasActiveFilters: Boolean,
    currentSortOrder: String?,
    onFilterClick: () -> Unit,
    onSortClick: () -> Unit,
    onCreateClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Bouton Filtre
        OutlinedButton(
            onClick = onFilterClick,
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = if (hasActiveFilters) AccentBlue else Color.White.copy(alpha = 0.7f)
            ),
            border = BorderStroke(
                1.dp,
                if (hasActiveFilters) AccentBlue else Color.White.copy(alpha = 0.3f)
            ),
            shape = RoundedCornerShape(12.dp),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.FilterList,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "Filtre",
                style = MaterialTheme.typography.bodySmall
            )
        }
        
        // Bouton Trier
        OutlinedButton(
            onClick = onSortClick,
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = if (currentSortOrder != null) AccentBlue else Color.White.copy(alpha = 0.7f)
            ),
            border = BorderStroke(
                1.dp,
                if (currentSortOrder != null) AccentBlue else Color.White.copy(alpha = 0.3f)
            ),
            shape = RoundedCornerShape(12.dp),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Text(
                text = "Trier",
                style = MaterialTheme.typography.bodySmall
            )
        }
        
        // Bouton Créer
        Button(
            onClick = onCreateClick,
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.buttonColors(
                containerColor = AccentBlue
            ),
            shape = RoundedCornerShape(12.dp),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "Créer",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

/**
 * Dialog de tri des tâches
 */
@Composable
fun TaskSortDialog(
    currentSortOrder: String?,
    onSortSelected: (String?) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF2C2C2E),
        title = {
            Text(
                text = "Trier les tâches",
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Option: Par priorité
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onSortSelected(if (currentSortOrder == "priority") null else "priority")
                            onDismiss()
                        },
                    shape = RoundedCornerShape(8.dp),
                    color = if (currentSortOrder == "priority") AccentBlue.copy(alpha = 0.3f) else Color(0xFF1A1A1C)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Flag,
                                contentDescription = null,
                                tint = if (currentSortOrder == "priority") AccentBlue else Color.White.copy(alpha = 0.7f)
                            )
                            Text(
                                text = "Par priorité",
                                color = Color.White,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                        if (currentSortOrder == "priority") {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = AccentBlue
                            )
                        }
                    }
                }
                
                // Option: Par date d'échéance
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onSortSelected(if (currentSortOrder == "deadline") null else "deadline")
                            onDismiss()
                        },
                    shape = RoundedCornerShape(8.dp),
                    color = if (currentSortOrder == "deadline") AccentBlue.copy(alpha = 0.3f) else Color(0xFF1A1A1C)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.DateRange,
                                contentDescription = null,
                                tint = if (currentSortOrder == "deadline") AccentBlue else Color.White.copy(alpha = 0.7f)
                            )
                            Text(
                                text = "Par date d'échéance",
                                color = Color.White,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                        if (currentSortOrder == "deadline") {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = AccentBlue
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Fermer", color = Color.White.copy(alpha = 0.7f))
            }
        }
    )
}

/**
 * Dialog de filtrage des tâches
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TaskFilterDialog(
    categories: List<String>,
    selectedCategory: String?,
    selectedPriority: TaskPriority?,
    selectedDeadlineDate: String?,
    onCategorySelected: (String?) -> Unit,
    onPrioritySelected: (TaskPriority?) -> Unit,
    onDeadlineDateSelected: (String?) -> Unit,
    onClearFilters: () -> Unit,
    onDismiss: () -> Unit
) {
    var showDatePicker by remember { mutableStateOf(false) }
    
    // DatePicker state
    val initialDateMillis = remember(selectedDeadlineDate) {
        if (selectedDeadlineDate != null) {
            try {
                val parts = selectedDeadlineDate.split("-")
                java.util.Calendar.getInstance().apply {
                    set(parts[0].toInt(), parts[1].toInt() - 1, parts[2].toInt(), 12, 0, 0)
                }.timeInMillis
            } catch (e: Exception) {
                System.currentTimeMillis()
            }
        } else {
            System.currentTimeMillis()
        }
    }
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialDateMillis)
    
    // DatePicker dialog
    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val calendar = java.util.Calendar.getInstance().apply {
                                timeInMillis = millis
                            }
                            val year = calendar.get(java.util.Calendar.YEAR)
                            val month = calendar.get(java.util.Calendar.MONTH) + 1
                            val day = calendar.get(java.util.Calendar.DAY_OF_MONTH)
                            onDeadlineDateSelected(String.format("%04d-%02d-%02d", year, month, day))
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("Valider", color = AccentBlue)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Annuler", color = Color.White.copy(alpha = 0.7f))
                }
            },
            colors = DatePickerDefaults.colors(containerColor = Color(0xFF2C2C2E))
        ) {
            DatePicker(
                state = datePickerState,
                showModeToggle = false,
                colors = DatePickerDefaults.colors(
                    containerColor = Color(0xFF2C2C2E),
                    titleContentColor = Color.White,
                    headlineContentColor = Color.White,
                    weekdayContentColor = Color.White.copy(alpha = 0.7f),
                    subheadContentColor = Color.White.copy(alpha = 0.7f),
                    yearContentColor = Color.White,
                    currentYearContentColor = AccentBlue,
                    selectedYearContentColor = Color.White,
                    selectedYearContainerColor = AccentBlue,
                    dayContentColor = Color.White,
                    selectedDayContentColor = Color.White,
                    selectedDayContainerColor = AccentBlue,
                    todayContentColor = AccentBlue,
                    todayDateBorderColor = AccentBlue,
                    navigationContentColor = Color.White
                )
            )
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF2C2C2E),
        title = {
            Text(
                text = "Filtrer les tâches",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                // Section Catégorie
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF1A1A1C))
                        .padding(10.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "Catégorie",
                            color = Color.White,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Option "Toutes"
                            FilterChip(
                                selected = selectedCategory == null,
                                onClick = { onCategorySelected(null) },
                                label = { Text("Toutes") },
                                leadingIcon = if (selectedCategory == null) {
                                    {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                } else null,
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = AccentBlue,
                                    selectedLabelColor = Color.White,
                                    selectedLeadingIconColor = Color.White,
                                    containerColor = Color(0xFF2C2C2E),
                                    labelColor = Color.White.copy(alpha = 0.7f)
                                )
                            )
                            
                            // Catégories existantes
                            categories.forEach { category ->
                                FilterChip(
                                    selected = selectedCategory == category,
                                    onClick = {
                                        onCategorySelected(if (selectedCategory == category) null else category)
                                    },
                                    label = { Text(category) },
                                    leadingIcon = if (selectedCategory == category) {
                                        {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    } else null,
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = AccentBlue,
                                        selectedLabelColor = Color.White,
                                        selectedLeadingIconColor = Color.White,
                                        containerColor = Color(0xFF2C2C2E),
                                        labelColor = Color.White.copy(alpha = 0.7f)
                                    )
                                )
                            }
                        }
                    }
                }
                
                // Section Priorité
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF1A1A1C))
                        .padding(10.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "Priorité",
                            color = Color.White,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Option "Toutes"
                            FilterChip(
                                selected = selectedPriority == null,
                                onClick = { onPrioritySelected(null) },
                                label = { Text("Toutes") },
                                leadingIcon = if (selectedPriority == null) {
                                    {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                } else null,
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = AccentBlue,
                                    selectedLabelColor = Color.White,
                                    selectedLeadingIconColor = Color.White,
                                    containerColor = Color(0xFF2C2C2E),
                                    labelColor = Color.White.copy(alpha = 0.7f)
                                )
                            )
                            
                            TaskPriority.values().forEach { priority ->
                                val priorityText = when (priority) {
                                    TaskPriority.P1 -> "P1 - Urgente"
                                    TaskPriority.P2 -> "P2 - Haute"
                                    TaskPriority.P3 -> "P3 - Moyenne"
                                    TaskPriority.P4 -> "P4 - Basse"
                                    TaskPriority.P5 -> "P5 - Faible"
                                }
                                val priorityColor = when (priority) {
                                    TaskPriority.P1 -> UrgentRed
                                    TaskPriority.P2 -> Color(0xFFFF6B35)
                                    TaskPriority.P3 -> NormalOrange
                                    TaskPriority.P4 -> Color(0xFFFFB84D)
                                    TaskPriority.P5 -> CompletedGreen
                                }
                                FilterChip(
                                    selected = selectedPriority == priority,
                                    onClick = {
                                        onPrioritySelected(if (selectedPriority == priority) null else priority)
                                    },
                                    label = { Text(priorityText) },
                                    leadingIcon = if (selectedPriority == priority) {
                                        {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    } else null,
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = priorityColor,
                                        selectedLabelColor = Color.White,
                                        selectedLeadingIconColor = Color.White,
                                        containerColor = Color(0xFF2C2C2E),
                                        labelColor = Color.White.copy(alpha = 0.7f)
                                    )
                                )
                            }
                        }
                    }
                }
                
                // Section Échéance
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF1A1A1C))
                        .padding(10.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "Échéance jusqu'au",
                            color = Color.White,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        
                        Text(
                            text = "Affiche les tâches dont la date limite est ≤ à la date sélectionnée",
                            color = Color.White.copy(alpha = 0.5f),
                            style = MaterialTheme.typography.bodySmall
                        )
                        
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = { showDatePicker = true },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = if (selectedDeadlineDate != null) AccentBlue else Color.White.copy(alpha = 0.7f)
                                ),
                                border = BorderStroke(
                                    1.dp,
                                    if (selectedDeadlineDate != null) AccentBlue else Color.White.copy(alpha = 0.3f)
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DateRange,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (selectedDeadlineDate == null) {
                                        "Toutes les dates"
                                    } else {
                                        try {
                                            val parts = selectedDeadlineDate.split("-")
                                            "${parts[2]}/${parts[1]}/${parts[0]}"
                                        } catch (e: Exception) {
                                            selectedDeadlineDate
                                        }
                                    }
                                )
                            }
                            
                            // Bouton pour effacer la date
                            if (selectedDeadlineDate != null) {
                                IconButton(
                                    onClick = { onDeadlineDateSelected(null) },
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "Effacer",
                                        tint = Color.White.copy(alpha = 0.7f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Bouton Réinitialiser
                TextButton(
                    onClick = onClearFilters,
                    enabled = selectedCategory != null || selectedPriority != null || selectedDeadlineDate != null
                ) {
                    Text(
                        "Réinitialiser",
                        color = if (selectedCategory != null || selectedPriority != null || selectedDeadlineDate != null)
                            Color.White.copy(alpha = 0.7f)
                        else
                            Color.White.copy(alpha = 0.3f)
                    )
                }
                
                // Bouton Appliquer
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Appliquer", fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {}
    )
}

/**
 * Dialog de création de tâche - Utilise le même style que TaskDetailsDialog
 * avec une tâche temporaire modifiable
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateTaskDialog(
    categories: List<String>,
    onDismiss: () -> Unit,
    onCreateTask: (title: String, description: String, priority: TaskPriority, category: String, deadline: String, estimatedDuration: String) -> Unit
) {
    // Calculer la date par défaut (aujourd'hui + 7 jours)
    val defaultDeadline = remember {
        val calendar = java.util.Calendar.getInstance().apply {
            add(java.util.Calendar.DAY_OF_YEAR, 7)
        }
        String.format("%04d-%02d-%02d", 
            calendar.get(java.util.Calendar.YEAR),
            calendar.get(java.util.Calendar.MONTH) + 1,
            calendar.get(java.util.Calendar.DAY_OF_MONTH)
        )
    }
    
    // État local de la tâche en création
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var status by remember { mutableStateOf(TaskStatus.TODO) }
    var priority by remember { mutableStateOf(TaskPriority.P3) }
    var category by remember { mutableStateOf("Aucune") }
    var deadlineDate by remember { mutableStateOf(defaultDeadline) }
    var estimatedDuration by remember { mutableStateOf("1 heure") }
    var subTasks by remember { mutableStateOf(listOf<SubTask>()) }
    var newSubTaskText by remember { mutableStateOf("") }
    
    var selectedTab by remember { mutableStateOf(0) }
    var showStatusDialog by remember { mutableStateOf(false) }
    var showPriorityDialog by remember { mutableStateOf(false) }
    var showDurationDialog by remember { mutableStateOf(false) }
    var showDeadlineDialog by remember { mutableStateOf(false) }
    var showCategoryDialog by remember { mutableStateOf(false) }
    
    // États pour l'édition inline
    var isEditingTitle by remember { mutableStateOf(false) }
    var isEditingDescription by remember { mutableStateOf(false) }
    
    // FocusRequesters
    val titleFocusRequester = remember { FocusRequester() }
    val descriptionFocusRequester = remember { FocusRequester() }
    val subTaskFocusRequester = remember { FocusRequester() }
    
    LaunchedEffect(isEditingTitle) {
        if (isEditingTitle) titleFocusRequester.requestFocus()
    }
    LaunchedEffect(isEditingDescription) {
        if (isEditingDescription) descriptionFocusRequester.requestFocus()
    }

    // Dialogs de sélection
    if (showStatusDialog) {
        StatusSelectionDialog(
            currentStatus = status,
            onDismiss = { showStatusDialog = false },
            onStatusSelected = { status = it; showStatusDialog = false }
        )
    }
    
    if (showPriorityDialog) {
        PrioritySelectionDialog(
            currentPriority = priority,
            onDismiss = { showPriorityDialog = false },
            onPrioritySelected = { priority = it; showPriorityDialog = false }
        )
    }
    
    if (showDurationDialog) {
        DurationInputDialog(
            currentDuration = estimatedDuration,
            onDismiss = { showDurationDialog = false },
            onDurationChanged = { estimatedDuration = it; showDurationDialog = false }
        )
    }
    
    if (showDeadlineDialog) {
        DeadlineDatePickerDialog(
            currentDeadlineDate = deadlineDate,
            onDismiss = { showDeadlineDialog = false },
            onDateSelected = { deadlineDate = it; showDeadlineDialog = false }
        )
    }
    
    if (showCategoryDialog) {
        CategorySelectionDialog(
            currentCategory = category,
            categories = categories,
            onDismiss = { showCategoryDialog = false },
            onCategorySelected = { category = it; showCategoryDialog = false }
        )
    }

    androidx.compose.ui.window.Dialog(
        onDismissRequest = { /* Ne pas fermer en cliquant à l'extérieur */ },
        properties = androidx.compose.ui.window.DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .clip(RoundedCornerShape(24.dp))
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF1C1C1E),
                            Color(0xFF2C2C2E)
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // En-tête avec titre éditable et bouton fermer
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    if (isEditingTitle) {
                        OutlinedTextField(
                            value = title,
                            onValueChange = { title = it },
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 8.dp)
                                .focusRequester(titleFocusRequester),
                            placeholder = {
                                Text(
                                    "Titre de la tâche",
                                    color = Color.White.copy(alpha = 0.5f),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            },
                            textStyle = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AccentBlue,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                                cursorColor = AccentBlue
                            ),
                            shape = RoundedCornerShape(8.dp),
                            singleLine = false,
                            maxLines = 3,
                            trailingIcon = {
                                IconButton(onClick = { isEditingTitle = false }) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Valider",
                                        tint = AccentBlue
                                    )
                                }
                            }
                        )
                    } else {
                        Text(
                            text = if (title.isEmpty()) "Titre de la tâche" else title,
                            color = if (title.isEmpty()) Color.White.copy(alpha = 0.5f) else Color.White,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { isEditingTitle = true }
                                .padding(end = 8.dp, top = 4.dp)
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Fermer",
                            tint = Color(0xFFC62828),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Métadonnées avec badges (comme TaskDetailsDialog)
                CreateTaskMetadata(
                    status = status,
                    priority = priority,
                    estimatedDuration = estimatedDuration,
                    deadlineDate = deadlineDate,
                    category = category,
                    onStatusClick = { showStatusDialog = true },
                    onPriorityClick = { showPriorityDialog = true },
                    onDurationClick = { showDurationDialog = true },
                    onDeadlineClick = { showDeadlineDialog = true },
                    onCategoryClick = { showCategoryDialog = true }
                )

                // Séparateur
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.White.copy(alpha = 0.1f),
                                    Color.Transparent
                                )
                            )
                        )
                )

                // Onglets Description / Sous-tâches
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF1A1A1C))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Onglet Description
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (selectedTab == 0) AccentBlue else Color.Transparent)
                            .clickable { selectedTab = 0 }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Description",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (selectedTab == 0) Color.White else Color.White.copy(alpha = 0.6f),
                            fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                    
                    // Onglet Sous-tâches
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (selectedTab == 1) AccentBlue else Color.Transparent)
                            .clickable { selectedTab = 1 }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "Sous-tâches",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (selectedTab == 1) Color.White else Color.White.copy(alpha = 0.6f),
                                fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal
                            )
                            if (subTasks.isNotEmpty()) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "(${subTasks.count { it.isCompleted }}/${subTasks.size})",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (selectedTab == 1) Color.White.copy(alpha = 0.8f) else Color.White.copy(alpha = 0.5f)
                                )
                            }
                        }
                    }
                }

                // Contenu Description/Sous-tâches
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF1A1A1C))
                        .padding(12.dp)
                ) {
                    when (selectedTab) {
                        0 -> {
                            if (isEditingDescription) {
                                Column {
                                    OutlinedTextField(
                                        value = description,
                                        onValueChange = { description = it },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .focusRequester(descriptionFocusRequester),
                                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                                            color = Color.White.copy(alpha = 0.9f),
                                            lineHeight = 22.sp
                                        ),
                                        placeholder = { Text("Ajouter une description...", color = Color.White.copy(alpha = 0.5f)) },
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = AccentBlue,
                                            unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                                            cursorColor = AccentBlue
                                        ),
                                        shape = RoundedCornerShape(8.dp),
                                        minLines = 3,
                                        maxLines = 6
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        horizontalArrangement = Arrangement.End,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        TextButton(onClick = { isEditingDescription = false }) {
                                            Text("Valider", color = AccentBlue, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            } else {
                                Text(
                                    text = if (description.isNotEmpty()) description else "Appuyez pour ajouter une description...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (description.isNotEmpty()) Color.White.copy(alpha = 0.9f) else Color.White.copy(alpha = 0.5f),
                                    lineHeight = 22.sp,
                                    fontStyle = if (description.isEmpty()) androidx.compose.ui.text.font.FontStyle.Italic else androidx.compose.ui.text.font.FontStyle.Normal,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { isEditingDescription = true }
                                )
                            }
                        }
                        1 -> {
                            // Onglet Sous-tâches
                            Column(
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Liste des sous-tâches
                                if (subTasks.isEmpty()) {
                                    Text(
                                        text = "Aucune sous-tâche",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color.White.copy(alpha = 0.5f),
                                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                                    )
                                } else {
                                    subTasks.forEachIndexed { index, subTask ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(8.dp))
                                                .clickable {
                                                    subTasks = subTasks.mapIndexed { i, st ->
                                                        if (i == index) st.copy(isCompleted = !st.isCompleted) else st
                                                    }
                                                }
                                                .padding(vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = if (subTask.isCompleted) 
                                                    Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                                                contentDescription = if (subTask.isCompleted) "Terminée" else "À faire",
                                                tint = if (subTask.isCompleted) AccentBlue else Color.White.copy(alpha = 0.6f),
                                                modifier = Modifier.size(24.dp)
                                            )
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Text(
                                                text = subTask.text,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = if (subTask.isCompleted) Color.White.copy(alpha = 0.5f) else Color.White,
                                                textDecoration = if (subTask.isCompleted) androidx.compose.ui.text.style.TextDecoration.LineThrough else androidx.compose.ui.text.style.TextDecoration.None,
                                                modifier = Modifier.weight(1f)
                                            )
                                            IconButton(
                                                onClick = {
                                                    subTasks = subTasks.filterIndexed { i, _ -> i != index }
                                                },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Close,
                                                    contentDescription = "Supprimer",
                                                    tint = Color.White.copy(alpha = 0.5f),
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                                
                                // Ajouter une sous-tâche
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedTextField(
                                        value = newSubTaskText,
                                        onValueChange = { newSubTaskText = it },
                                        modifier = Modifier
                                            .weight(1f)
                                            .focusRequester(subTaskFocusRequester),
                                        placeholder = { Text("Nouvelle sous-tâche...", color = Color.White.copy(alpha = 0.5f)) },
                                        textStyle = MaterialTheme.typography.bodyMedium.copy(color = Color.White),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = AccentBlue,
                                            unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                                            cursorColor = AccentBlue
                                        ),
                                        shape = RoundedCornerShape(8.dp),
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    IconButton(
                                        onClick = {
                                            if (newSubTaskText.isNotBlank()) {
                                                subTasks = subTasks + SubTask(text = newSubTaskText.trim())
                                                newSubTaskText = ""
                                            }
                                        },
                                        enabled = newSubTaskText.isNotBlank()
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = "Ajouter",
                                            tint = if (newSubTaskText.isNotBlank()) AccentBlue else Color.White.copy(alpha = 0.3f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Bouton Créer
                Button(
                    onClick = {
                        val finalCategory = if (category == "Aucune") "" else category
                        if (title.isNotBlank()) {
                            onCreateTask(title, description, priority, finalCategory, deadlineDate, estimatedDuration)
                            onDismiss()
                        }
                    },
                    enabled = title.isNotBlank(),
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AccentBlue,
                        disabledContainerColor = AccentBlue.copy(alpha = 0.3f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Créer la tâche", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/**
 * Métadonnées pour le dialog de création (identique à ModernCompactMetadata)
 */
@Composable
fun CreateTaskMetadata(
    status: TaskStatus,
    priority: TaskPriority,
    estimatedDuration: String,
    deadlineDate: String,
    category: String,
    onStatusClick: () -> Unit,
    onPriorityClick: () -> Unit,
    onDurationClick: () -> Unit,
    onDeadlineClick: () -> Unit,
    onCategoryClick: () -> Unit
) {
    val statusText = when (status) {
        TaskStatus.TODO -> "À faire"
        TaskStatus.IN_PROGRESS -> "En cours"
        TaskStatus.COMPLETED -> "Terminé"
    }
    val statusColor = when (status) {
        TaskStatus.COMPLETED -> CompletedGreen
        TaskStatus.IN_PROGRESS -> AccentBlue
        else -> NormalOrange  // Jaune/Orange pour "À faire"
    }
    
    val priorityText = when (priority) {
        TaskPriority.P1 -> "P1"
        TaskPriority.P2 -> "P2"
        TaskPriority.P3 -> "P3"
        TaskPriority.P4 -> "P4"
        TaskPriority.P5 -> "P5"
    }
    val priorityColor = when (priority) {
        TaskPriority.P1 -> UrgentRed
        TaskPriority.P2 -> Color(0xFFFF6B35)  // Orange foncé
        TaskPriority.P3 -> NormalOrange
        TaskPriority.P4 -> Color(0xFFFFB84D)  // Jaune
        TaskPriority.P5 -> CompletedGreen
    }
    
    // Formatter la deadline pour affichage (comme dans ModernCompactMetadata)
    val formattedDeadline = remember(deadlineDate) {
        com.max.aiassistant.data.api.formatDeadline(deadlineDate)
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // Première ligne : Statut, Priorité, Deadline
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Badge de statut (cliquable)
            Box(modifier = Modifier.clickable { onStatusClick() }) {
                ModernBadge(text = statusText, color = statusColor)
            }

            // Badge de priorité (cliquable)
            Box(modifier = Modifier.clickable { onPriorityClick() }) {
                ModernBadge(text = priorityText, color = priorityColor)
            }

            // Deadline (cliquable)
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable { onDeadlineClick() }
                    .padding(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Schedule,
                    contentDescription = null,
                    tint = AccentBlue.copy(alpha = 0.7f),
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = formattedDeadline,
                    color = AccentBlue.copy(alpha = 0.9f),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // Deuxième ligne : Catégorie et Durée
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Catégorie (cliquable)
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable { onCategoryClick() }
                    .padding(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Category,
                    contentDescription = null,
                    tint = AccentBlue.copy(alpha = 0.7f),
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = category,
                    color = if (category != "Aucune") Color.White.copy(alpha = 0.8f) else Color.White.copy(alpha = 0.5f),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium
                )
            }

            // Durée (cliquable)
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable { onDurationClick() }
                    .padding(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = AccentBlue.copy(alpha = 0.7f),
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = estimatedDuration,
                    color = AccentBlue.copy(alpha = 0.9f),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

/**
 * Contenu de l'onglet Tâches
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasksContent(
    tasks: List<Task>,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    onTaskClick: (Task) -> Unit,
    onTaskStatusChange: (String, TaskStatus) -> Unit,
    onSubTaskToggle: (subTaskId: String, text: String, isCompleted: Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val pullRefreshState = rememberPullToRefreshState()
    PullToRefreshBox(
        state = pullRefreshState,
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = modifier
            .fillMaxSize()
    ) {
        // Liste des tâches
        if (tasks.isEmpty() && !isRefreshing) {
            // État vide illustré
            EmptyStateView(
                icon = Icons.Default.CheckCircle,
                iconTint = Color(0xFF0A84FF),
                title = "Aucune tâche",
                subtitle = "Appuyez sur + pour ajouter une tâche\nou glissez vers le bas pour actualiser"
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(tasks, key = { it.id }) { task ->
                    TaskItemCompact(
                        task = task,
                        onClick = { onTaskClick(task) },
                        onStatusChange = { newStatus -> onTaskStatusChange(task.id, newStatus) },
                        onSubTaskToggle = onSubTaskToggle
                    )
                }
            }
        }
    }
}

/**
 * Enum pour les différentes vues du planning
 */
enum class PlanningViewType {
    WEEK, MONTH
}

/**
 * Contenu de l'onglet Planning (anciennement Agenda)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgendaContent(
    events: List<Event>,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedEvent by remember { mutableStateOf<Event?>(null) }
    var currentView by remember { mutableStateOf(PlanningViewType.WEEK) }
    // Offset pour la navigation (0 = semaine/mois actuel, -1 = précédent, +1 = suivant)
    var weekOffset by remember { mutableStateOf(0) }
    var monthOffset by remember { mutableStateOf(0) }
    val pullRefreshState = rememberPullToRefreshState()
    
    Column(modifier = modifier.fillMaxSize()) {
        // Sélecteur de vue
        PlanningViewSelector(
            currentView = currentView,
            onViewSelected = { 
                currentView = it
                // Reset offset quand on change de vue
                weekOffset = 0
                monthOffset = 0
            }
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        PullToRefreshBox(
            state = pullRefreshState,
            isRefreshing = isRefreshing,
            onRefresh = onRefresh,
            modifier = Modifier.fillMaxSize()
        ) {
            when (currentView) {
                PlanningViewType.WEEK -> {
                    PlanningWeekView(
                        events = events,
                        weekOffset = weekOffset,
                        onPreviousWeek = { weekOffset-- },
                        onNextWeek = { weekOffset++ },
                        onEventClick = { selectedEvent = it }
                    )
                }
                PlanningViewType.MONTH -> {
                    PlanningMonthView(
                        events = events,
                        monthOffset = monthOffset,
                        onPreviousMonth = { monthOffset-- },
                        onNextMonth = { monthOffset++ },
                        onEventClick = { selectedEvent = it }
                    )
                }
            }
        }
    }

    // Dialog des détails de l'événement
    selectedEvent?.let { event ->
        EventDetailsDialog(
            event = event,
            onDismiss = { selectedEvent = null }
        )
    }
}

/**
 * Sélecteur de vue pour le planning (Semaine, Mois)
 */
@Composable
fun PlanningViewSelector(
    currentView: PlanningViewType,
    onViewSelected: (PlanningViewType) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(DarkSurface)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        PlanningViewTab(
            text = "Semaine",
            icon = Icons.Default.ViewWeek,
            isSelected = currentView == PlanningViewType.WEEK,
            onClick = { onViewSelected(PlanningViewType.WEEK) },
            modifier = Modifier.weight(1f)
        )
        PlanningViewTab(
            text = "Mois",
            icon = Icons.Default.CalendarMonth,
            isSelected = currentView == PlanningViewType.MONTH,
            onClick = { onViewSelected(PlanningViewType.MONTH) },
            modifier = Modifier.weight(1f)
        )
    }
}

/**
 * Tab individuel du sélecteur de vue
 */
@Composable
fun PlanningViewTab(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) AccentBlue else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = if (isSelected) Color.White else Color.White.copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall,
                color = if (isSelected) Color.White else Color.White.copy(alpha = 0.6f),
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}

/**
 * Vue Semaine - Affiche les 7 jours avec les événements et navigation
 */
@Composable
fun PlanningWeekView(
    events: List<Event>,
    weekOffset: Int,
    onPreviousWeek: () -> Unit,
    onNextWeek: () -> Unit,
    onEventClick: (Event) -> Unit
) {
    val baseCalendar = Calendar.getInstance()
    baseCalendar.add(Calendar.WEEK_OF_YEAR, weekOffset)
    
    // Génère les 7 jours de la semaine
    val weekDays = (0..6).map { offset ->
        val day = baseCalendar.clone() as Calendar
        day.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        day.add(Calendar.DAY_OF_MONTH, offset)
        day
    }
    
    // Titre de la période
    val firstDay = weekDays.first()
    val lastDay = weekDays.last()
    val periodTitle = if (firstDay.get(Calendar.MONTH) == lastDay.get(Calendar.MONTH)) {
        "${firstDay.get(Calendar.DAY_OF_MONTH)} - ${lastDay.get(Calendar.DAY_OF_MONTH)} ${SimpleDateFormat("MMMM yyyy", Locale.FRENCH).format(lastDay.time).replaceFirstChar { it.uppercase() }}"
    } else {
        "${firstDay.get(Calendar.DAY_OF_MONTH)} ${SimpleDateFormat("MMM", Locale.FRENCH).format(firstDay.time)} - ${lastDay.get(Calendar.DAY_OF_MONTH)} ${SimpleDateFormat("MMM yyyy", Locale.FRENCH).format(lastDay.time)}"
    }
    
    Column(modifier = Modifier.fillMaxSize()) {
        // En-tête avec navigation
        PlanningNavigationHeader(
            title = periodTitle,
            onPrevious = onPreviousWeek,
            onNext = onNextWeek,
            showTodayButton = weekOffset != 0,
            onToday = { /* Reset via parent */ }
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(weekDays) { day ->
                val dayEvents = getEventsForDay(events, day)
                WeekDayCard(
                    calendar = day,
                    isToday = isSameDay(day, Calendar.getInstance()),
                    events = dayEvents,
                    onEventClick = onEventClick
                )
            }
        }
    }
}

/**
 * En-tête de navigation pour les vues planning
 */
@Composable
fun PlanningNavigationHeader(
    title: String,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    showTodayButton: Boolean = false,
    onToday: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(DarkSurface)
            .padding(8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Bouton précédent
        IconButton(
            onClick = onPrevious,
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                imageVector = Icons.Default.ChevronLeft,
                contentDescription = "Précédent",
                tint = AccentBlue
            )
        }
        
        // Titre de la période
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = TextPrimary,
            fontWeight = FontWeight.Bold
        )
        
        // Bouton suivant
        IconButton(
            onClick = onNext,
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Suivant",
                tint = AccentBlue
            )
        }
    }
}

/**
 * Carte d'un jour dans la vue semaine
 */
@Composable
fun WeekDayCard(
    calendar: Calendar,
    isToday: Boolean,
    events: List<Event>,
    onEventClick: (Event) -> Unit
) {
    val dayName = SimpleDateFormat("EEEE", Locale.FRENCH).format(calendar.time)
        .replaceFirstChar { it.uppercase() }
    val dayNumber = calendar.get(Calendar.DAY_OF_MONTH)
    val monthName = SimpleDateFormat("MMMM", Locale.FRENCH).format(calendar.time)
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (isToday) AccentBlue.copy(alpha = 0.15f) else DarkSurface)
            .padding(12.dp)
    ) {
        // En-tête du jour
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = dayName,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (isToday) AccentBlue else TextPrimary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "$dayNumber $monthName",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
            if (isToday) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = AccentBlue
                ) {
                    Text(
                        text = "Aujourd'hui",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Événements du jour
        if (events.isEmpty()) {
            Text(
                text = "Aucun événement",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary.copy(alpha = 0.6f),
                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
            )
        } else {
            events.forEach { event ->
                WeekEventItem(
                    event = event,
                    onClick = { onEventClick(event) }
                )
                Spacer(modifier = Modifier.height(4.dp))
            }
        }
    }
}

/**
 * Item d'événement compact pour la vue semaine
 */
@Composable
fun WeekEventItem(
    event: Event,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(DarkSurfaceVariant)
            .clickable(onClick = onClick)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Indicateur de couleur
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(32.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(AccentBlue)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = event.title,
                style = MaterialTheme.typography.bodyMedium,
                color = TextPrimary,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = if (event.startTime == "Toute la journée") "Toute la journée" else "${event.startTime} - ${event.endTime}",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }
    }
}

/**
 * Vue Mois - Affiche le calendrier du mois avec navigation
 */
@Composable
fun PlanningMonthView(
    events: List<Event>,
    monthOffset: Int,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onEventClick: (Event) -> Unit
) {
    val calendar = Calendar.getInstance()
    calendar.add(Calendar.MONTH, monthOffset)
    val displayMonth = calendar.get(Calendar.MONTH)
    val displayYear = calendar.get(Calendar.YEAR)
    
    // Jour actuel (pour highlight)
    val todayCal = Calendar.getInstance()
    val isCurrentMonth = todayCal.get(Calendar.MONTH) == displayMonth && todayCal.get(Calendar.YEAR) == displayYear
    val today = if (isCurrentMonth) todayCal.get(Calendar.DAY_OF_MONTH) else -1
    
    // Premier jour du mois
    calendar.set(Calendar.DAY_OF_MONTH, 1)
    val firstDayOfWeek = (calendar.get(Calendar.DAY_OF_WEEK) + 5) % 7 // Lundi = 0
    val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
    
    val monthName = SimpleDateFormat("MMMM yyyy", Locale.FRENCH).format(calendar.time)
        .replaceFirstChar { it.uppercase() }
    
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // En-tête avec navigation
        PlanningNavigationHeader(
            title = monthName,
            onPrevious = onPreviousMonth,
            onNext = onNextMonth
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Jours de la semaine
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            listOf("Lun", "Mar", "Mer", "Jeu", "Ven", "Sam", "Dim").forEach { dayName ->
                Text(
                    text = dayName,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    modifier = Modifier.weight(1f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Grille du calendrier
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            val totalCells = firstDayOfWeek + daysInMonth
            val weeks = (totalCells + 6) / 7
            
            items(weeks) { week ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    for (dayOfWeek in 0..6) {
                        val cellIndex = week * 7 + dayOfWeek
                        val dayNumber = cellIndex - firstDayOfWeek + 1
                        
                        if (dayNumber in 1..daysInMonth) {
                            val dayCal = Calendar.getInstance()
                            dayCal.set(displayYear, displayMonth, dayNumber)
                            val dayEvents = getEventsForDay(events, dayCal)
                            
                            MonthDayCell(
                                dayNumber = dayNumber,
                                isToday = dayNumber == today,
                                events = dayEvents,
                                onEventClick = onEventClick,
                                modifier = Modifier.weight(1f)
                            )
                        } else {
                            // Case vide
                            Box(modifier = Modifier.weight(1f).height(80.dp))
                        }
                    }
                }
            }
        }
    }
}

/**
 * Cellule d'un jour dans la vue mois
 */
@Composable
fun MonthDayCell(
    dayNumber: Int,
    isToday: Boolean,
    events: List<Event>,
    onEventClick: (Event) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .height(80.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (isToday) AccentBlue.copy(alpha = 0.2f) else DarkSurface)
            .padding(4.dp)
    ) {
        // Numéro du jour
        Text(
            text = dayNumber.toString(),
            style = MaterialTheme.typography.labelMedium,
            color = if (isToday) AccentBlue else TextPrimary,
            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
        
        Spacer(modifier = Modifier.height(2.dp))
        
        // Événements (max 2 + indicateur "+" si plus)
        events.take(2).forEach { event ->
            Text(
                text = event.title,
                style = MaterialTheme.typography.labelSmall,
                color = AccentBlue,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontSize = 9.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(2.dp))
                    .background(AccentBlue.copy(alpha = 0.2f))
                    .clickable { onEventClick(event) }
                    .padding(horizontal = 2.dp, vertical = 1.dp)
            )
            Spacer(modifier = Modifier.height(1.dp))
        }
        
        if (events.size > 2) {
            Text(
                text = "+${events.size - 2}",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
                fontSize = 8.sp
            )
        }
    }
}

/**
 * Vérifie si deux Calendar représentent le même jour
 */
private fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
            cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
}

/**
 * Récupère les événements pour un jour donné
 */
private fun getEventsForDay(events: List<Event>, calendar: Calendar): List<Event> {
    val dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH)
    val month = calendar.get(Calendar.MONTH) + 1
    val year = calendar.get(Calendar.YEAR)
    
    return events.filter { event ->
        // Parse la date de l'événement (format "Dim. 30 novembre" ou similaire)
        try {
            val dateParts = event.date.split(" ")
            if (dateParts.size >= 3) {
                val eventDay = dateParts[1].toIntOrNull() ?: return@filter false
                val eventMonth = getMonthNumber(dateParts[2])
                // On suppose l'année courante si non spécifiée
                eventDay == dayOfMonth && eventMonth == month
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }
}

/**
 * Convertit le nom du mois en numéro (1-12)
 */
private fun getMonthNumber(monthName: String): Int {
    val months = mapOf(
        "janvier" to 1, "février" to 2, "mars" to 3, "avril" to 4,
        "mai" to 5, "juin" to 6, "juillet" to 7, "août" to 8,
        "septembre" to 9, "octobre" to 10, "novembre" to 11, "décembre" to 12
    )
    return months[monthName.lowercase()] ?: 0
}

/**
 * Mini calendrier affichant les 7 jours de la semaine
 * Le jour actuel est mis en évidence
 */
@Composable
fun WeekCalendar() {
    val calendar = Calendar.getInstance()
    val today = calendar.get(Calendar.DAY_OF_MONTH)

    // Calcule le lundi de la semaine actuelle
    calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
    val weekDays = (0..6).map { offset ->
        val day = calendar.clone() as Calendar
        day.add(Calendar.DAY_OF_MONTH, offset)
        day
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            weekDays.forEach { day ->
                val dayOfMonth = day.get(Calendar.DAY_OF_MONTH)
                val dayName = SimpleDateFormat("EEE", Locale.FRENCH)
                    .format(day.time)
                    .uppercase()

                CalendarDayItem(
                    dayName = dayName,
                    dayNumber = dayOfMonth,
                    isToday = dayOfMonth == today
                )
            }
        }
    }
}

/**
 * Item d'un jour dans le calendrier
 */
@Composable
fun CalendarDayItem(
    dayName: String,
    dayNumber: Int,
    isToday: Boolean
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(40.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (isToday) AccentBlue else Color.Transparent)
            .padding(vertical = 8.dp)
    ) {
        Text(
            text = dayName,
            style = MaterialTheme.typography.labelSmall,
            color = if (isToday) Color.White else TextSecondary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = dayNumber.toString(),
            style = MaterialTheme.typography.bodyMedium,
            color = if (isToday) Color.White else TextPrimary,
            fontWeight = FontWeight.SemiBold
        )
    }
}

/**
 * Item compact d'une tâche (vue liste)
 * Affiche le titre, deadline et badge de statut/priorité
 */
@Composable
fun TaskItemCompact(
    task: Task,
    onClick: () -> Unit,
    onStatusChange: (TaskStatus) -> Unit,
    onSubTaskToggle: (subTaskId: String, text: String, isCompleted: Boolean) -> Unit = { _, _, _ -> }
) {
    // État pour afficher le dialog des sous-tâches
    var showSubTasksDialog by remember { mutableStateOf(false) }
    
    // Couleurs de priorité
    val priorityText = when (task.priority) {
        TaskPriority.P1 -> "P1"
        TaskPriority.P2 -> "P2"
        TaskPriority.P3 -> "P3"
        TaskPriority.P4 -> "P4"
        TaskPriority.P5 -> "P5"
    }
    val priorityColor = when (task.priority) {
        TaskPriority.P1 -> UrgentRed
        TaskPriority.P2 -> Color(0xFFFF6B35)
        TaskPriority.P3 -> NormalOrange
        TaskPriority.P4 -> Color(0xFFFFB84D)
        TaskPriority.P5 -> CompletedGreen
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(DarkSurface)
            .clickable(onClick = onClick)
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            // Première ligne : Tag priorité + Titre (sur 2 lignes max)
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Top
            ) {
                // Tag de priorité
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = priorityColor.copy(alpha = 0.2f),
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    Text(
                        text = priorityText,
                        color = priorityColor,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                
                // Titre (jusqu'à 2 lignes)
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = TextPrimary,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            Spacer(modifier = Modifier.height(6.dp))
            
            // Ligne suivante : Catégorie + Échéance
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Catégorie si disponible (en premier)
                if (task.category.isNotEmpty()) {
                    Icon(
                        imageVector = Icons.Default.Category,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = TextSecondary
                    )
                    Text(
                        text = task.category,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                    Text(
                        text = "•",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }

                // Icône deadline (en second)
                Icon(
                    imageVector = Icons.Default.Schedule,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = TextSecondary
                )
                Text(
                    text = task.deadline,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        }
        
        // Compteur de sous-tâches (si la tâche en a) - cliquable pour ouvrir le dialog
        if (task.subTasks.isNotEmpty()) {
            val completedCount = task.subTasks.count { it.isCompleted }
            val totalCount = task.subTasks.size
            Text(
                text = "$completedCount/$totalCount",
                style = MaterialTheme.typography.bodySmall,
                color = AccentBlue,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .padding(start = 8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .clickable { showSubTasksDialog = true }
                    .padding(4.dp)
            )
        }
        
        // Dialog des sous-tâches
        if (showSubTasksDialog) {
            SubTasksQuickDialog(
                task = task,
                onDismiss = { showSubTasksDialog = false },
                onSubTaskToggle = onSubTaskToggle
            )
        }
        
        // Icône de statut à droite (cliquable pour changer le statut)
        val (statusIcon, statusColor, statusDescription) = when (task.status) {
            TaskStatus.TODO -> Triple(Icons.Outlined.Circle, TextSecondary, "À faire")
            TaskStatus.IN_PROGRESS -> Triple(Icons.Outlined.HourglassBottom, NormalOrange, "En cours")
            TaskStatus.COMPLETED -> Triple(Icons.Default.CheckCircle, CompletedGreen, "Terminé")
        }
        
        // Calculer le prochain statut (cycle: TODO -> IN_PROGRESS -> COMPLETED -> TODO)
        val nextStatus = when (task.status) {
            TaskStatus.TODO -> TaskStatus.IN_PROGRESS
            TaskStatus.IN_PROGRESS -> TaskStatus.COMPLETED
            TaskStatus.COMPLETED -> TaskStatus.TODO
        }
        
        Icon(
            imageVector = statusIcon,
            contentDescription = statusDescription,
            modifier = Modifier
                .padding(start = 12.dp)
                .size(24.dp)
                .clickable { onStatusChange(nextStatus) },
            tint = statusColor
        )
    }
}

/**
 * Dialog rapide pour cocher/décocher les sous-tâches
 */
@Composable
fun SubTasksQuickDialog(
    task: Task,
    onDismiss: () -> Unit,
    onSubTaskToggle: (subTaskId: String, text: String, isCompleted: Boolean) -> Unit
) {
    // État local des sous-tâches pour mise à jour immédiate
    var localSubTasks by remember(task.subTasks) { mutableStateOf(task.subTasks) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF2C2C2E),
        title = {
            Column {
                Text(
                    text = task.title,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                val completedCount = localSubTasks.count { it.isCompleted }
                Text(
                    text = "$completedCount/${localSubTasks.size} sous-tâches terminées",
                    color = AccentBlue,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 300.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                localSubTasks.forEachIndexed { index, subTask ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White.copy(alpha = 0.05f))
                            .clickable {
                                val newCompleted = !subTask.isCompleted
                                // Mise à jour locale immédiate
                                localSubTasks = localSubTasks.toMutableList().apply {
                                    this[index] = subTask.copy(isCompleted = newCompleted)
                                }
                                // Appeler l'API
                                if (subTask.id.isNotEmpty()) {
                                    onSubTaskToggle(subTask.id, subTask.text, newCompleted)
                                }
                            }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = subTask.isCompleted,
                            onCheckedChange = { checked ->
                                // Mise à jour locale immédiate
                                localSubTasks = localSubTasks.toMutableList().apply {
                                    this[index] = subTask.copy(isCompleted = checked)
                                }
                                // Appeler l'API
                                if (subTask.id.isNotEmpty()) {
                                    onSubTaskToggle(subTask.id, subTask.text, checked)
                                }
                            },
                            colors = CheckboxDefaults.colors(
                                checkedColor = AccentBlue,
                                uncheckedColor = Color.White.copy(alpha = 0.5f),
                                checkmarkColor = Color.White
                            )
                        )
                        Text(
                            text = subTask.text,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (subTask.isCompleted) Color.White.copy(alpha = 0.5f) else Color.White,
                            textDecoration = if (subTask.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Fermer", color = AccentBlue)
            }
        }
    )
}

/**
 * Bottom sheet de sélection du statut
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatusSelectionDialog(
    currentStatus: TaskStatus,
    onDismiss: () -> Unit,
    onStatusSelected: (TaskStatus) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1C1C1E),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 4.dp)
                    .width(36.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.White.copy(alpha = 0.3f))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Changer le statut",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            TaskStatus.values().forEach { status ->
                val statusText = when (status) {
                    TaskStatus.TODO -> "À faire"
                    TaskStatus.IN_PROGRESS -> "En cours"
                    TaskStatus.COMPLETED -> "Terminé"
                }
                val statusColor = when (status) {
                    TaskStatus.COMPLETED -> CompletedGreen
                    TaskStatus.IN_PROGRESS -> AccentBlue
                    else -> NormalOrange
                }
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onStatusSelected(status)
                            onDismiss()
                        },
                    shape = RoundedCornerShape(12.dp),
                    color = if (status == currentStatus) statusColor.copy(alpha = 0.2f) else Color(0xFF2C2C2E)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(statusColor)
                            )
                            Text(
                                text = statusText,
                                color = Color.White,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = if (status == currentStatus) FontWeight.SemiBold else FontWeight.Normal
                            )
                        }
                        if (status == currentStatus) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = statusColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * BottomSheet de sélection de la priorité
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrioritySelectionDialog(
    currentPriority: TaskPriority,
    onDismiss: () -> Unit,
    onPrioritySelected: (TaskPriority) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF1C1C1E),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 8.dp)
                    .size(width = 36.dp, height = 4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.White.copy(alpha = 0.3f))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Changer la priorité",
                color = Color.White,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            TaskPriority.values().forEach { priority ->
                val priorityText = when (priority) {
                    TaskPriority.P1 -> "P1 — Urgent"
                    TaskPriority.P2 -> "P2 — Haute"
                    TaskPriority.P3 -> "P3 — Normale"
                    TaskPriority.P4 -> "P4 — Basse"
                    TaskPriority.P5 -> "P5 — Optionnelle"
                }
                val priorityColor = when (priority) {
                    TaskPriority.P1 -> UrgentRed
                    TaskPriority.P2 -> Color(0xFFFF6B35)
                    TaskPriority.P3 -> NormalOrange
                    TaskPriority.P4 -> Color(0xFFFFB84D)
                    TaskPriority.P5 -> CompletedGreen
                }
                val isSelected = priority == currentPriority

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onPrioritySelected(priority)
                            onDismiss()
                        },
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSelected) priorityColor.copy(alpha = 0.2f) else Color(0xFF2C2C2E)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(priorityColor)
                            )
                            Text(
                                text = priorityText,
                                color = if (isSelected) Color.White else Color.White.copy(alpha = 0.8f),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                            )
                        }
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = priorityColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * BottomSheet de sélection de catégorie
 * Permet de choisir parmi les catégories existantes ou d'en créer une nouvelle
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CategorySelectionDialog(
    currentCategory: String,
    categories: List<String>,
    onDismiss: () -> Unit,
    onCategorySelected: (String) -> Unit
) {
    var showNewCategoryInput by remember { mutableStateOf(false) }
    var newCategoryText by remember { mutableStateOf("") }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF1C1C1E),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 8.dp)
                    .size(width = 36.dp, height = 4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.White.copy(alpha = 0.3f))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Changer la catégorie",
                color = Color.White,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 4.dp)
            )

            // Section catégories existantes
            if (categories.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF2C2C2E))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Catégories existantes",
                        color = Color.White.copy(alpha = 0.6f),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        categories.forEach { category ->
                            FilterChip(
                                selected = currentCategory == category,
                                onClick = { onCategorySelected(category) },
                                label = { Text(category) },
                                leadingIcon = if (currentCategory == category) {
                                    {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                } else null,
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = AccentBlue,
                                    selectedLabelColor = Color.White,
                                    selectedLeadingIconColor = Color.White,
                                    containerColor = Color(0xFF3A3A3C),
                                    labelColor = Color.White.copy(alpha = 0.8f)
                                )
                            )
                        }
                    }
                }
            }

            // Section nouvelle catégorie
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF2C2C2E))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Nouvelle catégorie",
                    color = Color.White.copy(alpha = 0.6f),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )

                if (!showNewCategoryInput) {
                    OutlinedButton(
                        onClick = { showNewCategoryInput = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentBlue),
                        border = BorderStroke(1.dp, AccentBlue.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Créer une catégorie")
                    }
                } else {
                    OutlinedTextField(
                        value = newCategoryText,
                        onValueChange = { newCategoryText = it },
                        placeholder = {
                            Text(
                                "Nom de la catégorie",
                                color = Color.White.copy(alpha = 0.5f)
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = AccentBlue,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                            cursorColor = AccentBlue
                        ),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedButton(
                            onClick = {
                                showNewCategoryInput = false
                                newCategoryText = ""
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color.White.copy(alpha = 0.7f)
                            ),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.3f)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Annuler")
                        }

                        Button(
                            onClick = {
                                if (newCategoryText.isNotBlank()) {
                                    onCategorySelected(newCategoryText.trim())
                                }
                            },
                            modifier = Modifier.weight(1f),
                            enabled = newCategoryText.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AccentBlue,
                                disabledContainerColor = AccentBlue.copy(alpha = 0.3f)
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Valider", color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Dialog de saisie de la durée estimée
 */
@Composable
fun DurationInputDialog(
    currentDuration: String,
    onDismiss: () -> Unit,
    onDurationChanged: (String) -> Unit
) {
    var hours by remember { mutableStateOf("") }
    var minutes by remember { mutableStateOf("") }
    var selectedUnit by remember { mutableStateOf(0) } // 0 = heures, 1 = minutes

    // Parse la durée actuelle pour pré-remplir les champs
    LaunchedEffect(currentDuration) {
        val regex = """(\d+)\s*(h|heure|heures|m|min|minute|minutes)""".toRegex(RegexOption.IGNORE_CASE)
        val match = regex.find(currentDuration)
        if (match != null) {
            val value = match.groupValues[1]
            val unit = match.groupValues[2].lowercase()
            if (unit.startsWith("h")) {
                hours = value
                selectedUnit = 0
            } else {
                minutes = value
                selectedUnit = 1
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF2C2C2E),
        title = {
            Text(
                text = "Durée estimée",
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // Sélecteur d'unité
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF1C1C1E), RoundedCornerShape(8.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf("Heures" to 0, "Minutes" to 1).forEach { (label, index) ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (selectedUnit == index) AccentBlue else Color.Transparent)
                                .clickable { selectedUnit = index }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                color = if (selectedUnit == index) Color.White else Color.White.copy(alpha = 0.6f),
                                fontWeight = if (selectedUnit == index) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }

                // Champ de saisie
                OutlinedTextField(
                    value = if (selectedUnit == 0) hours else minutes,
                    onValueChange = { value ->
                        // N'autoriser que les chiffres
                        if (value.all { it.isDigit() }) {
                            if (selectedUnit == 0) {
                                hours = value
                            } else {
                                minutes = value
                            }
                        }
                    },
                    label = { Text(if (selectedUnit == 0) "Nombre d'heures" else "Nombre de minutes") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AccentBlue,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                        focusedLabelColor = AccentBlue,
                        unfocusedLabelColor = Color.White.copy(alpha = 0.6f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val value = if (selectedUnit == 0) hours else minutes
                    if (value.isNotEmpty()) {
                        val duration = if (selectedUnit == 0) "${value}h" else "${value}min"
                        onDurationChanged(duration)
                    }
                    onDismiss()
                }
            ) {
                Text("Valider", color = AccentBlue, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler", color = Color.White.copy(alpha = 0.7f))
            }
        }
    )
}

/**
 * Dialog de sélection de date d'échéance
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeadlineDatePickerDialog(
    currentDeadlineDate: String,
    onDismiss: () -> Unit,
    onDateSelected: (String) -> Unit
) {
    // Parser la date actuelle pour initialiser le DatePicker
    val initialDateMillis = remember(currentDeadlineDate) {
        try {
            if (currentDeadlineDate.isNotEmpty()) {
                val parts = currentDeadlineDate.split("-")
                val year = parts[0].toInt()
                val month = parts[1].toInt()
                val day = parts[2].toInt()
                java.util.Calendar.getInstance().apply {
                    set(year, month - 1, day, 12, 0, 0)
                    set(java.util.Calendar.MILLISECOND, 0)
                }.timeInMillis
            } else {
                System.currentTimeMillis()
            }
        } catch (e: Exception) {
            System.currentTimeMillis()
        }
    }

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialDateMillis
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val calendar = java.util.Calendar.getInstance().apply {
                            timeInMillis = millis
                        }
                        val year = calendar.get(java.util.Calendar.YEAR)
                        val month = calendar.get(java.util.Calendar.MONTH) + 1
                        val day = calendar.get(java.util.Calendar.DAY_OF_MONTH)
                        val isoDate = String.format("%04d-%02d-%02d", year, month, day)
                        onDateSelected(isoDate)
                    }
                    onDismiss()
                }
            ) {
                Text("Valider", color = AccentBlue, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler", color = Color.White.copy(alpha = 0.7f))
            }
        },
        colors = DatePickerDefaults.colors(
            containerColor = Color(0xFF2C2C2E)
        )
    ) {
        DatePicker(
            state = datePickerState,
            showModeToggle = false,
            colors = DatePickerDefaults.colors(
                containerColor = Color(0xFF2C2C2E),
                titleContentColor = Color.White,
                headlineContentColor = Color.White,
                weekdayContentColor = Color.White.copy(alpha = 0.7f),
                subheadContentColor = Color.White.copy(alpha = 0.7f),
                yearContentColor = Color.White,
                currentYearContentColor = AccentBlue,
                selectedYearContentColor = Color.White,
                selectedYearContainerColor = AccentBlue,
                dayContentColor = Color.White,
                selectedDayContentColor = Color.White,
                selectedDayContainerColor = AccentBlue,
                todayContentColor = AccentBlue,
                todayDateBorderColor = AccentBlue,
                navigationContentColor = Color.White
            )
        )
    }
}

/**
 * Dialog affichant tous les détails d'une tâche (version moderne)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskDetailsDialog(
    task: Task,
    categories: List<String>,
    onDismiss: () -> Unit,
    onStatusChange: (TaskStatus) -> Unit,
    onPriorityChange: (TaskPriority) -> Unit,
    onDurationChange: (String) -> Unit,
    onDeadlineChange: (String) -> Unit,
    onCategoryChange: (String) -> Unit,
    onTitleChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onNoteChange: (String) -> Unit,
    onSubTaskCreate: (text: String) -> Unit,
    onSubTaskUpdate: (subTaskId: String, text: String, isCompleted: Boolean) -> Unit,
    onSubTaskDelete: (subTaskId: String) -> Unit,
    onDelete: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var showStatusDialog by remember { mutableStateOf(false) }
    var showPriorityDialog by remember { mutableStateOf(false) }
    var showDurationDialog by remember { mutableStateOf(false) }
    var showDeadlineDialog by remember { mutableStateOf(false) }
    var showCategoryDialog by remember { mutableStateOf(false) }
    
    // États pour les sous-tâches (initialisées depuis la tâche)
    var subTasks by remember(task.subTasks) { mutableStateOf(task.subTasks) }
    var newSubTaskText by remember { mutableStateOf("") }
    val subTaskFocusRequester = remember { FocusRequester() }
    
    // États pour l'édition d'une sous-tâche
    var editingSubTaskIndex by remember { mutableStateOf<Int?>(null) }
    var editingSubTaskText by remember { mutableStateOf("") }
    
    // États pour l'édition inline
    var isEditingTitle by remember { mutableStateOf(false) }
    var editedTitle by remember(task.title) { mutableStateOf(task.title) }
    var isEditingDescription by remember { mutableStateOf(false) }
    var editedDescription by remember(task.description) { mutableStateOf(task.description) }
    
    // FocusRequesters pour ouvrir le clavier automatiquement
    val titleFocusRequester = remember { FocusRequester() }
    val descriptionFocusRequester = remember { FocusRequester() }
    
    // Demander le focus quand on passe en mode édition
    LaunchedEffect(isEditingTitle) {
        if (isEditingTitle) {
            titleFocusRequester.requestFocus()
        }
    }
    LaunchedEffect(isEditingDescription) {
        if (isEditingDescription) {
            descriptionFocusRequester.requestFocus()
        }
    }

    // Dialog de confirmation de suppression
    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            containerColor = Color(0xFF2C2C2E),
            title = {
                Text(
                    text = "Supprimer la tâche ?",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Cette action est irréversible. Voulez-vous vraiment supprimer cette tâche ?",
                    color = Color.White.copy(alpha = 0.8f),
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirmation = false
                        onDelete()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = UrgentRed
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Supprimer", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showDeleteConfirmation = false },
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color.White
                    ),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.3f)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Annuler", color = Color.White.copy(alpha = 0.8f))
                }
            }
        )
    }

    // Dialogs de modification
    if (showStatusDialog) {
        StatusSelectionDialog(
            currentStatus = task.status,
            onDismiss = { showStatusDialog = false },
            onStatusSelected = { newStatus ->
                onStatusChange(newStatus)
                showStatusDialog = false
            }
        )
    }

    if (showPriorityDialog) {
        PrioritySelectionDialog(
            currentPriority = task.priority,
            onDismiss = { showPriorityDialog = false },
            onPrioritySelected = { newPriority ->
                onPriorityChange(newPriority)
                showPriorityDialog = false
            }
        )
    }

    if (showDurationDialog) {
        DurationInputDialog(
            currentDuration = task.estimatedDuration,
            onDismiss = { showDurationDialog = false },
            onDurationChanged = { newDuration ->
                onDurationChange(newDuration)
                showDurationDialog = false
            }
        )
    }

    if (showDeadlineDialog) {
        DeadlineDatePickerDialog(
            currentDeadlineDate = task.deadlineDate,
            onDismiss = { showDeadlineDialog = false },
            onDateSelected = { newDate ->
                onDeadlineChange(newDate)
                showDeadlineDialog = false
            }
        )
    }

    if (showCategoryDialog) {
        CategorySelectionDialog(
            currentCategory = task.category,
            categories = categories,
            onDismiss = { showCategoryDialog = false },
            onCategorySelected = { newCategory ->
                onCategoryChange(newCategory)
                showCategoryDialog = false
            }
        )
    }

    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss
    ) {
        // Conteneur principal avec gradient de fond (largeur augmentée)
        Box(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .clip(RoundedCornerShape(24.dp))
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF1C1C1E),
                            Color(0xFF2C2C2E)
                        )
                    )
                )
        ) {
                Column(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // En-tête avec titre et icônes d'action
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        // Titre éditable
                        if (isEditingTitle) {
                            OutlinedTextField(
                                value = editedTitle,
                                onValueChange = { editedTitle = it },
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 8.dp)
                                    .focusRequester(titleFocusRequester),
                                textStyle = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                ),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = AccentBlue,
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                                    cursorColor = AccentBlue
                                ),
                                shape = RoundedCornerShape(8.dp),
                                singleLine = false,
                                maxLines = 3,
                                trailingIcon = {
                                    IconButton(
                                        onClick = {
                                            if (editedTitle.isNotBlank() && editedTitle != task.title) {
                                                onTitleChange(editedTitle.trim())
                                            }
                                            isEditingTitle = false
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Valider",
                                            tint = AccentBlue
                                        )
                                    }
                                }
                            )
                        } else {
                            Text(
                                text = task.title,
                                color = Color.White,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { isEditingTitle = true }
                                    .padding(end = 8.dp, top = 4.dp)
                            )
                        }

                        // Icônes d'action (plus hautes et plus compactes)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(0.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            // Icône supprimer
                            IconButton(
                                onClick = { showDeleteConfirmation = true },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Supprimer",
                                    tint = Color(0xFFB71C1C),
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            // Icône fermer
                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Fermer",
                                    tint = Color(0xFFC62828),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    // Métadonnées avec badges modernes (clickable pour changer statut, priorité, durée, date, catégorie)
                    ModernCompactMetadata(
                        task = task,
                        onStatusClick = { showStatusDialog = true },
                        onPriorityClick = { showPriorityDialog = true },
                        onDurationClick = { showDurationDialog = true },
                        onDeadlineClick = { showDeadlineDialog = true },
                        onCategoryClick = { showCategoryDialog = true }
                    )

                    // Séparateur élégant
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color.White.copy(alpha = 0.1f),
                                        Color.Transparent
                                    )
                                )
                            )
                    )

                    // Onglets Description / Notes / Sous-tâches
                    ModernTaskContentTabSelector(
                        selectedIndex = selectedTab,
                        onTabSelected = { selectedTab = it },
                        hasDescription = task.description.isNotEmpty(),
                        hasNote = task.note.isNotEmpty(),
                        showSubTasks = true
                    )

                    // Contenu avec fond subtil
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF1A1A1C))
                            .padding(12.dp)
                    ) {
                        when (selectedTab) {
                            0 -> {
                                // Description éditable
                                if (isEditingDescription) {
                                    Column {
                                        OutlinedTextField(
                                            value = editedDescription,
                                            onValueChange = { editedDescription = it },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .focusRequester(descriptionFocusRequester),
                                            textStyle = MaterialTheme.typography.bodyMedium.copy(
                                                color = Color.White.copy(alpha = 0.9f),
                                                lineHeight = 22.sp
                                            ),
                                            placeholder = {
                                                Text(
                                                    "Ajouter une description...",
                                                    color = Color.White.copy(alpha = 0.5f)
                                                )
                                            },
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = AccentBlue,
                                                unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                                                cursorColor = AccentBlue
                                            ),
                                            shape = RoundedCornerShape(8.dp),
                                            minLines = 3,
                                            maxLines = 6
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Row(
                                            horizontalArrangement = Arrangement.End,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            TextButton(
                                                onClick = {
                                                    editedDescription = task.description
                                                    isEditingDescription = false
                                                }
                                            ) {
                                                Text("Annuler", color = Color.White.copy(alpha = 0.7f))
                                            }
                                            TextButton(
                                                onClick = {
                                                    if (editedDescription != task.description) {
                                                        onDescriptionChange(editedDescription.trim())
                                                    }
                                                    isEditingDescription = false
                                                }
                                            ) {
                                                Text("Valider", color = AccentBlue, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                } else {
                                    Text(
                                        text = if (task.description.isNotEmpty()) task.description else "Appuyez pour ajouter une description...",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = if (task.description.isNotEmpty()) Color.White.copy(alpha = 0.9f) else Color.White.copy(alpha = 0.5f),
                                        lineHeight = 22.sp,
                                        fontStyle = if (task.description.isEmpty()) androidx.compose.ui.text.font.FontStyle.Italic else androidx.compose.ui.text.font.FontStyle.Normal,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { isEditingDescription = true }
                                    )
                                }
                            }
                            1 -> {
                                // Sous-tâches
                                Column(
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    // Champ d'ajout de sous-tâche
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedTextField(
                                            value = newSubTaskText,
                                            onValueChange = { newSubTaskText = it },
                                            modifier = Modifier.weight(1f),
                                            placeholder = {
                                                Text(
                                                    "Nouvelle sous-tâche...",
                                                    color = Color.White.copy(alpha = 0.5f)
                                                )
                                            },
                                            textStyle = MaterialTheme.typography.bodyMedium.copy(
                                                color = Color.White
                                            ),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = AccentBlue,
                                                unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                                                cursorColor = AccentBlue
                                            ),
                                            shape = RoundedCornerShape(8.dp),
                                            singleLine = true,
                                            keyboardOptions = KeyboardOptions(
                                                capitalization = KeyboardCapitalization.Sentences
                                            )
                                        )
                                        IconButton(
                                            onClick = {
                                                if (newSubTaskText.isNotBlank()) {
                                                    // Appeler l'API pour créer la sous-tâche
                                                    onSubTaskCreate(newSubTaskText.trim())
                                                    newSubTaskText = ""
                                                }
                                            },
                                            enabled = newSubTaskText.isNotBlank()
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Add,
                                                contentDescription = "Ajouter",
                                                tint = if (newSubTaskText.isNotBlank()) AccentBlue else Color.White.copy(alpha = 0.3f)
                                            )
                                        }
                                    }

                                    if (subTasks.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(12.dp))
                                        
                                        // Liste des sous-tâches
                                        val subTasksScrollState = rememberScrollState()
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .heightIn(max = 200.dp)
                                                .verticalScroll(subTasksScrollState),
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            subTasks.forEachIndexed { index, subTask ->
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .background(Color.White.copy(alpha = 0.05f))
                                                        .clickable {
                                                            // Mettre à jour l'état local immédiatement pour feedback visuel
                                                            val newCompleted = !subTask.isCompleted
                                                            subTasks = subTasks.toMutableList().apply {
                                                                this[index] = subTask.copy(isCompleted = newCompleted)
                                                            }
                                                            // Appeler l'API pour persister
                                                            if (subTask.id.isNotEmpty()) {
                                                                onSubTaskUpdate(subTask.id, subTask.text, newCompleted)
                                                            }
                                                        }
                                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Checkbox(
                                                        checked = subTask.isCompleted,
                                                        onCheckedChange = { checked ->
                                                            // Mettre à jour l'état local immédiatement pour feedback visuel
                                                            subTasks = subTasks.toMutableList().apply {
                                                                this[index] = subTask.copy(isCompleted = checked)
                                                            }
                                                            // Appeler l'API pour persister
                                                            if (subTask.id.isNotEmpty()) {
                                                                onSubTaskUpdate(subTask.id, subTask.text, checked)
                                                            }
                                                        },
                                                        colors = CheckboxDefaults.colors(
                                                            checkedColor = AccentBlue,
                                                            uncheckedColor = Color.White.copy(alpha = 0.5f),
                                                            checkmarkColor = Color.White
                                                        )
                                                    )
                                                    
                                                    // Mode édition ou affichage du texte
                                                    if (editingSubTaskIndex == index) {
                                                        // Mode édition inline
                                                        OutlinedTextField(
                                                            value = editingSubTaskText,
                                                            onValueChange = { editingSubTaskText = it },
                                                            modifier = Modifier
                                                                .weight(1f)
                                                                .padding(end = 4.dp),
                                                            textStyle = MaterialTheme.typography.bodyMedium.copy(
                                                                color = Color.White
                                                            ),
                                                            colors = OutlinedTextFieldDefaults.colors(
                                                                focusedBorderColor = AccentBlue,
                                                                unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                                                                cursorColor = AccentBlue
                                                            ),
                                                            shape = RoundedCornerShape(6.dp),
                                                            singleLine = true,
                                                            keyboardOptions = KeyboardOptions(
                                                                capitalization = KeyboardCapitalization.Sentences
                                                            ),
                                                            trailingIcon = {
                                                                Row {
                                                                    // Bouton valider
                                                                    IconButton(
                                                                        onClick = {
                                                                            if (editingSubTaskText.isNotBlank()) {
                                                                                // Mettre à jour localement
                                                                                subTasks = subTasks.toMutableList().apply {
                                                                                    this[index] = subTask.copy(text = editingSubTaskText.trim())
                                                                                }
                                                                                // Appeler l'API
                                                                                if (subTask.id.isNotEmpty()) {
                                                                                    onSubTaskUpdate(subTask.id, editingSubTaskText.trim(), subTask.isCompleted)
                                                                                }
                                                                            }
                                                                            editingSubTaskIndex = null
                                                                        },
                                                                        modifier = Modifier.size(24.dp)
                                                                    ) {
                                                                        Icon(
                                                                            imageVector = Icons.Default.Check,
                                                                            contentDescription = "Valider",
                                                                            tint = AccentBlue,
                                                                            modifier = Modifier.size(16.dp)
                                                                        )
                                                                    }
                                                                    // Bouton annuler
                                                                    IconButton(
                                                                        onClick = { editingSubTaskIndex = null },
                                                                        modifier = Modifier.size(24.dp)
                                                                    ) {
                                                                        Icon(
                                                                            imageVector = Icons.Default.Close,
                                                                            contentDescription = "Annuler",
                                                                            tint = Color.White.copy(alpha = 0.5f),
                                                                            modifier = Modifier.size(16.dp)
                                                                        )
                                                                    }
                                                                }
                                                            }
                                                        )
                                                    } else {
                                                        // Mode affichage - cliquable pour éditer
                                                        Text(
                                                            text = subTask.text,
                                                            style = MaterialTheme.typography.bodyMedium,
                                                            color = if (subTask.isCompleted) Color.White.copy(alpha = 0.5f) else Color.White,
                                                            textDecoration = if (subTask.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                                                            modifier = Modifier
                                                                .weight(1f)
                                                                .clickable {
                                                                    // Ouvrir le mode édition
                                                                    editingSubTaskText = subTask.text
                                                                    editingSubTaskIndex = index
                                                                }
                                                                .padding(vertical = 4.dp)
                                                        )
                                                    }
                                                    
                                                    // Bouton supprimer (masqué en mode édition)
                                                    if (editingSubTaskIndex != index) {
                                                        IconButton(
                                                            onClick = {
                                                                // Supprimer de l'état local immédiatement
                                                                subTasks = subTasks.toMutableList().apply {
                                                                    removeAt(index)
                                                                }
                                                                // Appeler l'API pour supprimer
                                                                if (subTask.id.isNotEmpty()) {
                                                                    onSubTaskDelete(subTask.id)
                                                                }
                                                            },
                                                            modifier = Modifier.size(32.dp)
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Default.Close,
                                                                contentDescription = "Supprimer",
                                                                tint = Color.White.copy(alpha = 0.5f),
                                                                modifier = Modifier.size(16.dp)
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                        
                                        // Compteur
                                        val completedCount = subTasks.count { it.isCompleted }
                                        Text(
                                            text = "$completedCount/${subTasks.size} terminées",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color.White.copy(alpha = 0.5f),
                                            modifier = Modifier.padding(top = 8.dp)
                                        )
                                    } else {
                                        // Message si aucune sous-tâche
                                        Text(
                                            text = "Aucune sous-tâche",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = Color.White.copy(alpha = 0.5f),
                                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                            modifier = Modifier.padding(top = 12.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
    }
}

/**
 * Composable pour un bouton moderne avec fond coloré
 */
@Composable
fun ModernButton(
    text: String,
    onClick: () -> Unit,
    backgroundColor: Color,
    textColor: Color,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = backgroundColor
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(
            text = text,
            color = textColor,
            fontWeight = FontWeight.SemiBold,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

/**
 * Métadonnées modernes avec badges colorés (cliquables)
 */
@Composable
fun ModernCompactMetadata(
    task: Task,
    onStatusClick: () -> Unit = {},
    onPriorityClick: () -> Unit = {},
    onDurationClick: () -> Unit = {},
    onDeadlineClick: () -> Unit = {},
    onCategoryClick: () -> Unit = {}
) {
    val statusText = when (task.status) {
        TaskStatus.TODO -> "À faire"
        TaskStatus.IN_PROGRESS -> "En cours"
        TaskStatus.COMPLETED -> "Terminé"
    }
    val statusColor = when (task.status) {
        TaskStatus.COMPLETED -> CompletedGreen
        TaskStatus.IN_PROGRESS -> AccentBlue
        else -> NormalOrange
    }

    val priorityText = when (task.priority) {
        TaskPriority.P1 -> "P1"
        TaskPriority.P2 -> "P2"
        TaskPriority.P3 -> "P3"
        TaskPriority.P4 -> "P4"
        TaskPriority.P5 -> "P5"
    }
    val priorityColor = when (task.priority) {
        TaskPriority.P1 -> UrgentRed
        TaskPriority.P2 -> Color(0xFFFF6B35)  // Orange foncé
        TaskPriority.P3 -> NormalOrange
        TaskPriority.P4 -> Color(0xFFFFB84D)  // Jaune
        TaskPriority.P5 -> CompletedGreen
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // Première ligne avec badges
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Badge de statut (cliquable)
            Box(modifier = Modifier.clickable { onStatusClick() }) {
                ModernBadge(text = statusText, color = statusColor)
            }

            // Badge de priorité (cliquable)
            Box(modifier = Modifier.clickable { onPriorityClick() }) {
                ModernBadge(text = priorityText, color = priorityColor)
            }

            // Deadline (cliquable pour modifier la date)
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable { onDeadlineClick() }
                    .padding(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Schedule,
                    contentDescription = null,
                    tint = AccentBlue.copy(alpha = 0.7f),
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = task.deadline,
                    color = AccentBlue.copy(alpha = 0.9f),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // Deuxième ligne (catégorie et durée)
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Catégorie (cliquable)
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable { onCategoryClick() }
                    .padding(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Category,
                    contentDescription = null,
                    tint = AccentBlue.copy(alpha = 0.7f),
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = if (task.category.isNotEmpty()) task.category else "Ajouter catégorie",
                    color = if (task.category.isNotEmpty()) Color.White.copy(alpha = 0.8f) else Color.White.copy(alpha = 0.5f),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium
                )
            }

            if (task.estimatedDuration.isNotEmpty()) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { onDurationClick() }
                ) {
                    Icon(
                        imageVector = Icons.Default.Timer,
                        contentDescription = null,
                        tint = AccentBlue.copy(alpha = 0.7f),
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = task.estimatedDuration,
                        color = Color.White.copy(alpha = 0.8f),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

/**
 * Badge moderne avec fond coloré
 */
@Composable
fun ModernBadge(text: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = text,
            color = color,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold
        )
    }
}

/**
 * Sélecteur d'onglets moderne avec animation
 */
@Composable
fun ModernTaskContentTabSelector(
    selectedIndex: Int,
    onTabSelected: (Int) -> Unit,
    hasDescription: Boolean,
    hasNote: Boolean,
    showSubTasks: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF1A1A1C))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Onglet Description
        ModernTabItem(
            text = "Description",
            isSelected = selectedIndex == 0,
            onClick = { onTabSelected(0) },
            modifier = Modifier.weight(1f)
        )

        // Onglet Sous-tâches (toujours affiché)
        ModernTabItem(
            text = "Sous-tâches",
            isSelected = selectedIndex == 1,
            onClick = { onTabSelected(1) },
            modifier = Modifier.weight(1f)
        )
    }
}

/**
 * Item d'onglet moderne
 */
@Composable
fun ModernTabItem(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (isSelected) AccentBlue else Color.Transparent
            )
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = if (isSelected) Color.White else Color.White.copy(alpha = 0.6f),
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

/**
 * Métadonnées compactes affichées horizontalement
 * Format: "À faire • Normale • 6 jours • Travail • 2h"
 */
@Composable
fun CompactMetadata(task: Task) {
    val statusText = when (task.status) {
        TaskStatus.TODO -> "À faire"
        TaskStatus.IN_PROGRESS -> "En cours"
        TaskStatus.COMPLETED -> "Terminé"
    }
    val statusColor = when (task.status) {
        TaskStatus.COMPLETED -> CompletedGreen
        TaskStatus.IN_PROGRESS -> AccentBlue
        else -> NormalOrange
    }

    val priorityText = when (task.priority) {
        TaskPriority.P1 -> "P1"
        TaskPriority.P2 -> "P2"
        TaskPriority.P3 -> "P3"
        TaskPriority.P4 -> "P4"
        TaskPriority.P5 -> "P5"
    }
    val priorityColor = when (task.priority) {
        TaskPriority.P1 -> UrgentRed
        TaskPriority.P2 -> Color(0xFFFF6B35)  // Orange foncé
        TaskPriority.P3 -> NormalOrange
        TaskPriority.P4 -> Color(0xFFFFB84D)  // Jaune
        TaskPriority.P5 -> CompletedGreen
    }

    // Construction de la chaîne de métadonnées
    val metadata = buildList {
        add(statusText to statusColor)
        add(priorityText to priorityColor)
        add(task.deadline to TextPrimary)
        if (task.category.isNotEmpty()) add(task.category to TextPrimary)
        if (task.estimatedDuration.isNotEmpty()) add(task.estimatedDuration to TextPrimary)
    }

    // Affichage sur deux lignes si nécessaire
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // Première ligne (statut, priorité, deadline)
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            metadata.take(3).forEachIndexed { index, (text, color) ->
                if (index > 0) {
                    Text(
                        text = "•",
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Text(
                    text = text,
                    color = color,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // Deuxième ligne si nécessaire (catégorie, durée)
        if (metadata.size > 3) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                metadata.drop(3).forEachIndexed { index, (text, color) ->
                    if (index > 0) {
                        Text(
                            text = "•",
                            color = TextSecondary,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Text(
                        text = text,
                        color = color,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

/**
 * Sélecteur d'onglets pour Description / Notes
 */
@Composable
fun TaskContentTabSelector(
    selectedIndex: Int,
    onTabSelected: (Int) -> Unit,
    hasDescription: Boolean,
    hasNote: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(DarkSurfaceVariant, RoundedCornerShape(8.dp))
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        if (hasDescription) {
            TaskContentTabItem(
                text = "Description",
                isSelected = selectedIndex == 0,
                onClick = { onTabSelected(0) },
                modifier = Modifier.weight(1f)
            )
        }

        if (hasNote) {
            TaskContentTabItem(
                text = "Notes",
                isSelected = selectedIndex == 1 || (!hasDescription && selectedIndex == 0),
                onClick = { onTabSelected(1) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/**
 * Item d'onglet pour le contenu de tâche
 */
@Composable
fun TaskContentTabItem(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(
                if (isSelected) AccentBlue else Color.Transparent
            )
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = if (isSelected) Color.White else TextSecondary,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

/**
 * Ligne de détail simple pour les événements
 */
@Composable
fun EventDetailRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = TextSecondary,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = TextPrimary
        )
    }
}

/**
 * Item d'un événement du planning
 */
@Composable
fun EventItem(
    event: Event,
    onClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(DarkSurface)
            .clickable(onClick = onClick)
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            // Titre en gras en première ligne
            Text(
                text = event.title,
                style = MaterialTheme.typography.titleSmall,
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            // Date et heures en seconde ligne
            val timeInfo = if (event.startTime == "Toute la journée") {
                if (event.date.isNotEmpty()) "${event.date} • Toute la journée" else "Toute la journée"
            } else {
                if (event.date.isNotEmpty()) "${event.date} • ${event.startTime} - ${event.endTime}" else "${event.startTime} - ${event.endTime}"
            }
            Text(
                text = timeInfo,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
        }

        // Icône de l'événement
        Icon(
            imageVector = Icons.Default.CalendarToday,
            contentDescription = "Calendar event",
            tint = AccentBlue,
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(DarkSurfaceVariant)
                .padding(6.dp)
        )
    }
}

/**
 * Composable pour afficher du HTML formaté
 */
@Composable
fun HtmlText(
    html: String,
    modifier: Modifier = Modifier,
    color: Color = TextPrimary,
    fontSize: Float = 14f
) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            TextView(context).apply {
                textSize = fontSize
                setTextColor(color.toArgb())
                // Utiliser le thème pour les liens, etc.
                setLinkTextColor(AccentBlue.toArgb())
            }
        },
        update = { textView ->
            val spanned = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                Html.fromHtml(html, Html.FROM_HTML_MODE_COMPACT)
            } else {
                @Suppress("DEPRECATION")
                Html.fromHtml(html)
            }
            textView.text = spanned
        }
    )
}

/**
 * Dialog affichant tous les détails d'un événement
 */
@Composable
fun EventDetailsDialog(
    event: Event,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkSurface,
        title = {
            Text(
                text = event.title,
                color = TextPrimary,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Horaires
                EventDetailRow(
                    icon = Icons.Default.Schedule,
                    value = "${event.startTime} - ${event.endTime}"
                )

                // Lieu si disponible
                if (event.location.isNotEmpty()) {
                    EventDetailRow(
                        icon = Icons.Default.Place,
                        value = event.location
                    )
                }

                // Description si disponible
                if (event.description.isNotEmpty()) {
                    HorizontalDivider(
                        color = DarkSurfaceVariant,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Description",
                            style = MaterialTheme.typography.labelLarge,
                            color = TextSecondary,
                            fontWeight = FontWeight.Bold
                        )
                        HtmlText(
                            html = event.description,
                            color = TextPrimary,
                            fontSize = 14f
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Fermer", color = AccentBlue)
            }
        }
    )
}
