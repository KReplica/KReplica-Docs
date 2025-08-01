package io.availe.kreplicadocs.services

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
        return cacheManager.getCache("playground-templates-cache")?.get(sourceCode, CompileResponse::class.java)
    }

    fun compile(
        request: CompileRequest,
        cancellationTokenSource: CancellationTokenSource
    ): CompletableFuture<CompileResponse> {
        val cachedResponse = isResultInPermanentCache(request.sourceCode)
        if (cachedResponse != null) {
            return CompletableFuture.completedFuture(cachedResponse)
        }
        return asyncCompilerService.runCompilation(request, cancellationTokenSource)
    }
}