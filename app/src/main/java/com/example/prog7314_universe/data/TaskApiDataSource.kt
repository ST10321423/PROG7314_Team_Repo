package com.example.prog7314_universe.data

import com.example.prog7314_universe.Models.Task
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.tasks.await
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

private val moshi = Moshi.Builder()
    .addLast(KotlinJsonAdapterFactory())
    .build()

class TaskApiDataSource(
    private val api: TaskApi
) {

    suspend fun list(): List<Task> =
        try {
            api.list(authHeader()).map { it.toDomain() }
        } catch (e: HttpException) {
            throw e.asAppError()
        }

    suspend fun add(title: String, description: String?, dueIso: String?): Task {
        val payload = CreateTaskRequest(
            title = title.trim(),
            description = description?.takeIf { it.isNotBlank() }?.trim(),
            dueDate = dueIso?.takeIf { it.isNotBlank() }
        )
        return try {
            api.create(authHeader(), payload).toDomain()
        } catch (e: HttpException) {
            throw e.asAppError()
        }
    }

    suspend fun update(task: Task): Task {
        require(task.id.isNotBlank()) { "Task id required" }

        val payload = UpdateTaskRequest(
            title = task.title.trim(),
            description = task.description.takeIf { it.isNotBlank() }?.trim(),
            completed = task.isCompleted,
            dueDate = task.dueDate.takeIf { it.isNotBlank() }
        )
        return try {
            api.update(authHeader(), task.id, payload).toDomain()
        } catch (e: HttpException) {
            throw e.asAppError()
        }
    }

    suspend fun delete(id: String) {
        require(id.isNotBlank()) { "Task id required" }
        try {
            api.delete(authHeader(), id)
        } catch (e: HttpException) {
            throw e.asAppError()
        }
    }

    private suspend fun authHeader(): String {
        val user = Firebase.auth.currentUser ?: error("Not signed in")
        val token = user.getIdToken(false).await()?.token ?: error("Missing ID token")
        return "Bearer $token"
    }

    private fun ApiTask.toDomain(): Task = Task(
        id = id,
        title = title,
        description = description.orEmpty(),
        dueDate = dueDate ?: "",
        isCompleted = completed
    )

    private fun HttpException.asAppError(): RuntimeException {
        val body = response()?.errorBody()?.string().orEmpty()
        val parsed = body.takeIf { it.isNotBlank() }
            ?.let {
                runCatching { moshi.adapter(ApiError::class.java).fromJson(it) }.getOrNull()
            }
        val message = parsed?.error ?: message()
        return RuntimeException(message, this)
    }

}

fun createTaskApi(baseUrl: String): TaskApi =
    Retrofit.Builder()
        .baseUrl(baseUrl)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()
        .create(TaskApi::class.java)

interface TaskApi {
    @GET("tasks")
    suspend fun list(
        @Header("Authorization") authHeader: String
    ): List<ApiTask>

    @POST("tasks")
    suspend fun create(
        @Header("Authorization") authHeader: String,
        @Body request: CreateTaskRequest
    ): ApiTask

    @PUT("tasks/{id}")
    suspend fun update(
        @Header("Authorization") authHeader: String,
        @Path("id") id: String,
        @Body request: UpdateTaskRequest
    ): ApiTask

    @DELETE("tasks/{id}")
    suspend fun delete(
        @Header("Authorization") authHeader: String,
        @Path("id") id: String
    )
}

data class ApiTask(
    val id: String,
    val title: String,
    val description: String? = null,
    val completed: Boolean = false,
    val dueDate: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null
)

data class CreateTaskRequest(
    val title: String,
    val description: String? = null,
    val dueDate: String? = null
)

data class UpdateTaskRequest(
    val title: String,
    val description: String? = null,
    val completed: Boolean = false,
    val dueDate: String? = null
)

private data class ApiError(
    val error: String? = null
)