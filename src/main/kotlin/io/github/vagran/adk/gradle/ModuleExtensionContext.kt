package io.github.vagran.adk.gradle

import org.gradle.api.Project
import java.io.File
import java.util.*

/**
 * @param nestedName Nested module name for nested context.
 */
class ModuleExtensionContext(project: Project, val baseDir: File, val nestedName: String?) {

    val nameProp = AdkProperty(project, String::class.java, conventionValue = nestedName,
         readOnlyMessage = if (nestedName != null) "Name already set by named module block" else null)
    var name: String by nameProp

    var main by AdkProperty(project, Boolean::class.java, conventionValue = false)

    val includeProp = AdkFileListProperty(project, baseDir = baseDir)
    var include: List<File> by includeProp

    val libDirProp = AdkFileListProperty(project, baseDir = baseDir)
    var libDir: List<File> by libDirProp

    val submodulesProp = AdkFileListProperty(project, baseDir = baseDir)
    var submodules: List<File> by submodulesProp

    val implProp = AdkFileListProperty(project, baseDir = baseDir)
    var impl: List<File> by implProp

    val moduleMapProp = AdkFileListProperty(project, baseDir = baseDir)
    var moduleMap: List<File> by moduleMapProp

    val dependsProp = AdkStringListProperty(project)
    var depends: List<String> by dependsProp

    val defineProp = AdkStringListProperty(project)
    var define: List<String> by defineProp

    val cflagsProp = AdkStringListProperty(project)
    var cflags: List<String> by cflagsProp

    val linkflagsProp = AdkStringListProperty(project)
    var linkflags: List<String> by linkflagsProp

    val libsProp = AdkStringListProperty(project)
    var libs: List<String> by libsProp

    val excludeProp = AdkFileListProperty(project, baseDir = baseDir)
    var exclude: List<File> by excludeProp

    val childContexts = TreeMap<String, ModuleExtensionContext>()
}