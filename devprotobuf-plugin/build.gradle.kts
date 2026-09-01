import org.jetbrains.changelog.Changelog
import org.jetbrains.changelog.markdownToHTML
import org.jetbrains.kotlin.gradle.internal.ensureParentDirsCreated
import org.jetbrains.intellij.platform.gradle.TestFrameworkType
import org.gradle.kotlin.dsl.register

fun properties(key: String): Provider<String> = providers.gradleProperty(key)

fun environment(key: String): Provider<String> = providers.environmentVariable(key)

plugins {
    id("java") // Java support
    id("maven-publish")
    alias(libs.plugins.kotlin) // Kotlin support
    alias(libs.plugins.intelliJPlatform) // Gradle IntelliJ Plugin
    alias(libs.plugins.changelog) // Gradle Changelog Plugin
    alias(libs.plugins.qodana) // Gradle Qodana Plugin
    alias(libs.plugins.kover) // Gradle Kover Plugin
    kotlin("plugin.lombok") version "2.3.0"
}

group = properties("pluginGroup").get()
version = properties("pluginVersion").get()
val artifactId = "intellij-devProtobuf-plugin"
// Configure project's dependencies
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

    // IntelliJ Platform Gradle Plugin Repositories Extension - read more: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin-repositories-extension.html
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    // ⭐ 引入自己的 core
    implementation(project(":devprotobuf-core"))

    implementation(libs.fastutil)
    testRuntimeOnly(libs.fastutil)
    runtimeOnly(libs.asm.all)

    // 测试
    testImplementation(libs.junit)
    testImplementation(platform(libs.junit.bom))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher") {
        because("Only needed to run tests in a version of IntelliJ IDEA that bundles older versions")
    }
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
    testRuntimeOnly("org.junit.vintage:junit-vintage-engine")
    // IntelliJ Platform Gradle Plugin Dependencies Extension - read more: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin-dependencies-extension.html

    intellijPlatform {
        this.create(
            providers.gradleProperty("platformType"),
            providers.gradleProperty("platformVersion")
        ) { this.useInstaller = false }

        // Plugin Dependencies. Uses `platformBundledPlugins` property from the gradle.properties file for bundled IntelliJ Platform plugins.
        bundledPlugins(providers.gradleProperty("platformBundledPlugins").map { it.split(',') })

        // bundledModules(providers.gradleProperty("platformModulePlugins").map { it.split(',') })

        // Plugin Dependencies. Uses `platformPlugins` property from the gradle.properties file for plugin from JetBrains Marketplace.
        plugins(providers.gradleProperty("platformPlugins").map { it.split(',') })

        testFramework(TestFrameworkType.Platform)
    }
    testImplementation(kotlin("test"))
}

// Set the JVM language level used to build the project.
kotlin {
    jvmToolchain(25)
    compilerOptions {
        freeCompilerArgs.add("-Xjvm-default=all")
    }
}

// Configure IntelliJ Platform Gradle Plugin - read more: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin-extension.html
intellijPlatform {
    pluginConfiguration {
        version = providers.gradleProperty("pluginVersion")

        // Extract the <!-- Plugin description --> section from README.md and provide for the plugin's manifest
        description = providers.fileContents(layout.projectDirectory.file("README.md")).asText.map {
            val start = "<!-- Plugin description -->"
            val end = "<!-- Plugin description end -->"

            with(it.lines()) {
                if (!containsAll(listOf(start, end))) {
                    throw GradleException("Plugin description section not found in README.md:\n$start ... $end")
                }
                subList(indexOf(start) + 1, indexOf(end)).joinToString("\n").let(::markdownToHTML)
            }
        }

        val changelog = project.changelog // local variable for configuration cache compatibility
        // Get the latest available change notes from the changelog file
        changeNotes = providers.gradleProperty("pluginVersion").map { pluginVersion ->
            with(changelog) {
                renderItem(
                    (getOrNull(pluginVersion) ?: getUnreleased())
                        .withHeader(false)
                        .withEmptySections(false),
                    Changelog.OutputType.HTML,
                )
            }
        }

        ideaVersion {
            sinceBuild = providers.gradleProperty("pluginSinceBuild")
            untilBuild = providers.gradleProperty("pluginUntilBuild")
        }
    }

    signing {
        certificateChain = providers.environmentVariable("CERTIFICATE_CHAIN")
        privateKey = providers.environmentVariable("PRIVATE_KEY")
        password = providers.environmentVariable("PRIVATE_KEY_PASSWORD")
    }

    publishing {
        token = providers.environmentVariable("PUBLISH_TOKEN")
        // The pluginVersion is based on the SemVer (https://semver.org) and supports pre-release labels, like 2.1.7-alpha.3
        // Specify pre-release label to publish the plugin in a custom Release Channel automatically. Read more:
        // https://plugins.jetbrains.com/docs/intellij/deployment.html#specifying-a-release-channel
        channels = providers.gradleProperty("pluginVersion")
            .map { listOf(it.substringAfter('-', "").substringBefore('.').ifEmpty { "default" }) }
    }

    pluginVerification {
        ides {
            recommended()
        }
    }
}

// Configure Gradle Changelog Plugin - read more: https://github.com/JetBrains/gradle-changelog-plugin
changelog {
    groups.empty()
    repositoryUrl = providers.gradleProperty("pluginRepositoryUrl")
}

// Configure Gradle Kover Plugin - read more: https://github.com/Kotlin/kotlinx-kover#configuration
kover {
    reports {
        total {
            xml {
                onCheck = true
            }
        }
    }
}

tasks {
    prepareSandbox {
        val file = sandboxConfigDirectory.file("disabled_plugins.txt").get().asFile
        doLast {
            file.ensureParentDirsCreated()
            file.writeText(
                buildString {
                    appendLine("idea.plugin.protoeditor")
                    appendLine("com.intellij.grpc")
                },
            )
        }

    }

    publishPlugin {
        dependsOn(patchChangelog)
    }
}



intellijPlatformTesting {
    runIde {
        register("runIdeForUiTests") {
            task {
                jvmArgumentProviders += CommandLineArgumentProvider {
                    listOf(
                        "-Drobot-server.port=8082",
                        "-Dide.mac.message.dialogs.as.sheets=false",
                        "-Djb.privacy.policy.text=<!--999.999-->",
                        "-Djb.consents.confirmation.enabled=false",
                    )
                }
            }

            plugins {
                robotServerPlugin()
            }
        }
    }
}