package io.availe.kreplicadocs.services

import io.availe.kreplicadocs.model.CompileRequest
import io.availe.kreplicadocs.model.CompileResponse
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import java.util.concurrent.CompletableFuture

@Service
class AsyncCompilerService(private val compilerService: CompilerService) {

    @Async("compilationTaskExecutor")
    fun runCompilation(request: CompileRequest): CompletableFuture<CompileResponse> {
        val result = compilerService.compile(request)
        return CompletableFuture.completedFuture(result)
    }
}