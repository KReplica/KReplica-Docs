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
import org.gradle.tooling.CancellationTokenSource
import org.gradle.tooling.GradleConnector
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.ui.ModelMap
import org.springframework.web.bind.annotation.*
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import org.springframework.web.servlet.view.FragmentsRendering
import java.io.IOException
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

data class CompileRequestForm(val source: String, val tabSessionId: String)

data class JobContext(
    val future: CompletableFuture<CompileResponse>,
    val cancellationTokenSource: CancellationTokenSource,
    val isCancelled: AtomicBoolean = AtomicBoolean(false),
    val tabSessionId: String
)

@Controller
class PlaygroundController(
    private val viewModelFactory: ViewModelFactory,
    private val snippetProvider: CodeSnippetProvider,
    private val sandboxService: SandboxService,
    private val templateEngine: TemplateEngine,
    private val compilationTaskExecutor: ThreadPoolTaskExecutor,
) {

    private val activeJobs = ConcurrentHashMap<String, JobContext>()
    private val activeTabJobs = ConcurrentHashMap<String, String>()
    private val emitters = ConcurrentHashMap<String, SseEmitter>()
    private val SSE_TIMEOUT = TimeUnit.MINUTES.toMillis(5)

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
    fun compile(@ModelAttribute compileRequest: CompileRequestForm, model: Model): String {
        activeTabJobs.remove(compileRequest.tabSessionId)?.let { oldJobId ->
            activeJobs[oldJobId]?.let { oldJobContext ->
                oldJobContext.isCancelled.set(true)
                oldJobContext.cancellationTokenSource.cancel()
            }
        }

        val jobId = UUID.randomUUID().toString()
        val request = CompileRequest(jobId, compileRequest.source)
        val cancellationTokenSource = GradleConnector.newCancellationTokenSource()

        val jobsAhead = compilationTaskExecutor.activeCount + compilationTaskExecutor.queueSize

        val responseFuture = sandboxService.compile(request, cancellationTokenSource)
        val jobContext = JobContext(responseFuture, cancellationTokenSource, tabSessionId = compileRequest.tabSessionId)
        activeJobs[jobId] = jobContext
        activeTabJobs[compileRequest.tabSessionId] = jobId

        if (responseFuture.isDone && !responseFuture.isCompletedExceptionally()) {
            val response = responseFuture.getNow(null)
            if (response != null) {
                model.addAttribute("files", response.generatedFiles)
                activeJobs.remove(jobId)
                activeTabJobs.remove(compileRequest.tabSessionId)
                return "fragments/playground-results"
            }
        }

        model.addAttribute("jobId", jobId)
        model.addAttribute("jobsAhead", jobsAhead)
        return "fragments/playground-compiling"
    }

    @GetMapping("/playground/status/{jobId}")
    fun getCompilationStatus(@PathVariable jobId: String): SseEmitter {
        val emitter = SseEmitter(SSE_TIMEOUT)
        val jobContext = activeJobs[jobId]

        if (jobContext == null) {
            emitter.completeWithError(IllegalStateException("Job not found or already completed."))
            return emitter
        }

        emitters[jobId] = emitter
        val cleanup = Runnable {
            emitters.remove(jobId)
            activeJobs[jobId]?.let {
                it.isCancelled.set(true)
                it.cancellationTokenSource.cancel()
                activeTabJobs.remove(it.tabSessionId)
            }
            activeJobs.remove(jobId)
        }
        emitter.onCompletion(cleanup)
        emitter.onTimeout(cleanup)

        jobContext.future.whenComplete { response, throwable ->
            emitters.remove(jobId)
            activeJobs.remove(jobId)
            activeTabJobs.remove(jobContext.tabSessionId)

            updateWaitingClients()

            val finalEmitter = emitters[jobId] ?: emitter
            try {
                val modelMap = ModelMap()
                val output = StringOutput()
                val template: String

                if (throwable != null) {
                    modelMap.addAttribute("message", "An unexpected error occurred: ${throwable.message}")
                    template = "fragments/playground-error"
                } else if (response.success) {
                    modelMap.addAttribute("files", response.generatedFiles)
                    template = "fragments/playground-results"
                } else {
                    modelMap.addAttribute("message", response.message)
                    template = "fragments/playground-error"
                }

                templateEngine.render(template, modelMap, output)
                finalEmitter.send(SseEmitter.event().name("message").data(output.toString()))
                finalEmitter.complete()
            } catch (e: Exception) {
                finalEmitter.completeWithError(e)
            }
        }
        return emitter
    }

    private fun updateWaitingClients() {
        val currentQueueSize = compilationTaskExecutor.queueSize
        val activeCount = compilationTaskExecutor.activeCount
        val jobsAhead = activeCount + currentQueueSize - 1

        emitters.keys.forEach { waitingJobId ->
            emitters[waitingJobId]?.let { emitter ->
                try {
                    val modelMap = ModelMap().apply {
                        addAttribute("jobId", waitingJobId)
                        addAttribute("jobsAhead", jobsAhead)
                    }
                    val output = StringOutput()
                    templateEngine.render(FragmentTemplate.PLAYGROUND_COMPILING_OOB.path, modelMap, output)
                    emitter.send(
                        SseEmitter.event()
                            .name("message")
                            .data(output.toString())
                    )
                } catch (e: IOException) {
                    emitters.remove(waitingJobId)
                }
            }
        }
    }
}