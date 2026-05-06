package com.occasi.application.util

object InputSanitizer {

    private val SCRIPT_BLOCK_REGEX = Regex(
        "<script[^>]*>.*?</script>",
        setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE)
    )
    private val HTML_TAG_REGEX = Regex("<[^>]+>")

    /**
     * Strips HTML tags and script blocks from input text.
     * Returns the plain text content, or empty string if input was only HTML.
     */
    fun sanitize(input: String): String {
        // First remove script blocks (including content between tags)
        val withoutScripts = SCRIPT_BLOCK_REGEX.replace(input, "")
        // Then remove remaining HTML tags
        val withoutTags = HTML_TAG_REGEX.replace(withoutScripts, "")
        return withoutTags.trim()
    }
}
