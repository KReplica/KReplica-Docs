package io.availe.kreplicadocs.model.view

import gg.jte.Content
import io.availe.kreplicadocs.common.CodeSnippet
import io.availe.kreplicadocs.common.GuideContentTemplate
import io.availe.kreplicadocs.common.PageId
import io.availe.kreplicadocs.config.AppProperties
import io.availe.kreplicadocs.model.FileName
import io.availe.kreplicadocs.model.NavLink
import io.availe.kreplicadocs.model.TemplateSlug

data class SubSectionRequest(
    val id: String,
    val title: String,
    val contentTemplate: GuideContentTemplate,
    val exampleSnippet: CodeSnippet? = null,
    val tabsKey: String? = null
)

data class SectionRequest(
    val id: String,
    val title: String,
    val description: Content,
    val subsections: List<SubSectionRequest>
)

data class GuideNavSubSection(
    val id: String,
    val title: String,
)

data class GuideNavSection(
    val id: String,
    val title: String,
    val subsections: List<GuideNavSubSection> = emptyList(),
)

data class GuideContentSubSection(
    val id: String,
    val title: String,
    val content: Content,
)

data class GuideContentSection(
    val id: String,
    val title: String,
    val description: Content,
    val subsections: List<GuideContentSubSection>,
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
    val whenTabs: List<Tab>,
) : PageViewModel

data class ProcessedGuideExample(
    val inputCode: String,
    val outputFiles: Map<FileName, String>?,
    val inputTabLabel: String,
    val outputTabLabel: String,
)

data class GuideViewModel(
    override val navLinks: List<NavLink>,
    override val properties: AppProperties,
    override val currentPage: PageId,
    val snippets: Map<CodeSnippet, String>,
    val guideNav: List<GuideNavSection>,
    val examples: Map<String, ProcessedGuideExample>,
    val tabs: Map<String, List<Tab>>,
    val guideContent: List<GuideContentSection>,
) : PageViewModel

data class PlaygroundViewModel(
    override val navLinks: List<NavLink>,
    override val properties: AppProperties,
    override val currentPage: PageId,
    val availableTemplates: List<SelectOption>,
    val initialSourceCode: String,
    val activeTemplateSlug: TemplateSlug,
) : PageViewModel