package io.availe.kreplicadocs.services

import io.availe.kreplicadocs.common.PageId
import io.availe.kreplicadocs.config.AppProperties
import io.availe.kreplicadocs.model.TemplateSlug
import io.availe.kreplicadocs.model.view.GuideViewModel
import io.availe.kreplicadocs.model.view.IndexViewModel
import io.availe.kreplicadocs.model.view.PlaygroundViewModel
import io.availe.kreplicadocs.model.view.SelectOption
import org.springframework.stereotype.Service

@Service
class ViewModelFactory(
    private val snippetProvider: CodeSnippetProvider,
    private val appProperties: AppProperties,
    private val navigationProvider: NavigationProvider,
    private val tabProvider: TabProvider,
    private val sourceCodeNormalizer: SourceCodeNormalizer
) {

    fun createIndexViewModel(): IndexViewModel {
        return IndexViewModel(
            navLinks = navigationProvider.getNavLinks(),
            properties = appProperties,
            currentPage = PageId.INDEX,
            snippets = snippetProvider.getSnippets(),
            heroDemoTabs = tabProvider.getTabs("heroDemoTabs"),
            whenTabs = tabProvider.getTabs("whenTabs")
        )
    }

    fun createGuideViewModel(): GuideViewModel {
        return GuideViewModel(
            navLinks = navigationProvider.getNavLinks(),
            properties = appProperties,
            currentPage = PageId.GUIDE,
            snippets = snippetProvider.getSnippets(),
            guideNav = navigationProvider.getGuideNav(),
            whenTabs = tabProvider.getTabs("whenTabs"),
            contextualNestingTabs = tabProvider.getTabs("contextualNestingTabs"),
            apiMapperTabs = tabProvider.getTabs("apiMapperTabs")
        )
    }

    fun createPlaygroundViewModel(): PlaygroundViewModel {
        val templates = snippetProvider.getPlaygroundTemplates()
        val activeTemplate = templates.firstOrNull()
            ?: throw IllegalStateException("No playground templates found")

        val activeTemplateSlug = TemplateSlug(activeTemplate.slug)
        val initialSource = snippetProvider.getPlaygroundTemplateSource(activeTemplateSlug)
        val templateOptions = templates.map {
            SelectOption(
                value = it.slug,
                label = it.name,
                selected = it.slug == activeTemplate.slug
            )
        }

        return PlaygroundViewModel(
            navLinks = navigationProvider.getNavLinks(),
            properties = appProperties,
            currentPage = PageId.PLAYGROUND,
            availableTemplates = templateOptions,
            initialSourceCode = initialSource,
            activeTemplateSlug = activeTemplateSlug
        )
    }
}