package io.github.vagran.adk.gradle

import java.io.File
import java.util.*
import kotlin.collections.LinkedHashSet

class ModuleRegistry(private val adkConfig: AdkExtension) {

    fun interface ModuleScriptReader {
        /**
         * @param dirPath Directory to check for module script.
         * @return Extension context with all read data, or null if no script file found.
         */
        fun Read(dirPath: File): ModuleExtensionContext?
    }

    fun Build(moduleScriptReader: ModuleScriptReader)
    {
        for (modPath in adkConfig.modules) {
            ScanDirectory(modPath.normalize(), null, moduleScriptReader)
        }
        ResolveDependencies()
    }

    companion object {
        /** Get last component of fully qualified module name. */
        fun GetSubmoduleName(moduleName: String): String = moduleName.substringAfterLast('.')

        /**
         * Gather full modules list which also includes all direct and indirect dependencies of the
         * specified modules.
         */
        fun GatherAllDependencies(modules: List<ModuleNode>): Iterable<ModuleNode>
        {
            val result = LinkedHashSet<ModuleNode>()

            fun AddDependencies(module: ModuleNode)
            {
                if (result.contains(module)) {
                    return
                }
                result.add(module)
                for (dep in module.dependNodes) {
                    AddDependencies(dep)
                }
            }

            modules.forEach(::AddDependencies)

            return result
        }

        fun GatherAllDependencies(module: ModuleNode): Iterable<ModuleNode>
        {
            return GatherAllDependencies(listOf(module))
        }
    }

    val modules = TreeMap<String, ModuleNode>()
    val mainModules = ArrayList<ModuleNode>()

    // /////////////////////////////////////////////////////////////////////////////////////////////

    /** Indexes file names both with and without extension. */
    private class FileNameSet {
        /** Key is basename, value is extension. */
        val baseNames = TreeMap<String, String>()

        fun Add(fileName: String)
        {
            val baseName = AdkExtension.StripFileNameExtension(fileName)
            val ext = baseNames[baseName]
            if (ext != null) {
                throw Error("Several files have the same basename in one directory which is " +
                            "disallowed: `$fileName` and `$baseName.$ext`")
            }
            baseNames[baseName] = AdkExtension.GetFileNameExtension(fileName)
        }

        fun Remove(baseName: String)
        {
            baseNames.remove(baseName)
        }

        fun GetFileName(baseName: String): String?
        {
            val ext = baseNames[baseName] ?: return null
            return "$baseName.$ext"
        }
    }

    private fun ScanDirectory(dirPath: File, implicitModuleName: String?,
                              moduleScriptReader: ModuleScriptReader)
    {
        val moduleScript = moduleScriptReader.Read(dirPath)
        val dirs = TreeSet<String>()
        val implFiles = FileNameSet()
        val ifaceFiles = FileNameSet()
        val moduleMapFiles = FileNameSet()
        val excludedFiles = TreeSet<File>()

        if (moduleScript != null) {
            for (file in moduleScript.exclude) {
                excludedFiles.add(file.normalize())
            }
        }

        for (fileName in dirPath.list() ?: emptyArray()) {
            val path = dirPath.resolve(fileName)
            if (path in excludedFiles) {
                continue
            }
            if (path.isDirectory) {
                dirs.add(fileName)
                continue
            }
            if (path.isFile) {
                if (adkConfig.IsCppImplFile(fileName)) {
                    implFiles.Add(fileName)
                    continue
                } else if (adkConfig.IsCppModuleIfaceFile(fileName)) {
                    ifaceFiles.Add(fileName)
                    continue
                } else if (adkConfig.IsCppModuleMapFile(fileName)) {
                    moduleMapFiles.Add(fileName)
                }
            }
        }

        fun SetModuleDefaultFiles(module: ModuleNode, name: String)
        {
            val baseName = GetSubmoduleName(name)
            ifaceFiles.GetFileName(baseName)?.also {
                module.SetIfaceFile(dirPath.resolve(it))
                ifaceFiles.Remove(baseName)
            }
            implFiles.GetFileName(baseName)?.also {
                module.AddImplFile(dirPath.resolve(it), adkConfig)
                implFiles.Remove(baseName)
            }
            moduleMapFiles.GetFileName(baseName)?.also {
                module.moduleMap.add(dirPath.resolve(it))
                moduleMapFiles.Remove(baseName)
            }
        }

        val isMain = moduleScript?.main ?: false
        /* Default module is one represented by this directory. There may be other modules implied
        * by module interface files and optionally configured by named module block in the module
        * script.
        */
        val defaultModuleName =
            if (moduleScript != null && moduleScript.nameProp.prop.isPresent) {
                moduleScript.name
            } else if (implicitModuleName != null) {
                implicitModuleName
            } else if (isMain) {
                "main"
            } else {
                throw Error("Module name should be specified for modules root directory $dirPath")
            }
        val defaultModule = ModuleNode(defaultModuleName, dirPath, true, isMain)
        if (moduleScript != null) {
            defaultModule.Configure(moduleScript, adkConfig)
        }
        defaultModule.FinishConfiguration(adkConfig)
        if (!isMain) {
            SetModuleDefaultFiles(defaultModule, defaultModuleName)
        }

        /* All the rest interface files imply submodules. */
        val modules = TreeMap<String, ModuleNode>()
        while (ifaceFiles.baseNames.isNotEmpty()) {
            val submoduleName = ifaceFiles.baseNames.keys.first()
            val moduleName = defaultModule.GetSubmoduleFullName(submoduleName)
            val module = ModuleNode(moduleName, dirPath, false)
            SetModuleDefaultFiles(module, moduleName)
            val moduleConfig = moduleScript?.childContexts?.get(submoduleName)
            if (moduleConfig != null) {
                module.Configure(moduleConfig, adkConfig)
            }
            module.FinishConfiguration(adkConfig)
            modules[moduleName] = module
        }

        /* Do not allow name module block without interface file. */
        if (moduleScript != null) {
            moduleScript.childContexts.keys.forEach {
                submoduleName ->
                val moduleName = defaultModule.GetSubmoduleFullName(submoduleName)
                if (!modules.containsKey(moduleName)) {
                    throw Error("Named block `$submoduleName` specified without corresponding " +
                                "module interface file in $dirPath")
                }
            }
        }

        fun IsImplFileReferenced(fileName: String): Boolean
        {
            val path = dirPath.resolve(fileName)
            if (defaultModule.implFiles.contains(path)) {
                return true
            }
            for (module in modules.values) {
                if (module.implFiles.contains(path)) {
                    return true
                }
            }
            return false
        }

        /* Filter out implementation files reference from modules configuration. */
        implFiles.baseNames.entries.removeAll { IsImplFileReferenced("${it.key}.${it.value}") }

        /* Add all leftover implementation files to default module. */
        implFiles.baseNames.entries.forEach {
            defaultModule.AddImplFile(dirPath.resolve("${it.key}.${it.value}"), adkConfig)
        }

        fun AddModule(module: ModuleNode) {
            if (module.isMain) {
                mainModules.add(module)
                return
            }
            val existingModule = this.modules[module.name]
            if (existingModule != null) {
                throw Error("Module already registered: $module, " +
                            "previous definition: $existingModule")
            }
            this.modules[module.name] = module
        }
        AddModule(defaultModule)
        modules.values.forEach(::AddModule)

        val submodulePaths = TreeSet<File>()
        fun ProcessSubmodules(module: ModuleNode) {
            val config = module.config ?: return
            for (submodulePath in config.submodules.map { it.normalize() }) {
                submodulePaths.add(submodulePath)

                ScanDirectory(submodulePath,
                              if (submodulePath.parentFile == dirPath)
                                  module.GetSubmoduleFullName(submodulePath.name) else null,
                              moduleScriptReader)
            }
        }

        ProcessSubmodules(defaultModule)
        modules.values.forEach { ProcessSubmodules(it) }

        subdirLoop@ for (subdirName in dirs) {
            val subdirPath = dirPath.resolve(subdirName)
            if (submodulePaths.contains(subdirPath)) {
                continue
            }
            if (defaultModule.IsDirReferenced(subdirPath)) {
                continue
            }
            for (module in modules.values) {
                if (module.IsDirReferenced(subdirPath)) {
                    continue@subdirLoop
                }
            }
            ScanDirectory(subdirPath, defaultModule.GetSubmoduleFullName(subdirName),
                          moduleScriptReader)
        }
    }

    private fun ResolveDependencies()
    {
        val stack = ArrayList<ModuleNode>()
        for (module in mainModules) {
            ResolveDependencies(module, stack)
        }
    }

    private fun ResolveDependencies(module: ModuleNode, stack: MutableList<ModuleNode>)
    {
        for (depName in module.depends) {
            val depModule = modules[depName]
                ?: throw Error("Module $module dependency not satisfied: $depName")
            if (stack.contains(depModule)) {
                throw Error("Circular reference detected for module $depModule when resolving " +
                            "dependency of $module")
            }
            module.dependNodes.add(depModule)
            stack.add(module)
            ResolveDependencies(depModule, stack)
            stack.removeLast()
        }
    }
}