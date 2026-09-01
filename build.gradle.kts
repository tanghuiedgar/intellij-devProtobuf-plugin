fun properties(key: String): Provider<String> = providers.gradleProperty(key)

fun environment(key: String): Provider<String> = providers.environmentVariable(key)

plugins {
    id("java")
    alias(libs.plugins.kotlin)
    kotlin("plugin.lombok") version "2.3.0"
}

group = properties("pluginGroup").get()
version = properties("pluginVersion").get()
val artifactId = "intellij-devProtobuf-plugin"

repositories {
    mavenLocal()
    // 阿里云中央仓库镜像
    maven(url = "https://maven.aliyun.com/repository/public")
    // 阿里云JCenter镜像（部分旧构件仍在用）
    maven(url = "https://maven.aliyun.com/repository/jcenter")
    // Google镜像（如需Android/Guava等）
    maven(url = "https://maven.aliyun.com/repository/google")
    // Gradle插件门户镜像（如有用到插件坐标）
    maven(url = "https://maven.aliyun.com/repository/gradle-plugin")
    maven("https://cache-redirector.jetbrains.com/intellij-repository/releases")
    maven("https://cache-redirector.jetbrains.com/intellij-repository/snapshots")
    // Maven中央仓库作为兜底
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
}

// Set the JVM language level used to build the project.
kotlin {
    jvmToolchain(25)
    compilerOptions {
        freeCompilerArgs.add("-Xjvm-default=all")
    }
}