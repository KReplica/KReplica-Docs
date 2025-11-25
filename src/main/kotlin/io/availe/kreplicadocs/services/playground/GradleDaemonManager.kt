package io.availe.kreplicadocs.services.playground

import io.availe.kreplicadocs.common.CodeSnippet
import io.availe.kreplicadocs.config.CacheNames
import io.availe.kreplicadocs.model.CompileRequest
import io.availe.kreplicadocs.model.JobId
import io.availe.kreplicadocs.model.TemplateSlug
import io.availe.kreplicadocs.services.CodeSnippetProvider
import org.gradle.tooling.GradleConnector
import org.slf4j.LoggerFactory
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.cache.Cache
import org.springframework.cache.CacheManager
import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import tools.jackson.databind.JsonNode
import java.util.*
import java.util.concurrent.TimeUnit

@Service
class GradleDaemonManager(
    private val gradleCompiler: GradleCompiler,
    private val codeSnippetProvider: CodeSnippetProvider,
    private val cacheManager: CacheManager,
    private val sourceCodeNormalizer: SourceCodeNormalizer,
) {

    private val log = LoggerFactory.getLogger(GradleDaemonManager::class.java)
    private val keepAliveTemplateSlug = TemplateSlug("basic-replication")
    private lateinit var permanentCache: Cache

    @EventListener(ApplicationReadyEvent::class)
    fun warmupAndPrimeCache() {
        log.info("Starting Gradle daemon warmup and cache priming...")
        permanentCache = cacheManager.getCache(CacheNames.PERMANENT_TEMPLATES)
            ?: throw IllegalStateException("Cache '${CacheNames.PERMANENT_TEMPLATES}' not found.")

        val sourcesToPrime = getSourcesToPrime()
        log.info("Found ${sourcesToPrime.size} unique source files to pre-compile and cache.")

        sourcesToPrime.forEachIndexed { index, sourceCode ->
            try {
                val cacheKey = sourceCodeNormalizer.getCacheKey(sourceCode)
                if (permanentCache[cacheKey] != null) {
                    log.debug("Cache already primed for source #{}", index + 1)
                    return@forEachIndexed
                }

                val request = CompileRequest(
                    jobId = JobId("warmup-${UUID.randomUUID()}"),
                    sourceCode = sourceCode
                )
                val cancellationTokenSource = GradleConnector.newCancellationTokenSource()
                val response = gradleCompiler.compile(request, cancellationTokenSource)

                if (response.success) {
                    permanentCache.put(cacheKey, response)
                    log.info("Successfully compiled and cached source #${index + 1}")
                } else {
                    log.error("Warmup compilation FAILED for source #${index + 1}: ${response.message}")
                }
            } catch (e: Exception) {
                log.error("Exception during warmup for source #${index + 1}", e)
            }
        }
        log.info("Gradle daemon warmup and cache priming complete.")
    }

    private fun getSourcesToPrime(): Set<String> {
        val allSnippets = codeSnippetProvider.getSnippets()
        val sources = mutableSetOf<String>()

        sources.addAll(primeFromCodeSnippets(allSnippets))
        sources.addAll(primeFromPlaygroundTemplates())

        return sources
    }

    private fun primeFromCodeSnippets(allSnippets: Map<CodeSnippet, String>): Set<String> {
        val snippetsToCompile = mutableSetOf<CodeSnippet>()

        try {
            val tabsJson = codeSnippetProvider.getTabsJson()
            tabsJson.properties().forEach { entry ->
                val tabGroupNode = entry.value
                tabGroupNode.forEach { tabNode ->
                    if (tabNode.path("requiresCompilation").asBoolean(false)) {
                        addSnippetKeyFromNode(tabNode, "exampleSnippetKey", snippetsToCompile)
                        addSnippetKeyFromNode(tabNode, "generatedFrom", snippetsToCompile)
                        addSnippetKeyFromNode(tabNode, "codeSnippetKey", snippetsToCompile)
                    }
                }
            }
        } catch (e: Exception) {
            log.error("Could not parse tabs.json for cache priming.", e)
        }

        snippetsToCompile.add(CodeSnippet.API_REPLICATE_MODEL)
        snippetsToCompile.add(CodeSnippet.API_REPLICATE_PROPERTY)
        snippetsToCompile.add(CodeSnippet.API_REPLICATE_APPLY)
        snippetsToCompile.add(CodeSnippet.API_REPLICATE_SCHEMA_VERSION)
        snippetsToCompile.add(CodeSnippet.API_REPLICATE_HIDE)
        snippetsToCompile.add(CodeSnippet.API_AUTO_CONTEXTUAL)
        snippetsToCompile.add(CodeSnippet.GUIDE_REF_VERSIONING)
        snippetsToCompile.add(CodeSnippet.GUIDE_REF_SERIALIZATION_BASIC)

        return snippetsToCompile.mapNotNull { allSnippets[it] }.toSet()
    }

    private fun addSnippetKeyFromNode(node: JsonNode, fieldName: String, set: MutableSet<CodeSnippet>) {
        node.path(fieldName).takeIf { !it.isMissingNode }?.asText()?.let {
            runCatching { CodeSnippet.valueOf(it) }
                .onSuccess(set::add)
                .onFailure { e -> log.warn("Invalid CodeSnippet key in metadata: '$it'", e) }
        }
    }

    private fun primeFromPlaygroundTemplates(): Set<String> {
        return codeSnippetProvider.getPlaygroundTemplates().mapNotNull { template ->
            try {
                codeSnippetProvider.getPlaygroundTemplateSource(TemplateSlug(template.slug))
            } catch (e: Exception) {
                log.error("Could not load playground template source for slug: ${template.slug}", e)
                null
            }
        }
            .toSet()
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