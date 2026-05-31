package com.occasi.application.constants

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.string.shouldContain
import io.kotest.property.Arb
import io.kotest.property.PropertyTesting
import io.kotest.property.arbitrary.*
import io.kotest.property.forAll

// Feature: centralize-constants
class BackendMessagesPropertyTest : StringSpec({

    beforeSpec {
        PropertyTesting.defaultIterationCount = 100
    }

    // Property 3: Format functions produce strings containing their dynamic arguments
    // **Validates: Requirements 3.1, 4.7**
    "Property 3: Format functions produce strings containing their dynamic arguments" {
        val fieldsArb = Arb.string(1..100, Codepoint.alphanumeric())

        forAll(fieldsArb) { fields ->
            val result = BackendMessages.Validation.validationFailed(fields)
            result.contains(fields)
        }
    }
})
