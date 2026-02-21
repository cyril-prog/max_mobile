package com.max.aiassistant.ui.notes

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.max.aiassistant.model.ChecklistItem
import com.max.aiassistant.model.Note
import com.max.aiassistant.model.NoteContentType
import com.max.aiassistant.ui.theme.AccentBlue
import com.max.aiassistant.ui.theme.DarkBackground
import com.max.aiassistant.ui.theme.DarkSurface
import com.max.aiassistant.ui.theme.DarkSurfaceVariant
import com.max.aiassistant.ui.theme.TextPrimary
import com.max.aiassistant.ui.theme.TextSecondary
import com.max.aiassistant.ui.common.EmptyStateView
import com.max.aiassistant.ui.common.NavigationSidebarScaffold
import com.max.aiassistant.ui.common.NavigationScreen
import com.max.aiassistant.ui.common.rememberNavigationSidebarState
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.foundation.ScrollState
import androidx.compose.ui.graphics.drawscope.ContentDrawScope

/**
 * Modifier pour afficher une barre de défilement verticale animée
 * avec un track (fond) et un thumb (ascenseur) distincts
 */
@Composable
fun Modifier.verticalScrollbar(
    scrollState: ScrollState,
    itemCount: Int,
    visibleItems: Int = 8,
    scrollbarWidth: Dp = 8.dp
): Modifier {
    val scrollValue = scrollState.value
    val maxValue = scrollState.maxValue
    
    return this.drawWithContent {
        drawContent()
        
        // Seulement afficher si on a plus d'items que visible
        if (itemCount > visibleItems) {
            val viewportHeight = size.height
            val scrollbarWidthPx = scrollbarWidth.toPx()
            val trackX = size.width - scrollbarWidthPx - 2.dp.toPx()
            val cornerRadius = CornerRadius(scrollbarWidthPx / 2, scrollbarWidthPx / 2)
            
            // Dessiner le track (fond de la scrollbar) - gris foncé
            drawRoundRect(
                color = Color(0xFF3A3A3C),
                topLeft = Offset(trackX, 0f),
                size = Size(scrollbarWidthPx, viewportHeight),
                cornerRadius = cornerRadius
            )
            
            // Calculer la taille du thumb basée sur le ratio items visibles / total
            // Plus il y a d'items, plus le thumb est petit
            val thumbHeightRatio = visibleItems.toFloat() / itemCount.toFloat()
            val thumbHeight = (viewportHeight * thumbHeightRatio).coerceIn(50f, viewportHeight * 0.5f)
            
            // Calculer la position du thumb
            val availableTrack = viewportHeight - thumbHeight
            val thumbOffsetY = if (maxValue > 0) {
                (scrollValue.toFloat() / maxValue.toFloat()) * availableTrack
            } else {
                0f
            }
            
            // Dessiner le thumb (ascenseur) - bleu vif
            drawRoundRect(
                color = Color(0xFF0A84FF),
                topLeft = Offset(trackX, thumbOffsetY),
                size = Size(scrollbarWidthPx, thumbHeight),
                cornerRadius = cornerRadius
            )
        }
    }
}

/**
 * Écran de prise de notes
 * Permet de créer, afficher et supprimer des notes (texte ou checklist)
 * - Sidebar de navigation accessible par swipe vers la gauche depuis le bord droit
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesScreen(
    notes: List<Note>,
    onAddNote: (String, String) -> Unit,
    onUpdateNote: (String, String, String) -> Unit,
    onDeleteNote: (String) -> Unit,
    onNavigateBack: () -> Unit,
    onNavigateToChat: () -> Unit = {},
    onNavigateToTasks: () -> Unit = {},
    onNavigateToPlanning: () -> Unit = {},
    onNavigateToWeather: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val sidebarState = rememberNavigationSidebarState()
    
    NavigationSidebarScaffold(
        currentScreen = NavigationScreen.NOTES,
        onNavigateToScreen = { screen ->
            when (screen) {
                NavigationScreen.HOME -> onNavigateBack()
                NavigationScreen.VOICE -> onNavigateBack() // Retourne à Home pour accéder à Voice
                NavigationScreen.CHAT -> onNavigateToChat()
                NavigationScreen.TASKS -> onNavigateToTasks()
                NavigationScreen.PLANNING -> onNavigateToPlanning()
                NavigationScreen.WEATHER -> onNavigateToWeather()
                NavigationScreen.NOTES -> { /* Déjà sur cet écran */ }
            }
        },
        sidebarState = sidebarState
    ) {
        NotesScreenContent(
            notes = notes,
            onAddNote = onAddNote,
            onUpdateNote = onUpdateNote,
            onDeleteNote = onDeleteNote,
            onNavigateBack = onNavigateBack,
            modifier = modifier
        )
    }
}

/**
 * Contenu de l'écran Notes
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NotesScreenContent(
    notes: List<Note>,
    onAddNote: (String, String) -> Unit,
    onUpdateNote: (String, String, String) -> Unit,
    onDeleteNote: (String) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var noteToEdit by remember { mutableStateOf<Note?>(null) }
    var noteToView by remember { mutableStateOf<Note?>(null) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 48.dp)
        ) {
            // En-tête personnalisé sans flèche de retour
            Text(
                text = "Notes",
                style = MaterialTheme.typography.headlineMedium,
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 20.dp)
            )

            if (notes.isEmpty()) {
                // État vide illustré
                EmptyStateView(
                    icon = Icons.Default.Description,
                    iconTint = androidx.compose.ui.graphics.Color(0xFF32D74B),
                    title = "Aucune note",
                    subtitle = "Appuyez sur + pour créer votre première note",
                    modifier = Modifier.weight(1f)
                )
            } else {
                // Liste des notes
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(notes) { note ->
                        NoteCard(
                            note = note,
                            onClick = { noteToView = note },
                            onEdit = { noteToEdit = note },
                            onDelete = { onDeleteNote(note.id) },
                            onToggleCheckbox = { itemId ->
                                // Mettre à jour l'état de la checkbox dans le contenu
                                val updatedContent = toggleCheckboxInContent(note.content, itemId)
                                onUpdateNote(note.id, note.title, updatedContent)
                            }
                        )
                    }
                }
            }
        }

        // Bouton flottant pour ajouter une note
        FloatingActionButton(
            onClick = { showAddDialog = true },
            containerColor = AccentBlue,
            contentColor = Color.White,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(20.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Ajouter une note")
        }
    }

    // Dialog pour ajouter une note
    if (showAddDialog) {
        AddNoteDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { title, content ->
                onAddNote(title, content)
                showAddDialog = false
            }
        )
    }

    // Dialog pour modifier une note
    noteToEdit?.let { note ->
        EditNoteDialog(
            note = note,
            onDismiss = { noteToEdit = null },
            onSave = { title, content ->
                onUpdateNote(note.id, title, content)
                noteToEdit = null
            },
            onDelete = {
                onDeleteNote(note.id)
                noteToEdit = null
            }
        )
    }

    // Dialog pour visualiser une note (lecture seule avec checkboxes)
    noteToView?.let { note ->
        ViewNoteDialog(
            note = note,
            onDismiss = { noteToView = null },
            onToggleCheckbox = { itemId ->
                val updatedContent = toggleCheckboxInContent(note.content, itemId)
                onUpdateNote(note.id, note.title, updatedContent)
                // Mettre à jour la note affichée
                noteToView = note.copy(content = updatedContent)
            }
        )
    }
}

/**
 * Carte affichant une note (texte ou checklist)
 */
@Composable
fun NoteCard(
    note: Note,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggleCheckbox: (String) -> Unit = {}
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    // Icône selon le type (détection automatique)
                    val isChecklist = note.content.contains("|||") && note.content.contains("::")
                    Icon(
                        imageVector = if (isChecklist) 
                            Icons.Default.Checklist else Icons.Default.Description,
                        contentDescription = null,
                        tint = AccentBlue,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = note.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Modifier",
                            tint = TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Supprimer",
                            tint = TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Contenu selon le type - détecter automatiquement si c'est une checklist
            val isChecklist = note.content.contains("|||") && note.content.contains("::")
            
            if (isChecklist) {
                // Parser et afficher les items de la checklist
                val parsedItems = parseChecklistContent(note.content)
                parsedItems.take(4).forEach { item ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .padding(vertical = 2.dp)
                            .clickable { onToggleCheckbox(item.id) }
                    ) {
                        Icon(
                            imageVector = if (item.isChecked) 
                                Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                            contentDescription = if (item.isChecked) "Décocher" else "Cocher",
                            tint = if (item.isChecked) AccentBlue else TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = item.text,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (item.isChecked) TextSecondary else TextPrimary,
                            textDecoration = if (item.isChecked) TextDecoration.LineThrough else TextDecoration.None,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                if (parsedItems.size > 4) {
                    Text(
                        text = "+${parsedItems.size - 4} autres",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            } else {
                // Note texte classique
                if (note.content.isNotEmpty()) {
                    Text(
                        text = note.content,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary.copy(alpha = 0.8f),
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Date
            Text(
                text = formatTimestamp(note.timestamp),
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary.copy(alpha = 0.6f)
            )
        }
    }
}

/**
 * Dialog moderne pour ajouter une nouvelle note
 */
@Composable
fun AddNoteDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(NoteContentType.TEXT) }
    var checklistItems by remember { mutableStateOf(listOf<ChecklistItem>()) }
    var newItemText by remember { mutableStateOf("") }

    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .wrapContentHeight(),
            shape = RoundedCornerShape(24.dp),
            color = DarkSurface
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                // En-tête
                Text(
                    text = "Nouvelle note",
                    style = MaterialTheme.typography.headlineSmall,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Sélecteur de type
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(DarkSurfaceVariant)
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    NoteTypeTab(
                        text = "Texte",
                        icon = Icons.Default.Description,
                        isSelected = selectedType == NoteContentType.TEXT,
                        onClick = { selectedType = NoteContentType.TEXT },
                        modifier = Modifier.weight(1f)
                    )
                    NoteTypeTab(
                        text = "Checklist",
                        icon = Icons.Default.Checklist,
                        isSelected = selectedType == NoteContentType.CHECKLIST,
                        onClick = { selectedType = NoteContentType.CHECKLIST },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Titre
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Titre") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = AccentBlue,
                        unfocusedBorderColor = TextSecondary.copy(alpha = 0.3f),
                        focusedLabelColor = AccentBlue,
                        unfocusedLabelColor = TextSecondary,
                        cursorColor = AccentBlue
                    ),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Contenu selon le type
                when (selectedType) {
                    NoteContentType.TEXT -> {
                        OutlinedTextField(
                            value = content,
                            onValueChange = { content = it },
                            label = { Text("Contenu") },
                            minLines = 4,
                            maxLines = 6,
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Sentences
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedBorderColor = AccentBlue,
                                unfocusedBorderColor = TextSecondary.copy(alpha = 0.3f),
                                focusedLabelColor = AccentBlue,
                                unfocusedLabelColor = TextSecondary,
                                cursorColor = AccentBlue
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                    NoteContentType.CHECKLIST -> {
                        // Liste des items existants
                        val scrollState = rememberScrollState()
                        val needsScroll = checklistItems.size > 8
                        
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(DarkSurfaceVariant)
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (checklistItems.isEmpty()) {
                                Text(
                                    text = "Ajoutez des éléments à votre liste",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            } else {
                                Column(
                                    modifier = if (needsScroll) {
                                        Modifier
                                            .heightIn(max = 320.dp)
                                            .verticalScroll(scrollState)
                                            .verticalScrollbar(scrollState, checklistItems.size)
                                    } else {
                                        Modifier
                                    },
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    checklistItems.forEachIndexed { index, item ->
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.CheckBoxOutlineBlank,
                                                contentDescription = null,
                                                tint = TextSecondary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = item.text,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = TextPrimary,
                                                modifier = Modifier.weight(1f)
                                            )
                                            IconButton(
                                                onClick = {
                                                    checklistItems = checklistItems.filterIndexed { i, _ -> i != index }
                                                },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Close,
                                                    contentDescription = "Supprimer",
                                                    tint = TextSecondary,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // Ajouter un nouvel item
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                OutlinedTextField(
                                    value = newItemText,
                                    onValueChange = { newItemText = it },
                                    placeholder = { Text("Nouvel élément...", color = TextSecondary) },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f),
                                    keyboardOptions = KeyboardOptions(
                                        capitalization = KeyboardCapitalization.Sentences
                                    ),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = TextPrimary,
                                        unfocusedTextColor = TextPrimary,
                                        focusedBorderColor = AccentBlue,
                                        unfocusedBorderColor = TextSecondary.copy(alpha = 0.3f),
                                        cursorColor = AccentBlue
                                    ),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                IconButton(
                                    onClick = {
                                        if (newItemText.isNotBlank()) {
                                            checklistItems = checklistItems + ChecklistItem(text = newItemText.trim())
                                            newItemText = ""
                                        }
                                    },
                                    enabled = newItemText.isNotBlank()
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Ajouter",
                                        tint = if (newItemText.isNotBlank()) AccentBlue else TextSecondary
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Boutons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = TextSecondary
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            TextSecondary.copy(alpha = 0.3f)
                        )
                    ) {
                        Text("Annuler")
                    }

                    Button(
                        onClick = {
                            val finalContent = when (selectedType) {
                                NoteContentType.TEXT -> content
                                NoteContentType.CHECKLIST -> {
                                    // Encode checklist as JSON-like string for storage
                                    checklistItems.joinToString("|||") { "${it.id}::${it.text}::${it.isChecked}" }
                                }
                            }
                            onConfirm(title, finalContent)
                        },
                        modifier = Modifier.weight(1f),
                        enabled = title.isNotBlank() && (
                            selectedType == NoteContentType.TEXT || 
                            checklistItems.isNotEmpty()
                        ),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AccentBlue,
                            contentColor = Color.White
                        )
                    ) {
                        Text("Créer", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

/**
 * Dialog pour visualiser une note (lecture seule avec checkboxes interactives)
 */
@Composable
fun ViewNoteDialog(
    note: Note,
    onDismiss: () -> Unit,
    onToggleCheckbox: (String) -> Unit
) {
    val isChecklist = note.content.contains("|||") && note.content.contains("::")
    val checklistItems = if (isChecklist) parseChecklistContent(note.content) else emptyList()

    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .wrapContentHeight(),
            shape = RoundedCornerShape(24.dp),
            color = DarkSurface
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                // En-tête avec titre
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = if (isChecklist) Icons.Default.Checklist else Icons.Default.Description,
                            contentDescription = null,
                            tint = AccentBlue,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = note.title,
                            style = MaterialTheme.typography.headlineSmall,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Fermer",
                            tint = TextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Contenu
                if (isChecklist) {
                    // Checklist avec cases interactives
                    val scrollState = rememberScrollState()
                    val needsScroll = checklistItems.size > 8
                    
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(DarkSurfaceVariant)
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Column(
                            modifier = if (needsScroll) {
                                Modifier
                                    .heightIn(max = 320.dp)
                                    .verticalScroll(scrollState)
                                    .verticalScrollbar(scrollState, checklistItems.size)
                            } else {
                                Modifier
                            },
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            checklistItems.forEach { item ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { onToggleCheckbox(item.id) }
                                        .padding(vertical = 8.dp, horizontal = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = if (item.isChecked) 
                                            Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                                        contentDescription = if (item.isChecked) "Décocher" else "Cocher",
                                        tint = if (item.isChecked) AccentBlue else TextSecondary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = item.text,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = if (item.isChecked) TextSecondary else TextPrimary,
                                        textDecoration = if (item.isChecked) TextDecoration.LineThrough else TextDecoration.None
                                    )
                                }
                            }
                        }
                        
                        // Progression
                        val checkedCount = checklistItems.count { it.isChecked }
                        val totalCount = checklistItems.size
                        if (totalCount > 0) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                LinearProgressIndicator(
                                    progress = { checkedCount.toFloat() / totalCount },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp)),
                                    color = AccentBlue,
                                    trackColor = TextSecondary.copy(alpha = 0.2f)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "$checkedCount/$totalCount",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                } else {
                    // Note texte simple - sélectionnable
                    if (note.content.isNotEmpty()) {
                        SelectionContainer {
                            Text(
                                text = note.content,
                                style = MaterialTheme.typography.bodyLarge,
                                color = TextPrimary.copy(alpha = 0.9f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(DarkSurfaceVariant)
                                    .padding(16.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Date de création
                Text(
                    text = "Créée le ${formatTimestamp(note.timestamp)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary.copy(alpha = 0.6f)
                )
            }
        }
    }
}

/**
 * Dialog pour modifier une note existante
 */
@Composable
fun EditNoteDialog(
    note: Note,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit,
    onDelete: () -> Unit
) {
    // Déterminer si c'est une checklist
    val isChecklist = note.content.contains("|||") && note.content.contains("::")
    
    var title by remember { mutableStateOf(note.title) }
    var content by remember { mutableStateOf(if (isChecklist) "" else note.content) }
    var checklistItems by remember { 
        mutableStateOf(
            if (isChecklist) parseChecklistContent(note.content) else emptyList()
        )
    }
    var newItemText by remember { mutableStateOf("") }

    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .wrapContentHeight(),
            shape = RoundedCornerShape(24.dp),
            color = DarkSurface
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                // En-tête avec bouton supprimer
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Modifier la note",
                        style = MaterialTheme.typography.headlineSmall,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDelete) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Supprimer",
                            tint = Color(0xFFFF6B6B)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Titre
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Titre") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = AccentBlue,
                        unfocusedBorderColor = TextSecondary.copy(alpha = 0.3f),
                        focusedLabelColor = AccentBlue,
                        unfocusedLabelColor = TextSecondary,
                        cursorColor = AccentBlue
                    ),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Contenu selon le type
                if (isChecklist) {
                    // Liste des items avec cases à cocher
                    val scrollState = rememberScrollState()
                    val needsScroll = checklistItems.size > 8
                    
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(DarkSurfaceVariant)
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Éléments de la liste",
                            style = MaterialTheme.typography.labelMedium,
                            color = TextSecondary
                        )
                        
                        Column(
                            modifier = if (needsScroll) {
                                Modifier
                                    .heightIn(max = 320.dp)
                                    .verticalScroll(scrollState)
                                    .verticalScrollbar(scrollState, checklistItems.size)
                            } else {
                                Modifier
                            },
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            checklistItems.forEachIndexed { index, item ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            // Toggle checkbox
                                            checklistItems = checklistItems.mapIndexed { i, it ->
                                                if (i == index) it.copy(isChecked = !it.isChecked) else it
                                            }
                                        }
                                        .padding(vertical = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = if (item.isChecked) 
                                            Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                                        contentDescription = null,
                                        tint = if (item.isChecked) AccentBlue else TextSecondary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = item.text,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = if (item.isChecked) TextSecondary else TextPrimary,
                                        textDecoration = if (item.isChecked) TextDecoration.LineThrough else TextDecoration.None,
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(
                                        onClick = {
                                            checklistItems = checklistItems.filterIndexed { i, _ -> i != index }
                                        },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Supprimer",
                                            tint = TextSecondary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // Ajouter un nouvel item
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = newItemText,
                                onValueChange = { newItemText = it },
                                placeholder = { Text("Nouvel élément...", color = TextSecondary) },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                keyboardOptions = KeyboardOptions(
                                    capitalization = KeyboardCapitalization.Sentences
                                ),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary,
                                    focusedBorderColor = AccentBlue,
                                    unfocusedBorderColor = TextSecondary.copy(alpha = 0.3f),
                                    cursorColor = AccentBlue
                                ),
                                shape = RoundedCornerShape(8.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            IconButton(
                                onClick = {
                                    if (newItemText.isNotBlank()) {
                                        checklistItems = checklistItems + ChecklistItem(text = newItemText.trim())
                                        newItemText = ""
                                    }
                                },
                                enabled = newItemText.isNotBlank()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Ajouter",
                                    tint = if (newItemText.isNotBlank()) AccentBlue else TextSecondary
                                )
                            }
                        }
                    }
                } else {
                    // Note texte
                    OutlinedTextField(
                        value = content,
                        onValueChange = { content = it },
                        label = { Text("Contenu") },
                        minLines = 4,
                        maxLines = 8,
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Sentences
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = AccentBlue,
                            unfocusedBorderColor = TextSecondary.copy(alpha = 0.3f),
                            focusedLabelColor = AccentBlue,
                            unfocusedLabelColor = TextSecondary,
                            cursorColor = AccentBlue
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Boutons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = TextSecondary
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            TextSecondary.copy(alpha = 0.3f)
                        )
                    ) {
                        Text("Annuler")
                    }

                    Button(
                        onClick = {
                            val finalContent = if (isChecklist) {
                                checklistItems.joinToString("|||") { "${it.id}::${it.text}::${it.isChecked}" }
                            } else {
                                content
                            }
                            onSave(title, finalContent)
                        },
                        modifier = Modifier.weight(1f),
                        enabled = title.isNotBlank(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AccentBlue,
                            contentColor = Color.White
                        )
                    ) {
                        Text("Enregistrer", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

/**
 * Tab pour sélectionner le type de note
 */
@Composable
fun NoteTypeTab(
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
            .padding(vertical = 10.dp, horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = if (isSelected) Color.White else TextSecondary
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isSelected) Color.White else TextSecondary,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}

/**
 * Toggle l'état d'une checkbox dans le contenu encodé
 */
private fun toggleCheckboxInContent(content: String, itemId: String): String {
    return content.split("|||").joinToString("|||") { itemStr ->
        val parts = itemStr.split("::")
        if (parts.size >= 3 && parts[0] == itemId) {
            val newChecked = !(parts[2].toBooleanStrictOrNull() ?: false)
            "${parts[0]}::${parts[1]}::$newChecked"
        } else {
            itemStr
        }
    }
}

/**
 * Parse le contenu encodé d'une checklist
 * Format: "id::text::isChecked|||id::text::isChecked"
 */
private fun parseChecklistContent(content: String): List<ChecklistItem> {
    if (content.isBlank()) return emptyList()
    
    return try {
        content.split("|||").mapNotNull { itemStr ->
            val parts = itemStr.split("::")
            if (parts.size >= 3) {
                ChecklistItem(
                    id = parts[0],
                    text = parts[1],
                    isChecked = parts[2].toBooleanStrictOrNull() ?: false
                )
            } else if (parts.size >= 2) {
                ChecklistItem(
                    id = parts[0],
                    text = parts[1],
                    isChecked = false
                )
            } else {
                null
            }
        }
    } catch (e: Exception) {
        emptyList()
    }
}

/**
 * Formate un timestamp en date lisible
 */
private fun formatTimestamp(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd MMMM yyyy à HH:mm", Locale.FRANCE)
    return sdf.format(Date(timestamp))
}
