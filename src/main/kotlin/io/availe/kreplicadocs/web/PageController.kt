package io.availe.kreplicadocs.web

import io.availe.kreplicadocs.common.FragmentTemplate
import io.availe.kreplicadocs.common.PageTemplate
import io.availe.kreplicadocs.common.PartialTemplate
import io.availe.kreplicadocs.common.WebApp
import io.availe.kreplicadocs.services.ViewModelFactory
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.servlet.view.FragmentsRendering

@Controller
class PageController(private val viewModelFactory: ViewModelFactory) {

    @GetMapping(WebApp.Endpoints.Pages.INDEX)
    fun index(model: Model, @RequestHeader(name = "HX-Request", required = false) hxRequest: String?): Any {
        model.addAttribute("vm", viewModelFactory.createIndexViewModel())
        return render(hxRequest, PageTemplate.INDEX, PartialTemplate.CONTENT_INDEX)
    }

    @GetMapping(WebApp.Endpoints.Pages.GUIDE)
    fun guide(model: Model, @RequestHeader(name = "HX-Request", required = false) hxRequest: String?): Any {
        model.addAttribute("vm", viewModelFactory.createGuideViewModel())
        return render(hxRequest, PageTemplate.GUIDE, PartialTemplate.CONTENT_GUIDE)
    }

    private fun render(hxRequest: String?, page: PageTemplate, partial: PartialTemplate): Any {
        return if (hxRequest != null) {
            FragmentsRendering
                .with(partial.path)
                .fragment(FragmentTemplate.NAV_UPDATE_OOB.path)
                .fragment(FragmentTemplate.FAB_UPDATE_OOB.path)
                .build()
        } else {
            page.path
        }
    }
}