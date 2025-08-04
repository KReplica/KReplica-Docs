package io.availe.kreplicadocs.model.view

data class Tab(
    val id: String,
    val label: String,
    val description: String? = null,
    val codeSnippet: String? = null,
    val example: ProcessedGuideExample? = null
)