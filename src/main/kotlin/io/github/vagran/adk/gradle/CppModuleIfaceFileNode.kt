package io.github.vagran.adk.gradle

import java.io.File

class CppModuleIfaceFileNode(val module: ModuleNode,
                             recipe: Recipe? = null): FileNode(module.ifaceFile!!, recipe)