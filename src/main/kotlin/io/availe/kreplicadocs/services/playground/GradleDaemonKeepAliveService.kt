package io.availe.kreplicadocs.services.playground

import io.availe.kreplicadocs.model.CompileRequest
import io.availe.kreplicadocs.model.JobId
import io.availe.kreplicadocs.model.TemplateSlug
import io.availe.kreplicadocs.services.CodeSnippetProvider
import org.gradle.tooling.GradleConnector
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.util.*

@Service
class GradleDaemonKeepAliveService(
    private val sandboxService: SandboxService,
    private val codeSnippetProvider: CodeSnippetProvider
) {

    private val log = LoggerFactory.getLogger(GradleDaemonKeepAliveService::class.java)
    private val keepAliveTemplateSlug = TemplateSlug("basic-replication")

    @Scheduled(fixedRateString = "PT10M")
    fun pingDaemon() {
        try {
            val sourceCode = codeSnippetProvider.getPlaygroundTemplateSource(keepAliveTemplateSlug)
            val request = CompileRequest(
                jobId = JobId("keep-alive-${UUID.randomUUID()}"),
                sourceCode = sourceCode
            )
            val cancellationTokenSource = GradleConnector.newCancellationTokenSource()
            val response = sandboxService.compile(request, cancellationTokenSource).get()
            if (!response.success) {
                log.warn("Gradle daemon ping compilation failed: {}", response.message)
            }
        } catch (e: Exception) {
            log.error("Error during Gradle daemon keep-alive ping", e)
        }
    }
}