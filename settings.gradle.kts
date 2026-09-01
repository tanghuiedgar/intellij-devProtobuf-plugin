pluginManagement {
    plugins {
        id("org.jetbrains.kotlin.jvm") version "1.9.23"
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
rootProject.name = "intellij-devProtobuf-plugin"
include("devprotobuf-core")
include("devprotobuf-plugin")