package io.github.vagran.adk.gradle

import java.io.File

/** @param recipe Null for pre-existing nodes (e.g. source files). */
open class FileNode(val path: File, recipe: Recipe?): BuildNode(recipe) {

    override fun toString(): String
    {
        return path.name.toString()
    }
}