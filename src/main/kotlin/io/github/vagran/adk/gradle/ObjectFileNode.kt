package io.github.vagran.adk.gradle

import java.io.File

class ObjectFileNode(path: File, recipe: Recipe, val module: ModuleNode? = null):
    FileNode(path, recipe)