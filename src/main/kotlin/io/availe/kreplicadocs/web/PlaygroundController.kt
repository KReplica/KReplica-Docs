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

data class CompileRequestForm(val source: String, val tabSessionId: String)

data class ActiveJobState(val jobId: String, val sourceCode: String)

data class JobContext(
    val future: CompletableFuture<CompileResponse>,
    val cancellationTokenSource: CancellationTokenSource,
    val tabSessionId: String
)

@Controller
class PlaygroundController(
    private val viewModelFactory: ViewModelFactory,
    private val snippetProvider: CodeSnippetProvider,
    private val sandboxService: SandboxService,
    private val templateEngine: TemplateEngine,
    private val cacheManager: CacheManager,
) {

    private val activeJobs = ConcurrentHashMap<String, JobContext>()
    private val activeTabJobs = ConcurrentHashMap<String, ActiveJobState>()
    private lateinit var completedJobsCache: Cache
    private val SSE_TIMEOUT = TimeUnit.MINUTES.toMillis(5)

    @PostConstruct
    fun init() {
        completedJobsCache = cacheManager.getCache("completed-jobs-cache")
            ?: throw IllegalStateException("Cache 'completed-jobs-cache' not found.")
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
        synchronized(activeTabJobs) {
            val existingJobState = activeTabJobs[compileRequestForm.tabSessionId]
            if (existingJobState != null && existingJobState.sourceCode == compileRequestForm.source) {
                model.addAttribute("jobId", existingJobState.jobId)
                return "fragments/playground-compiling"
            }

            existingJobState?.let {
                activeJobs[it.jobId]?.cancellationTokenSource?.cancel()
            }

            val jobId = UUID.randomUUID().toString()
            val newJobState = ActiveJobState(jobId, compileRequestForm.source)
            activeTabJobs[compileRequestForm.tabSessionId] = newJobState

            val request = CompileRequest(jobId, compileRequestForm.source)
            val permanentCacheHit = sandboxService.isResultInPermanentCache(request.sourceCode)

            if (permanentCacheHit != null) {
                model.addAttribute("files", permanentCacheHit.generatedFiles)
                return "fragments/playground-results"
            }

            val cancellationTokenSource = GradleConnector.newCancellationTokenSource()
            val responseFuture = sandboxService.compile(request, cancellationTokenSource)
            val jobContext = JobContext(responseFuture, cancellationTokenSource, compileRequestForm.tabSessionId)

            activeJobs[jobId] = jobContext

            model.addAttribute("jobId", jobId)
            return "fragments/playground-compiling"
        }
    }


    @GetMapping("/playground/status/{jobId}")
    fun getCompilationStatus(@PathVariable jobId: String): SseEmitter {
        val emitter = SseEmitter(SSE_TIMEOUT)

        val completedResponse = completedJobsCache.get(jobId, CompileResponse::class.java)
        if (completedResponse != null) {
            val html = renderResult(completedResponse)
            emitter.send(SseEmitter.event().name("message").data(html))
            emitter.complete()
            return emitter
        }

        val jobContext = activeJobs[jobId]
        if (jobContext != null) {
            emitter.onCompletion { jobContext.cancellationTokenSource.cancel() }
            emitter.onTimeout { jobContext.cancellationTokenSource.cancel() }

            jobContext.future.whenComplete { response, _ ->
                activeJobs.remove(jobId)
                activeTabJobs.remove(jobContext.tabSessionId)

                if (response != null && response.success) {
                    completedJobsCache.put(jobId, response)
                }

                val html = renderResult(response)
                emitter.send(SseEmitter.event().name("message").data(html))
                emitter.complete()
            }
        } else {
            emitter.complete()
        }

        return emitter
    }

    private fun renderResult(response: CompileResponse?): String {
        val modelMap = ModelMap()
        val output = StringOutput()

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

        return """<div hx-swap-oob="innerHTML:#playground-output">${output.toString()}</div><div hx-swap-oob="delete" id="job-status-$response?.jobId"></div>"""
    }
}