package com.example.pocketpay

import androidx.compose.ui.text.AnnotatedString
import com.example.pocketpay.ui.components.ThousandsTransformation
import org.junit.Assert.assertEquals
import org.junit.Test

class ThousandsTransformationTest {
    private fun fmt(s: String) = ThousandsTransformation.filter(AnnotatedString(s)).text.text

    @Test fun groupsByThousands() {
        assertEquals("", fmt(""))
        assertEquals("5", fmt("5"))
        assertEquals("999", fmt("999"))
        assertEquals("1.000", fmt("1000"))
        assertEquals("23.500", fmt("23500"))
        assertEquals("1.234.567", fmt("1234567"))
        assertEquals("100.000.000", fmt("100000000"))
    }

    @Test fun cursorMappingRoundTrips() {
        for (digits in listOf("1", "12", "123", "1234", "23500", "1234567")) {
            val t = ThousandsTransformation.filter(AnnotatedString(digits))
            for (offset in 0..digits.length) {
                val shown = t.offsetMapping.originalToTransformed(offset)
                assertEquals("digits=$digits offset=$offset", offset, t.offsetMapping.transformedToOriginal(shown))
            }
        }
    }

    @Test fun cursorAtEndStaysAtEnd() {
        val t = ThousandsTransformation.filter(AnnotatedString("23500"))
        assertEquals(6, t.offsetMapping.originalToTransformed(5)) // "23.500" has 6 characters
    }
}
