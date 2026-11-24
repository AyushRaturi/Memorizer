package com.example.recall

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= 33) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        // Keep the unlock listener alive (re-starting an already running service is harmless).
        RecallActions.startListener(this)
        setContent { RecallScreen() }
    }
}

private const val MAX_WORDS = 30

@Composable
fun RecallScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var notes by remember { mutableStateOf(NotesStore.getNotes(context)) }
    var input by remember { mutableStateOf("") }
    var editIndex by remember { mutableStateOf<Int?>(null) }
    var headsUp by remember { mutableStateOf(NotesStore.headsUp(context)) }
    val words = input.trim().split(Regex("\\s+")).filter { it.isNotBlank() }.size

    fun save() {
        val clean = input.trim()
        if (clean.isBlank() || words > MAX_WORDS) return
        val next = notes.toMutableList()
        val idx = editIndex
        if (idx == null || idx !in next.indices) next.add(clean) else next[idx] = clean
        notes = next
        NotesStore.saveNotes(context, next)
        NotesStore.setCurrent(context, clean)
        input = ""
        editIndex = null
        scope.launch { refreshRecallWidgets(context) }
    }

    MaterialTheme {
        Scaffold { pad ->
            Column(
                Modifier.fillMaxSize().padding(pad).padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Recall", style = MaterialTheme.typography.headlineLarge)
                Text(
                    "Your notes, shown when you unlock your phone.",
                    style = MaterialTheme.typography.bodyMedium
                )
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Write a reminder or note") },
                    minLines = 2,
                    maxLines = 4,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
                )
                Text(
                    "$words / $MAX_WORDS words",
                    color = if (words > MAX_WORDS) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Button(
                    onClick = { save() },
                    enabled = input.isNotBlank() && words <= MAX_WORDS,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (editIndex == null) "Save note" else "Update note")
                }
                if (editIndex != null) {
                    TextButton(onClick = { input = ""; editIndex = null }) { Text("Cancel edit") }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Also show a pop-up notification", Modifier.weight(1f))
                    Switch(
                        checked = headsUp,
                        onCheckedChange = {
                            headsUp = it
                            NotesStore.setHeadsUp(context, it)
                        }
                    )
                }
                HorizontalDivider()
                Text("Saved notes (${notes.size})", style = MaterialTheme.typography.titleMedium)
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(notes) { index, note ->
                        Card(shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(12.dp)) {
                                Text(note)
                                Row {
                                    TextButton(onClick = { input = note; editIndex = index }) { Text("Edit") }
                                    TextButton(onClick = {
                                        val next = notes.toMutableList().also { it.removeAt(index) }
                                        notes = next
                                        NotesStore.saveNotes(context, next)
                                        if (NotesStore.current(context) == note) {
                                            NotesStore.setCurrent(context, next.firstOrNull() ?: "")
                                        }
                                        // Indices shifted, so drop any edit in progress.
                                        input = ""
                                        editIndex = null
                                        scope.launch { refreshRecallWidgets(context) }
                                    }) { Text("Delete") }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
