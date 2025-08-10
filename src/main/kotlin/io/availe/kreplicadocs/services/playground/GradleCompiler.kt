package io.availe.kreplicadocs.services.playground

import io.availe.kreplicadocs.model.CompileRequest
import io.availe.kreplicadocs.model.CompileResponse
import io.availe.kreplicadocs.model.FileName
import org.gradle.tooling.CancellationTokenSource
import org.gradle.tooling.GradleConnector
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.io.ByteArrayOutputStream
import java.io.File
import java.net.URI
import java.nio.file.Files

@Service
class GradleCompiler {

    private val log = LoggerFactory.getLogger(GradleCompiler::class.java)

    fun compile(request: CompileRequest, cancellationTokenSource: CancellationTokenSource): CompileResponse {
        log.info("Starting Gradle build for job: {}", request.jobId.value)
        val projectDir = Files.createTempDirectory("kreplica-job-${request.jobId.value}").toFile()

        try {
            createProjectFiles(projectDir, request.sourceCode)

            val stdoutStream = ByteArrayOutputStream()
            val stderrStream = ByteArrayOutputStream()

            try {
                GradleConnector.newConnector()
                    .forProjectDirectory(projectDir)
                    .useDistribution(URI("https://services.gradle.org/distributions/gradle-9.0.0-bin.zip"))
                    .connect().use { connection ->
                        connection.newBuild()
                            .forTasks("build")
                            .setJvmArguments("-Dorg.gradle.daemon.idletimeout=2147483647")
                            .setStandardOutput(stdoutStream)
                            .setStandardError(stderrStream)
                            .withCancellationToken(cancellationTokenSource.token())
                            .run()
                    }

                val outputDir = File(projectDir, "build/generated-src/kotlin-poet")
                val generatedFiles = if (outputDir.exists()) {
                    outputDir.walk()
                        .filter { it.isFile && it.extension == "kt" }
                        .associate { FileName(it.name) to it.readText() }
                } else {
                    emptyMap()
                }

                log.info("Gradle build SUCCEEDED for job: {}", request.jobId.value)
                return CompileResponse(
                    jobId = request.jobId,
                    sourceCode = request.sourceCode,
                    success = true,
                    generatedFiles = generatedFiles,
                    message = "Compilation successful.\n${stdoutStream}"
                )

            } catch (e: Exception) {
                log.warn("Gradle build FAILED for job: {}. Exception: {}", request.jobId.value, e.javaClass.simpleName)
                return CompileResponse(
                    jobId = request.jobId,
                    sourceCode = request.sourceCode,
                    success = false,
                    message = "Compilation failed:\n${stderrStream}"
                )
            }
        } finally {
            projectDir.deleteRecursively()
        }
    }

    private fun createProjectFiles(projectDir: File, sourceCode: String) {
        File(projectDir, "settings.gradle.kts").writeText(
            readResource("/templates/gradle/settings.gradle.kts.template")
        )

        File(projectDir, "build.gradle.kts").writeText(
            readResource("/templates/gradle/build.gradle.kts.template")
        )

        val srcDir = File(projectDir, "src/main/kotlin/io/availe/playground")
        srcDir.mkdirs()
        File(srcDir, "Source.kt").writeText(sourceCode)
    }

    private fun readResource(path: String): String {
        return javaClass.getResource(path)?.readText()
            ?: throw IllegalStateException("Cannot find resource: $path")
    }
}