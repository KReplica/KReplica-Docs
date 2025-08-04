package io.availe.kreplicadocs.services

import gg.jte.TemplateEngine
import gg.jte.output.StringOutput
import io.availe.kreplicadocs.common.CodeSnippet
import io.availe.kreplicadocs.common.PageId
import io.availe.kreplicadocs.config.AppProperties
import io.availe.kreplicadocs.config.CacheNames
import io.availe.kreplicadocs.model.CompileResponse
import io.availe.kreplicadocs.model.TemplateSlug
import io.availe.kreplicadocs.model.view.*
import org.springframework.cache.Cache
import org.springframework.cache.CacheManager
import org.springframework.stereotype.Service
import org.springframework.ui.ModelMap

@Service
class ViewModelFactory(
    private val snippetProvider: CodeSnippetProvider,
    private val appProperties: AppProperties,
    private val navigationProvider: NavigationProvider,
    private val tabProvider: TabProvider,
    private val sourceCodeNormalizer: SourceCodeNormalizer,
    private val guideContentProvider: GuideContentProvider,
    private val cacheManager: CacheManager,
    private val templateEngine: TemplateEngine,
) {

    private val permanentCache: Cache by lazy {
        cacheManager.getCache(CacheNames.PERMANENT_TEMPLATES)
            ?: throw IllegalStateException("Cache '${CacheNames.PERMANENT_TEMPLATES}' not found.")
    }

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
        val guideNav = navigationProvider.getGuideNav()
        val guideContentStubs = guideContentProvider.getGuideContent().associateBy { it.id }
        val allSnippets = snippetProvider.getSnippets()

        val processedContent = guideNav.map { sectionNav ->
            val sectionStub = guideContentStubs[sectionNav.id]
            val processedSubsections = sectionNav.subsections.map { subsectionNav ->
                val subsectionStub = sectionStub?.subsections?.find { it.id == subsectionNav.id }

                val example = subsectionStub?.exampleSnippetKey?.let { key ->
                    processGuideExample(CodeSnippet.valueOf(key))
                }

                val tabs = subsectionStub?.useTabsKey?.let { key ->
                    tabProvider.getTabs(key).map { tab ->
                        val nestedExampleKey = getSnippetKeyFromTab(key, tab.id)
                        if (nestedExampleKey != null) {
                            val processedExample = processGuideExample(CodeSnippet.valueOf(nestedExampleKey))
                            tab.copy(example = processedExample, codeSnippet = null)
                        } else {
                            tab
                        }
                    }
                }

                val referenceSnippet = subsectionStub?.snippetKey?.let { allSnippets[CodeSnippet.valueOf(it)] }

                ProcessedGuideSubsection(
                    id = subsectionNav.id,
                    title = subsectionNav.title,
                    example = example,
                    tabs = tabs,
                    referenceSnippet = referenceSnippet
                )
            }

            ProcessedGuideSection(
                id = sectionNav.id,
                title = sectionNav.title,
                subsections = processedSubsections
            )
        }

        return GuideViewModel(
            navLinks = navigationProvider.getNavLinks(),
            properties = appProperties,
            currentPage = PageId.GUIDE,
            snippets = allSnippets,
            guideNav = guideNav,
            content = processedContent
        )
    }

    private fun getSnippetKeyFromTab(tabKey: String, tabId: String): String? {
        val jsonNode = snippetProvider.getTabsJson()
        return jsonNode.path(tabKey).find { it.path("id").asText() == tabId }?.path("exampleSnippetKey")?.asText(null)
    }

    private fun processGuideExample(snippet: CodeSnippet): ProcessedGuideExample {
        val originalSource = snippetProvider.getSnippets()[snippet]
            ?: error("Snippet ${snippet.name} not found.")
        val cacheKey = sourceCodeNormalizer.getCacheKey(originalSource)
        val response = permanentCache.get(cacheKey, CompileResponse::class.java)

        return ProcessedGuideExample(
            inputCode = sourceCodeNormalizer.forDisplay(originalSource),
            outputFiles = response?.generatedFiles,
            inputTabLabel = "Your Interface",
            outputTabLabel = "Generated DTOs"
        )
    }

    private fun renderExampleToString(example: ProcessedGuideExample): String {
        val modelMap = ModelMap("example", example)
        val output = StringOutput()
        templateEngine.render("tags/guide-example.kte", modelMap, output)
        return output.toString()
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