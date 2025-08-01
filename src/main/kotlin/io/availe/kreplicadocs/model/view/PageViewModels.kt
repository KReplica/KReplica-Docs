package io.availe.kreplicadocs.model.view

import io.availe.kreplicadocs.common.CodeSnippet
import io.availe.kreplicadocs.common.PageId
import io.availe.kreplicadocs.config.AppProperties
import io.availe.kreplicadocs.model.NavLink

data class GuideSection(
    val id: String,
    val title: String,
    val subsections: List<GuideSubSection> = emptyList()
)

data class GuideSubSection(
    val id: String,
    val title: String
)

interface PageViewModel {
    val navLinks: List<NavLink>
    val properties: AppProperties
    val currentPage: PageId
}

data class IndexViewModel(
    override val navLinks: List<NavLink>,
    override val properties: AppProperties,
    override val currentPage: PageId,
    val snippets: Map<CodeSnippet, String>
) : PageViewModel

data class GuideViewModel(
    override val navLinks: List<NavLink>,
    override val properties: AppProperties,
    override val currentPage: PageId,
    val snippets: Map<CodeSnippet, String>,
    val guideNav: List<GuideSection>
) : PageViewModel

data class PlaygroundViewModel(
    override val navLinks: List<NavLink>,
    override val properties: AppProperties,
    override val currentPage: PageId,
    val availableTemplates: List<SelectOption>,
    val initialSourceCode: String,
    val activeTemplateSlug: String
) : PageViewModel