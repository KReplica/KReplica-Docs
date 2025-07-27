package io.availe.kreplicadocs.services

import io.availe.kreplicadocs.model.CompileRequest
import io.availe.kreplicadocs.model.CompileResponse
import org.springframework.cache.CacheManager
import org.springframework.stereotype.Service
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

@Service
class SandboxService(
    private val compilerService: CompilerService,
    private val cacheManager: CacheManager
) {

    private val compilationSemaphore =
        Semaphore((Runtime.getRuntime().availableProcessors() / 2).coerceAtLeast(1), true)

    fun compile(request: CompileRequest): CompileResponse {
        val permanentCache = cacheManager.getCache("playground-templates-cache")

        permanentCache?.get(request.sourceCode, CompileResponse::class.java)?.let {
            return it
        }

        if (!compilationSemaphore.tryAcquire(30, TimeUnit.SECONDS)) {
            throw TimeoutException("Could not acquire compilation lock within 30 seconds. The server may be busy.")
        }

        try {
            return compilerService.compile(request)
        } finally {
            compilationSemaphore.release()
        }
    }
}