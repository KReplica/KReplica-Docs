package io.availe.kreplicadocs.services

import org.springframework.stereotype.Service

@Service
class SourceCodeNormalizer {

    private val blockCommentRegex = Regex("/\\*(?!\\*)[\\s\\S]*?\\*/")
    private val singleLineCommentRegex = Regex("//.*")
    private val windowsLineEndingRegex = Regex("\r\n")

    fun getCacheKey(source: String): String {
        return source
            .replace(windowsLineEndingRegex, "\n")
            .replace(blockCommentRegex, "")
            .replace(singleLineCommentRegex, "")
            .lines()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .joinToString("\n")
    }
}