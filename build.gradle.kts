group = "io.github.vagran"
version = "1.0"


plugins {
    kotlin("jvm") version "1.4.31"
    id("java-gradle-plugin")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib", "1.4.31"))
}

gradlePlugin {
    plugins {
        create("customPlugin") {
            id = "io.github.vagran.adk.gradle"
            implementationClass = "io.github.vagran.adk.gradle.AdkPlugin"
        }
    }
}