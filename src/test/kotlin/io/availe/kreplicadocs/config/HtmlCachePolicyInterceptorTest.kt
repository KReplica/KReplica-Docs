package io.availe.kreplicadocs.config

import org.springframework.http.HttpHeaders
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class HtmlCachePolicyInterceptorTest {
    private val interceptor = HtmlCachePolicyInterceptor()

    @Test
    fun `full documents are revalidated by browsers and cached briefly at the edge`() {
        val request = MockHttpServletRequest("GET", "/guide")
        val response = MockHttpServletResponse()

        interceptor.preHandle(request, response, Any())

        assertEquals("HX-Request", response.getHeader(HttpHeaders.VARY))
        assertEquals("no-cache", response.getHeader(HttpHeaders.CACHE_CONTROL))
        assertEquals(
            "public, max-age=300, stale-while-revalidate=86400",
            response.getHeader("CDN-Cache-Control"),
        )
        assertEquals("docs-html", response.getHeader("Cache-Tag"))
    }

    @Test
    fun `htmx responses cannot enter browser or edge caches`() {
        val request = MockHttpServletRequest("GET", "/guide")
        request.addHeader("HX-Request", "true")
        val response = MockHttpServletResponse()

        interceptor.preHandle(request, response, Any())

        assertEquals("private, no-store", response.getHeader(HttpHeaders.CACHE_CONTROL))
        assertEquals("no-store", response.getHeader("CDN-Cache-Control"))
        assertNull(response.getHeader("Cache-Tag"))
    }

    @Test
    fun `dedicated fragment urls cannot enter edge caches without an htmx header`() {
        val request = MockHttpServletRequest("GET", "/fragments/guide")
        val response = MockHttpServletResponse()

        interceptor.preHandle(request, response, Any())

        assertEquals("private, no-store", response.getHeader(HttpHeaders.CACHE_CONTROL))
        assertEquals("no-store", response.getHeader("CDN-Cache-Control"))
    }
}
