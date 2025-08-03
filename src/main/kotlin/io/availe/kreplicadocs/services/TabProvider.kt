package io.availe.kreplicadocs.services

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import io.availe.kreplicadocs.common.CodeSnippet
import io.availe.kreplicadocs.model.view.Tab
import jakarta.annotation.PostConstruct
import org.springframework.core.io.support.ResourcePatternResolver
import org.springframework.stereotype.Service

private data class TabDefinition(
    val id: String,
    val label: String,
    val description: String? = null,
    val codeSnippetKey: String
)

@Service
class TabProvider(
    private val resourcePatternResolver: ResourcePatternResolver,
    private val objectMapper: ObjectMapper,
    private val codeSnippetProvider: CodeSnippetProvider
) {
    private lateinit var tabGroups: Map<String, List<TabDefinition>>

    @PostConstruct
    fun init() {
        val resource = resourcePatternResolver.getResource("classpath:metadata/tabs.json")
        tabGroups = resource.inputStream.use {
            objectMapper.readValue(it, object : TypeReference<Map<String, List<TabDefinition>>>() {})
        }
    }

    fun getTabs(key: String): List<Tab> {
        val definitions = tabGroups[key]
            ?: throw IllegalArgumentException("Tab group with key '$key' not found in tabs.json")
        val snippets = codeSnippetProvider.getSnippets()

        return definitions.map { def ->
            val snippetEnum = CodeSnippet.valueOf(def.codeSnippetKey)
            Tab(
                id = def.id,
                label = def.label,
                description = def.description,
                codeSnippet = snippets[snippetEnum]
                    ?: "Error: Snippet for key '${def.codeSnippetKey}' not found."
            )
        }
    }
}