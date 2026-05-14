package com.example.lab08.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.lab08.data.Category
import com.example.lab08.data.Priority
import com.example.lab08.data.Task
import com.example.lab08.ui.AlarmScheduler
import com.example.lab08.ui.components.PriorityChip
import com.example.lab08.ui.components.TaskTimePicker
import com.example.lab08.viewmodel.TaskViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTaskScreen(
    taskId: Int,
    viewModel: TaskViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    var description by remember { mutableStateOf("") }
    var selectedPriority by remember { mutableStateOf(Priority.MEDIUM) }
    var selectedCategory by remember { mutableStateOf(Category.PERSONAL) }
    var isRecurring by remember { mutableStateOf(false) }
    var isCompleted by remember { mutableStateOf(false) }
    var scheduledTime by remember { mutableStateOf<Long?>(null) }
    var taskFirebaseId by remember { mutableStateOf("") }
    var taskCreatedAt by remember { mutableStateOf(0L) }
    var showError by remember { mutableStateOf(false) }

    LaunchedEffect(taskId) {
        viewModel.getTaskById(taskId)?.let { task ->
            description = task.description
            selectedPriority = task.priority
            selectedCategory = task.category
            isRecurring = task.isRecurring
            isCompleted = task.isCompleted
            taskFirebaseId = task.firebaseId
            taskCreatedAt = task.createdAt
            scheduledTime = task.scheduledTime
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Editar Tarea", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = description,
                onValueChange = { description = it; showError = false },
                label = { Text("Descripción de la tarea") },
                modifier = Modifier.fillMaxWidth(),
                isError = showError && description.isBlank(),
                minLines = 2,
                maxLines = 4,
                shape = RoundedCornerShape(12.dp)
            )

            Text("Prioridad", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Priority.values().forEach { priority ->
                    PriorityChip(
                        priority = priority,
                        selected = selectedPriority == priority,
                        onClick = { selectedPriority = priority }
                    )
                }
            }

            Text("Categoría", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(Category.WORK to "Trabajo", Category.STUDY to "Estudio", Category.PERSONAL to "Personal")
                    .forEach { (cat, label) ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = cat },
                            label = { Text(label, fontSize = 12.sp) },
                            shape = RoundedCornerShape(50)
                        )
                    }
            }

            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Tarea recurrente", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                    Switch(checked = isRecurring, onCheckedChange = { isRecurring = it })
                }
            }

            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Completada", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                    Switch(checked = isCompleted, onCheckedChange = { isCompleted = it })
                }
            }

            Text("Recordatorio", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            TaskTimePicker(
                scheduledTime = scheduledTime,
                onTimeSelected = { scheduledTime = it }
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    if (description.isBlank()) {
                        showError = true
                    } else {
                        val updatedTask = Task(
                            id = taskId,
                            description = description.trim(),
                            priority = selectedPriority,
                            category = selectedCategory,
                            isRecurring = isRecurring,
                            isCompleted = isCompleted,
                            firebaseId = taskFirebaseId,
                            createdAt = taskCreatedAt,
                            scheduledTime = scheduledTime
                        )
                        viewModel.updateTask(updatedTask, context)
                        onNavigateBack()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Actualizar Tarea", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}