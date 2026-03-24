package com.flow.pharos.core.llm

object FilenameSanitizer {
    fun sanitize(name: String): String {
        return name
            .trim()
            .replace(Regex("[^a-zA-Z0-9äöüÄÖÜß_\\-]"), "_")
            .replace(Regex("_+"), "_")
            .trimStart('_')
            .trimEnd('_')
            .take(100)
            .ifEmpty { "unnamed" }
    }
}
