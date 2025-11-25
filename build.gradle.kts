repositories {
    mavenCentral()
    maven {
        name = "Gradle Releases"
        url = uri("https://repo.gradle.org/gradle/libs-releases")
    }
}

plugins {
    val kotlinVersion = "2.3.0-RC"
    kotlin("jvm") version kotlinVersion
    kotlin("plugin.spring") version kotlinVersion
    id("org.springframework.boot") version "3.5.3"
    id("io.spring.dependency-management") version "1.1.7"
    id("gg.jte.gradle") version "3.2.1"
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
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("gg.jte:jte-spring-boot-starter-3:3.2.1")
    compileOnly("gg.jte:jte-kotlin:3.2.1")
    implementation("io.github.wimdeblauwe:htmx-spring-boot:4.0.1")
    implementation("org.springframework.boot:spring-boot-starter-websocket")
    implementation("org.gradle:gradle-tooling-api:8.14.3")
    implementation("com.github.ben-manes.caffeine:caffeine:3.1.8")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict")
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