package io.availe.kreplicadocs.model

import io.availe.kreplicadocs.common.PageId

@JvmInline
value class JobId(val value: String)

@JvmInline
value class TabSessionId(val value: String)

@JvmInline
value class TemplateSlug(val value: String)

data class NavLink(
    val href: String,
    val pageId: PageId,
    val label: String
)

data class CompileRequest(
    val jobId: JobId,
    val sourceCode: String
)

data class CompileResponse(
    val jobId: JobId,
    val sourceCode: String,
    val success: Boolean,
    val generatedFiles: Map<FileName, String>? = null,
    val message: String
)

@JvmInline
value class FileName(val value: String)

data class PlaygroundTemplate(
    val slug: String,
    val name: String,
    val description: String
)