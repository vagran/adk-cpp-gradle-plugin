package io.github.vagran.adk.gradle

enum class BuildType(val value: String) {
    RELEASE("release"),
    DEBUG("debug")
}

enum class BinType(val value: String) {
    APP("app"),
    LIB("lib"),
    SHARED_LIB("sharedLib")
}

enum class PropName(val value: String) {
    ADK_CXX("adkCxx"),
    ADK_BUILD_TYPE("adkBuildType")
}