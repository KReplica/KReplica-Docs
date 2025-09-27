package io.availe.kreplicadocs.services.playground

import io.availe.kreplicadocs.model.CompileRequest
import io.availe.kreplicadocs.model.CompileResponse
import io.availe.kreplicadocs.model.FileName
import jakarta.annotation.PostConstruct
import jakarta.annotation.PreDestroy
import org.gradle.tooling.CancellationTokenSource
import org.gradle.tooling.GradleConnector
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.io.ByteArrayOutputStream
import java.io.File
import java.net.URI
import java.nio.charset.StandardCharsets
import java.nio.file.Files

@Service
class GradleCompiler {

    private val log = LoggerFactory.getLogger(GradleCompiler::class.java)
    private lateinit var sandboxDir: File

    @PostConstruct
    fun initialize() {
        sandboxDir = Files.createTempDirectory("kreplica-sandbox-").toFile()
        log.info("Created shared sandbox directory for Gradle builds at: {}", sandboxDir.absolutePath)
    }

    @PreDestroy
    fun cleanup() {
        if (::sandboxDir.isInitialized && sandboxDir.exists()) {
            log.info("Deleting shared sandbox directory at: {}", sandboxDir.absolutePath)
            sandboxDir.deleteRecursively()
        }
    }

    fun compile(request: CompileRequest, cancellationTokenSource: CancellationTokenSource): CompileResponse {
        log.info("Starting Gradle build for job: {} in sandbox", request.jobId.value)
        createProjectFiles(sandboxDir, request.sourceCode)

        val stdoutStream = ByteArrayOutputStream()
        val stderrStream = ByteArrayOutputStream()

        try {
            GradleConnector.newConnector()
                .forProjectDirectory(sandboxDir)
                .useDistribution(URI("https://services.gradle.org/distributions/gradle-9.1.0-bin.zip"))
                .connect().use { connection ->
                    connection.newBuild()
                        .forTasks("kspKotlin")
                        .withArguments("-x", "test")
                        .setStandardOutput(stdoutStream)
                        .setStandardError(stderrStream)
                        .withCancellationToken(cancellationTokenSource.token())
                        .run()
                }

            val outputDir = File(sandboxDir, "build/generated/ksp")
            val generatedFiles = if (outputDir.exists()) {
                outputDir.walk()
                    .filter { it.isFile && it.extension == "kt" }
                    .associate { FileName(it.name) to it.readText() }
            } else {
                emptyMap()
            }

            val stdout = stdoutStream.toString(StandardCharsets.UTF_8)
            log.info("Gradle build SUCCEEDED for job: {}", request.jobId.value)
            return CompileResponse(
                jobId = request.jobId,
                sourceCode = request.sourceCode,
                success = true,
                generatedFiles = generatedFiles,
                message = "Compilation successful.\n$stdout"
            )

        } catch (e: Exception) {
            val stderr = stderrStream.toString(StandardCharsets.UTF_8)
            log.warn("Gradle build FAILED for job: {}. Exception: {}", request.jobId.value, e.javaClass.simpleName)
            return CompileResponse(
                jobId = request.jobId,
                sourceCode = request.sourceCode,
                success = false,
                message = "Compilation failed:\n$stderr"
            )
        }
    }

    private fun createProjectFiles(projectDir: File, sourceCode: String) {
        File(projectDir, "settings.gradle.kts").writeText(
            readResource("/templates/gradle/settings.gradle.kts.template")
        )

        File(projectDir, "build.gradle.kts").writeText(
            readResource("/templates/gradle/build.gradle.kts.template")
        )

        File(projectDir, "gradle.properties").writeText(
            """
            org.gradle.caching=true
            org.gradle.configuration-cache=true
            """.trimIndent()
        )

        val srcDir = File(projectDir, "src/main/kotlin/io/availe/playground")
        srcDir.mkdirs()
        File(srcDir, "Source.kt").writeText(sourceCode)
    }

    private fun readResource(path: String): String {
        return javaClass.getResource(path)?.readText()
            ?: throw kotlin.IllegalStateException("Cannot find resource: $path")
    }
}