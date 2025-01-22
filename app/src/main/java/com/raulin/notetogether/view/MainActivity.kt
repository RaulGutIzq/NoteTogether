package com.raulin.notetogether.view

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.raulin.notetogether.view.ui.theme.NoteTogetherTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.toObject

data class Note(
    val id: String = System.currentTimeMillis().toString(), // Identificador único
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
                    // Usuario autenticado, cargar las notas del usuario
                    MaterialDesignView(userId = currentUser.uid)
                } else {
                    // Si no está autenticado, redirigir a la pantalla de inicio de sesión
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

    // Sincronizar las notas en tiempo real desde Firestore
    LaunchedEffect(Unit) {
        notesRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                // Manejar errores si los hay
                return@addSnapshotListener
            }
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
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.primary,
                ),
                title = { Text("NoteTogether") }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    // Abrir popup para añadir una nueva nota
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
                    .padding(top = 16.dp)
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp)
            ) {
                NotasGrid(
                    notes = notes,
                    onNoteClick = { note ->
                        // Abrir popup para editar una nota existente
                        currentNote = note
                        showDialog = true
                    }
                )
            }
        }
    )

    // Mostrar el popup si está activo
    if (showDialog) {
        NoteDialog(
            note = currentNote,
            onDismiss = { showDialog = false }, // Cerrar el popup
            onSave = { note ->
                if (currentNote == null) {
                    // Añadir nueva nota a Firestore
                    notesRef.add(note)
                } else {
                    // Actualizar la nota existente en Firestore
                    notesRef.document(note.id.toString()).set(note)
                }
                showDialog = false
            },
            onDelete = { note ->
                // Eliminar la nota de Firestore
                notesRef.document(note.id.toString()).delete()
                showDialog = false
            }
        )
    }
}


@Composable
fun NotasGrid(
    notes: List<Note>,
    onNoteClick: (Note) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.fillMaxSize(),
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
        onClick=onClick,
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(2f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
        ) {
            // Título
            Text(
                text = nota.title,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp) // Ajuste del padding para separación
            )
            // Cuerpo con truncado y máximo de 3 líneas
            Text(
                text = nota.body.take(100), // Muestra los primeros 100 caracteres del cuerpo
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.weight(1f)) // Usado para distribuir espacio si es necesario
        }
    }
}

@Composable
fun NoteDialog(
    note: Note?, // La nota a editar, o null si es una nueva
    onDismiss: () -> Unit,
    onSave: (Note) -> Unit,
    onDelete: (Note) -> Unit // Añadido para manejar la eliminación de una nota
) {
    // Estados para el título y el cuerpo de la nota
    var title by remember { mutableStateOf(note?.title ?: "") }
    var body by remember { mutableStateOf(note?.body ?: "") }
    var showConfirmDialog by remember { mutableStateOf(false) } // Estado para mostrar el diálogo de confirmación

    Dialog(onDismissRequest = { onDismiss() }) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            tonalElevation = 8.dp,
            modifier = Modifier.padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = if (note == null) "Añadir Nota" else "Editar Nota",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Título") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = body,
                    onValueChange = { body = it },
                    label = { Text("Cuerpo") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    maxLines = 5
                )

                Row(
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp)
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancelar")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val updatedNote = note?.copy(title = title, body = body)
                                ?: Note(title = title, body = body)
                            onSave(updatedNote)
                        }
                    ) {
                        Text("Guardar")
                    }
                }

                // Botón de eliminar solo si estamos editando una nota existente
                if (note != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { showConfirmDialog = true }, // Muestra el diálogo de confirmación
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Eliminar", color = MaterialTheme.colorScheme.onError)
                    }
                }
            }
        }
    }

    // Mostrar el diálogo de confirmación
    if (showConfirmDialog) {
        ConfirmDialog(
            message = "¿Estás seguro de que deseas eliminar esta nota?",
            onConfirm = {
                onDelete(note!!) // Elimina la nota
                onDismiss() // Cierra el diálogo principal
                showConfirmDialog = false // Cierra el diálogo de confirmación
            },
            onDismiss = { showConfirmDialog = false } // Cierra solo el diálogo de confirmación
        )
    }
}

@Composable
fun ConfirmDialog(
    message: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = { onDismiss() }) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            tonalElevation = 8.dp,
            modifier = Modifier.padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Row(
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancelar")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = onConfirm,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Confirmar", color = MaterialTheme.colorScheme.onError)
                    }
                }
            }
        }
    }
}


fun generateRandomNotes(count: Int): List<Note> {
    val randomTitles = listOf(
        "Nota importante", "Reunión de equipo", "Tareas pendientes", "Recordatorio",
        "Ideas para el proyecto", "Compras para el hogar", "Cosas por hacer", "Plan de vacaciones",
        "Notas de la clase", "Proyectos futuros"
    )

    val randomBodies = listOf(
        "Recuerda revisar este tema en profundidad y discutirlo con el equipo.",
        "Asegúrate de que todas las tareas estén completadas antes del plazo.",
        "No olvides realizar la investigación necesaria para el proyecto.",
        "Verifica que todos los datos estén correctos antes de enviarlos.",
        "Revisar los requisitos del cliente y hacer los ajustes necesarios.",
        "Compra pan, leche y huevos para la semana.",
        "Llamar a Juan para confirmar la cita.",
        "Enviar el informe al jefe antes de las 5 PM.",
        "Revisar el presupuesto y ajustarlo según las recomendaciones.",
        "Actualizar la documentación del proyecto con los últimos cambios."
    )

    return List(count) {
        val title = randomTitles.random()
        val body = randomBodies.random()
        Note(title = title, body = body)
    }
}
