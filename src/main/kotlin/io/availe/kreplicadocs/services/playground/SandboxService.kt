package io.availe.kreplicadocs.services.playground

import io.availe.kreplicadocs.model.CompileRequest
import io.availe.kreplicadocs.model.CompileResponse
import org.gradle.tooling.CancellationTokenSource
import org.springframework.cache.CacheManager
import org.springframework.stereotype.Service
import java.util.concurrent.CompletableFuture

@Service
class SandboxService(
    private val asyncCompilerService: AsyncCompilerService,
    private val cacheManager: CacheManager
) {

    fun isResultInPermanentCache(sourceCode: String): CompileResponse? {
        val cache = cacheManager.getCache("playground-templates-cache")
        val response = cache?.get(sourceCode, CompileResponse::class.java)
        if (response != null) {
            println("[SANDBOX] Permanent cache HIT.")
        } else {
            println("[SANDBOX] Permanent cache MISS.")
        }
        return response
    }

    fun compile(
        request: CompileRequest,
        cancellationTokenSource: CancellationTokenSource
    ): CompletableFuture<CompileResponse> {
        println("[SANDBOX] Compile request for job: ${request.jobId}")
        val cachedResponse = isResultInPermanentCache(request.sourceCode)
        if (cachedResponse != null) {
            return CompletableFuture.completedFuture(cachedResponse)
        }

        println("[SANDBOX] Delegating to async compiler for job: ${request.jobId}")
        return asyncCompilerService.runCompilation(request, cancellationTokenSource)
    }
}