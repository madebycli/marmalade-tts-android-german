package app.marmalade.tts.engine.pocket

import app.marmalade.tts.phonemizer.GermanTextNormalizer

/** Text only, never IPA/espeak. German numbers/dates use the existing Apache-2.0 normalizer.
 * Apply Kyutai config/german.yaml replacements before the common smart punctuation fold.
 * Keep newlines for PocketLineBreaks; normalization is idempotent across chunking passes.
 */
internal object PocketGermanText {
    private val temperature = Regex("([−-]?\\d+(?:,\\d+)?)\\s*°\\s*C\\b")
    fun normalize(raw: String): String {
        var text = raw
        text = temperature.replace(text) { "${it.groupValues[1]} Grad Celsius" }
        text = text.replace('−', '-')
        text = Regex("(?<![\\p{L}\\d])-+(?=\\d)").replace(text, "minus ")
        text = text.replace("→", " nach ").replace("←", " zurück ")
            .replace("%", " Prozent ").replace("&", " und ")
        val filtered = buildString(text.length) {
            for (c in text) when (c) {
                '"', '“', '”', '„', '«', '»', '(', ')', '[', ']' -> Unit
                '’', '‘' -> append('\'')
                '–', '—', ';' -> append(", ")
                else -> append(c)
            }
        }
        val normalized = GermanTextNormalizer.normalize(filtered)
            .replace(Regex("[ \\t]+(?=[,.!?])"), "")
        val date = Regex("\\b(am|vom|zum|ab dem|seit dem|bis zum) ([\\p{L}]+te)(?= (?:Januar|Februar|März|April|Mai|Juni|Juli|August|September|Oktober|November|Dezember)\\b)", RegexOption.IGNORE_CASE)
        return date.replace(normalized) { "${it.groupValues[1]} ${it.groupValues[2].dropLast(1)}en" }.trim()
    }
}
