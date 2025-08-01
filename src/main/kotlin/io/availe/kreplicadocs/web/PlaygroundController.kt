package io.availe.kreplicadocs.web

import gg.jte.TemplateEngine
import gg.jte.output.StringOutput
import io.availe.kreplicadocs.common.FragmentTemplate
import io.availe.kreplicadocs.common.PartialTemplate
import io.availe.kreplicadocs.common.WebApp
import io.availe.kreplicadocs.config.CacheNames
import io.availe.kreplicadocs.model.CompileRequest
import io.availe.kreplicadocs.model.CompileResponse
import io.availe.kreplicadocs.services.CodeSnippetProvider
import io.availe.kreplicadocs.services.SandboxService
import io.availe.kreplicadocs.services.ViewModelFactory
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
data class ActiveJobState(val jobId: String, val sourceCode: String)
data class JobContext(
    val future: CompletableFuture<CompileResponse>,
    val cancellationTokenSource: CancellationTokenSource,
    val tabSessionId: String,
    val activeJobState: ActiveJobState,
    val jobHasCompleted: AtomicBoolean = AtomicBoolean(false)
)

@Controller
class PlaygroundController(
    private val viewModelFactory: ViewModelFactory,
    private val snippetProvider: CodeSnippetProvider,
    private val sandboxService: SandboxService,
    private val templateEngine: TemplateEngine,
    private val cacheManager: CacheManager
) {

    private val log = LoggerFactory.getLogger(PlaygroundController::class.java)
    private val activeJobs = ConcurrentHashMap<String, JobContext>()
    private val activeTabJobs = ConcurrentHashMap<String, ActiveJobState>()
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

    @GetMapping("/playground")
    fun playground(model: Model, @RequestHeader(name = "HX-Request", required = false) hxRequest: String?): Any {
        model.addAttribute("vm", viewModelFactory.createPlaygroundViewModel())
        return if (hxRequest != null) {
            FragmentsRendering.with(PartialTemplate.CONTENT_PLAYGROUND.path)
                .fragment(FragmentTemplate.NAV_UPDATE_OOB.path)
                .fragment(FragmentTemplate.FAB_UPDATE_OOB.path)
                .build()
        } else {
            "pages/playground"
        }
    }

    @GetMapping(WebApp.Endpoints.Playground.TEMPLATE_SWAP)
    fun getPlaygroundTemplate(@RequestParam("template-select") slug: String, model: Model): String {
        val sourceCode = snippetProvider.getPlaygroundTemplateSource(slug)
        model.addAttribute("code", sourceCode)
        model.addAttribute("slug", slug)
        return "fragments/playground-editor-swap"
    }

    @PostMapping(WebApp.Endpoints.Playground.COMPILE)
    fun compile(@ModelAttribute compileRequestForm: CompileRequestForm, model: Model): String {
        log.debug("Received compile request for tab: {}", compileRequestForm.tabSessionId)

        val sourceCode = compileRequestForm.source
        val normalizedSourceCode = sourceCode.trim().replace("\r\n", "\n")

        permanentCache.get(normalizedSourceCode, CompileResponse::class.java)?.let {
            log.debug("Permanent cache HIT. Returning results directly.")
            model.addAttribute("files", it.generatedFiles)
            return "fragments/playground-results"
        }

        synchronized(activeTabJobs) {
            val existingJobState = activeTabJobs[compileRequestForm.tabSessionId]
            if (existingJobState != null && existingJobState.sourceCode == sourceCode) {
                log.debug(
                    "Duplicate request for tab {}. Reconnecting to existing jobId {}",
                    compileRequestForm.tabSessionId,
                    existingJobState.jobId
                )
                model.addAttribute("jobId", existingJobState.jobId)
                return "fragments/playground-compiling"
            }

            completedJobsCache.get(normalizedSourceCode, CompileResponse::class.java)?.let {
                log.debug("Completed jobs cache HIT. Returning results directly.")
                model.addAttribute("files", it.generatedFiles)
                return "fragments/playground-results"
            }

            existingJobState?.let {
                log.debug("Tab {} has existing job {}. Cancelling it.", compileRequestForm.tabSessionId, it.jobId)
                activeJobs[it.jobId]?.cancellationTokenSource?.cancel()
            }

            val jobId = UUID.randomUUID().toString()
            val newJobState = ActiveJobState(jobId, sourceCode)
            activeTabJobs[compileRequestForm.tabSessionId] = newJobState
            log.debug("Created new job {} for tab {}.", jobId, compileRequestForm.tabSessionId)

            val request = CompileRequest(jobId, sourceCode)
            val cancellationTokenSource = GradleConnector.newCancellationTokenSource()
            val responseFuture = sandboxService.compile(request, cancellationTokenSource)
            val jobContext =
                JobContext(responseFuture, cancellationTokenSource, compileRequestForm.tabSessionId, newJobState)

            activeJobs[jobId] = jobContext
            log.debug("Job {} is async. Returning 'compiling' fragment.", jobId)
            model.addAttribute("jobId", jobId)
            return "fragments/playground-compiling"
        }
    }

    @GetMapping("/playground/status/{jobId}")
    fun getCompilationStatus(@PathVariable jobId: String): SseEmitter {
        log.debug("SSE client trying to connect for jobId: {}", jobId)
        val emitter = SseEmitter(sseTimeout)

        completedJobsCache.get(jobId, CompileResponse::class.java)?.let { completedResponse ->
            log.debug("Job {} found in COMPLETED cache. Sending result.", jobId)
            sendSseResult(emitter, completedResponse)
            return emitter
        }

        val jobContext = activeJobs[jobId]
        if (jobContext != null) {
            log.debug("Job {} found in ACTIVE map. Attaching listener.", jobId)
            setupSseLifecycle(emitter, jobContext)
            setupJobCompletion(emitter, jobContext)
        } else {
            log.warn("Job {} not found in active or completed. Assuming expired. Completing connection.", jobId)
            emitter.complete()
        }

        return emitter
    }

    private fun setupSseLifecycle(emitter: SseEmitter, jobContext: JobContext) {
        val onTermination = {
            if (!jobContext.jobHasCompleted.get()) {
                log.warn(
                    "Emitter for job {} completed or timed out BEFORE job finished. Cancelling Gradle task.",
                    jobContext.activeJobState.jobId
                )
                jobContext.cancellationTokenSource.cancel()
            } else {
                log.debug(
                    "Emitter for job {} completed or timed out AFTER job finished. No action needed.",
                    jobContext.activeJobState.jobId
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
                jobContext.activeJobState.jobId,
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
            val normalizedSourceCode = response.sourceCode.trim().replace("\r\n", "\n")
            completedJobsCache.put(response.jobId, response)
            completedJobsCache.put(normalizedSourceCode, response)
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
            log.error("Failed to send SSE result for job {}: {}", response?.jobId, e.message)
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
                    templateEngine.render("fragments/playground-results", modelMap, output)
                }

                response != null -> {
                    modelMap.addAttribute("message", response.message)
                    templateEngine.render("fragments/playground-error", modelMap, output)
                }

                else -> {
                    modelMap.addAttribute("message", "An unexpected error occurred or the job was cancelled.")
                    templateEngine.render("fragments/playground-error", modelMap, output)
                }
            }
            return output.toString()
        } catch (e: Exception) {
            log.error("Error during template rendering for job {}: {}", response?.jobId, e.message, e)
            throw e
        }
    }
}