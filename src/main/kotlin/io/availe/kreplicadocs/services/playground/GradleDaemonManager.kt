package io.availe.kreplicadocs.services.playground

import io.availe.kreplicadocs.common.CodeSnippet
import io.availe.kreplicadocs.config.CacheNames
import io.availe.kreplicadocs.model.CompileRequest
import io.availe.kreplicadocs.model.JobId
import io.availe.kreplicadocs.model.TemplateSlug
import io.availe.kreplicadocs.services.CodeSnippetProvider
import io.availe.kreplicadocs.services.GuideContentProvider
import io.availe.kreplicadocs.services.SourceCodeNormalizer
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
    private val gradleCompiler: GradleCompiler,
    private val codeSnippetProvider: CodeSnippetProvider,
    private val cacheManager: CacheManager,
    private val sourceCodeNormalizer: SourceCodeNormalizer,
    private val guideContentProvider: GuideContentProvider
) {

    private val log = LoggerFactory.getLogger(GradleDaemonManager::class.java)
    private val keepAliveTemplateSlug = TemplateSlug("basic-replication")

    @EventListener(ApplicationReadyEvent::class)
    fun warmupAndPrimeCache() {
        log.info("Starting Gradle daemon warmup and cache priming...")
        val permanentCache = cacheManager.getCache(CacheNames.PERMANENT_TEMPLATES)
            ?: throw IllegalStateException("Cache '${CacheNames.PERMANENT_TEMPLATES}' not found.")

        val snippetsToCompile = mutableSetOf<CodeSnippet>()

        guideContentProvider.getGuideContent().asSequence()
            .flatMap { it.subsections }
            .mapNotNull { it.exampleSnippetKey }
            .forEach { snippetsToCompile.add(CodeSnippet.valueOf(it)) }

        try {
            val tabsJson = codeSnippetProvider.getTabsJson()
            tabsJson.fields().forEach { (_, tabGroupNode) ->
                tabGroupNode.forEach { tabNode ->
                    tabNode.path("exampleSnippetKey").takeIf { !it.isMissingNode }?.asText()?.let {
                        snippetsToCompile.add(CodeSnippet.valueOf(it))
                    }
                    tabNode.path("generatedFrom").takeIf { !it.isMissingNode }?.asText()?.let {
                        snippetsToCompile.add(CodeSnippet.valueOf(it))
                    }
                }
            }
        } catch (e: Exception) {
            log.error("Could not parse tabs.json for cache priming.", e)
        }

        snippetsToCompile.add(CodeSnippet.HOMEPAGE_DEMO_SOURCE)

        val allSnippets = codeSnippetProvider.getSnippets()

        snippetsToCompile.forEach { snippet ->
            try {
                val sourceCode = allSnippets[snippet]
                    ?: throw IllegalStateException("Source for ${snippet.name} not found")

                val cacheKey = sourceCodeNormalizer.getCacheKey(sourceCode)
                if (permanentCache[cacheKey] != null) {
                    log.debug("Cache already primed for: {}", snippet.name)
                    return@forEach
                }

                val request = CompileRequest(
                    jobId = JobId("warmup-${snippet.name}-${UUID.randomUUID()}"),
                    sourceCode = sourceCode
                )
                val cancellationTokenSource = GradleConnector.newCancellationTokenSource()
                val response = gradleCompiler.compile(request, cancellationTokenSource)

                if (response.success) {
                    permanentCache.put(cacheKey, response)
                    log.info("Successfully compiled and cached: {}", snippet.name)
                } else {
                    log.error("Warmup compilation FAILED for {}: {}", snippet.name, response.message)
                }
            } catch (e: Exception) {
                log.error("Exception during warmup for '{}'", snippet.name, e)
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
            val response = gradleCompiler.compile(request, cancellationTokenSource)

            if (response.success) {
                log.info("Gradle daemon ping successful.")
            } else {
                log.warn("Gradle daemon ping compilation failed: {}", response.message)
            }
        } catch (e: Exception) {
            log.error("Failed to submit Gradle daemon keep-alive ping", e)
        }
    }
}