package com.example.lab08.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.lab08.data.*
import com.example.lab08.ui.AlarmScheduler
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class FilterType { ALL, PENDING, COMPLETED }
enum class SortType { DATE, NAME, PRIORITY }

data class TaskUiState(
    val tasks: List<Task> = emptyList(),
    val searchQuery: String = "",
    val filterType: FilterType = FilterType.ALL,
    val sortType: SortType = SortType.DATE,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isSyncing: Boolean = false
)

class TaskViewModel(application: Application) : AndroidViewModel(application) {

    private val db = TaskDatabase.getDatabase(application)
    private val dao = db.taskDao()
    private val firebaseRepo = FirebaseRepository()

    private val _uiState = MutableStateFlow(TaskUiState())
    val uiState: StateFlow<TaskUiState> = _uiState.asStateFlow()

    // Lista "raw" para aplicar filtros sin perderla
    private var rawTasks: List<Task> = emptyList()

    init {
        viewModelScope.launch {
            dao.getAllTasks().collect { tasks ->
                rawTasks = tasks
                _uiState.update { state ->
                    state.copy(tasks = applyFiltersAndSort(tasks, state))
                }
            }
        }
    }

    private fun applyFiltersAndSort(tasks: List<Task>, state: TaskUiState): List<Task> {
        var result = tasks

        if (state.searchQuery.isNotBlank()) {
            result = result.filter {
                it.description.contains(state.searchQuery, ignoreCase = true)
            }
        }

        result = when (state.filterType) {
            FilterType.PENDING -> result.filter { !it.isCompleted }
            FilterType.COMPLETED -> result.filter { it.isCompleted }
            FilterType.ALL -> result
        }

        result = when (state.sortType) {
            SortType.DATE -> result.sortedByDescending { it.createdAt }
            SortType.NAME -> result.sortedBy { it.description }
            SortType.PRIORITY -> result.sortedBy {
                when (it.priority) { Priority.HIGH -> 0; Priority.MEDIUM -> 1; Priority.LOW -> 2 }
            }
        }

        return result
    }

    fun addTask(
        description: String,
        priority: Priority = Priority.MEDIUM,
        category: Category = Category.PERSONAL,
        isRecurring: Boolean = false,
        scheduledTime: Long? = null,
        context: Context? = null
    ) {
        if (description.isBlank()) return
        viewModelScope.launch {
            val task = Task(
                description = description.trim(),
                priority = priority,
                category = category,
                isRecurring = isRecurring,
                scheduledTime = scheduledTime
            )
            val id = dao.insertTask(task)
            val taskWithId = task.copy(id = id.toInt())

            // Programar alarma si tiene hora asignada
            if (scheduledTime != null && context != null) {
                AlarmScheduler.scheduleAlarm(
                    context = context,
                    taskId = id.toInt(),
                    taskDescription = description,
                    timeMillis = scheduledTime
                )
            }

            // Sync Firebase
            try {
                val fbId = firebaseRepo.saveTask(taskWithId)
                dao.updateTask(taskWithId.copy(firebaseId = fbId))
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Guardado localmente. Sin conexión a Firebase.") }
            }
        }
    }

    fun updateTask(task: Task, context: Context? = null) {
        viewModelScope.launch {
            dao.updateTask(task)

            // Manejar alarma: cancelar anterior y reprogramar si tiene nueva hora
            if (context != null) {
                AlarmScheduler.cancelAlarm(context, task.id)
                if (task.scheduledTime != null) {
                    AlarmScheduler.scheduleAlarm(
                        context = context,
                        taskId = task.id,
                        taskDescription = task.description,
                        timeMillis = task.scheduledTime
                    )
                }
            }

            try {
                firebaseRepo.saveTask(task)
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Actualizado localmente. Sin conexión a Firebase.") }
            }
        }
    }

    fun toggleTask(task: Task, context: Context? = null) =
        updateTask(task.copy(isCompleted = !task.isCompleted), context)

    fun deleteTask(task: Task, context: Context? = null) {
        viewModelScope.launch {
            // Cancelar alarma si tenía
            if (context != null) {
                AlarmScheduler.cancelAlarm(context, task.id)
            }
            dao.deleteTask(task)
            try {
                firebaseRepo.deleteTask(task.firebaseId)
            } catch (e: Exception) { /* offline */ }
        }
    }

    fun deleteAllTasks(context: Context? = null) {
        viewModelScope.launch {
            // Cancelar todas las alarmas activas
            if (context != null) {
                rawTasks.forEach { AlarmScheduler.cancelAlarm(context, it.id) }
            }
            dao.deleteAllTasks()
            try {
                firebaseRepo.deleteAllTasks()
            } catch (e: Exception) { /* offline */ }
        }
    }

    fun syncFromFirebase() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSyncing = true) }
            try {
                val remoteTasks = firebaseRepo.getAllTasks()
                dao.deleteAllTasks()
                remoteTasks.forEach { dao.insertTask(it) }
                _uiState.update { it.copy(isSyncing = false, errorMessage = "Sincronización exitosa") }
            } catch (e: Exception) {
                _uiState.update { it.copy(isSyncing = false, errorMessage = "Error al sincronizar: ${e.message}") }
            }
        }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { state ->
            state.copy(
                searchQuery = query,
                tasks = applyFiltersAndSort(rawTasks, state.copy(searchQuery = query))
            )
        }
    }

    fun setFilter(filter: FilterType) {
        _uiState.update { state ->
            state.copy(
                filterType = filter,
                tasks = applyFiltersAndSort(rawTasks, state.copy(filterType = filter))
            )
        }
    }

    fun setSortType(sort: SortType) {
        _uiState.update { state ->
            state.copy(
                sortType = sort,
                tasks = applyFiltersAndSort(rawTasks, state.copy(sortType = sort))
            )
        }
    }

    fun clearError() = _uiState.update { it.copy(errorMessage = null) }

    suspend fun getTaskById(id: Int): Task? = dao.getTaskById(id)
}
