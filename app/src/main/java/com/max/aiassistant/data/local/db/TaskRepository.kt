package com.max.aiassistant.data.local.db

import androidx.room.Embedded
import androidx.room.Relation
import com.max.aiassistant.data.api.formatDeadline
import com.max.aiassistant.model.SubTask
import com.max.aiassistant.model.Task
import com.max.aiassistant.model.TaskPriority
import com.max.aiassistant.model.TaskStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

data class TaskWithSubTasks(
    @Embedded
    val task: TaskEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "task_id"
    )
    val subTasks: List<SubTaskEntity>
)

class TaskRepository(
    private val database: MaxDatabase
) {

    private val taskDao = database.taskDao()
    private val subTaskDao = database.subTaskDao()

    fun observeTasks(): Flow<List<Task>> {
        return taskDao.observeAllWithSubTasks()
            .map { tasks -> tasks.map { it.toDomain() } }
    }

    suspend fun getTasks(): List<Task> {
        return taskDao.getAllWithSubTasks().map { it.toDomain() }
    }

    suspend fun createTask(
        titre: String,
        categorie: String,
        description: String = "",
        note: String = "",
        priorite: TaskPriority = TaskPriority.P3,
        dateLimite: String = "",
        dureeEstimee: String = "",
        status: TaskStatus = TaskStatus.TODO,
        now: Long = System.currentTimeMillis()
    ): Task {
        val task = TaskEntity(
            id = UUID.randomUUID().toString(),
            title = titre,
            description = description,
            note = note,
            status = status.name,
            priority = priorite.name,
            deadlineDate = dateLimite,
            category = categorie,
            estimatedDuration = dureeEstimee,
            createdAt = now,
            updatedAt = now
        )
        taskDao.upsert(task)
        return TaskWithSubTasks(task = task, subTasks = emptyList()).toDomain()
    }

    suspend fun updateTask(task: Task, now: Long = System.currentTimeMillis()) {
        val existing = taskDao.getById(task.id) ?: return
        taskDao.upsert(
            existing.copy(
                title = task.title,
                description = task.description,
                note = task.note,
                status = task.status.name,
                priority = task.priority.name,
                deadlineDate = task.deadlineDate,
                category = task.category,
                estimatedDuration = task.estimatedDuration,
                updatedAt = now
            )
        )
    }

    suspend fun deleteTask(taskId: String) {
        taskDao.deleteById(taskId)
    }

    suspend fun createSubTask(
        taskId: String,
        text: String,
        isCompleted: Boolean = false,
        now: Long = System.currentTimeMillis()
    ): SubTask {
        val subTask = SubTaskEntity(
            id = UUID.randomUUID().toString(),
            taskId = taskId,
            text = text,
            isCompleted = isCompleted,
            createdAt = now,
            updatedAt = now
        )
        subTaskDao.upsert(subTask)
        return subTask.toDomain()
    }

    suspend fun updateSubTask(
        subTaskId: String,
        text: String,
        isCompleted: Boolean,
        now: Long = System.currentTimeMillis()
    ) {
        val existing = subTaskDao.getById(subTaskId) ?: return
        subTaskDao.upsert(
            existing.copy(
                text = text,
                isCompleted = isCompleted,
                updatedAt = now
            )
        )
    }

    suspend fun deleteSubTask(subTaskId: String) {
        subTaskDao.deleteById(subTaskId)
    }

    private fun TaskWithSubTasks.toDomain(): Task {
        return Task(
            id = task.id,
            title = task.title,
            description = task.description,
            note = task.note,
            status = task.status.toTaskStatus(),
            priority = task.priority.toTaskPriority(),
            deadline = formatDeadline(task.deadlineDate),
            deadlineDate = task.deadlineDate,
            category = task.category,
            estimatedDuration = task.estimatedDuration,
            subTasks = subTasks
                .sortedBy { it.createdAt }
                .map { it.toDomain() }
        )
    }

    private fun SubTaskEntity.toDomain(): SubTask {
        return SubTask(
            id = id,
            taskId = taskId,
            text = text,
            isCompleted = isCompleted
        )
    }

    private fun String.toTaskStatus(): TaskStatus {
        return runCatching { TaskStatus.valueOf(this) }.getOrDefault(TaskStatus.TODO)
    }

    private fun String.toTaskPriority(): TaskPriority {
        return runCatching { TaskPriority.valueOf(this) }.getOrDefault(TaskPriority.P3)
    }
}
