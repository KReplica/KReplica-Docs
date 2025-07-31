package io.availe.kreplicadocs.services

import io.availe.kreplicadocs.model.CompileRequest
import io.availe.kreplicadocs.model.CompileResponse
import org.springframework.cache.CacheManager
import org.springframework.stereotype.Service
import java.util.concurrent.CompletableFuture

@Service
class SandboxService(
    private val asyncCompilerService: AsyncCompilerService,
    private val cacheManager: CacheManager
) {

    fun compile(request: CompileRequest): CompletableFuture<CompileResponse> {
        val permanentCache = cacheManager.getCache("playground-templates-cache")

        permanentCache?.get(request.sourceCode, CompileResponse::class.java)?.let {
            return CompletableFuture.completedFuture(it)
        }

        return asyncCompilerService.runCompilation(request)
    }
}