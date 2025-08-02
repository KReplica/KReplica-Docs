package io.availe.kreplicadocs.services.playground

import io.availe.kreplicadocs.config.CacheNames
import io.availe.kreplicadocs.model.CompileRequest
import io.availe.kreplicadocs.model.CompileResponse
import org.gradle.tooling.CancellationTokenSource
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.cache.Cache
import org.springframework.cache.CacheManager
import org.springframework.stereotype.Service
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor

@Service
class PlaygroundService(
    private val gradleCompiler: GradleCompiler,
    private val cacheManager: CacheManager,
    @param:Qualifier("compilationTaskExecutor") private val compilationExecutor: Executor
) {
    private val log = LoggerFactory.getLogger(PlaygroundService::class.java)
    private val permanentCache: Cache by lazy {
        cacheManager.getCache(CacheNames.PERMANENT_TEMPLATES)
            ?: throw IllegalStateException("Cache '${CacheNames.PERMANENT_TEMPLATES}' not found.")
    }

    fun submitCompilation(
        request: CompileRequest,
        cancellationTokenSource: CancellationTokenSource
    ): CompletableFuture<CompileResponse> {
        val normalizedSourceCode = request.sourceCode.trim().replace("\r\n", "\n")

        permanentCache.get(normalizedSourceCode, CompileResponse::class.java)?.let {
            log.debug("Permanent cache HIT for job {}. Returning cached response.", request.jobId.value)
            return CompletableFuture.completedFuture(it)
        }

        log.debug("Permanent cache MISS for job {}. Submitting to compiler.", request.jobId.value)
        return CompletableFuture.supplyAsync(
            { gradleCompiler.compile(request, cancellationTokenSource) },
            compilationExecutor
        ).thenApply { response ->
            if (response.success) {
                permanentCache.put(normalizedSourceCode, response)
                log.debug("Compilation for job {} successful. Result cached.", request.jobId.value)
            }
            response
        }
    }
}