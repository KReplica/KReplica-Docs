package io.availe.kreplicadocs.services

import io.availe.kreplicadocs.common.CodeSnippet
import io.availe.kreplicadocs.model.PlaygroundTemplate
import io.availe.kreplicadocs.model.TemplateSlug
import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.core.io.support.ResourcePatternResolver
import org.springframework.stereotype.Service
import tools.jackson.core.type.TypeReference
import tools.jackson.databind.JsonNode
import tools.jackson.databind.ObjectMapper
import java.io.FileNotFoundException

@Service
class CodeSnippetProvider(
    private val resourcePatternResolver: ResourcePatternResolver,
    private val objectMapper: ObjectMapper,
) {
    private val log = LoggerFactory.getLogger(CodeSnippetProvider::class.java)
    private val snippets = mutableMapOf<CodeSnippet, String>()
    private val playgroundTemplates = mutableListOf<PlaygroundTemplate>()
    private var tabsJson: JsonNode? = null

    @PostConstruct
    fun init() {
        loadAllSnippets()
        loadPlaygroundTemplates()
    }

    private fun loadAllSnippets() {
        for (snippet in CodeSnippet.entries) {
            try {
                val resource = resourcePatternResolver.getResource("classpath:code-snippets/${snippet.path}")
                if (resource.exists()) {
                    snippets[snippet] = resource.inputStream.bufferedReader().use { it.readText() }
                } else {
                    log.warn("Code snippet not found and will be skipped: {}", snippet.path)
                    snippets[snippet] = "Error: Snippet not found at ${snippet.path}"
                }
            } catch (e: Exception) {
                log.error("Error loading code snippet: ${snippet.path}", e)
                snippets[snippet] = "Error loading snippet: ${e.message}"
            }
        }
    }

    private fun loadPlaygroundTemplates() {
        val templatesResource = resourcePatternResolver.getResource("classpath:metadata/playground-templates.json")
        if (!templatesResource.exists()) return

        val templates: List<PlaygroundTemplate> = templatesResource.inputStream.use {
            objectMapper.readValue(it, object : TypeReference<List<PlaygroundTemplate>>() {})
        }
        playgroundTemplates.addAll(templates)
    }

    fun getSnippets(): Map<CodeSnippet, String> = snippets

    fun getPlaygroundTemplates(): List<PlaygroundTemplate> = playgroundTemplates

    fun getPlaygroundTemplateSource(slug: TemplateSlug): String {
        val resource = resourcePatternResolver.getResource("classpath:playground-templates/${slug.value}.kt")
        if (!resource.exists()) throw FileNotFoundException("Playground template not found: ${slug.value}")
        return resource.inputStream.bufferedReader().use { it.readText() }
    }

    fun getTabsJson(): JsonNode {
        if (tabsJson == null) {
            val resource = resourcePatternResolver.getResource("classpath:metadata/tabs.json")
            tabsJson = resource.inputStream.use {
                objectMapper.readTree(it)
            }
        }
        return tabsJson!!
    }
}