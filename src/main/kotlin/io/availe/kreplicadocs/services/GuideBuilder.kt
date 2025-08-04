package io.availe.kreplicadocs.services

import gg.jte.Content
import io.availe.kreplicadocs.model.view.GuideContentSection
import io.availe.kreplicadocs.model.view.GuideContentSubSection

class GuideBuilder {
    private val sections = mutableListOf<GuideContentSection>()
    private var currentSectionBuilder: GuideContentSectionBuilder? = null

    fun startSection(id: String, title: String, description: Content) {
        if (currentSectionBuilder != null) {
            throw IllegalStateException("Previous section '${currentSectionBuilder?.title}' was not ended.")
        }
        currentSectionBuilder = GuideContentSectionBuilder(id, title, description)
    }

    fun addSubSection(id: String, title: String, content: Content) {
        currentSectionBuilder?.addSubSection(GuideContentSubSection(id, title, content))
            ?: throw IllegalStateException("Cannot add a subsection outside of a section context.")
    }

    fun endSection() {
        currentSectionBuilder?.let {
            sections.add(it.build())
            currentSectionBuilder = null
        } ?: throw IllegalStateException("endSection called without a starting section.")
    }

    fun addSection(id: String, title: String, content: Content) {
        sections.add(GuideContentSection(id, title, content, emptyList()))
    }

    fun build(): List<GuideContentSection> {
        if (currentSectionBuilder != null) {
            throw IllegalStateException("A section was started but never ended.")
        }
        return sections
    }

    private class GuideContentSectionBuilder(
        private val id: String,
        val title: String,
        private val description: Content
    ) {
        private val subsections = mutableListOf<GuideContentSubSection>()

        fun addSubSection(subsection: GuideContentSubSection) {
            subsections.add(subsection)
        }

        fun build(): GuideContentSection = GuideContentSection(id, title, description, subsections)
    }
}