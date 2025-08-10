package io.availe.kreplicadocs.services

import gg.jte.Content
import gg.jte.TemplateEngine
import gg.jte.TemplateOutput
import gg.jte.output.StringOutput
import io.availe.kreplicadocs.common.CodeSnippet
import io.availe.kreplicadocs.common.PageId
import io.availe.kreplicadocs.config.AppProperties
import io.availe.kreplicadocs.config.CacheNames
import io.availe.kreplicadocs.model.CompileResponse
import io.availe.kreplicadocs.model.NavLink
import io.availe.kreplicadocs.model.TemplateSlug
import io.availe.kreplicadocs.model.view.*
import io.availe.kreplicadocs.services.playground.SourceCodeNormalizer
import org.springframework.cache.Cache
import org.springframework.cache.CacheManager
import org.springframework.stereotype.Service

@Service
class ViewModelFactory(
    private val snippetProvider: CodeSnippetProvider,
    private val appProperties: AppProperties,
    private val navigationProvider: NavigationProvider,
    private val tabProvider: TabProvider,
    private val sourceCodeNormalizer: SourceCodeNormalizer,
    private val cacheManager: CacheManager,
    private val templateEngine: TemplateEngine,
) {

    private val permanentCache: Cache by lazy {
        cacheManager.getCache(CacheNames.PERMANENT_TEMPLATES)
            ?: throw IllegalStateException("Cache '${CacheNames.PERMANENT_TEMPLATES}' not found.")
    }

    private fun getTabsForKey(key: String): List<Tab> {
        val definitions = tabProvider.getTabDefinitions(key)
        val allSnippets = snippetProvider.getSnippets()

        return definitions.map { def ->
            if (def.exampleSnippetKey != null) {
                val processedExample = processGuideExample(CodeSnippet.valueOf(def.exampleSnippetKey))
                Tab(
                    id = def.id,
                    label = def.label,
                    description = def.description,
                    example = processedExample,
                )
            } else {
                val codeSnippet = when {
                    def.codeSnippetKey != null -> {
                        val snippetEnum = CodeSnippet.valueOf(def.codeSnippetKey)
                        allSnippets[snippetEnum] ?: "Error: Snippet for key '${def.codeSnippetKey}' not found."
                    }

                    def.generatedFrom != null -> {
                        val sourceSnippetEnum = CodeSnippet.valueOf(def.generatedFrom)
                        val sourceCode = allSnippets[sourceSnippetEnum]
                            ?: "Error: Source snippet for key '${def.generatedFrom}' not found."
                        val cacheKey = sourceCodeNormalizer.getCacheKey(sourceCode)

                        val cachedResponse = permanentCache.get(cacheKey, CompileResponse::class.java)
                        cachedResponse?.generatedFiles?.values?.firstOrNull()
                            ?: "Compiling... please wait a moment and refresh."
                    }

                    else -> null
                }
                Tab(
                    id = def.id,
                    label = def.label,
                    description = def.description,
                    codeSnippet = codeSnippet,
                )
            }
        }
    }

    fun createIndexViewModel(): IndexViewModel {
        return IndexViewModel(
            navLinks = navigationProvider.getNavLinks(),
            properties = appProperties,
            currentPage = PageId.INDEX,
            snippets = snippetProvider.getSnippets(),
            heroDemoTabs = getTabsForKey("heroDemoTabs"),
            whenTabs = getTabsForKey("whenTabs"),
        )
    }

    fun createGuideViewModel(): GuideViewModel {
        val builder = GuideBuilder()
        val tempVm = object : PageViewModel {
            override val navLinks: List<NavLink> = emptyList()
            override val properties = appProperties
            override val currentPage = PageId.GUIDE
        }
        templateEngine.render("guide/manifest.kte", mapOf("vm" to tempVm, "builder" to builder), StringOutput())

        val sectionRequests = builder.getSectionRequests()
        val allSubSectionRequests = sectionRequests.flatMap { it.subsections }

        val finalExamples = mutableMapOf<String, ProcessedGuideExample>()
        val finalTabs = mutableMapOf<String, List<Tab>>()

        allSubSectionRequests.forEach { request ->
            request.exampleSnippet?.let { snippet ->
                finalExamples[request.id] = processGuideExample(snippet)
            }
            request.tabsKey?.let { key ->
                finalTabs[request.id] = getTabsForKey(key)
            }
        }

        val finalVm = GuideViewModel(
            navLinks = navigationProvider.getNavLinks(),
            properties = appProperties,
            currentPage = PageId.GUIDE,
            snippets = snippetProvider.getSnippets(),
            guideNav = emptyList(),
            examples = finalExamples,
            tabs = finalTabs,
            guideContent = emptyList()
        )

        val guideContent = sectionRequests.map { sectionRequest ->
            GuideContentSection(
                id = sectionRequest.id,
                title = sectionRequest.title,
                description = sectionRequest.description,
                subsections = sectionRequest.subsections.map { subSectionRequest ->
                    val content = object : Content {
                        override fun writeTo(templateOutput: TemplateOutput) {
                            val model = mapOf("vm" to finalVm)
                            templateEngine.render(subSectionRequest.contentTemplate.path, model, templateOutput)
                        }
                    }
                    GuideContentSubSection(subSectionRequest.id, subSectionRequest.title, content)
                }
            )
        }

        val guideNav = guideContent.map { section ->
            GuideNavSection(
                id = section.id,
                title = section.title,
                subsections = section.subsections.map { subsection ->
                    GuideNavSubSection(id = subsection.id, title = subsection.title)
                },
            )
        }

        return finalVm.copy(guideContent = guideContent, guideNav = guideNav)
    }

    private fun processGuideExample(snippet: CodeSnippet): ProcessedGuideExample {
        val originalSource = snippetProvider.getSnippets()[snippet]
            ?: error("Snippet ${snippet.name} not found.")
        val cacheKey = sourceCodeNormalizer.getCacheKey(originalSource)
        val response = permanentCache.get(cacheKey, CompileResponse::class.java)

        return ProcessedGuideExample(
            inputCode = originalSource,
            outputFiles = response?.generatedFiles,
            inputTabLabel = "Your Interface",
            outputTabLabel = "Generated DTOs",
        )
    }

    fun createPlaygroundViewModel(templateSlugValue: String? = null): PlaygroundViewModel {
        val templates = snippetProvider.getPlaygroundTemplates()
        val activeTemplate = templates.find { it.slug == templateSlugValue }
            ?: templates.firstOrNull()
            ?: throw IllegalStateException("No playground templates found")

        val activeTemplateSlug = TemplateSlug(activeTemplate.slug)
        val initialSource = snippetProvider.getPlaygroundTemplateSource(activeTemplateSlug)
        val templateOptions = templates.map {
            SelectOption(
                value = it.slug,
                label = it.name,
                selected = it.slug == activeTemplateSlug.value,
            )
        }

        return PlaygroundViewModel(
            navLinks = navigationProvider.getNavLinks(),
            properties = appProperties,
            currentPage = PageId.PLAYGROUND,
            availableTemplates = templateOptions,
            initialSourceCode = initialSource,
            activeTemplateSlug = activeTemplateSlug,
        )
    }
}