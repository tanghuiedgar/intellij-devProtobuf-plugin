import org.gradle.kotlin.dsl.register
import org.jetbrains.grammarkit.tasks.GenerateLexerTask
import org.jetbrains.grammarkit.tasks.GenerateParserTask
import org.jetbrains.intellij.platform.gradle.TestFrameworkType
import org.jetbrains.kotlin.gradle.internal.ensureParentDirsCreated

fun properties(key: String): Provider<String> = providers.gradleProperty(key)

plugins {
    id("java") // Java support
    id("maven-publish")
    alias(libs.plugins.kotlin) // Kotlin support
    alias(libs.plugins.intelliJPlatform) // Gradle IntelliJ Plugin
    //alias(libs.plugins.changelog) // Gradle Changelog Plugin
    //alias(libs.plugins.qodana) // Gradle Qodana Plugin
    alias(libs.plugins.kover) // Gradle Kover Plugin
    alias(libs.plugins.grammarkit) // IntelliJ Grammark kit Plugin
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
    maven(url = "https://cache-redirector.jetbrains.com/intellij-repository/releases")
    maven(url = "https://cache-redirector.jetbrains.com/intellij-repository/snapshots")
    // Maven中央仓库作为兜底
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    compileOnly(libs.jansi)
    compileOnly(libs.commons.lang3)
    // implementation group: 'com.google.googlejavaformat', name: 'google-java-format', version: '1.15.0'
    // implementation group: 'com.googlecode.protobuf-java-format', name: 'protobuf-java-format', version: '1.4'
    implementation(libs.fastjson)
    /*compileOnly group: 'com.google.guava', name: 'guava', version: '32.1.1-jre'*/
    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)
    implementation(libs.jgit)
    testAnnotationProcessor(libs.lombok)
    implementation(libs.kotlin.stdlib.jdk8)
    /*val withoutStuff = fun ExternalModuleDependency.() {
        exclude( module = "zookeeper")
    }
    implementation("org.apache.curator:curator-framework:4.0.0", withoutStuff)
    */
    /*implementation("com.github.mwiede:jsch:0.2.21")*/
    /*implementation("io.grpc:grpc-netty-shaded:1.76.0")*/
    implementation(libs.grpc.protobuf)
    implementation(libs.grpc.stub)
    implementation(libs.grpc.netty) {
        // 强制引入更高版本的 netty-codec
        exclude(group = "io.netty", module = "netty-codec-http2")
        exclude(group = "io.netty", module = "netty-codec-http")
    }
    implementation(libs.netty.codec.http2)
    implementation(libs.netty.codec)
    implementation(libs.grpc.protobuf.lite)
    implementation(libs.druid)
    implementation(libs.snakeyaml)
    implementation(libs.jsqlparser)
    implementation(libs.commonmark)
    implementation(libs.commonmark.gfm.tables)
    implementation(libs.commonmark.autolink)
    implementation(libs.sisyphus.grpc)
    implementation(libs.flexmark.tables)
    implementation(libs.flexmark.math)
    implementation(libs.sisyphus.jackson.protobuf)
    implementation(libs.poi.ooxml) {
        exclude(group = "org.apache.commons", module = "commons-lang3")
    }
    implementation(libs.okhttp)
    implementation(libs.okio)

    implementation("commons-collections:commons-collections:3.2.2")

    // xml解析
    implementation(libs.xstream)

    implementation(libs.sshj)
    implementation(libs.commons.pool2)

    // implementation("it.unimi.dsi:fastutil:8.5.6")
    implementation(kotlin("stdlib"))

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

kotlin {
    jvmToolchain(25)
    compilerOptions {
        freeCompilerArgs.add("-Xjvm-default=all")
    }
}

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
    generateLexer {
        sourceFile = layout.projectDirectory.file("src/main/grammar/protobuf.flex")
        targetOutputDir =
            layout.buildDirectory.dir("generated/sources/grammar/io/kanro/idea/plugin/protobuf/lang/lexer/proto")
        purgeOldFiles = true
    }

    generateParser {
        sourceFile = layout.projectDirectory.file("src/main/grammar/protobuf.bnf")
        targetRootOutputDir = layout.buildDirectory.dir("generated/sources/grammar")
        purgeOldFiles = true
        pathToParser = "io/kanro/idea/plugin/protobuf/lang/psi/proto/parser/ProtobufParser.java"
        pathToPsiRoot = "io/kanro/idea/plugin/protobuf/lang/psi/proto"
    }

    /*buildSearchableOptions {
        enabled = false
    }*/

    register<GenerateParserTask>("generateTextParser") {
        sourceFile = layout.projectDirectory.file("src/main/grammar/prototext.bnf")
        targetRootOutputDir = layout.buildDirectory.dir("generated/sources/grammar")
        purgeOldFiles = true
        pathToParser = "io/kanro/idea/plugin/protobuf/lang/psi/text/parser/ProtoTextParser.java"
        pathToPsiRoot = "io/kanro/idea/plugin/protobuf/lang/psi/text"
    }

    register<GenerateLexerTask>("generateTextLexer") {
        sourceFile = layout.projectDirectory.file("src/main/grammar/prototext.flex")
        targetOutputDir = layout.buildDirectory.dir(
            "generated/sources/grammar/io/kanro/idea/plugin/protobuf/lang/lexer/text"
        )
        purgeOldFiles = true
    }

    /*compileKotlin {
        dependsOn(generateParser, named("generateTextParser"), generateLexer, named("generateTextLexer"))
    }*/
    // 统一挂到编译任务上（避免任务引用提前创建）
    named("compileKotlin") {
        dependsOn("generateParser", "generateTextParser", "generateLexer", "generateTextLexer")
    }
}

sourceSets {
    named("main") {
        java {
            srcDirs("src/main/kotlin")
            srcDir(layout.buildDirectory.dir("generated/sources/grammar"))
        }
    }
}

publishing {
    publications {
        create<MavenPublication>("repoHsdzo") {
            groupId = project.group.toString()
            artifactId = artifactId
            version = project.version.toString()
            // 如果是普通 Java/JAR 项目
            from(components["java"])
        }
    }

    repositories {
        maven {
            name = "repoHsdzo"

            url = uri(
                "https://tanghuigood-cn-hangzhou.devops.aliyuncs.com/packages/api/protocol/maven/6686-release-p4r2f6"
            )

            credentials {
                username = "b059571a-4876-40ce-9405-1a28138ec200"
                password = "r4BHqLYAj)ql"
            }
        }
    }
}