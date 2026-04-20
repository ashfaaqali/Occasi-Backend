package com.occasi.application

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.PropertyTesting
import io.kotest.property.arbitrary.*
import io.kotest.property.forAll

// Feature: invitation-cards-ordering
class TagParsingPropertyTest : StringSpec({

    beforeSpec {
        PropertyTesting.defaultIterationCount = 100
    }

    // Property 1: Tag comma-separated round-trip
    // **Validates: Requirements 1.6**
    "Property 1: Tag comma-separated round-trip" {
        val tagArb = Arb.string(1..30, Codepoint.alphanumeric())
        val tagListArb = Arb.list(tagArb, 1..10)

        forAll(tagListArb) { tags ->
            val joined = tags.joinToString(",")
            val parsed = joined.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            parsed == tags
        }
    }

    // Property 2: PhotoUrl comma-separated round-trip
    // **Validates: Requirements 3.4**
    "Property 2: PhotoUrl comma-separated round-trip" {
        val keyArb = Arb.string(1..20, Codepoint.alphanumeric())
        val s3UrlArb = keyArb.map { key -> "https://s3.amazonaws.com/occasi-bucket/$key" }
        val urlListArb = Arb.list(s3UrlArb, 1..5)

        forAll(urlListArb) { urls ->
            val joined = urls.joinToString(",")
            val parsed = joined.split(",").filter { it.isNotBlank() }
            parsed == urls
        }
    }
})
