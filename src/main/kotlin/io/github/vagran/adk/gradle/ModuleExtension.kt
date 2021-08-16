package io.github.vagran.adk.gradle

import groovy.lang.Closure
import org.gradle.api.Project
import java.io.File
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.KProperty

open class ModuleExtension(private val project: Project, private val nestedName: String?) {

    fun CreateContext(baseDir: File): ModuleExtensionContext
    {
        return ModuleExtensionContext(project, baseDir, nestedName).also { _ctx = it }
    }

    fun CloseContext()
    {
        _ctx = null
    }

    // /////////////////////////////////////////////////////////////////////////////////////////////

    var name: String by ContextProp(ModuleExtensionContext::name)

    var main: Boolean by ContextProp(ModuleExtensionContext::main)

    fun main()
    {
        ctx.main = true
    }

    var include: List<File> by ContextProp(ModuleExtensionContext::include)

    fun include(vararg items: Any)
    {
        ctx.includeProp.Append(items)
    }

    var libDir: List<File> by ContextProp(ModuleExtensionContext::libDir)

    fun libDir(vararg items: Any)
    {
        ctx.libDirProp.Append(items)
    }

    var submodules: List<File> by ContextProp(ModuleExtensionContext::submodules)

    fun submodules(vararg items: Any)
    {
        ctx.submodulesProp.Append(items)
    }

    var impl: List<File> by ContextProp(ModuleExtensionContext::impl)

    fun impl(vararg items: Any)
    {
        ctx.implProp.Append(items)
    }

    var moduleMap: List<File> by ContextProp(ModuleExtensionContext::moduleMap)

    fun moduleMap(vararg items: Any)
    {
        ctx.moduleMapProp.Append(items)
    }

    var depends: List<String> by ContextProp(ModuleExtensionContext::depends)

    fun depends(vararg items: String)
    {
        ctx.dependsProp.Append(items)
    }

    var define: List<String> by ContextProp(ModuleExtensionContext::define)

    fun define(vararg items: String)
    {
        ctx.defineProp.Append(items)
    }

    var cflags: List<String> by ContextProp(ModuleExtensionContext::cflags)

    fun cflags(vararg items: String)
    {
        ctx.cflagsProp.Append(items)
    }

    var linkflags: List<String> by ContextProp(ModuleExtensionContext::linkflags)

    fun linkflags(vararg items: String)
    {
        ctx.linkflagsProp.Append(items)
    }

    var libs: List<String> by ContextProp(ModuleExtensionContext::libs)

    fun libs(vararg items: String)
    {
        ctx.libsProp.Append(items)
    }

    var exclude: List<File> by ContextProp(ModuleExtensionContext::exclude)

    fun exclude(vararg items: Any)
    {
        ctx.excludeProp.Append(items)
    }

    fun module(name: String, config: Closure<ModuleExtension>) {
        if (nestedName != null) {
            throw Error("Module block can be nested in top-level module block only")
        }
        val e = ModuleExtension(project, name)
        if (name in ctx.childContexts) {
            throw Error("Named module block specified twice: $name")
        }
        ctx.childContexts[name] = (e.CreateContext(ctx.baseDir))
        project.configure(e, config)
        e.CloseContext()
    }

    // /////////////////////////////////////////////////////////////////////////////////////////////

    private var _ctx: ModuleExtensionContext? = null

    private val ctx: ModuleExtensionContext get()
    {
        return _ctx ?: throw Error("Module extension members can be accessed in module script only")
    }

    private inner class ContextProp<V>(private val prop: KMutableProperty1<ModuleExtensionContext, V>) {

        operator fun getValue(thisRef: Any, property: KProperty<*>) = prop.get(ctx)

        operator fun setValue(thisRef: Any, property: KProperty<*>, value: V) = prop.set(ctx, value)
    }
}