package io.availe.kreplicadocs.services.playground

import io.availe.kreplicadocs.model.CompileRequest
import io.availe.kreplicadocs.model.CompileResponse
import org.gradle.tooling.CancellationTokenSource
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor

@Service
class PlaygroundService(
    private val gradleCompiler: GradleCompiler,
    @param:Qualifier("compilationTaskExecutor") private val compilationExecutor: Executor
) {
    private val log = LoggerFactory.getLogger(PlaygroundService::class.java)

    fun submitCompilation(
        request: CompileRequest,
        cancellationTokenSource: CancellationTokenSource
    ): CompletableFuture<CompileResponse> {
        log.debug("Submitting job {} to compiler executor.", request.jobId.value)
        return CompletableFuture.supplyAsync(
            { gradleCompiler.compile(request, cancellationTokenSource) },
            compilationExecutor
        )
    }
}