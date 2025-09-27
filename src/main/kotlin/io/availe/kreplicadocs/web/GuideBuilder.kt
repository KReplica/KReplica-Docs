package io.availe.kreplicadocs.web

import gg.jte.Content
import io.availe.kreplicadocs.common.CodeSnippet
import io.availe.kreplicadocs.common.GuideContentTemplate
import io.availe.kreplicadocs.model.view.SectionRequest
import io.availe.kreplicadocs.model.view.SubSectionRequest

class GuideBuilder {
    private val sectionRequests = mutableListOf<SectionRequest>()
    private var currentSectionBuilder: SectionRequestBuilder? = null

    fun startSection(id: String, title: String, description: Content) {
        if (currentSectionBuilder != null) {
            throw IllegalStateException("Previous section '${currentSectionBuilder?.title}' was not ended.")
        }
        currentSectionBuilder = SectionRequestBuilder(id, title, description)
    }

    fun addSubSection(
        id: String,
        title: String,
        contentTemplate: GuideContentTemplate,
        exampleSnippet: CodeSnippet? = null,
        tabsKey: String? = null
    ) {
        val request = SubSectionRequest(
            id = id,
            title = title,
            contentTemplate = contentTemplate,
            exampleSnippet = exampleSnippet,
            tabsKey = tabsKey
        )
        currentSectionBuilder?.addSubSection(request)
            ?: throw IllegalStateException("Cannot add a subsection outside of a section context.")
    }

    fun endSection() {
        currentSectionBuilder?.let {
            sectionRequests.add(it.build())
            currentSectionBuilder = null
        } ?: throw IllegalStateException("endSection called without a starting section.")
    }

    fun getSectionRequests(): List<SectionRequest> {
        if (currentSectionBuilder != null) {
            throw IllegalStateException("A section was started but never ended.")
        }
        return sectionRequests
    }

    private class SectionRequestBuilder(
        private val id: String,
        val title: String,
        private val description: Content
    ) {
        private val subsections = mutableListOf<SubSectionRequest>()

        fun addSubSection(subsection: SubSectionRequest) {
            subsections.add(subsection)
        }

        fun build(): SectionRequest = SectionRequest(id, title, description, subsections)
    }
}