package io.availe.kreplicadocs.web

import io.availe.kreplicadocs.common.FragmentTemplate
import io.availe.kreplicadocs.common.PartialTemplate
import io.availe.kreplicadocs.common.WebApp
import io.availe.kreplicadocs.model.CompileRequest
import io.availe.kreplicadocs.services.CodeSnippetProvider
import io.availe.kreplicadocs.services.SandboxService
import io.availe.kreplicadocs.services.ViewModelFactory
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.*
import org.springframework.web.servlet.view.FragmentsRendering
import java.util.*
import java.util.concurrent.TimeoutException

data class CompileRequestForm(val source: String)

@Controller
class PlaygroundController(
    private val viewModelFactory: ViewModelFactory,
    private val snippetProvider: CodeSnippetProvider,
    private val sandboxService: SandboxService
) {

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
        return try {
            val jobId = UUID.randomUUID().toString()
            val request = CompileRequest(jobId, compileRequest.source)
            val response = sandboxService.compile(request)

            if (response.success) {
                model.addAttribute("files", response.generatedFiles)
                "fragments/playground-results"
            } else {
                model.addAttribute("message", response.message)
                "fragments/playground-error"
            }
        } catch (e: TimeoutException) {
            model.addAttribute("message", "Compilation timed out or the server is busy. Please try again shortly.")
            "fragments/playground-error"
        } catch (e: Exception) {
            model.addAttribute("message", "An unexpected error occurred: ${e.message}")
            "fragments/playground-error"
        }
    }
}