package io.availe.kreplicadocs.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@Configuration
class VirtualThreadConfig {
    @Bean(destroyMethod = "close")
    fun virtualThreadExecutor(): ExecutorService =
        Executors.newVirtualThreadPerTaskExecutor()
}
