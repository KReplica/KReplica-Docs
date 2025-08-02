package io.availe.kreplicadocs.services.playground

import io.availe.kreplicadocs.config.CacheNames
import io.availe.kreplicadocs.model.CompileRequest
import io.availe.kreplicadocs.model.JobId
import io.availe.kreplicadocs.model.TemplateSlug
import io.availe.kreplicadocs.services.CodeSnippetProvider
import org.gradle.tooling.GradleConnector
import org.slf4j.LoggerFactory
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.cache.CacheManager
import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.util.*
import java.util.concurrent.TimeUnit

@Service
class GradleDaemonManager(
    private val playgroundService: PlaygroundService,
    private val gradleCompiler: GradleCompiler,
    private val codeSnippetProvider: CodeSnippetProvider,
    private val cacheManager: CacheManager
) {

    private val log = LoggerFactory.getLogger(GradleDaemonManager::class.java)
    private val keepAliveTemplateSlug = TemplateSlug("basic-replication")

    @EventListener(ApplicationReadyEvent::class)
    fun warmupAndPrimeCache() {
        log.info("Starting Gradle daemon warmup and cache priming...")
        val permanentCache = cacheManager.getCache(CacheNames.PERMANENT_TEMPLATES)
            ?: throw IllegalStateException("Cache '${CacheNames.PERMANENT_TEMPLATES}' not found.")

        codeSnippetProvider.getPlaygroundTemplates().forEach { template ->
            try {
                val slug = TemplateSlug(template.slug)
                val sourceCode = codeSnippetProvider.getPlaygroundTemplateSource(slug)
                val normalizedSourceCode = sourceCode.trim().replace("\r\n", "\n")

                if (permanentCache[normalizedSourceCode] != null) {
                    log.debug("Cache already primed for template: {}", slug.value)
                    return@forEach
                }

                val request = CompileRequest(
                    jobId = JobId("warmup-${template.slug}-${UUID.randomUUID()}"),
                    sourceCode = sourceCode
                )
                val cancellationTokenSource = GradleConnector.newCancellationTokenSource()
                val response = gradleCompiler.compile(request, cancellationTokenSource)

                if (response.success) {
                    permanentCache.put(normalizedSourceCode, response)
                    log.info("Successfully compiled and cached starter template: {}", template.slug)
                } else {
                    log.error("Warmup compilation FAILED for template {}: {}", template.slug, response.message)
                }
            } catch (e: Exception) {
                log.error("Exception during warmup for template '{}'", template.name, e)
            }
        }
        log.info("Gradle daemon warmup and cache priming complete.")
    }

    @Scheduled(fixedRate = 10, timeUnit = TimeUnit.MINUTES)
    fun pingDaemon() {
        log.info("Pinging Gradle daemon to keep it alive...")
        try {
            val sourceCode = codeSnippetProvider.getPlaygroundTemplateSource(keepAliveTemplateSlug)
            val request = CompileRequest(
                jobId = JobId("keep-alive-${UUID.randomUUID()}"),
                sourceCode = sourceCode
            )
            val cancellationTokenSource = GradleConnector.newCancellationTokenSource()
            val responseFuture = playgroundService.submitCompilation(request, cancellationTokenSource)

            responseFuture.whenComplete { response, throwable ->
                if (throwable != null) {
                    log.error("Error during Gradle daemon keep-alive ping", throwable)
                } else if (response != null && !response.success) {
                    log.warn("Gradle daemon ping compilation failed: {}", response.message)
                } else {
                    log.info("Gradle daemon ping successful.")
                }
            }
        } catch (e: Exception) {
            log.error("Failed to submit Gradle daemon keep-alive ping", e)
        }
    }
}