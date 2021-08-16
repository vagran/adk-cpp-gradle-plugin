package io.github.vagran.adk.gradle

import java.io.File

class CppCompiledModuleFileNode(path: File, val module: ModuleNode, recipe: Recipe):
    FileNode(path, recipe)