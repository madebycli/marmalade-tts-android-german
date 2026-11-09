package app.marmalade.tts.engine

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import app.marmalade.tts.data.PocketGermanVoiceCatalog
import app.marmalade.tts.data.PocketVoiceCatalog
import app.marmalade.tts.data.SettingsRepository
import app.marmalade.tts.ui.screen.NoOpPreferencesDataStore
import java.io.DataOutputStream
import java.io.File
import java.util.zip.GZIPOutputStream
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PocketGermanEngineTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val german = File(context.filesDir, "engines/${PocketGermanVoiceCatalog.ENGINE}")
    private val english = File(context.filesDir, "engines/${PocketVoiceCatalog.ENGINE}")
    private fun fixture(dir: File) {
        dir.mkdirs()
        for (name in listOf("tokenizer.model", "bos_before_voice.npy", "text.onnx", "encoder.onnx", "decoder.onnx", "main.onnx", "flow.onnx"))
            File(dir, name).writeText("fixture, not a neural model")
        File(dir, "bundle.json").writeText("""{"sample_rate":24000,"frame_rate":12.5,"samples_per_frame":1920,
            "latent_dim":32,"conditioning_dim":1024,"max_token_per_chunk":50,"model_recommended_frames_after_eos":null,
            "predefined_voices":["anna"],"tokenizer_normalization":"identity",
            "onnx_files":{"text_conditioner":"text.onnx","mimi_encoder":"encoder.onnx","mimi_decoder":"decoder.onnx",
            "flow_lm_main":"main.onnx","flow_lm_flow":"flow.onnx"},"flow_lm_state_manifest":[],"mimi_state_manifest":[]}""")
    }
    private fun prepared(key: String) {
        val file = File(german, "voice_states/$key.prompt.gz"); file.parentFile!!.mkdirs()
        DataOutputStream(GZIPOutputStream(file.outputStream())).use {
            it.writeInt(0x50505331); it.writeInt(1); it.writeInt(0)
        }
    }
    @Before fun setUp() { german.deleteRecursively(); english.deleteRecursively() }
    @After fun tearDown() { german.deleteRecursively(); english.deleteRecursively() }
    private fun engine() = PocketGermanEngine(context, SettingsRepository(NoOpPreferencesDataStore))
    @Test fun `an English installation cannot make German appear installed`() {
        fixture(english); assertFalse(engine().isInstalled())
    }
    @Test fun `German requires its default official state and all five graphs`() {
        fixture(german); assertFalse(engine().isInstalled())
        prepared("anna"); assertTrue(engine().isInstalled())
        File(german, "main.onnx").delete(); assertFalse(engine().isInstalled())
    }
    @Test fun `German default and missing voice fallback never use English Alba`() {
        fixture(german); prepared("anna")
        assertEquals("anna", engine().voiceToRender("pocket-tts-de:missing"))
    }
    @Test fun `only German voices with their own state are offered`() {
        fixture(german); prepared("anna"); prepared("vera")
        assertEquals(setOf("anna", "vera"), engine().installedVoiceKeys())
    }
    @Test fun `construction and availability probes never load the model`() {
        fixture(german); prepared("anna"); val engine = engine()
        engine.isInstalled(); engine.installedVoiceKeys(); assertFalse(engine.isLoaded())
    }
    @Test fun `noncloning checkpoint rejects cloning before loading or writing`() = runTest {
        val engine = engine()
        try { engine.cloneVoice("Test", floatArrayOf(.1f), 24000); fail("Cloning must not run") }
        catch (_: IllegalStateException) { }
        assertFalse(engine.isLoaded())
        assertFalse(File(german, "cloned_voices").exists())
    }
}
