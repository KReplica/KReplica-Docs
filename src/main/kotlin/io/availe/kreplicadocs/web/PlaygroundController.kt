package io.availe.kreplicadocs.web

import gg.jte.TemplateEngine
import gg.jte.output.StringOutput
import io.availe.kreplicadocs.common.FragmentTemplate
import io.availe.kreplicadocs.common.PartialTemplate
import io.availe.kreplicadocs.common.WebApp
import io.availe.kreplicadocs.model.CompileRequest
import io.availe.kreplicadocs.model.CompileResponse
import io.availe.kreplicadocs.services.CodeSnippetProvider
import io.availe.kreplicadocs.services.SandboxService
import io.availe.kreplicadocs.services.ViewModelFactory
import jakarta.annotation.PostConstruct
import org.gradle.tooling.CancellationTokenSource
import org.gradle.tooling.GradleConnector
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

    private val activeJobs = ConcurrentHashMap<String, JobContext>()
    private val activeTabJobs = ConcurrentHashMap<String, ActiveJobState>()
    private lateinit var completedJobsCache: Cache
    private lateinit var permanentCache: Cache
    private val sseTimeout = TimeUnit.MINUTES.toMillis(5)

    @PostConstruct
    fun init() {
        completedJobsCache = cacheManager.getCache("completed-jobs-cache")
            ?: throw IllegalStateException("Cache 'completed-jobs-cache' not found.")
        permanentCache = cacheManager.getCache("playground-templates-cache")
            ?: throw IllegalStateException("Cache 'playground-templates-cache' not found.")
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
        println("[COMPILE] Received request for tab: ${compileRequestForm.tabSessionId}")

        val sourceCode = compileRequestForm.source
        val normalizedSourceCode = sourceCode.trim().replace("\r\n", "\n")

        permanentCache.get(normalizedSourceCode, CompileResponse::class.java)?.let {
            println("[COMPILE] Permanent cache HIT. Returning results directly.")
            model.addAttribute("files", it.generatedFiles)
            return "fragments/playground-results"
        }

        synchronized(activeTabJobs) {
            val existingJobState = activeTabJobs[compileRequestForm.tabSessionId]
            if (existingJobState != null && existingJobState.sourceCode == sourceCode) {
                println("[COMPILE] Duplicate request for tab ${compileRequestForm.tabSessionId}. Reconnecting to existing jobId ${existingJobState.jobId}")
                model.addAttribute("jobId", existingJobState.jobId)
                return "fragments/playground-compiling"
            }

            completedJobsCache.get(normalizedSourceCode, CompileResponse::class.java)?.let {
                println("[COMPILE] Completed jobs cache HIT. Returning results directly.")
                model.addAttribute("files", it.generatedFiles)
                return "fragments/playground-results"
            }

            existingJobState?.let {
                println("[COMPILE] Tab ${compileRequestForm.tabSessionId} has existing job ${it.jobId}. Cancelling it.")
                activeJobs[it.jobId]?.cancellationTokenSource?.cancel()
            }

            val jobId = UUID.randomUUID().toString()
            val newJobState = ActiveJobState(jobId, sourceCode)
            activeTabJobs[compileRequestForm.tabSessionId] = newJobState
            println("[COMPILE] Created new job $jobId for tab ${compileRequestForm.tabSessionId}.")

            val request = CompileRequest(jobId, sourceCode)
            val cancellationTokenSource = GradleConnector.newCancellationTokenSource()
            val responseFuture = sandboxService.compile(request, cancellationTokenSource)
            val jobContext =
                JobContext(responseFuture, cancellationTokenSource, compileRequestForm.tabSessionId, newJobState)

            activeJobs[jobId] = jobContext
            println("[COMPILE] Job $jobId is async. Returning 'compiling' fragment.")
            model.addAttribute("jobId", jobId)
            return "fragments/playground-compiling"
        }
    }

    @GetMapping("/playground/status/{jobId}")
    fun getCompilationStatus(@PathVariable jobId: String): SseEmitter {
        println("[SSE] Client trying to connect for jobId: $jobId")
        val emitter = SseEmitter(sseTimeout)

        completedJobsCache.get(jobId, CompileResponse::class.java)?.let { completedResponse ->
            println("[SSE] Job $jobId found in COMPLETED cache. Sending result.")
            try {
                val html = renderResult(completedResponse)
                emitter.send(SseEmitter.event().name("compile-result").data(html))
                emitter.complete()
            } catch (e: Exception) {
                println("[SSE-ERROR] Failed to send completed result for job $jobId: ${e.message}")
                emitter.completeWithError(e)
            }
            return emitter
        }

        val jobContext = activeJobs[jobId]
        if (jobContext != null) {
            println("[SSE] Job $jobId found in ACTIVE map. Attaching listener.")

            val onTermination = {
                if (!jobContext.jobHasCompleted.get()) {
                    println("[SSE-TERMINATION] Emitter for job $jobId completed or timed out BEFORE job finished. Cancelling Gradle task.")
                    jobContext.cancellationTokenSource.cancel()
                } else {
                    println("[SSE-TERMINATION] Emitter for job $jobId completed or timed out AFTER job finished. No action needed.")
                }
            }
            emitter.onCompletion(onTermination)
            emitter.onTimeout(onTermination)

            jobContext.future.whenComplete { response, throwable ->
                jobContext.jobHasCompleted.set(true)
                println("[FUTURE] Future completed for job $jobId. Success: ${response != null}, Error: ${throwable != null}")
                try {
                    if (response != null && response.success) {
                        println("[FUTURE] Caching successful result for job $jobId.")
                        val normalizedSourceCode = response.sourceCode.trim().replace("\r\n", "\n")
                        completedJobsCache.put(jobId, response)
                        completedJobsCache.put(normalizedSourceCode, response)
                    }

                    println("[FUTURE] Removing job $jobId from activeJobs.")
                    activeJobs.remove(jobId)

                    synchronized(activeTabJobs) {
                        val removed = activeTabJobs.remove(jobContext.tabSessionId, jobContext.activeJobState)
                        println("[FUTURE] Conditionally removing job state for tab ${jobContext.tabSessionId}. Removed: $removed")
                    }

                    val html = renderResult(response)
                    println("[FUTURE] Sending final SSE event for job $jobId.")
                    emitter.send(SseEmitter.event().name("compile-result").data(html))
                    println("[FUTURE] SSE events sent for job $jobId.")
                    emitter.complete()
                } catch (e: Exception) {
                    println("[FUTURE-ERROR] Exception in whenComplete for job $jobId: ${e.message}")
                    emitter.completeWithError(e)
                }
            }
        } else {
            println("[SSE] Job $jobId not found in active or completed. Assuming expired. Completing connection.")
            emitter.complete()
        }

        return emitter
    }

    private fun renderResult(response: CompileResponse?): String {
        println("[RENDER] Rendering result for job ${response?.jobId}")
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
            val finalHtml = output.toString()
            println("[RENDER] Finished rendering for job ${response?.jobId}")
            return finalHtml
        } catch (e: Exception) {
            println("[RENDER-ERROR] Error during template rendering for job ${response?.jobId}: ${e.message}")
            throw e
        }
    }
}