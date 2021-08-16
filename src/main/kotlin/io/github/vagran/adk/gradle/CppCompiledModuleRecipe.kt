package io.github.vagran.adk.gradle

import org.gradle.api.Task
import org.gradle.api.tasks.Exec

/** @param modules Target module with all direct and indirect dependencies. */
class CppCompiledModuleRecipe(private val compilerInfo: CompilerInfo,
                              private val modules: Iterable<ModuleNode>,
                              private val defines: Iterable<String>): Recipe {

    override fun CreateTask(artifact: BuildNode, taskFactory: Recipe.TaskFactory<*>): Task
    {
        artifact as CppCompiledModuleFileNode
        val task = taskFactory.CreateTask<Exec>()
        val cmd = compilerInfo.GetCommandLineBuilder()

        cmd.SetAction(CommandLineBuilder.Action.COMPILE_CPP_MODULE)

        val src = artifact.FindDep<CppModuleIfaceFileNode>().first().path
        cmd.AddInput(src)

        cmd.SetOutput(artifact.path)

        val depModules = artifact.FindDep<CppCompiledModuleFileNode>()
        depModules.map { it.path }.forEach { cmd.AddModuleDep(it) }

        compilerInfo.adkConfig.cflags.forEach { cmd.AddFlag(it) }
        artifact.module.cflags.forEach { cmd.AddFlag(it) }

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

    override val name = "Compile C++ module"
    override val taskNamePrefix = "cppm_compile_"
}