package com.example.taskapi.task

import org.springframework.stereotype.Repository
import java.util.concurrent.ConcurrentHashMap

interface TaskRepository {
    fun list(): List<Task>
    fun get(id: String): Task?
    fun create(task: Task): Task
    fun update(id: String, task: Task): Task?
    fun delete(id: String): Boolean
}

@Repository
class InMemoryTaskRepository : TaskRepository {
    private val store = ConcurrentHashMap<String, Task>()

    override fun list(): List<Task> = store.values.toList()
    override fun get(id: String): Task? = store[id]

    override fun create(task: Task): Task {
        store[task.id] = task
        return task
    }

    override fun update(id: String, task: Task): Task? {
        if (!store.containsKey(id)) return null
        store[id] = task.copy(id = id)
        return store[id]
    }

    override fun delete(id: String): Boolean = store.remove(id) != null
}
