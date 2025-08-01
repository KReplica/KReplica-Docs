package io.availe.kreplicadocs.services

import io.availe.kreplicadocs.config.CacheNames
import io.availe.kreplicadocs.model.CompileRequest
import org.gradle.tooling.GradleConnector
import org.slf4j.LoggerFactory
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.cache.CacheManager
import org.springframework.context.ApplicationListener
import org.springframework.stereotype.Service
import java.util.*

@Service
class GradleDaemonWarmupService(
    private val codeSnippetProvider: CodeSnippetProvider,
    private val compilerService: CompilerService,
    private val cacheManager: CacheManager
) : ApplicationListener<ApplicationReadyEvent> {

    private val log = LoggerFactory.getLogger(GradleDaemonWarmupService::class.java)

    override fun onApplicationEvent(event: ApplicationReadyEvent) {
        primeCacheAndWarmupDaemon()
    }

    fun primeCacheAndWarmupDaemon() {
        val permanentCache = cacheManager.getCache(CacheNames.PERMANENT_TEMPLATES) ?: return
        val templates = codeSnippetProvider.getPlaygroundTemplates()

        templates.forEach { template ->
            try {
                val sourceCode = codeSnippetProvider.getPlaygroundTemplateSource(template.slug)
                val request = CompileRequest(
                    jobId = "warmup-${template.slug}-${UUID.randomUUID()}",
                    sourceCode = sourceCode
                )
                val cancellationTokenSource = GradleConnector.newCancellationTokenSource()
                val response = compilerService.compile(request, cancellationTokenSource)
                if (response.success) {
                    val normalizedSourceCode = sourceCode.trim().replace("\r\n", "\n")
                    permanentCache.put(normalizedSourceCode, response)
                    log.info("Successfully compiled and cached starter template: {}", template.slug)
                }
            } catch (e: Exception) {
                log.error("Exception during warmup for template '{}'", template.name, e)
            }
        }
    }
}