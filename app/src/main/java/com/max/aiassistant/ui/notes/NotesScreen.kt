package com.max.aiassistant.ui.notes

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
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.max.aiassistant.model.ChecklistItem
import com.max.aiassistant.model.Note
import com.max.aiassistant.model.NoteContentType
import com.max.aiassistant.ui.common.EmptyStateView
import com.max.aiassistant.ui.common.NavigationScreen
import com.max.aiassistant.ui.common.NavigationSidebarScaffold
import com.max.aiassistant.ui.common.rememberNavigationSidebarState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val NotesInk = Color(0xFF090C12)
private val NotesPanel = Color(0xFF141A28)
private val NotesPanelSoft = Color(0xFF1C2437)
private val NotesCyan = Color(0xFF74D1FF)
private val NotesPeach = Color(0xFFFFC08A)
private val NotesLilac = Color(0xFFC9B5FF)
private val NotesDim = Color(0xFF98A2B8)

private enum class NoteDeck { ALL, TEXT, LIST }

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
    onNavigateToActu: () -> Unit = {},
    showChrome: Boolean = true,
    modifier: Modifier = Modifier
) {
    val content: @Composable () -> Unit = {
        NotesContent(notes, onAddNote, onUpdateNote, onDeleteNote, showChrome, modifier)
    }
    if (showChrome) {
        val sidebarState = rememberNavigationSidebarState()
        NavigationSidebarScaffold(
            currentScreen = NavigationScreen.NOTES,
            onNavigateToScreen = { screen -> when (screen) {
                NavigationScreen.HOME -> onNavigateBack()
                NavigationScreen.VOICE -> onNavigateBack()
                NavigationScreen.CHAT -> onNavigateToChat()
                NavigationScreen.TASKS -> onNavigateToTasks()
                NavigationScreen.PLANNING -> onNavigateToPlanning()
                NavigationScreen.WEATHER -> onNavigateToWeather()
                NavigationScreen.NOTES -> Unit
                NavigationScreen.ACTU -> onNavigateToActu()
            } },
            sidebarState = sidebarState,
            content = content
        )
    } else content()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NotesContent(notes: List<Note>, onAddNote: (String, String) -> Unit, onUpdateNote: (String, String, String) -> Unit, onDeleteNote: (String) -> Unit, showChrome: Boolean, modifier: Modifier) {
    var deck by remember { mutableStateOf(NoteDeck.ALL) }
    var editingNoteId by remember { mutableStateOf<String?>(null) }
    var showCreate by remember { mutableStateOf(false) }
    val editingNote = notes.firstOrNull { it.id == editingNoteId }
    val visible = remember(notes, deck) { notes.filter { when (deck) {
        NoteDeck.ALL -> true
        NoteDeck.TEXT -> !isChecklist(it)
        NoteDeck.LIST -> isChecklist(it)
    } } }
    Box(
        modifier
            .fillMaxSize()
            .background(NotesInk)
            .then(if (showChrome) Modifier.windowInsetsPadding(WindowInsets.navigationBars) else Modifier)
    ) {
        Column(Modifier.fillMaxSize()) {
            if (showChrome) Column(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 20.dp, vertical = 14.dp)) {
                Text("ORGANISER", color = NotesCyan, style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.height(4.dp))
                Text("Notes", color = Color.White, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
            }
            LazyColumn(contentPadding = PaddingValues(20.dp, 8.dp, 20.dp, 120.dp), verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxSize()) {
                item { NoteSummary(notes) }
                item { NoteTabs(deck) { deck = it } }
                if (visible.isEmpty()) item {
                    EmptyStateView(Icons.Default.EditNote, NotesCyan, "Aucune note visible", "Le tableau reste propre tant qu'il n'y a rien a feuilleter.")
                }
                items(visible, key = { it.id }) { note -> NoteCardBlock(note, onClick = { editingNoteId = note.id }) }
            }
        }
        FloatingActionButton(
            onClick = { showCreate = true },
            containerColor = NotesCyan,
            contentColor = Color(0xFF081120),
            modifier = Modifier.align(Alignment.BottomEnd).padding(end = 20.dp, bottom = 24.dp)
        ) { Icon(Icons.Default.Add, contentDescription = "Nouvelle note") }
    }
    if (showCreate) NoteSheet(null, { showCreate = false }, onDelete = {}, onSave = { title, content -> onAddNote(title, content); showCreate = false })
    editingNote?.let { note -> NoteSheet(note, { editingNoteId = null }, onDelete = { onDeleteNote(note.id); editingNoteId = null }, onSave = { title, content -> onUpdateNote(note.id, title, content); editingNoteId = null }) }
}

@Composable private fun NoteSummary(notes: List<Note>) {
    val lists = notes.count { isChecklist(it) }
    val text = notes.size - lists
    Card(shape = RoundedCornerShape(28.dp), colors = CardDefaults.cardColors(containerColor = NotesPanel)) {
        Column(Modifier.padding(20.dp)) {
            Text("Atelier de notes", color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(6.dp))
            Text("Fiches, checklists et captures rapides dans une meme grammaire editoriale.", color = NotesDim, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(18.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                NoteMetric(notes.size, "total", NotesCyan, Modifier.weight(1f))
                NoteMetric(text, "texte", NotesPeach, Modifier.weight(1f))
                NoteMetric(lists, "checklists", NotesLilac, Modifier.weight(1f))
            }
        }
    }
}

@Composable private fun NoteMetric(value: Int, label: String, tint: Color, modifier: Modifier = Modifier) {
    Surface(modifier, color = NotesPanelSoft, shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 16.dp)) {
            Text(value.toString(), color = Color.White, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(label, color = tint, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable private fun NoteTabs(deck: NoteDeck, onSelect: (NoteDeck) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        NoteTab("Tout", deck == NoteDeck.ALL, NotesCyan, Modifier.weight(1f)) { onSelect(NoteDeck.ALL) }
        NoteTab("Texte", deck == NoteDeck.TEXT, NotesPeach, Modifier.weight(1f)) { onSelect(NoteDeck.TEXT) }
        NoteTab("Checklist", deck == NoteDeck.LIST, NotesLilac, Modifier.weight(1f)) { onSelect(NoteDeck.LIST) }
    }
}

@Composable private fun NoteTab(label: String, selected: Boolean, tint: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(modifier.clip(RoundedCornerShape(18.dp)).clickable(onClick = onClick), color = if (selected) tint.copy(alpha = 0.18f) else NotesPanel, shape = RoundedCornerShape(18.dp)) {
        Box(Modifier.fillMaxWidth().padding(vertical = 12.dp), contentAlignment = Alignment.Center) { Text(label, color = if (selected) Color.White else NotesDim, style = MaterialTheme.typography.bodyMedium) }
    }
}

@Composable private fun NoteCardBlock(note: Note, onClick: () -> Unit) {
    val checklist = checklistItems(note)
    val tint = if (checklist.isEmpty()) NotesPeach else NotesLilac
    Card(Modifier.fillMaxWidth().clickable(onClick = onClick), shape = RoundedCornerShape(26.dp), colors = CardDefaults.cardColors(containerColor = NotesPanel)) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(color = tint.copy(alpha = 0.18f), shape = CircleShape) { Icon(if (checklist.isEmpty()) Icons.Default.Description else Icons.Default.CheckBox, contentDescription = null, tint = tint, modifier = Modifier.padding(12.dp).size(20.dp)) }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(note.title, color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(formatDate(note.timestamp), color = NotesDim, style = MaterialTheme.typography.bodySmall)
                }
            }
            Spacer(Modifier.height(14.dp))
            if (checklist.isEmpty()) {
                Text(note.content.ifBlank { "Note vide" }, color = NotesDim, style = MaterialTheme.typography.bodyMedium, maxLines = 4, overflow = TextOverflow.Ellipsis)
            } else {
                checklist.take(4).forEach { item ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 3.dp)) {
                        Icon(if (item.isChecked) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(10.dp))
                        Text(item.text, color = if (item.isChecked) NotesDim else Color.White, textDecoration = if (item.isChecked) TextDecoration.LineThrough else TextDecoration.None, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable private fun NoteSheet(note: Note?, onDismiss: () -> Unit, onDelete: () -> Unit, onSave: (String, String) -> Unit) {
    val initialChecklist = note?.let { checklistItems(it) }.orEmpty()
    var title by remember(note?.id) { mutableStateOf(note?.title.orEmpty()) }
    var text by remember(note?.id) { mutableStateOf(if (initialChecklist.isEmpty()) note?.content.orEmpty() else "") }
    var checklist by remember(note?.id) { mutableStateOf(initialChecklist) }
    var newItem by remember(note?.id) { mutableStateOf("") }
    var type by remember(note?.id) { mutableStateOf(if (initialChecklist.isEmpty()) NoteContentType.TEXT else NoteContentType.CHECKLIST) }
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = NotesPanel, dragHandle = null) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 12.dp).padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(if (note == null) "Nouvelle note" else "Edition", color = Color.White, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                if (note != null) IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, contentDescription = "Supprimer", tint = NotesPeach) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NoteTab("Texte", type == NoteContentType.TEXT, NotesPeach, Modifier.weight(1f)) { type = NoteContentType.TEXT }
                NoteTab("Checklist", type == NoteContentType.CHECKLIST, NotesLilac, Modifier.weight(1f)) { type = NoteContentType.CHECKLIST }
            }
            OutlinedTextField(value = title, onValueChange = { title = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Titre") }, keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences), shape = RoundedCornerShape(18.dp))
            if (type == NoteContentType.TEXT) {
                OutlinedTextField(value = text, onValueChange = { text = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Contenu") }, minLines = 6, keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences), shape = RoundedCornerShape(18.dp))
            } else {
                checklist.forEachIndexed { index, item ->
                    Surface(color = NotesPanelSoft, shape = RoundedCornerShape(18.dp)) {
                        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = item.isChecked, onCheckedChange = { checked -> checklist = checklist.toMutableList().also { it[index] = item.copy(isChecked = checked) } })
                            Spacer(Modifier.width(8.dp))
                            Text(item.text, color = if (item.isChecked) NotesDim else Color.White, modifier = Modifier.weight(1f))
                            IconButton(onClick = { checklist = checklist.filterIndexed { i, _ -> i != index } }) { Icon(Icons.Default.Delete, contentDescription = "Supprimer", tint = NotesDim) }
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(value = newItem, onValueChange = { newItem = it }, modifier = Modifier.weight(1f), label = { Text("Nouvel element") }, shape = RoundedCornerShape(18.dp))
                    Spacer(Modifier.width(8.dp))
                    IconButton(onClick = { if (newItem.isNotBlank()) { checklist = checklist + ChecklistItem(text = newItem.trim()); newItem = "" } }) { Icon(Icons.Default.Add, contentDescription = "Ajouter", tint = NotesCyan) }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("Fermer", color = NotesDim) }
                Button(onClick = { onSave(title.trim(), if (type == NoteContentType.TEXT) text.trim() else encodeChecklist(checklist)) }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(18.dp), enabled = title.isNotBlank(), colors = ButtonDefaults.buttonColors(containerColor = NotesCyan, contentColor = Color(0xFF081120))) { Text("Enregistrer") }
            }
        }
    }
}

private fun isChecklist(note: Note): Boolean = checklistItems(note).isNotEmpty()

private fun checklistItems(note: Note): List<ChecklistItem> {
    if (note.checklistItems.isNotEmpty()) return note.checklistItems
    if (!note.content.contains("::")) return emptyList()
    return note.content.split("|||").mapNotNull { part ->
        val bits = part.split("::")
        if (bits.size >= 3) ChecklistItem(id = bits[0], text = bits[1], isChecked = bits[2].toBooleanStrictOrNull() ?: false) else null
    }
}

private fun encodeChecklist(items: List<ChecklistItem>): String = items.joinToString("|||") { "${it.id}::${it.text}::${it.isChecked}" }

private fun formatDate(timestamp: Long): String = SimpleDateFormat("dd MMM yyyy", Locale.FRANCE).format(Date(timestamp))
