package io.availe.kreplicadocs.web

import gg.jte.TemplateEngine
import gg.jte.output.StringOutput
import io.availe.kreplicadocs.common.FragmentTemplate
import io.availe.kreplicadocs.common.PageTemplate
import io.availe.kreplicadocs.common.PartialTemplate
import io.availe.kreplicadocs.common.WebApp
import io.availe.kreplicadocs.config.CacheNames
import io.availe.kreplicadocs.model.*
import io.availe.kreplicadocs.services.CodeSnippetProvider
import io.availe.kreplicadocs.services.playground.SourceCodeNormalizer
import io.availe.kreplicadocs.services.ViewModelFactory
import io.availe.kreplicadocs.services.playground.PlaygroundService
import jakarta.annotation.PostConstruct
import org.gradle.tooling.CancellationTokenSource
import org.gradle.tooling.GradleConnector
import org.slf4j.LoggerFactory
import org.springframework.cache.Cache
import org.springframework.cache.CacheManager
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.ui.ModelMap
import org.springframework.web.bind.annotation.*
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import org.springframework.web.servlet.view.FragmentsRendering
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

data class CompileRequestForm(val source: String, val tabSessionId: String)
data class ActiveJobState(val jobId: JobId, val sourceCode: String)
data class JobContext(
    val future: CompletableFuture<CompileResponse>,
    val cancellationTokenSource: CancellationTokenSource,
    val tabSessionId: TabSessionId,
    val activeJobState: ActiveJobState,
    val jobHasCompleted: AtomicBoolean = AtomicBoolean(false)
)

@Controller
class PlaygroundController(
    private val viewModelFactory: ViewModelFactory,
    private val snippetProvider: CodeSnippetProvider,
    private val playgroundService: PlaygroundService,
    private val templateEngine: TemplateEngine,
    private val cacheManager: CacheManager,
    private val sourceCodeNormalizer: SourceCodeNormalizer
) {

    private val log = LoggerFactory.getLogger(PlaygroundController::class.java)
    private val activeJobs = ConcurrentHashMap<JobId, JobContext>()
    private val activeTabJobs = ConcurrentHashMap<TabSessionId, ActiveJobState>()
    private lateinit var completedJobsCache: Cache
    private lateinit var permanentCache: Cache
    private val sseTimeout = TimeUnit.MINUTES.toMillis(5)

    @PostConstruct
    fun init() {
        completedJobsCache = cacheManager.getCache(CacheNames.COMPLETED_JOBS)
            ?: throw IllegalStateException("Cache '${CacheNames.COMPLETED_JOBS}' not found.")
        permanentCache = cacheManager.getCache(CacheNames.PERMANENT_TEMPLATES)
            ?: throw IllegalStateException("Cache '${CacheNames.PERMANENT_TEMPLATES}' not found.")
    }

    @GetMapping(WebApp.Endpoints.Pages.PLAYGROUND)
    fun playground(
        model: Model,
        @RequestParam(name = "template", required = false) templateSlug: String?,
        @RequestHeader(name = "HX-Request", required = false) hxRequest: String?,
    ): Any {
        model.addAttribute("vm", viewModelFactory.createPlaygroundViewModel(templateSlug))
        return if (hxRequest != null) {
            FragmentsRendering.with(PartialTemplate.CONTENT_PLAYGROUND.path)
                .fragment(FragmentTemplate.NAV_UPDATE_OOB.path)
                .fragment(FragmentTemplate.FAB_UPDATE_OOB.path)
                .build()
        } else {
            PageTemplate.PLAYGROUND.path
        }
    }

    @GetMapping(WebApp.Endpoints.Playground.TEMPLATE_SWAP)
    fun getPlaygroundTemplate(@RequestParam("template-select") slug: String, model: Model): String {
        val templateSlug = TemplateSlug(slug)
        val sourceCode = snippetProvider.getPlaygroundTemplateSource(templateSlug)
        model.addAttribute("code", sourceCode)
        model.addAttribute("slug", slug)
        return FragmentTemplate.PLAYGROUND_EDITOR_SWAP.path
    }

    @PostMapping(WebApp.Endpoints.Playground.COMPILE)
    fun compile(@ModelAttribute compileRequestForm: CompileRequestForm, model: Model): String {
        val tabSessionId = TabSessionId(compileRequestForm.tabSessionId)
        log.debug("Received compile request for tab: {}", tabSessionId.value)

        val sourceCode = compileRequestForm.source
        val cacheKey = sourceCodeNormalizer.getCacheKey(sourceCode)

        permanentCache.get(cacheKey, CompileResponse::class.java)?.let {
            log.debug("Permanent cache HIT. Returning results directly.")
            model.addAttribute("files", it.generatedFiles)
            return FragmentTemplate.PLAYGROUND_RESULTS.path
        }

        synchronized(activeTabJobs) {
            val existingJobState = activeTabJobs[tabSessionId]
            if (existingJobState != null && existingJobState.sourceCode == sourceCode) {
                log.debug(
                    "Duplicate request for tab {}. Reconnecting to existing jobId {}",
                    tabSessionId.value,
                    existingJobState.jobId.value
                )
                model.addAttribute("jobId", existingJobState.jobId.value)
                return FragmentTemplate.PLAYGROUND_COMPILING.path
            }

            completedJobsCache.get(cacheKey, CompileResponse::class.java)?.let {
                log.debug("Completed jobs cache HIT. Returning results directly.")
                model.addAttribute("files", it.generatedFiles)
                return FragmentTemplate.PLAYGROUND_RESULTS.path
            }

            existingJobState?.let {
                log.debug("Tab {} has existing job {}. Cancelling it.", tabSessionId.value, it.jobId.value)
                activeJobs[it.jobId]?.cancellationTokenSource?.cancel()
            }

            val jobId = JobId(UUID.randomUUID().toString())
            val newJobState = ActiveJobState(jobId, sourceCode)
            activeTabJobs[tabSessionId] = newJobState
            log.debug("Created new job {} for tab {}.", jobId.value, tabSessionId.value)

            val request = CompileRequest(jobId, sourceCode)
            val cancellationTokenSource = GradleConnector.newCancellationTokenSource()
            val responseFuture = playgroundService.submitCompilation(request, cancellationTokenSource)
            val jobContext =
                JobContext(responseFuture, cancellationTokenSource, tabSessionId, newJobState)

            activeJobs[jobId] = jobContext
            log.debug("Job {} is async. Returning 'compiling' fragment.", jobId.value)
            model.addAttribute("jobId", jobId.value)
            return FragmentTemplate.PLAYGROUND_COMPILING.path
        }
    }

    @GetMapping("/playground/status/{jobId}")
    fun getCompilationStatus(@PathVariable jobId: String): SseEmitter {
        val typedJobId = JobId(jobId)
        log.debug("SSE client trying to connect for jobId: {}", typedJobId.value)
        val emitter = SseEmitter(sseTimeout)

        completedJobsCache.get(typedJobId, CompileResponse::class.java)?.let { completedResponse ->
            log.debug("Job {} found in COMPLETED cache. Sending result.", typedJobId.value)
            sendSseResult(emitter, completedResponse)
            return emitter
        }

        val jobContext = activeJobs[typedJobId]
        if (jobContext != null) {
            log.debug("Job {} found in ACTIVE map. Attaching listener.", typedJobId.value)
            setupSseLifecycle(emitter, jobContext)
            setupJobCompletion(emitter, jobContext)
        } else {
            log.warn(
                "Job {} not found in active or completed. Assuming expired. Completing connection.",
                typedJobId.value
            )
            emitter.complete()
        }

        return emitter
    }

    private fun setupSseLifecycle(emitter: SseEmitter, jobContext: JobContext) {
        val onTermination = {
            if (!jobContext.jobHasCompleted.get()) {
                log.warn(
                    "Emitter for job {} completed or timed out BEFORE job finished. Cancelling Gradle task.",
                    jobContext.activeJobState.jobId.value
                )
                jobContext.cancellationTokenSource.cancel()
            } else {
                log.debug(
                    "Emitter for job {} completed or timed out AFTER job finished. No action needed.",
                    jobContext.activeJobState.jobId.value
                )
            }
        }
        emitter.onCompletion(onTermination)
        emitter.onTimeout(onTermination)
    }

    private fun setupJobCompletion(emitter: SseEmitter, jobContext: JobContext) {
        jobContext.future.whenComplete { response, throwable ->
            jobContext.jobHasCompleted.set(true)
            log.debug(
                "Future completed for job {}. Success: {}, Error: {}",
                jobContext.activeJobState.jobId.value,
                response != null,
                throwable != null
            )
            handleJobResult(response)
            sendSseResult(emitter, response)
            cleanupJobState(jobContext)
        }
    }

    private fun handleJobResult(response: CompileResponse?) {
        if (response != null && response.success) {
            val cacheKey = sourceCodeNormalizer.getCacheKey(response.sourceCode)
            completedJobsCache.put(response.jobId, response)
            permanentCache.put(cacheKey, response)
        }
    }

    private fun cleanupJobState(jobContext: JobContext) {
        activeJobs.remove(jobContext.activeJobState.jobId)
        synchronized(activeTabJobs) {
            activeTabJobs.remove(jobContext.tabSessionId, jobContext.activeJobState)
        }
    }

    private fun sendSseResult(emitter: SseEmitter, response: CompileResponse?) {
        try {
            val html = renderResult(response)
            emitter.send(SseEmitter.event().name("compile-result").data(html))
            emitter.complete()
        } catch (e: Exception) {
            log.error("Failed to send SSE result for job {}: {}", response?.jobId?.value, e.message)
            emitter.completeWithError(e)
        }
    }

    private fun renderResult(response: CompileResponse?): String {
        val modelMap = ModelMap()
        val output = StringOutput()
        try {
            when {
                response?.success == true -> {
                    modelMap.addAttribute("files", response.generatedFiles)
                    templateEngine.render(FragmentTemplate.PLAYGROUND_RESULTS.path, modelMap, output)
                }

                response != null -> {
                    modelMap.addAttribute("message", response.message)
                    templateEngine.render(FragmentTemplate.PLAYGROUND_ERROR.path, modelMap, output)
                }

                else -> {
                    modelMap.addAttribute("message", "An unexpected error occurred or the job was cancelled.")
                    templateEngine.render(FragmentTemplate.PLAYGROUND_ERROR.path, modelMap, output)
                }
            }
            return output.toString()
        } catch (e: Exception) {
            log.error("Error during template rendering for job {}: {}", response?.jobId?.value, e.message, e)
            throw e
        }
    }
}