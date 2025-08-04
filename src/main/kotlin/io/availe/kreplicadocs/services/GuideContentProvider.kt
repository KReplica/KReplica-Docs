package io.availe.kreplicadocs.services

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import io.availe.kreplicadocs.model.view.GuideSectionStub
import jakarta.annotation.PostConstruct
import org.springframework.core.io.support.ResourcePatternResolver
import org.springframework.stereotype.Service

@Service
class GuideContentProvider(
    private val resourcePatternResolver: ResourcePatternResolver,
    private val objectMapper: ObjectMapper,
) {
    private lateinit var guideContent: List<GuideSectionStub>

    @PostConstruct
    fun init() {
        val resource = resourcePatternResolver.getResource("classpath:metadata/guide-content.json")
        guideContent = resource.inputStream.use {
            objectMapper.readValue(it, object : TypeReference<List<GuideSectionStub>>() {})
        }
    }

    fun getGuideContent(): List<GuideSectionStub> = guideContent
}