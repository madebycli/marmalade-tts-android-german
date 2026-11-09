package app.marmalade.tts.data

import app.marmalade.tts.engine.EngineRegistry
import app.marmalade.tts.install.EngineCatalog
import org.junit.Assert.*
import org.junit.Test

class PocketGermanVoiceCatalogTest {
    @Test fun `all official German voice IDs are included exactly once`() {
        val official = setOf("alba", "anna", "azelma", "bill_boerst", "caro_davy", "charles", "cosette", "daan", "eponine", "estelle", "eve", "fantine", "george", "giovanni", "jane", "javert", "jean", "juergen", "lola", "marius", "mary", "michael", "paul", "peter_yearsley", "rafael", "stuart_bell", "vera")
        assertEquals(27, PocketGermanVoiceCatalog.voices.size)
        assertEquals(official, PocketGermanVoiceCatalog.KEYS.toSet())
        assertEquals(27, PocketGermanVoiceCatalog.voices.map { it.id }.distinct().size)
        assertTrue(PocketGermanVoiceCatalog.voices.all { it.languageCode == "de-DE" && it.sampleRate == 24000 })
    }
    @Test fun `several official female voices and Anna default are selectable`() {
        assertTrue(PocketGermanVoiceCatalog.voices.count { it.gender == "female" } >= 12)
        assertEquals("pocket-tts-de:anna", PocketGermanVoiceCatalog.DEFAULT_VOICE_ID)
        assertEquals("Jürgen", PocketGermanVoiceCatalog.displayNameFor("juergen"))
    }
    @Test fun `German Mary is VCTK and never the unrelated English donor`() {
        assertEquals("CC BY 4.0", PocketGermanVoiceCatalog.creditFor("mary")!!.license)
        assertTrue(PocketVoiceCatalog.creditFor("mary")!!.donor)
    }
    @Test fun `all voices have explicit credits including both noncommercial voices`() {
        assertTrue(PocketGermanVoiceCatalog.KEYS.all { PocketGermanVoiceCatalog.creditFor(it) != null })
        assertEquals(setOf("cosette", "jean"), PocketGermanVoiceCatalog.KEYS.filter {
            PocketGermanVoiceCatalog.creditFor(it)?.license == "CC BY-NC 4.0"
        }.toSet())
    }
    @Test fun `German engine participates in the same registry and system TTS table`() {
        assertEquals(PocketGermanVoiceCatalog.ENGINE, EngineRegistry.engineNameFor(PocketGermanVoiceCatalog.DEFAULT_VOICE_ID))
        assertTrue(EngineRegistry.isSystemTtsEngine(PocketGermanVoiceCatalog.ENGINE))
        assertTrue(Expressiveness.appliesTo(PocketGermanVoiceCatalog.ENGINE))
    }
    @Test fun `new German voice needs its own files and does not inherit English availability`() {
        val anna = PocketGermanVoiceCatalog.voices.first()
        val installed = InstalledVoiceAssets(engines = setOf(PocketGermanVoiceCatalog.ENGINE), germanPocketVoices = setOf("anna"))
        assertTrue(isVoiceAvailable(anna, installed))
        assertFalse(isVoiceAvailable(anna, installed.copy(germanPocketVoices = emptySet())))
        assertFalse(isVoiceAvailable(anna, installed.copy(engines = setOf(PocketVoiceCatalog.ENGINE))))
    }
    @Test fun `German entry does not invent a performance or listening rating`() {
        val entry = EngineCatalog.byName(PocketGermanVoiceCatalog.ENGINE)!!
        assertFalse(entry.hasMeasuredRatings)
        assertNull(entry.quality)
        assertNull(entry.speedTier)
        assertFalse(entry.isRecommended)
        assertEquals(listOf("de"), entry.languageCodes)
        assertTrue(entry.archive.url.contains("madebycli/marmalade-tts-android-german/"))
    }
}
