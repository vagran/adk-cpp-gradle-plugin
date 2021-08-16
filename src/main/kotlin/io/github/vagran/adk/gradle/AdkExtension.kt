@file:Suppress("MemberVisibilityCanBePrivate", "unused")

package io.github.vagran.adk.gradle

import org.gradle.api.Project
import java.io.File


open class AdkExtension(internal val project: Project) {

    var cxx: String by AdkProperty(project, String::class.java,
        conventionValueProvider = {
            if (project.hasProperty(PropName.ADK_CXX.value)) {
                project.property(PropName.ADK_CXX.value).toString()
            } else {
                System.getenv("CXX") ?: ""
            }
        })

    var buildType: String by AdkProperty(project, String::class.java,
        conventionValueProvider = {
            if (project.hasProperty(PropName.ADK_BUILD_TYPE.value)) {
                project.property(PropName.ADK_BUILD_TYPE.value).toString()
            } else {
                "release"
            }
        },
        validator = {
            v ->
            if (v !in BuildType.values().map { it.value }) {
                throw Error("Unsupported build type: $v")
            }
        })

    var binType: String by AdkProperty(project, String::class.java, BinType.APP.value,
        validator = {
            v ->
            if (v !in BinType.values().map { it.value }) {
                throw Error("Unsupported build type: $v")
            }
        })

    var binName: String by AdkProperty(project, String::class.java,
                                       conventionValueProvider =  { project.name })

    internal val defineProp = AdkStringListProperty(project, emptyList())
    var define: List<String> by defineProp

    fun define(vararg items: String)
    {
        defineProp.Append(items)
    }

    internal val cflagsProp = AdkStringListProperty(project, emptyList())
    var cflags: List<String> by cflagsProp

    fun cflags(vararg items: String)
    {
        cflagsProp.Append(items)
    }

    internal val linkflagsProp = AdkStringListProperty(project, emptyList())
    var linkflags: List<String> by linkflagsProp

    fun linkflags(vararg items: String)
    {
        linkflagsProp.Append(items)
    }

    internal val libsProp = AdkStringListProperty(project, emptyList())
    var libs: List<String> by libsProp

    fun libs(vararg items: String)
    {
        libsProp.Append(items)
    }

    var cppModuleIfaceExt: List<String> by AdkStringListProperty(project, listOf("cppm"))

    var cppModuleImplExt: List<String> by AdkStringListProperty(project, listOf("cpp"))

    var cppModuleMapExt: List<String> by AdkStringListProperty(project, listOf("modulemap"))

    internal val includeProp = AdkFileListProperty(project, conventionValue = emptyList())
    var include: List<File> by includeProp

    fun include(vararg items: Any)
    {
        includeProp.Append(items)
    }

    internal val libDirProp = AdkFileListProperty(project, conventionValue =  emptyList())
    var libDir: List<File> by libDirProp

    fun libDir(vararg items: Any)
    {
        libDirProp.Append(items)
    }

    internal val modulesProp = AdkFileListProperty(project, conventionValue = emptyList())
    var modules: List<File> by modulesProp

    fun modules(vararg items: Any)
    {
        modulesProp.Append(items)
    }

    companion object {
        fun StripFileNameExtension(fileName: String): String = fileName.substringBeforeLast('.')

        fun GetFileNameExtension(fileName: String): String = fileName.substringAfterLast('.')
    }

    fun IsCppModuleIfaceFile(fileName: String): Boolean
    {
        return GetFileNameExtension(fileName) in cppModuleIfaceExt
    }

    fun IsCppImplFile(fileName: String): Boolean
    {
        return GetFileNameExtension(fileName) in cppModuleImplExt
    }

    fun IsCppModuleMapFile(fileName: String): Boolean
    {
        return GetFileNameExtension(fileName) in cppModuleMapExt
    }
}