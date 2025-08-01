package io.availe.kreplicadocs.config

import com.github.benmanes.caffeine.cache.Caffeine
import org.springframework.cache.CacheManager
import org.springframework.cache.caffeine.CaffeineCache
import org.springframework.cache.support.SimpleCacheManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.util.concurrent.TimeUnit

object CacheNames {
    const val COMPLETED_JOBS = "completed-jobs-cache"
    const val PERMANENT_TEMPLATES = "playground-templates-cache"
}

@Configuration
class CachingConfig {
    @Bean
    fun cacheManager(): CacheManager {
        val completedJobsCache = CaffeineCache(
            CacheNames.COMPLETED_JOBS,
            Caffeine.newBuilder()
                .maximumSize(100)
                .expireAfterWrite(15, TimeUnit.MINUTES)
                .build()
        )

        val permanentTemplatesCache = CaffeineCache(
            CacheNames.PERMANENT_TEMPLATES,
            Caffeine.newBuilder()
                .maximumSize(50)
                .build()
        )

        val cacheManager = SimpleCacheManager()
        cacheManager.setCaches(listOf(completedJobsCache, permanentTemplatesCache))
        return cacheManager
    }
}