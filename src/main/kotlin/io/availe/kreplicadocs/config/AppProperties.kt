package io.availe.kreplicadocs.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("app.docs")
data class AppProperties(
    val repoUrl: String,
    val issuesUrl: String,
    val personalUrl: String,
)