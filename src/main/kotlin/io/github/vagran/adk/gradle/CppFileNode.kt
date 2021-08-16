package io.github.vagran.adk.gradle

import java.io.File

class CppFileNode(path: File, recipe: Recipe? = null, val module: ModuleNode? = null):
    FileNode(path, recipe)