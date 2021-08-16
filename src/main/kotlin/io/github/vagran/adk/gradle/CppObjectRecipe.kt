package io.github.vagran.adk.gradle

import org.gradle.api.Task
import org.gradle.api.tasks.Exec

class CppObjectRecipe(private val compilerInfo: CompilerInfo,
                      private val modules: Iterable<ModuleNode>,
                      private val defines: Iterable<String>): Recipe {

    override fun CreateTask(artifact: BuildNode, taskFactory: Recipe.TaskFactory<*>): Task
    {
        artifact as ObjectFileNode
        val task = taskFactory.CreateTask<Exec>()
        val cmd = compilerInfo.GetCommandLineBuilder()

        cmd.SetAction(CommandLineBuilder.Action.COMPILE_CPP)

        val src = artifact.FindDep<CppFileNode>().first().path
        cmd.AddInput(src)

        cmd.SetOutput(artifact.path)

        val depModules = artifact.FindDep<CppCompiledModuleFileNode>()
        depModules.map { it.path }.forEach { cmd.AddModuleDep(it) }

        compilerInfo.adkConfig.cflags.forEach { cmd.AddFlag(it) }
        artifact.module?.also { it.cflags.forEach { cmd.AddFlag(it) } }

        defines.forEach { cmd.AddDefine(it) }

        compilerInfo.adkConfig.include.forEach { cmd.AddInclude(it) }
        modules.flatMap { it.include }.forEach { cmd.AddInclude(it) }

        modules.flatMap { it.moduleMap }.forEach { cmd.AddModuleMap(it) }

        task.commandLine = cmd.Build().toList()
        task.inputs.file(src)
        task.outputs.file(artifact.path)
        task.setDependsOn(depModules.map { it.task })
        return task
    }

    override val name = "Compile C++ object"
    override val taskNamePrefix = "cpp_compile_"
}