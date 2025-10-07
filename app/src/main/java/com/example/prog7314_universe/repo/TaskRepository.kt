package com.example.prog7314_universe.repo

import com.example.prog7314_universe.BuildConfig
import com.example.prog7314_universe.Models.Task
import com.example.prog7314_universe.data.TaskApiDataSource
import com.example.prog7314_universe.data.createTaskApi

class TaskRepository {
    private val ds = TaskApiDataSource(
        createTaskApi(BuildConfig.TASK_API_BASE_URL)
    )

    suspend fun list(): List<Task> = ds.list()

    suspend fun add(title: String, desc: String?, dueIso: String?): Task =
        ds.add(title, desc, dueIso)

    suspend fun update(task: Task): Task = ds.update(task)

    suspend fun delete(id: String) = ds.delete(id)
}
