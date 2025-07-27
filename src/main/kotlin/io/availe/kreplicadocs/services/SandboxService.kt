package io.availe.kreplicadocs.services

import io.availe.kreplicadocs.model.CompileRequest
import io.availe.kreplicadocs.model.CompileResponse
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

@Service
class SandboxService(private val compilerService: CompilerService) {

    private val compilationSemaphore =
        Semaphore((Runtime.getRuntime().availableProcessors() / 2).coerceAtLeast(1), true)

    @Cacheable("playground-cache", key = "#request.sourceCode")
    fun compile(request: CompileRequest): CompileResponse {
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