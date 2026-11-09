package app.marmalade.tts.data

import app.marmalade.tts.data.db.VoiceMeta

/** All 27 official German voice states. See scripts/pocket-bundle/german-voices.json for sources. */
object PocketGermanVoiceCatalog {
    const val ENGINE = "pocket-tts-de"
    const val LANGUAGE = "de-DE"
    const val DEFAULT_KEY = "anna"
    const val DEFAULT_VOICE_ID = "$ENGINE:$DEFAULT_KEY"
    val KEYS = listOf("anna", "alba", "azelma", "bill_boerst", "caro_davy", "charles", "cosette", "daan", "eponine", "estelle", "eve", "fantine", "george", "giovanni", "jane", "javert", "jean", "juergen", "lola", "marius", "mary", "michael", "paul", "peter_yearsley", "rafael", "stuart_bell", "vera")
    private val female = setOf("anna", "azelma", "caro_davy", "cosette", "eponine", "estelle", "eve", "fantine", "jane", "lola", "mary", "vera")
    private val male = setOf("bill_boerst", "charles", "daan", "george", "giovanni", "javert", "jean", "juergen", "michael", "paul", "peter_yearsley", "rafael", "stuart_bell")
    val voices: List<VoiceMeta> = KEYS.map { key ->
        VoiceMeta(id = "$ENGINE:$key", engine = ENGINE, displayName = displayNameFor(key),
            languageCode = LANGUAGE, sampleRate = PocketVoiceCatalog.SAMPLE_RATE,
            gender = when (key) { in female -> "female"; in male -> "male"; else -> null },
            isInstalled = false, sortOrder = KEYS.indexOf(key))
    }

    fun displayNameFor(key: String): String = when (key) {
        "juergen" -> "Jürgen"
        else -> key.split('_').joinToString(" ") { it.replaceFirstChar(Char::uppercase) }
    }

    fun creditFor(key: String): VoiceCredit? = when (key) {
        "anna" -> VoiceCredit(source = "VCTK (University of Edinburgh)", license = "CC BY 4.0")
        "alba" -> VoiceCredit(source = "Alba MacKenna (Kyutai)", license = "CC BY 4.0")
        "azelma" -> VoiceCredit(source = "VCTK (University of Edinburgh)", license = "CC BY 4.0")
        "bill_boerst" -> VoiceCredit(source = "Voice-Zero / LibriVox", license = "CC0")
        "caro_davy" -> VoiceCredit(source = "Voice-Zero / LibriVox", license = "CC0")
        "charles" -> VoiceCredit(source = "VCTK (University of Edinburgh)", license = "CC BY 4.0")
        "cosette" -> VoiceCredit(source = "Expresso", license = "CC BY-NC 4.0")
        "daan" -> VoiceCredit(source = "Kyutai (official Pocket voice catalog)", license = "CC BY 4.0")
        "eponine" -> VoiceCredit(source = "VCTK (University of Edinburgh)", license = "CC BY 4.0")
        "estelle" -> VoiceCredit(source = "Kyutai Unmute", license = "CC0")
        "eve" -> VoiceCredit(source = "VCTK (University of Edinburgh)", license = "CC BY 4.0")
        "fantine" -> VoiceCredit(source = "VCTK (University of Edinburgh)", license = "CC BY 4.0")
        "george" -> VoiceCredit(source = "VCTK (University of Edinburgh)", license = "CC BY 4.0")
        "giovanni" -> VoiceCredit(source = "Kyutai (official Pocket voice catalog)", license = "CC BY 4.0")
        "jane" -> VoiceCredit(source = "VCTK (University of Edinburgh)", license = "CC BY 4.0")
        "javert" -> VoiceCredit(source = "Kyutai Unmute voice donation", license = "CC0")
        "jean" -> VoiceCredit(source = "EARS (University of Hamburg)", license = "CC BY-NC 4.0")
        "juergen" -> VoiceCredit(source = "Kyutai (official Pocket voice catalog)", license = "CC BY 4.0")
        "lola" -> VoiceCredit(source = "Kyutai (official Pocket voice catalog)", license = "CC BY 4.0")
        "marius" -> VoiceCredit(source = "Kyutai Unmute voice donation", license = "CC0")
        "mary" -> VoiceCredit(source = "VCTK (University of Edinburgh)", license = "CC BY 4.0")
        "michael" -> VoiceCredit(source = "VCTK (University of Edinburgh)", license = "CC BY 4.0")
        "paul" -> VoiceCredit(source = "VCTK (University of Edinburgh)", license = "CC BY 4.0")
        "peter_yearsley" -> VoiceCredit(source = "Voice-Zero / LibriVox", license = "CC0")
        "rafael" -> VoiceCredit(source = "Kyutai (official Pocket voice catalog)", license = "CC BY 4.0")
        "stuart_bell" -> VoiceCredit(source = "Voice-Zero / LibriVox", license = "CC0")
        "vera" -> VoiceCredit(source = "VCTK (University of Edinburgh)", license = "CC BY 4.0")
        else -> null
    }
}
