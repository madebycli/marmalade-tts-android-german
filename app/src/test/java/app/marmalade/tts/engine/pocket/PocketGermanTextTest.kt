package app.marmalade.tts.engine.pocket

import org.junit.Assert.assertEquals
import org.junit.Test

class PocketGermanTextTest {
    @Test fun `times are spoken once with the correct minutes`() {
        assertEquals("Um sechzehn Uhr dreißig.", PocketGermanText.normalize("Um 16:30 Uhr."))
        assertEquals("Um dreizehn Uhr.", PocketGermanText.normalize("Um 13:00 Uhr."))
    }
    @Test fun `German date ordinals and umlauts survive`() {
        assertEquals("am vierzehnten März", PocketGermanText.normalize("am 14. März"))
        assertEquals("Äpfel, Öl und süße Grüße!", PocketGermanText.normalize("Äpfel, Öl und süße Grüße!"))
    }
    @Test fun `common abbreviations are readable text and never IPA`() {
        assertEquals("Doktor Müller kommt zum Beispiel.", PocketGermanText.normalize("Dr. Müller kommt z. B.."))
    }
    @Test fun `percent ampersands and arrows become German words`() {
        assertEquals("fünfzig Prozent und Köln nach Düsseldorf", PocketGermanText.normalize("50 % & Köln → Düsseldorf"))
        assertEquals("Der Akku hat fünfundzwanzig Prozent.", PocketGermanText.normalize("Der Akku hat 25%."))
    }
    @Test fun `negative temperature is spoken as degrees`() {
        assertEquals("minus vier Grad Celsius", PocketGermanText.normalize("−4°C"))
    }
    @Test fun `upstream German punctuation rules run before the English smart fold`() {
        assertEquals("Hallo, Welt, schön.", PocketGermanText.normalize("„Hallo“ — (Welt); [schön]."))
    }
    @Test fun `German currency expands with cents`() {
        assertEquals("eintausendzweihundertvierunddreißig Euro und sechsundfünfzig Cent", PocketGermanText.normalize("1.234,56 €"))
    }
    @Test fun `empty text stays empty and paragraphs remain paragraphs`() {
        assertEquals("", PocketGermanText.normalize("  "))
        assertEquals("Eins\nZwei", PocketGermanText.normalize("Eins\nZwei"))
    }
    @Test fun `repeated chunk preparation is idempotent`() {
        for (text in listOf("Am 14. März um 16:30 Uhr: 25%.", "Äpfel & Öl → Küche.", "−4 °C", "3,14")) {
            val once = PocketGermanText.normalize(text)
            assertEquals(text, once, PocketGermanText.normalize(once))
        }
    }
}
