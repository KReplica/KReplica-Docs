repositories {
    mavenCentral()
    maven {
        name = "Gradle Releases"
        url = uri("https://repo.gradle.org/gradle/libs-releases")
    }
}

abstract class CssBundleTask : DefaultTask() {
    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val sourceDirectory: DirectoryProperty

    @get:Input
    abstract val sourceSpecs: ListProperty<String>

    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    @TaskAction
    fun bundle() {
        val sourceDirectory = sourceDirectory.get()
        val entrypointLines = sourceDirectory.file("styles.css").asFile.readLines()
        val entrypointBody = entrypointLines
            .filterNot { line ->
                val trimmed = line.trimStart()
                trimmed.startsWith("@import ") || trimmed.startsWith("@layer reset, base,")
            }
            .joinToString("\n")

        val bundle = buildString {
            appendLine("@layer reset, base, layout, components, pages, utilities;")
            sourceSpecs.get().forEach { sourceSpec ->
                val path = sourceSpec.substringBeforeLast('|')
                val layer = sourceSpec.substringAfterLast('|')
                appendLine()
                appendLine("@layer $layer {")
                append(sourceDirectory.file(path).asFile.readText().trimEnd().prependIndent("    "))
                appendLine()
                appendLine("}")
            }
            appendLine()
            append(entrypointBody.trimStart())
            appendLine()
        }

        outputFile.get().asFile.apply {
            parentFile.mkdirs()
            writeText(bundle)
        }
    }
}

plugins {
    val kotlinVersion = "2.2.21"
    kotlin("jvm") version kotlinVersion
    kotlin("plugin.spring") version kotlinVersion
    id("org.springframework.boot") version "4.1.1"
    id("io.spring.dependency-management") version "1.1.7"
    id("gg.jte.gradle") version "3.2.4"
}

group = "io.availe"
version = "0.0.1-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-cache")
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("tools.jackson.module:jackson-module-kotlin")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("gg.jte:jte-spring-boot-starter-3:3.2.4")
    compileOnly("gg.jte:jte-kotlin:3.2.4")
    implementation("io.github.wimdeblauwe:htmx-spring-boot:5.0.0-rc.1")
    implementation("org.springframework.boot:spring-boot-starter-websocket")
    implementation("org.gradle:gradle-tooling-api:9.1.0-rc-3")
    implementation("com.github.ben-manes.caffeine:caffeine:3.2.3")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(26)
    }
}
kotlin {
    jvmToolchain(26)
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict")
    }
}

val cssBundleSources = listOf(
    "base/_variables.css" to "base",
    "base/_typography.css" to "base",
    "components/_icons.css" to "components",
    "components/_buttons.css" to "components",
    "components/_code-blocks.css" to "components",
    "components/_animations.css" to "components",
    "components/_fab.css" to "components",
    "components/_theme-switcher.css" to "components",
    "layout/_header.css" to "layout",
    "layout/_footer.css" to "layout",
    "layout/_sidebar.css" to "layout",
    "pages/_playground.css" to "pages",
    "pages/_index.css" to "pages",
)
val cssSourceDirectory = layout.projectDirectory.dir("src/main/resources/static/css")
val bundledCssFile = layout.buildDirectory.file("generated-resources/static/css/styles.css")
val bundleCss = tasks.register<CssBundleTask>("bundleCss") {
    group = "build"
    description = "Bundles layered CSS into one deployable stylesheet."
    sourceDirectory.set(cssSourceDirectory)
    sourceSpecs.set(cssBundleSources.map { (path, layer) -> "$path|$layer" })
    outputFile.set(bundledCssFile)
}

sourceSets {
    main {
        resources.exclude("static/css/styles.css")
    }
}

tasks.processResources {
    dependsOn(bundleCss)
    from(bundledCssFile) {
        into("static/css")
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

jte {
    precompile()
}

tasks.bootJar {
    dependsOn(tasks.precompileJte)
    with(bootInf {
        from(fileTree("jte-classes") {
            include("**/*.class")
        }).into("classes")
    })
    manifest {
        attributes["Enable-Native-Access"] = "ALL-UNNAMED"
    }
}


tasks.register<Exec>("bootJarAndRun") {
    dependsOn(tasks.bootJar)
    group = "application"
    description = "Custom logic as to allow Spring Boot to find JTE templates."
    val jarFile = tasks.bootJar.get().archiveFile.get().asFile
    commandLine("java", "-jar", jarFile.absolutePath)
}