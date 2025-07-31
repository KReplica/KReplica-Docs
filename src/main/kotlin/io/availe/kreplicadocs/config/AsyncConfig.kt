package io.availe.kreplicadocs.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor

@Configuration
class AsyncConfig {

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
}