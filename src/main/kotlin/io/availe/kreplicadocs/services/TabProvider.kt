package io.availe.kreplicadocs.services

import jakarta.annotation.PostConstruct
import org.springframework.core.io.support.ResourcePatternResolver
import org.springframework.stereotype.Service
import tools.jackson.core.type.TypeReference
import tools.jackson.databind.ObjectMapper

data class TabDefinition(
    val id: String,
    val label: String,
    val description: String? = null,
    val codeSnippetKey: String? = null,
    val generatedFrom: String? = null,
    val exampleSnippetKey: String? = null,
    val requiresCompilation: Boolean? = null
)

@Service
class TabProvider(
    private val resourcePatternResolver: ResourcePatternResolver,
    private val objectMapper: ObjectMapper,
) {
    private lateinit var tabGroups: Map<String, List<TabDefinition>>

    @PostConstruct
    fun init() {
        val resource = resourcePatternResolver.getResource("classpath:metadata/tabs.json")
        tabGroups = resource.inputStream.use {
            objectMapper.readValue(it, object : TypeReference<Map<String, List<TabDefinition>>>() {})
        }
    }

    fun getTabDefinitions(key: String): List<TabDefinition> {
        return tabGroups[key]
            ?: throw IllegalArgumentException("Tab group with key '$key' not found in tabs.json")
    }
}