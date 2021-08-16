package io.github.vagran.adk.gradle

import org.gradle.api.Project
import org.gradle.api.internal.file.BaseDirFileResolver
import java.io.File
import kotlin.reflect.KProperty

/** Helper class for delegated property mapped to Gradle Property class.
 * @param readOnlyMessage Throw exception with this message if attempting to set value,
 *  when non-null.
 */
class AdkProperty<T>(project: Project, type: Class<T>, conventionValue: T? = null,
                     conventionValueProvider: (() -> T)? = null,
                     val validator: ((T) -> Unit)? = null,
                     val readOnlyMessage: String? = null) {

    val prop = project.objects.property(type).apply {
        if (conventionValue != null) {
            convention(conventionValue)
        } else if (conventionValueProvider !== null) {
            convention(project.provider(conventionValueProvider))
        }
    }

    operator fun getValue(thisRef: Any, property: KProperty<*>): T
    {
        val value = prop.get()
        validator?.invoke(value)
        return value
    }

    operator fun setValue(thisRef: Any, property: KProperty<*>, value: T)
    {
        readOnlyMessage?.also { throw Error(it) }
        validator?.invoke(value)
        prop.set(value)
    }
}


class AdkStringListProperty(project: Project, conventionValue: Iterable<String>? = null,
                            conventionValueProvider: (() -> Iterable<String>)? = null) {

    val prop = project.objects.listProperty(String::class.java).apply {
        if (conventionValue != null) {
            convention(conventionValue)
        } else if (conventionValueProvider !== null) {
            convention(project.provider(conventionValueProvider))
        }
    }

    operator fun getValue(thisRef: Any, property: KProperty<*>): List<String> = prop.get()

    operator fun setValue(thisRef: Any, property: KProperty<*>, value: List<String>)
    {
        prop.set(value)
    }

    fun Append(items: Iterable<String>)
    {
        prop.addAll(items)
    }

    fun Append(items: Array<out String>)
    {
        prop.addAll(*items)
    }
}


class AdkFileListProperty(project: Project,
                          baseDir: Any? = null,
                          conventionValue: Iterable<File>? = null,
                          conventionValueProvider: (() -> Iterable<File>)? = null) {

    val prop = project.objects.listProperty(File::class.java).apply {
        if (conventionValue != null) {
            convention(conventionValue)
        } else if (conventionValueProvider !== null) {
            convention(project.provider(conventionValueProvider))
        }
    }

    private val resolver = BaseDirFileResolver(
        if (baseDir != null) project.file(baseDir) else project.projectDir)

    operator fun getValue(thisRef: Any, property: KProperty<*>): List<File> = prop.get()

    operator fun setValue(thisRef: Any, property: KProperty<*>, value: List<File>)
    {
        prop.set(value)
    }

    fun Append(items: Iterable<Any>)
    {
        prop.addAll(items.map { resolver.resolve(it) })
    }

    fun Append(items: Array<out Any>)
    {
        prop.addAll(items.map { resolver.resolve(it) })
    }
}



