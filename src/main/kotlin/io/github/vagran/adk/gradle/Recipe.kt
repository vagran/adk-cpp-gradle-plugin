package io.github.vagran.adk.gradle

import org.gradle.api.Task
import kotlin.reflect.KClass

interface Recipe {
    fun interface TaskFactory<T: Task> {
        fun CreateTask(cls: KClass<T>): T
    }

    /** Create task for the recipe execution for the specified node. All dependant nodes should
     * already have tasks assigned.
     */
    fun CreateTask(artifact: BuildNode, taskFactory: TaskFactory<*>): Task

    val name: String
    val taskNamePrefix: String
    val taskGroup get() = name
}

inline fun <reified T: Task> Recipe.TaskFactory<*>.CreateTask(): T
{
    @Suppress("UNCHECKED_CAST")
    return (this as Recipe.TaskFactory<T>).CreateTask(T::class)
}