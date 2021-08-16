group = "io.github.vagran"
version = "1.0"


plugins {
    kotlin("jvm") version "1.4.31"
    id("java-gradle-plugin")
    id("maven-publish")
    id("com.gradle.plugin-publish") version "0.14.0"
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib", "1.4.31"))
}

pluginBundle {
    website = "https://github.com/vagran/adk-cpp-gradle-plugin"
    vcsUrl = "https://github.com/vagran/adk-cpp-gradle-plugin.git"
    tags = listOf("c++", "c++20", "modules", "clang", "adk")
}

gradlePlugin {
    plugins {
        create("customPlugin") {
            id = "io.github.vagran.adk.gradle"
            displayName = "ADK C++ builder"
            description = "C++ builder with C++20 modules support"
            implementationClass = "io.github.vagran.adk.gradle.AdkPlugin"
        }
    }
}