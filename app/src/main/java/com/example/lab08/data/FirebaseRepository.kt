package com.example.lab08.data

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.tasks.await

class FirebaseRepository {

    private val db: FirebaseFirestore = Firebase.firestore
    private val tasksCollection = db.collection("tasks")

    suspend fun saveTask(task: Task): String {
        val data = hashMapOf(
            "description" to task.description,
            "isCompleted" to task.isCompleted,
            "priority" to task.priority.name,
            "category" to task.category.name,
            "isRecurring" to task.isRecurring,
            "createdAt" to task.createdAt
        )
        return if (task.firebaseId.isNotEmpty()) {
            tasksCollection.document(task.firebaseId).set(data).await()
            task.firebaseId
        } else {
            tasksCollection.add(data).await().id
        }
    }

    suspend fun deleteTask(firebaseId: String) {
        if (firebaseId.isNotEmpty()) {
            tasksCollection.document(firebaseId).delete().await()
        }
    }

    suspend fun deleteAllTasks() {
        val snapshot = tasksCollection.get().await()
        val batch = db.batch()
        snapshot.documents.forEach { batch.delete(it.reference) }
        batch.commit().await()
    }

    suspend fun getAllTasks(): List<Task> {
        return tasksCollection.get().await().documents.mapNotNull { doc ->
            try {
                Task(
                    firebaseId = doc.id,
                    description = doc.getString("description") ?: "",
                    isCompleted = doc.getBoolean("isCompleted") ?: false,
                    priority = Priority.valueOf(doc.getString("priority") ?: "MEDIUM"),
                    category = Category.valueOf(doc.getString("category") ?: "PERSONAL"),
                    isRecurring = doc.getBoolean("isRecurring") ?: false,
                    createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()
                )
            } catch (e: Exception) { null }
        }
    }
}
