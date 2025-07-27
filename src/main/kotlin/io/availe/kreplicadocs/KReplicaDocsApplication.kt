package io.availe.kreplicadocs

import io.availe.kreplicadocs.config.AppProperties
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication
import org.springframework.cache.annotation.EnableCaching
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableConfigurationProperties(AppProperties::class)
@EnableCaching
@EnableScheduling
class KReplicaDocsApplication

fun main(args: Array<String>) {
    runApplication<KReplicaDocsApplication>(*args)
}