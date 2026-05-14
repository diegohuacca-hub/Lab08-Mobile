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
import com.example.lab08.ui.AlarmScheduler
import com.example.lab08.ui.components.PriorityChip
import com.example.lab08.ui.components.TaskTimePicker
import com.example.lab08.viewmodel.TaskViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTaskScreen(
    viewModel: TaskViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    var description by remember { mutableStateOf("") }
    var selectedPriority by remember { mutableStateOf(Priority.MEDIUM) }
    var selectedCategory by remember { mutableStateOf(Category.PERSONAL) }
    var isRecurring by remember { mutableStateOf(false) }
    var scheduledTime by remember { mutableStateOf<Long?>(null) }
    var showError by remember { mutableStateOf(false) }
    var showAlarmWarning by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nueva Tarea", fontWeight = FontWeight.Bold) },
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
                supportingText = if (showError && description.isBlank()) {
                    { Text("La descripción no puede estar vacía") }
                } else null,
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
                    Column {
                        Text("Tarea recurrente", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                        Text("Se repetirá periódicamente", fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    }
                    Switch(checked = isRecurring, onCheckedChange = { isRecurring = it })
                }
            }

            Text("Recordatorio", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            TaskTimePicker(
                scheduledTime = scheduledTime,
                onTimeSelected = { time ->
                    scheduledTime = time
                    if (time != null && !AlarmScheduler.canScheduleExactAlarms(context)) {
                        showAlarmWarning = true
                    }
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    if (description.isBlank()) {
                        showError = true
                    } else {
                        viewModel.addTask(
                            description = description,
                            priority = selectedPriority,
                            category = selectedCategory,
                            isRecurring = isRecurring,
                            scheduledTime = scheduledTime,
                            context = context
                        )
                        onNavigateBack()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Guardar Tarea", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }

    if (showAlarmWarning) {
        AlertDialog(
            onDismissRequest = { showAlarmWarning = false },
            title = { Text("Permiso requerido") },
            text = { Text("Para notificaciones exactas ve a: Ajustes → Aplicaciones → Lab08 → Alarmas y recordatorios → Activar.") },
            confirmButton = { TextButton(onClick = { showAlarmWarning = false }) { Text("Entendido") } }
        )
    }
}
