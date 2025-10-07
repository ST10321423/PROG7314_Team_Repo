package com.example.prog7314_universe.data

import com.example.prog7314_universe.BuildConfig
import com.example.prog7314_universe.Models.Task
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNames
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

class RenderTaskApiDataSource(
    private val client: OkHttpClient = OkHttpClient(),
    private val json: Json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }
) {
    private val baseUrl: String = BuildConfig.TASK_API_BASE_URL
        .trim()
        .removeSuffix("/")
        .ifBlank { throw IllegalStateException("TASK_API_BASE_URL is not configured") }

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun list(): List<Task> = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("$baseUrl/tasks")
            .get()
            .build()

        client.newCall(request).execute().use { response ->
            val payload = response.body?.string()
            if (!response.isSuccessful) {
                throw unexpectedResponse("load tasks", response.code, payload)
            }

            if (payload.isNullOrBlank()) {
                emptyList()
            } else {
                json.decodeFromString<List<TaskDto>>(payload)
                    .map { it.toModel() }
            }
        }
    }

    suspend fun add(title: String, desc: String?, dueIso: String?): Task = withContext(Dispatchers.IO) {
        val body = CreateTaskRequest(
            title = title,
            description = desc?.takeIf { it.isNotBlank() },
            dueAt = dueIso?.takeIf { it.isNotBlank() }
        )

        val request = Request.Builder()
            .url("$baseUrl/tasks")
            .post(json.encodeToString(body).toRequestBody(jsonMediaType))
            .build()

        client.newCall(request).execute().use { response ->
            val payload = response.body?.string()
            if (!response.isSuccessful) {
                throw unexpectedResponse("create task", response.code, payload)
            }

            val dto = payload?.takeIf { it.isNotBlank() }
                ?.let { json.decodeFromString(TaskDto.serializer(), it) }
                ?: throw IOException("Empty response while creating task")

            dto.toModel()
        }
    }

    suspend fun update(task: Task): Task = withContext(Dispatchers.IO) {
        require(task.id.isNotBlank()) { "Task id required" }

        val body = UpdateTaskRequest(
            title = task.title,
            description = task.description.takeIf { it.isNotBlank() },
            isCompleted = task.isCompleted,
            dueAt = task.dueDate.takeIf { it.isNotBlank() }
        )

        val request = Request.Builder()
            .url("$baseUrl/tasks/${task.id}")
            .put(json.encodeToString(body).toRequestBody(jsonMediaType))
            .build()

        client.newCall(request).execute().use { response ->
            val payload = response.body?.string()
            if (!response.isSuccessful) {
                throw unexpectedResponse("update task", response.code, payload)
            }

            val dto = payload?.takeIf { it.isNotBlank() }
                ?.let { json.decodeFromString(TaskDto.serializer(), it) }
                ?: throw IOException("Empty response while updating task")

            dto.toModel()
        }
    }

    suspend fun delete(id: String) = withContext(Dispatchers.IO) {
        require(id.isNotBlank()) { "Task id required" }

        val request = Request.Builder()
            .url("$baseUrl/tasks/$id")
            .delete()
            .build()

        client.newCall(request).execute().use { response ->
            if (response.isSuccessful || response.code == 204) return@use

            if (response.code == 404) {
                throw NoSuchElementException("Task not found")
            }

            val payload = response.body?.string()
            throw unexpectedResponse("delete task", response.code, payload)
        }
    }

    private fun unexpectedResponse(action: String, status: Int, body: String?): IOException {
        val details = body?.takeIf { it.isNotBlank() }?.let { ": ${it.trim()}" }.orEmpty()
        return IOException("Failed to $action (HTTP $status$details)")
    }

    @Serializable
    private data class TaskDto(
        val id: String,
        val title: String,
        val description: String? = null,
        @JsonNames("isCompleted", "completed")
        @SerialName("isCompleted")
        val isCompleted: Boolean = false,
        val dueAt: String? = null
    ) {
        fun toModel(): Task = Task(
            id = id,
            title = title,
            description = description.orEmpty(),
            dueDate = dueAt ?: "",
            isCompleted = isCompleted
        )
    }

    @Serializable
    private data class CreateTaskRequest(
        val title: String,
        val description: String? = null,
        val dueAt: String? = null
    )

    @Serializable
    private data class UpdateTaskRequest(
        val title: String,
        val description: String? = null,
        @JsonNames("isCompleted", "completed")
        @SerialName("isCompleted")
        val isCompleted: Boolean = false,
        val dueAt: String? = null
    )
}
