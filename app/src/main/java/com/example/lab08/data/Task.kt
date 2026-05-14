package com.example.lab08.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class Priority { HIGH, MEDIUM, LOW }
enum class Category { WORK, STUDY, PERSONAL }

@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val description: String,
    val isCompleted: Boolean = false,
    val priority: Priority = Priority.MEDIUM,
    val category: Category = Category.PERSONAL,
    val isRecurring: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val firebaseId: String = "",
    // Hora programada para la notificación (null = sin alarma)
    val scheduledTime: Long? = null
)
