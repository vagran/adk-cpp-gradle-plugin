package io.github.vagran.adk.gradle

import java.io.File

/** Provides info about compiler(s). Currently it is mostly fixed to clang compiler. */
class CompilerInfo(val adkConfig: AdkExtension, val moduleCacheDir: File?) {
    val cxx: String

    val cppCompiledModuleExt: String = "pcm"
    val objFileExt: String = "o"

    init {
        if (adkConfig.cxx.isBlank()) {
            throw Error("C++ compiler executable must be specified")
        }
        cxx = adkConfig.cxx
    }

    fun GetCommandLineBuilder(): CommandLineBuilder = ClangCommandLineBuilder(this)
}

interface CommandLineBuilder {

    enum class Action {
        COMPILE_CPP,
        COMPILE_CPP_MODULE,
        LINK,
        DEPS
    }

    fun SetAction(action: Action)

    fun AddInput(input: File)

    fun SetOutput(output: File)

    fun AddFlag(flag: String)

    fun AddDefine(def: String)

    fun AddInclude(path: File)

    fun AddLibDir(path: File)

    fun AddLib(lib: String)

    fun AddModuleDep(path: File)

    fun AddModuleMap(path: File)

    fun Build(): Iterable<String>
}