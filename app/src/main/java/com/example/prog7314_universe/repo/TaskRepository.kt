package com.example.prog7314_universe.repo

import com.example.prog7314_universe.Models.Task
import com.example.prog7314_universe.data.RenderTaskApiDataSource

class TaskRepository {
    private val ds = RenderTaskApiDataSource()

    suspend fun list(): List<Task> = ds.list()

    suspend fun add(title: String, desc: String?, dueIso: String?): Task =
        ds.add(title, desc, dueIso)

    suspend fun update(task: Task): Task = ds.update(task)

    suspend fun delete(id: String) = ds.delete(id)
}
