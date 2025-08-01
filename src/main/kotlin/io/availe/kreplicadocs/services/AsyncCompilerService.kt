package io.availe.kreplicadocs.services

import io.availe.kreplicadocs.model.CompileRequest
import io.availe.kreplicadocs.model.CompileResponse
import org.gradle.tooling.CancellationTokenSource
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import java.util.concurrent.CompletableFuture

@Service
class AsyncCompilerService(private val compilerService: CompilerService) {

    @Async("compilationTaskExecutor")
    fun runCompilation(
        request: CompileRequest,
        cancellationTokenSource: CancellationTokenSource
    ): CompletableFuture<CompileResponse> {
        if (cancellationTokenSource.token().isCancellationRequested) {
            return CompletableFuture.failedFuture(org.gradle.tooling.BuildCancelledException("Job was cancelled before starting."))
        }
        val result = compilerService.compile(request, cancellationTokenSource)
        return CompletableFuture.completedFuture(result)
    }
}