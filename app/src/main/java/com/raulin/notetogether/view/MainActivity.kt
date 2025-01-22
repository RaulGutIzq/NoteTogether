package com.raulin.notetogether.view

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.toObject
import com.raulin.notetogether.view.ui.theme.NoteTogetherTheme

data class Note(
    val id: String = System.currentTimeMillis().toString(),
    val title: String = "",
    val body: String = ""
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NoteTogetherTheme {
                val auth = FirebaseAuth.getInstance()
                val currentUser = auth.currentUser

                if (currentUser != null) {
                    MaterialDesignView(userId = currentUser.uid)
                } else {
                    startActivity(Intent(this, LoginActivity::class.java))
                    finish()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MaterialDesignView(userId: String) {
    var showDialog by remember { mutableStateOf(false) }
    var currentNote by remember { mutableStateOf<Note?>(null) }
    val notes = remember { mutableStateListOf<Note>() }
    val db = FirebaseFirestore.getInstance()
    val notesRef = db.collection("notes").document(userId).collection("user_notes")

    LaunchedEffect(Unit) {
        notesRef.addSnapshotListener { snapshot, error ->
            if (error != null) return@addSnapshotListener
            if (snapshot != null) {
                notes.clear()
                notes.addAll(snapshot.documents.mapNotNull { document ->
                    document.toObject<Note>()?.copy(id = document.id)
                })
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("NoteTogether") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    currentNote = null
                    showDialog = true
                },
                shape = CircleShape,
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ) {
                Icon(Icons.Filled.Add, "Añadir nota")
            }
        },
        content = { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp)
            ) {
                NotasGrid(notes = notes, onNoteClick = { note ->
                    currentNote = note
                    showDialog = true
                })
            }
        }
    )

    if (showDialog) {
        NoteDialog(
            note = currentNote,
            onDismiss = { showDialog = false },
            onSave = { note ->
                if (currentNote == null) {
                    notesRef.add(note)
                } else {
                    notesRef.document(note.id).set(note)
                }
                showDialog = false
            },
            onDelete = { note ->
                notesRef.document(note.id).delete()
                showDialog = false
            }
        )
    }
}

@Composable
fun NotasGrid(notes: List<Note>, onNoteClick: (Note) -> Unit) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(notes) { note ->
            NotaCard(note, onClick = { onNoteClick(note) })
        }
    }
}

@Composable
fun NotaCard(nota: Note, onClick: () -> Unit) {
    ElevatedCard(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1.5f),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(8.dp)
        ) {
            Text(
                text = nota.title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = nota.body,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun NoteDialog(note: Note?, onDismiss: () -> Unit, onSave: (Note) -> Unit, onDelete: (Note) -> Unit) {
    var title by remember { mutableStateOf(note?.title ?: "") }
    var body by remember { mutableStateOf(note?.body ?: "") }

    Dialog(onDismissRequest = { onDismiss() }) {
        Surface(shape = MaterialTheme.shapes.medium, tonalElevation = 8.dp) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = if (note == null) "Añadir Nota" else "Editar Nota",
                    style = MaterialTheme.typography.titleLarge
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Título") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = body,
                    onValueChange = { body = it },
                    label = { Text("Cuerpo") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancelar") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = {
                        onSave(note?.copy(title = title, body = body) ?: Note(title = title, body = body))
                    }) { Text("Guardar") }
                    if (note != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                        TextButton(onClick = { onDelete(note) }) { Text("Eliminar") }
                    }
                }
            }
        }
    }
}
