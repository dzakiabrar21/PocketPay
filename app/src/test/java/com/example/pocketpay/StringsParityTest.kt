package com.example.pocketpay

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** Guards the translations: every key exists in both languages and uses the same format arguments. */
class StringsParityTest {

    private fun load(path: String): Map<String, String> {
        val file = File(path)
        assertTrue("missing $path (working dir ${File(".").absolutePath})", file.exists())
        return Regex("""<string name="([^"]+)"[^>]*>(.*?)</string>""", RegexOption.DOT_MATCHES_ALL)
            .findAll(file.readText()).associate { it.groupValues[1] to it.groupValues[2] }
    }

    private val id = load("src/main/res/values/strings.xml")
    private val en = load("src/main/res/values-en/strings.xml")

    private fun args(text: String) =
        Regex("""%(\d)\$[sd]""").findAll(text).map { it.groupValues[1] + it.value.last() }.sorted().toList()

    @Test
    fun everyIndonesianKeyHasAnEnglishTranslation() {
        // app_name is intentionally untranslated (brand name)
        val missing = id.keys - en.keys - "app_name"
        assertTrue("Missing English strings: $missing", missing.isEmpty())
    }

    @Test
    fun englishHasNoOrphanKeys() {
        val orphans = en.keys - id.keys
        assertTrue("English-only strings: $orphans", orphans.isEmpty())
    }

    @Test
    fun formatArgumentsMatchBetweenLanguages() {
        val mismatched = id.filterKeys { it in en }.filter { (k, v) -> args(v) != args(en.getValue(k)) }.keys
        assertTrue("Format arguments differ for: $mismatched", mismatched.isEmpty())
    }

    @Test
    fun noEmptyStrings() {
        assertEquals(emptyList<String>(), (id + en).filterValues { it.isBlank() }.keys.toList())
    }
}
