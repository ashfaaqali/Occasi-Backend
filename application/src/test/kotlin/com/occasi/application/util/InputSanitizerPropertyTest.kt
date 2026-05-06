package com.occasi.application.util

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.PropertyTesting
import io.kotest.property.arbitrary.*
import io.kotest.property.forAll

// Feature: input-validations, Property 8: Input sanitizer HTML removal and plain text preservation
class InputSanitizerPropertyTest : StringSpec({

    beforeSpec {
        PropertyTesting.defaultIterationCount = 100
    }

    // **Validates: Requirements 3.1, 3.2, 3.3, 3.5**

    "Property 8a: Plain text without angle brackets is preserved after trimming" {
        // Generate strings that contain no '<' or '>' characters
        val plainTextArb = Arb.string(0..200, Codepoint.ascii())
            .map { it.replace("<", "").replace(">", "") }

        forAll(plainTextArb) { input ->
            val result = InputSanitizer.sanitize(input)
            result == input.trim()
        }
    }

    "Property 8b: Output never contains HTML tag patterns" {
        // Generate strings with injected HTML tags
        val tagNames = listOf("div", "span", "p", "a", "b", "i", "img", "br", "h1", "table")
        val tagNameArb = Arb.element(tagNames)
        val plainContentArb = Arb.string(0..50, Codepoint.alphanumeric())

        val htmlStringArb = Arb.bind(
            tagNameArb,
            plainContentArb,
            plainContentArb
        ) { tag, content, prefix ->
            "$prefix<$tag>$content</$tag>"
        }

        forAll(htmlStringArb) { input ->
            val result = InputSanitizer.sanitize(input)
            !result.contains(Regex("<[^>]+>"))
        }
    }

    "Property 8c: Output never contains script block content" {
        // Generate strings with <script>...</script> blocks
        val scriptContentArb = Arb.string(1..100, Codepoint.alphanumeric())
        val prefixArb = Arb.string(0..30, Codepoint.alphanumeric())
        val suffixArb = Arb.string(0..30, Codepoint.alphanumeric())

        val scriptStringArb = Arb.bind(
            prefixArb,
            scriptContentArb,
            suffixArb
        ) { prefix, scriptContent, suffix ->
            "${prefix}<script>${scriptContent}</script>${suffix}"
        }

        forAll(scriptStringArb) { input ->
            val result = InputSanitizer.sanitize(input)
            !result.contains(Regex("<script", RegexOption.IGNORE_CASE)) &&
                !result.contains(Regex("</script>", RegexOption.IGNORE_CASE)) &&
                !result.contains(Regex("<[^>]+>"))
        }
    }

    "Property 8d: Strings containing only HTML tags produce empty string" {
        // Generate strings that are purely HTML tags with no plain text
        val tagNames = listOf("div", "span", "p", "a", "b", "i", "h1", "h2")
        val tagNameArb = Arb.element(tagNames)

        val pureHtmlArb = Arb.list(tagNameArb, 1..5).map { tags ->
            tags.joinToString("") { "<$it></$it>" }
        }

        forAll(pureHtmlArb) { input ->
            val result = InputSanitizer.sanitize(input)
            result == ""
        }
    }
})
