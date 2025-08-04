package io.availe.kreplicadocs.model.view

import io.availe.kreplicadocs.common.CodeSnippet
import io.availe.kreplicadocs.common.PageId
import io.availe.kreplicadocs.config.AppProperties
import io.availe.kreplicadocs.model.NavLink
import io.availe.kreplicadocs.model.TemplateSlug

data class GuideNavSubSection(
    val id: String,
    val title: String
)

data class GuideNavSection(
    val id: String,
    val title: String,
    val subsections: List<GuideNavSubSection> = emptyList()
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
    val snippets: Map<CodeSnippet, String>,
    val heroDemoTabs: List<Tab>,
    val whenTabs: List<Tab>
) : PageViewModel

data class ProcessedGuideExample(
    val inputCode: String,
    val outputFiles: Map<String, String>?,
    val inputTabLabel: String,
    val outputTabLabel: String
)

data class ProcessedGuideSubsection(
    val id: String,
    val example: ProcessedGuideExample? = null,
    val tabs: List<Tab>? = null
)

data class ProcessedGuideSection(
    val id: String,
    val subsections: List<ProcessedGuideSubsection>
)

data class GuideViewModel(
    override val navLinks: List<NavLink>,
    override val properties: AppProperties,
    override val currentPage: PageId,
    val snippets: Map<CodeSnippet, String>,
    val guideNav: List<GuideNavSection>,
    val content: List<ProcessedGuideSection>
) : PageViewModel

data class PlaygroundViewModel(
    override val navLinks: List<NavLink>,
    override val properties: AppProperties,
    override val currentPage: PageId,
    val availableTemplates: List<SelectOption>,
    val initialSourceCode: String,
    val activeTemplateSlug: TemplateSlug
) : PageViewModel