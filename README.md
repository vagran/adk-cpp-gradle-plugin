This Gradle plugin implements build system for C++20 modules. Currently, it only supports Clang 
compiler.

It is developed due to a current lack of good support of C++ modules in other mainstream build 
systems (e.g., CMake, however I know it is work in progress there on some experimental support), so 
I can play with modules in my pet projects. I do not have any serious plans for its further 
development, however, I will probably stick with it in my projects (I hate CMake syntax), so new 
features will be added as per my needs.

## Basic usage

Apply the plugin (published on 
[Gradle plugins portal](https://plugins.gradle.org/plugin/io.github.vagran.adk.gradle)):
```kotlin
plugins {
    id("io.github.vagran.adk.gradle") version "1.0.1"
}
```

Add `adk` block in your project `build.gradle.kts` file. This block defines some global build 
settings
```kotlin
adk {
    // Default is "adkCxx" project property or "CXX" environment variable value
    cxx = "/compiler/path"
    include("/usr/include", "/other/include")
    define("MYDEF", "MY_NAME=MY_VALUE")
    cflags("-Wall", "-pthread")
    linkflags("-pthread")
    libs("z")
    libdir("/usr/lib")
    // Default is "release" or from adkBuildType project property
    buildType = "debug"
    // "app" (default), "lib", "sharedLib"
    binType = "app"
    // Default is project.name
    binName = "myApp"
    // Default is ["cppm"]
    cppModuleIfaceExt = listOf("cxxm")
    // Default is ["cpp"]
    cppModuleImplExt = listOf("cxx")
    // Default is ["modulemap"]
    cppModuleMapExt = listOf("modulemap")
    modules("module", "root", "directories")
}
```
Most of function-style properties accept variable-length list of arguments, which are appended to
the global list.

Kotlin or Groovy syntax can be used for some build customizations, for example:
```kotlin
adk {
    if (buildType == "debug") {
        cflags("-g", "-O0")
        define("DEBUG")
    } else {
        cflags("-O3")
    }
}
```

`cxx` property should point to C++ compiler executable path. Currently, only Clang is supported (its
specific features and command line options are used), however, this compiler-specific logic is 
separated in the code, so other compilers may be added later. Usually specifying local paths should
not be done in a makefile, so it can be done by creating `gradle.properties` file (which should be
added to `.gitignore`) with the following content:
```
adkCxx=/path/to/clang/bin/clang++
```
Alternatively, command-line options (`-PadkCxx=/path/to/clang/bin/clang++`) or `CXX` environment 
variable can be used for that purpose.

Your project should contain one or more C++ modules. `modules` property specifies directories to 
look for modules in. When looking for a module, `module.gradle` file is evaluated against the 
project if present (Kotlin file seems to be not supported for that, I did not figured out how to use 
custom plugin from it). Particularly it is useful to place `module` block here which is evaluated 
against the current module.
```groovy
module {
    name = "module_name"
    // Fully qualified module names
    depends("dependency", "modules")
    // Default is "include" if present
    include("additional", "include", "directories")
    // Default is "impl" directory if present and cpp file matching module basename
    impl("implementation", "files", "and", "directories")
    // Default are all subdirectories except ones above
    submodules("submodules", "directories")
    // Default is "module_basename.modulemap" if present (extension may differ, depends on 
    // "adk.cppModuleMapExt")
    moduleMap("modulemap", "files")
    define(...)
    cflags(...)
    libs(...)
    libdir(...)
    exclude("excluded", "files", "and", "directories")
}
```
Most of the properties have reasonable defaults. `name` property is mandatory only for _top-level
module_ (i.e. one which path is specified in `adk.modules` property). Modules specified as _submodules_
by `submodules` property (or just by looking into subdirectories by default) have implicit name 
which is derived from parent module name by appending submodule directory name with dot. For example,
having such modules directories structure:
```
+-foo
  +-bar
```
implies `foo.bar` submodule name which still can be overridden by `name` property if needed.

Although, C++ standard does not restrict module naming convention, this plugin assumes modules
hierarchy reflected by a module name, where dot is submodules separator. Last component of a module
fully qualified name is considered module _basename_.

Each module may have exactly one module interface file (_partitions_ are not yet supported since they
are not yet supported by Clang). Its name is expected to be `module_basename.cppm` (extension may
differ if overridden by `adk.cppModuleIfaceExt` property).

Interface file is optional for a module. A module without interface may be useful just to imply
hierarchical name, or may be just to add some header files or libraries.

There may be other module interface files in a module directory, they will be considiered part of
the corresponding submodule. For example, in the following directory layout:
```
+-foo
  +-foo.cppm
  +-bar.cppm
```
modules `foo` (_directory default module_) and `foo.bar` will be created. Configuration for `foo.bar` 
module can be specified in a named module block in `module.gradle`:
```groovy
module("bar") {
    define("BAR")
}
```

Module may have implementation files. There are several rules for their matching. 
`module_basename.cpp` (extension may differ if overridden by `adk.cppModuleImplExt` property) is
matched to corresponding module. `impl` directory content is scanned recursively for implementation
files which are assigned to directory default module. Custom directories and files may be specified
by `impl` property. Any leftover files are assigned to a directory default module. For example, with
the following module directory layout:
```
+-foo
  +-impl
  | +-my_class.cpp
  +-foo.cppm
  +-foo.cpp
  +-bar.cppm
  +-bar.cpp
  +-other.cpp
```
`foo` module will have `foo.cpp`, `other.cpp` and `my_class.cpp` implementation files and `bar.cpp`
will be assigned to `foo.bar` module.

Module directory is traversed recursively by default to search for submodules. Directories specified
in `exclude`, `include`, `impl` etc. properties are not traversed. Exact list of submodule 
directories can be specified by `submodules` property. This will disable implicit search in current
directory.

Include directories, flags and libraries specified for a module are propagated to all its 
dependencies. Preprocessor symbols are propagated globally to whole the project (otherwise common
dependencies from standard library may be compiled several times and will conflict).

A project requires at least one _main_ module. It is typically one which defines `main()` function.
Main module is marked with `main` method in `module.gradle`:
```groovy
module {
    main()
}
```
Name is not required for such a module (it is silently ignored if specified). Submodules implied 
names are not prepended by any prefix. Several main modules may be specified, they are all linked
together into the resulting executable. Interface file is not really usable in main module if 
building application binary. However, all C++ files should be valid module files, so typical
`main.cpp` may look like this:
```cpp
module;

/* Include some headers which cannot be imported. */
#include <signal.h>

/* Still need some name to start module section. */
export module main;

import std.core;

/* Main function should be exported. */
export int
main(int argc, char **argv)
{
}
```

## Compilation database support

The plugin is able to generate 
[compilation database](https://clang.llvm.org/docs/JSONCompilationDatabase.html) JSON file for
integration with IDEs which support it. Use `generateCompileDb` target to generate it.