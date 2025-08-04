package io.availe.kreplicadocs.services

import io.availe.kreplicadocs.common.PageId
import io.availe.kreplicadocs.config.AppProperties
import io.availe.kreplicadocs.config.CacheNames
import io.availe.kreplicadocs.model.CompileResponse
import io.availe.kreplicadocs.model.TemplateSlug
import io.availe.kreplicadocs.model.view.*
import org.springframework.cache.CacheManager
import org.springframework.stereotype.Service

@Service
class ViewModelFactory(
    private val snippetProvider: CodeSnippetProvider,
    private val appProperties: AppProperties,
    private val navigationProvider: NavigationProvider,
    private val tabProvider: TabProvider,
    private val sourceCodeNormalizer: SourceCodeNormalizer,
    private val guideContentProvider: GuideContentProvider,
    private val cacheManager: CacheManager
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
        val permanentCache = cacheManager.getCache(CacheNames.PERMANENT_TEMPLATES)
            ?: throw IllegalStateException("Cache '${CacheNames.PERMANENT_TEMPLATES}' not found.")
        val guideContentStubs = guideContentProvider.getGuideContent()

        val processedContent = guideContentStubs.map { sectionStub ->
            val processedSubsections = sectionStub.subsections.map { subsectionStub ->
                val example = subsectionStub.exampleSlug?.let { slug ->
                    val originalSource = snippetProvider.getPlaygroundTemplateSource(TemplateSlug(slug))
                    val cacheKey = sourceCodeNormalizer.getCacheKey(originalSource)
                    val response = permanentCache.get(cacheKey, CompileResponse::class.java)
                    ProcessedGuideExample(
                        inputCode = sourceCodeNormalizer.forDisplay(originalSource),
                        outputFiles = response?.generatedFiles
                    )
                }
                val tabs = subsectionStub.useTabsKey?.let { tabProvider.getTabs(it) }

                ProcessedGuideSubsection(
                    id = subsectionStub.id,
                    example = example,
                    tabs = tabs
                )
            }
            ProcessedGuideSection(
                id = sectionStub.id,
                subsections = processedSubsections
            )
        }

        return GuideViewModel(
            navLinks = navigationProvider.getNavLinks(),
            properties = appProperties,
            currentPage = PageId.GUIDE,
            snippets = snippetProvider.getSnippets(),
            guideNav = navigationProvider.getGuideNav(),
            content = processedContent
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