package io.availe.kreplicadocs.services

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import io.availe.kreplicadocs.model.NavLink
import io.availe.kreplicadocs.model.view.GuideNavSection
import io.availe.kreplicadocs.model.view.GuideNavSubSection
import jakarta.annotation.PostConstruct
import org.springframework.core.io.support.ResourcePatternResolver
import org.springframework.stereotype.Service

private data class NavSubSectionConfig(
    @param:JsonProperty("id") val id: String,
    @param:JsonProperty("title") val title: String
)

private data class NavSectionConfig(
    @param:JsonProperty("id") val id: String,
    @param:JsonProperty("title") val title: String,
    @param:JsonProperty("subsections") val subsections: List<NavSubSectionConfig> = emptyList()
)

private data class NavigationConfig(
    val mainNav: List<NavLink>,
    val guideNav: List<NavSectionConfig>
)

@Service
class NavigationProvider(
    private val resourcePatternResolver: ResourcePatternResolver,
    private val objectMapper: ObjectMapper,
) {
    private lateinit var navigationConfig: NavigationConfig

    @PostConstruct
    fun init() {
        val resource = resourcePatternResolver.getResource("classpath:metadata/navigation.json")
        navigationConfig = resource.inputStream.use {
            objectMapper.readValue(it, object : TypeReference<NavigationConfig>() {})
        }
    }

    fun getNavLinks(): List<NavLink> = navigationConfig.mainNav

    fun getGuideNav(): List<GuideNavSection> = navigationConfig.guideNav.map { sectionConfig ->
        GuideNavSection(
            id = sectionConfig.id,
            title = sectionConfig.title,
            subsections = sectionConfig.subsections.map { subsectionConfig ->
                GuideNavSubSection(
                    id = subsectionConfig.id,
                    title = subsectionConfig.title
                )
            }
        )
    }
}