package app.marmalade.tts.engine

import android.content.Context
import app.marmalade.tts.data.PocketGermanVoiceCatalog
import app.marmalade.tts.data.SettingsRepository
import app.marmalade.tts.engine.pocket.PocketGermanText
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** German checkpoint through the production Pocket CPU/ORT streaming path. */
@Singleton
class PocketGermanEngine @Inject constructor(
    @ApplicationContext context: Context,
    settings: SettingsRepository,
) : PocketEngine(context, settings) {
    override val engineName: String = PocketGermanVoiceCatalog.ENGINE
    override val defaultVoiceKey: String get() = PocketGermanVoiceCatalog.DEFAULT_KEY
    override val offeredVoiceKeys: List<String> get() = PocketGermanVoiceCatalog.KEYS
    override val supportsVoiceCloning: Boolean get() = false
    override val warmupText: String get() = "Hallo."
    override fun prepareLanguageText(raw: String): String = PocketGermanText.normalize(raw)
}
