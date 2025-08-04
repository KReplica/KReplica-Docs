package io.availe.kreplicadocs.services

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import io.availe.kreplicadocs.common.CodeSnippet
import io.availe.kreplicadocs.config.CacheNames
import io.availe.kreplicadocs.model.CompileResponse
import io.availe.kreplicadocs.model.view.Tab
import jakarta.annotation.PostConstruct
import org.springframework.cache.Cache
import org.springframework.cache.CacheManager
import org.springframework.core.io.support.ResourcePatternResolver
import org.springframework.stereotype.Service

private data class TabDefinition(
    val id: String,
    val label: String,
    val description: String? = null,
    val codeSnippetKey: String? = null,
    val generatedFrom: String? = null
)

@Service
class TabProvider(
    private val resourcePatternResolver: ResourcePatternResolver,
    private val objectMapper: ObjectMapper,
    private val codeSnippetProvider: CodeSnippetProvider,
    private val cacheManager: CacheManager,
    private val sourceCodeNormalizer: SourceCodeNormalizer
) {
    private lateinit var tabGroups: Map<String, List<TabDefinition>>
    private lateinit var permanentCache: Cache

    @PostConstruct
    fun init() {
        val resource = resourcePatternResolver.getResource("classpath:metadata/tabs.json")
        tabGroups = resource.inputStream.use {
            objectMapper.readValue(it, object : TypeReference<Map<String, List<TabDefinition>>>() {})
        }
        permanentCache = cacheManager.getCache(CacheNames.PERMANENT_TEMPLATES)
            ?: throw IllegalStateException("Cache '${CacheNames.PERMANENT_TEMPLATES}' not found.")
    }

    fun getTabs(key: String): List<Tab> {
        val definitions = tabGroups[key]
            ?: throw IllegalArgumentException("Tab group with key '$key' not found in tabs.json")
        val snippets = codeSnippetProvider.getSnippets()

        return definitions.map { def ->
            val codeSnippet = when {
                def.codeSnippetKey != null -> {
                    val snippetEnum = CodeSnippet.valueOf(def.codeSnippetKey)
                    snippets[snippetEnum] ?: "Error: Snippet for key '${def.codeSnippetKey}' not found."
                }

                def.generatedFrom != null -> {
                    val sourceSnippetEnum = CodeSnippet.valueOf(def.generatedFrom)
                    val sourceCode = snippets[sourceSnippetEnum]
                        ?: "Error: Source snippet for key '${def.generatedFrom}' not found."
                    val cacheKey = sourceCodeNormalizer.getCacheKey(sourceCode)

                    val cachedResponse = permanentCache.get(cacheKey, CompileResponse::class.java)
                    cachedResponse?.generatedFiles?.values?.firstOrNull()
                        ?: "Compiling... please wait a moment and refresh."
                }

                else -> "Error: Tab definition for '${def.id}' is missing a code source."
            }

            Tab(
                id = def.id,
                label = def.label,
                description = def.description,
                codeSnippet = codeSnippet
            )
        }
    }
}