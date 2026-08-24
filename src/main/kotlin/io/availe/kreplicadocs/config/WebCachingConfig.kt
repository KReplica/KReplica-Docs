package io.availe.kreplicadocs.config

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.CacheControl
import org.springframework.http.HttpHeaders
import org.springframework.web.servlet.HandlerInterceptor
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import java.time.Duration

private const val HX_REQUEST = "HX-Request"
private const val CDN_CACHE_CONTROL = "CDN-Cache-Control"
private const val CACHE_TAG = "Cache-Tag"

class HtmlCachePolicyInterceptor : HandlerInterceptor {
    override fun preHandle(request: HttpServletRequest, response: HttpServletResponse, handler: Any): Boolean {
        response.addHeader(HttpHeaders.VARY, HX_REQUEST)

        if (request.getHeader(HX_REQUEST) != null || request.requestURI.startsWith("/fragments")) {
            response.setHeader(HttpHeaders.CACHE_CONTROL, "private, no-store")
            response.setHeader(CDN_CACHE_CONTROL, "no-store")
        } else {
            response.setHeader(HttpHeaders.CACHE_CONTROL, "no-cache")
            response.setHeader(CDN_CACHE_CONTROL, "public, max-age=300, stale-while-revalidate=86400")
            response.setHeader(CACHE_TAG, "docs-html")
        }

        return true
    }
}

@org.springframework.context.annotation.Configuration
class WebCachingConfig : WebMvcConfigurer {
    override fun addInterceptors(registry: InterceptorRegistry) {
        registry.addInterceptor(HtmlCachePolicyInterceptor())
            .addPathPatterns("/", "/guide", "/playground", "/fragments", "/fragments/**")
    }

    override fun addResourceHandlers(registry: ResourceHandlerRegistry) {
        val cacheControl = CacheControl.maxAge(Duration.ofHours(1))
            .cachePublic()
            .staleWhileRevalidate(Duration.ofDays(1))

        registry.addResourceHandler("/css/**")
            .addResourceLocations("classpath:/static/css/")
            .setCacheControl(cacheControl)
            .resourceChain(true)
        registry.addResourceHandler("/js/**")
            .addResourceLocations("classpath:/static/js/")
            .setCacheControl(cacheControl)
            .resourceChain(true)
        registry.addResourceHandler("/language-model.json")
            .addResourceLocations("classpath:/static/")
            .setCacheControl(cacheControl)
            .resourceChain(true)
    }
}
