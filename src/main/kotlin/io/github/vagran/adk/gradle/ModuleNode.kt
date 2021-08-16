package io.github.vagran.adk.gradle

import java.io.File
import java.util.*
import kotlin.collections.ArrayList


class ModuleNode(val name: String, val dirPath: File, val isDefault: Boolean,
                 val isMain: Boolean = false) {
    var config: ModuleExtensionContext? = null
    val include = ArrayList<File>()
    val libDir = ArrayList<File>()
    val define = ArrayList<String>()
    val cflags = ArrayList<String>()
    val linkflags = ArrayList<String>()
    val libs = ArrayList<String>()
    var ifaceFile: File? = null
    val implFiles = TreeSet<File>()
    val moduleMap = ArrayList<File>()
    val depends = ArrayList<String>()
    val dependNodes = ArrayList<ModuleNode>()

    fun GetSubmoduleFullName(submoduleName: String): String
    {
        return if (isMain) submoduleName else "$name.$submoduleName"
    }

    fun Configure(moduleExtensionContext: ModuleExtensionContext, adkConfig: AdkExtension)
    {
        this.config = moduleExtensionContext
        include.addAll(moduleExtensionContext.include.map { it.normalize() })
        libDir.addAll(moduleExtensionContext.libDir.map { it.normalize() })
        define.addAll(moduleExtensionContext.define)
        cflags.addAll(moduleExtensionContext.cflags)
        linkflags.addAll(moduleExtensionContext.linkflags)
        libs.addAll(moduleExtensionContext.libs)
        depends.addAll(moduleExtensionContext.depends)
        moduleMap.addAll(moduleExtensionContext.moduleMap.map { it.normalize() })
        for (implFile in moduleExtensionContext.impl) {
            AddImplFile(implFile, adkConfig)
        }
    }

    fun FinishConfiguration(adkConfig: AdkExtension)
    {
        if (include.isEmpty() && isDefault) {
            val defIncludePath = dirPath.resolve("include")
            if (defIncludePath.isDirectory) {
                include.add(defIncludePath)
            }
        }
        if (isDefault) {
            val defImplPath = dirPath.resolve("impl")
            if (defImplPath.isDirectory) {
                AddImplFile(defImplPath, adkConfig)
            }
        }
    }

    fun SetIfaceFile(path: File)
    {
        ifaceFile = path.normalize()
    }

    fun AddImplFile(path: File, adkConfig: AdkExtension)
    {
        if (path.isDirectory) {
            for (child in path.listFiles() ?: emptyArray()) {
                AddImplFile(child, adkConfig)
            }
            return
        }
        if (adkConfig.IsCppImplFile(path.name)) {
            implFiles.add(path.normalize())
        }
    }

    fun IsDirReferenced(dirPath: File): Boolean
    {
        return include.contains(dirPath) || libDir.contains(dirPath)
    }

    override fun toString(): String
    {
        return "$name@[$dirPath]"
    }
}