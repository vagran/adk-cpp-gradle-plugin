package io.github.vagran.adk.gradle

import com.google.gson.GsonBuilder
import org.gradle.api.Task
import org.gradle.api.tasks.Delete
import org.gradle.api.tasks.Exec
import java.io.File
import java.nio.file.Files
import java.util.*
import kotlin.reflect.KClass

/** Represents build plan with all artifacts, their dependencies and required actions to produce
 * each one.
 */
class BuildTree(private val adkConfig: AdkExtension) {

    val buildDir: File get() = adkConfig.project.buildDir.resolve(adkConfig.buildType)

    fun Build(moduleRegistry: ModuleRegistry)
    {
        rootNodes.addAll(Builder(moduleRegistry).Build())
    }

    fun CreateBuildTask(): Task
    {
        val tasks = ArrayList<Task>()
        rootNodes.forEach {
            val task = CreateTask(it)
            if (task != null) {
                tasks.add(task)
            }
        }
        val task = adkConfig.project.tasks.register("build", Task::class.java).get()
        task.group = "build"
        task.description = "Build ${adkConfig.binName}"
        task.setDependsOn(tasks)
        return task
    }

    fun CreateCleanTask(): Task
    {
        val task = adkConfig.project.tasks.register("clean", Delete::class.java).get()
        task.delete(buildDir)
        task.group = "clean"
        task.description = "Delete all build artifacts"
        return task
    }

    fun CreateCompileDbTask(): Task
    {
        val tasks = ArrayList<Task>()

        fun AddTask(node: BuildNode)
        {
            node.task?.also { tasks.add(it) }
        }

        rootNodes.forEach {
            it.FindDepDeep<CppCompiledModuleFileNode>().forEach(::AddTask)
            it.FindDepDeep<ObjectFileNode>().forEach(::AddTask)
        }

        val task = adkConfig.project.tasks.register("generateCompileDb", Task::class.java).get()
        task.group = "Compile DB"
        task.description = "Generator compile DB JSON file"
        val dbFile = buildDir.resolve("compile_commands.json")
        task.outputs.file(dbFile)

        task.doFirst {
            println("[Generate compilation database] $dbFile")
        }
        task.doLast {
            CreateCompileDbFile(tasks, dbFile)
        }
        return task
    }

    fun GetObjBuildDirectory(sourceDirPath: File): File
    {
        val projectDir = adkConfig.project.projectDir.normalize().toPath()
        val objDir = buildDir.resolve("obj").toPath()
        val srcDir = sourceDirPath.normalize().toPath()
        return if (srcDir.startsWith(projectDir)) {
            val relPath = projectDir.relativize(srcDir)
            objDir.resolve(relPath).toFile()
        } else {
            objDir.resolve("__external").resolve(srcDir.subpath(0, srcDir.nameCount)).toFile()
        }
    }

    fun GetObjBuildPath(sourcePath: File, replaceExtension: String? = null): File
    {
        val dir = GetObjBuildDirectory(sourcePath.parentFile)
        val name = if (replaceExtension != null) {
            sourcePath.nameWithoutExtension + "." + replaceExtension
        } else {
            sourcePath.name
        }
        return dir.resolve(name)
    }

    // /////////////////////////////////////////////////////////////////////////////////////////////

    private inner class Builder(private val moduleRegistry: ModuleRegistry) {

        private val compilerInfo = CompilerInfo(adkConfig, buildDir.resolve("modules-cache"))
        private val processedModules = HashMap<ModuleNode, Iterable<BuildNode>>()
        private val defines = HashSet<String>()

        fun Build(): List<BuildNode>
        {
            if (adkConfig.binType != BinType.APP.value) {
                throw Error("Binary type other than application is not yet supported")
            }

            if (moduleRegistry.mainModules.isEmpty()) {
                throw Error("No main module(s) specified")
            }

            val modules = ModuleRegistry.GatherAllDependencies(moduleRegistry.mainModules)

            defines.addAll(adkConfig.define)
            modules.forEach { defines.addAll(it.define) }

            val binRecipe = CppAppExecutableRecipe(compilerInfo, modules)
            val binFile = ExecutableFileNode(buildDir.resolve(adkConfig.binName), binRecipe)
            for (mainModule in moduleRegistry.mainModules) {
                binFile.dependencies.addAll(ProcessModule(mainModule))
            }
            return listOf(binFile)
        }

        /** @return Nodes produces by the specified module. */
        private fun ProcessModule(module: ModuleNode): Iterable<BuildNode>
        {
            val cached = processedModules[module]
            if (cached != null) {
                return cached
            }
            val modules = ModuleRegistry.GatherAllDependencies(module)
            val resultNodes = ArrayList<BuildNode>()

            val depNodes = ArrayList<BuildNode>()
            module.dependNodes.forEach {
                depModule ->
                depNodes.addAll(ProcessModule(depModule))
            }

            fun AddNode(node: BuildNode) {
                resultNodes.add(node)
                node.dependencies.addAll(depNodes)
            }

            var compiledModule: CppCompiledModuleFileNode? = null
            module.ifaceFile?.also {
                ifaceFile ->
                val _compiledModule = CppCompiledModuleFileNode(
                    GetObjBuildPath(ifaceFile, compilerInfo.cppCompiledModuleExt),
                    module,
                    CppCompiledModuleRecipe(compilerInfo, modules, defines))
                _compiledModule.dependencies.add(CppModuleIfaceFileNode(module))
                AddNode(_compiledModule)
                compiledModule = _compiledModule
            }

            module.implFiles.forEach {
                implFile ->
                val objectFile = ObjectFileNode(
                    GetObjBuildPath(implFile, compilerInfo.objFileExt),
                    CppObjectRecipe(compilerInfo, modules, defines),
                    module)
                objectFile.dependencies.add(CppFileNode(implFile, module = module))
                compiledModule?.also { objectFile.dependencies.add(it) }
                AddNode(objectFile)
            }

            processedModules[module] = resultNodes
            return resultNodes
        }
    }

    private class Counter(var value: Int = 0)

    private val taskNameCounters = TreeMap<String, Counter>()

    private val rootNodes = ArrayList<BuildNode>()

    private fun GetTaskName(prefix: String): String
    {
        val idx = taskNameCounters.computeIfAbsent(prefix) { Counter() }.value++
        return "$prefix$idx"
    }

    private fun GetTaskFactory(node: BuildNode): Recipe.TaskFactory<*>
    {
        val recipe = node.recipe!!
        return Recipe.TaskFactory {
            cls: KClass<Task> ->
            val name = GetTaskName(recipe.taskNamePrefix)
            val task = adkConfig.project.tasks.register(name, cls.java).get()
            task.group = recipe.taskGroup
            task.description = "${recipe.name} $node"
            task
        }
    }

    private fun CreateTask(node: BuildNode): Task?
    {
        node.task?.also { return it }
        node.dependencies.forEach { CreateTask(it) }
        node.recipe?.also {
            recipe ->
            val task = recipe.CreateTask(node, GetTaskFactory(node))
            node.task = task
            if (task is Exec) {
                task.doFirst {
                    println(task.commandLine.joinToString(" "))
                }
            }
            task.doFirst {
                println("[${recipe.name}] $node")
            }
            return task
        }
        return null
    }

    data class CompileDbEntry(val directory: String, val file: String, val arguments: List<String>,
                              val output: String)

    private fun CreateCompileDbFile(tasks: Iterable<Task>, filePath: File)
    {
        val builder = GsonBuilder()
        builder.setPrettyPrinting()
        builder.disableHtmlEscaping()
        val gson = builder.create()

        val db = ArrayList<CompileDbEntry>()

        val dirPath = adkConfig.project.rootDir.toString()
        tasks.forEach {
            task ->
            if (task !is Exec) {
                return@forEach
            }
            db.add(CompileDbEntry(dirPath,
                                  task.inputs.files.singleFile.absolutePath,
                                  task.commandLine,
                                  task.outputs.files.singleFile.absolutePath))
        }

        Files.newBufferedWriter(filePath.toPath()).use {
            writer ->
            gson.toJson(db, writer)
        }
    }
}
