package io.github.vagran.adk.gradle

import java.io.File
import kotlin.collections.ArrayList
import kotlin.collections.HashSet
import kotlin.collections.LinkedHashSet

class ClangCommandLineBuilder(private val compilerInfo: CompilerInfo): CommandLineBuilder {

    override fun SetAction(action: CommandLineBuilder.Action)
    {
        this.action = action
    }

    override fun AddInput(input: File)
    {
        this.inputs.add(input)
    }

    override fun SetOutput(output: File)
    {
        this.output = output
    }

    override fun AddFlag(flag: String)
    {
        flags.add(flag)
    }

    override fun AddDefine(def: String)
    {
        defines.add(def)
    }

    override fun AddInclude(path: File)
    {
        includes.add(path)
    }

    override fun AddLibDir(path: File)
    {
        libDirs.add(path)
    }

    override fun AddLib(lib: String)
    {
        libs.add(lib)
    }

    override fun AddModuleDep(path: File)
    {
        moduleDeps.add(path)
    }

    override fun AddModuleMap(path: File)
    {
        moduleMaps.add(path)
    }

    override fun Build(): List<String>
    {
        val result = ArrayList<String>()
        result.add(compilerInfo.cxx)
        if (action == CommandLineBuilder.Action.COMPILE_CPP ||
            action == CommandLineBuilder.Action.COMPILE_CPP_MODULE) {

            result.add("-c")
        }
        result.add("-std=c++20")
        result.add("-fmodules")
        result.add("-fimplicit-modules")
        compilerInfo.moduleCacheDir?.also { result.add("-fmodules-cache-path=$it") }
        result.add("-stdlib=libc++")
        result.addAll(flags)
        if (action == CommandLineBuilder.Action.COMPILE_CPP_MODULE) {
            result.addAll(arrayOf("--precompile", "-x", "c++-module"))
        }
        defines.forEach { result.add("-D$it") }
        includes.forEach { result.add("-I$it") }
        if (action == CommandLineBuilder.Action.LINK) {
            libDirs.forEach { result.add("-L$it") }
            libs.forEach { result.add("-l$it") }
        }
        moduleDeps.forEach { result.add("-fmodule-file=$it") }
        moduleMaps.forEach { result.add("-fmodule-map-file=$it") }
        inputs.forEach { result.add(it.path) }
        output?.also {
            result.add("-o")
            result.add(it.path)
        }
        if (action == CommandLineBuilder.Action.COMPILE_CPP_MODULE) {
            result.add("-Xclang")
            result.add("-emit-module-interface")
        }
        return result
    }

    private lateinit var action: CommandLineBuilder.Action
    private val inputs = LinkedHashSet<File>()
    private var output: File? = null
    private val flags = HashSet<String>()
    private val defines = HashSet<String>()
    private val includes = HashSet<File>()
    private val libDirs = HashSet<File>()
    private val libs = LinkedHashSet<String>()
    private val moduleDeps = HashSet<File>()
    private val moduleMaps = HashSet<File>()
}