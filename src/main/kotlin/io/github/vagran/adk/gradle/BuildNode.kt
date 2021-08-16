package io.github.vagran.adk.gradle

import org.gradle.api.Task

open class BuildNode(val recipe: Recipe?) {
    val dependencies = ArrayList<BuildNode>()
    var task: Task? = null

    override fun toString(): String
    {
        return ""
    }

    inline fun <reified T: BuildNode> FindDep(): Iterable<T>
    {
        return FindDep { it is T }
    }

    fun <T: BuildNode> FindDep(predicate: (BuildNode) -> Boolean): Iterable<T>
    {
        val result = ArrayList<T>()
        dependencies.forEach {
            if (predicate(it)) {
                @Suppress("UNCHECKED_CAST")
                result.add(it as T)
            }
        }
        return result
    }

    inline fun <reified T: BuildNode> FindDepDeep(): Iterable<T>
    {
        return FindDepDeep { it is T }
    }

    fun <T: BuildNode> FindDepDeep(predicate: (BuildNode) -> Boolean): Iterable<T>
    {
        val result = LinkedHashSet<T>()
        val visited = HashSet<BuildNode>()
        dependencies.forEach { FindDepDeep(result, visited, it, predicate) }
        return result
    }

    private fun <T: BuildNode> FindDepDeep(result: HashSet<T>,
                                           visited: HashSet<BuildNode>,
                                           node: BuildNode,
                                           predicate: (BuildNode) -> Boolean)
    {
        if (visited.contains(node)) {
            return
        }
        if (predicate(node)) {
            @Suppress("UNCHECKED_CAST")
            result.add(node as T)
        }
        node.dependencies.forEach { FindDepDeep(result, visited, it, predicate) }
    }
}