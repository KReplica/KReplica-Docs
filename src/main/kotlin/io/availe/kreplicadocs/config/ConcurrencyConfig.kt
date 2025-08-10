package io.availe.kreplicadocs.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@Configuration
class ConcurrencyConfig {

    @Bean(name = ["compilationTaskExecutor"])
    fun compilationTaskExecutor(): ThreadPoolTaskExecutor {
        val executor = ThreadPoolTaskExecutor()
        executor.corePoolSize = 1
        executor.maxPoolSize = 1
        executor.queueCapacity = 500
        executor.setThreadNamePrefix("Compiler-")
        executor.initialize()
        return executor
    }

    @Bean(destroyMethod = "close")
    fun virtualThreadExecutor(): ExecutorService =
        Executors.newVirtualThreadPerTaskExecutor()
}