package io.availe.kreplicadocs.model

import io.availe.kreplicadocs.common.PageId

data class NavLink(
    val href: String,
    val pageId: PageId,
    val label: String
)

data class CompileRequest(
    val jobId: String,
    val sourceCode: String
)

data class CompileResponse(
    val jobId: String,
    val sourceCode: String,
    val success: Boolean,
    val generatedFiles: Map<String, String>? = null,
    val message: String
)

@JvmInline
value class ExampleSlug(val value: String)

@JvmInline
value class FileName(val value: String)

data class FeatureTourSubStep(
    val title: String,
    val description: String,
    val file: FileName
)

data class FeatureTourStep(
    val title: String,
    val description: String,
    val file: FileName?,
    val part: Int,
    var endpoint: String = "",
    val subSteps: List<FeatureTourSubStep> = emptyList()
)

data class PlaygroundTemplate(
    val slug: String,
    val name: String,
    val description: String
)