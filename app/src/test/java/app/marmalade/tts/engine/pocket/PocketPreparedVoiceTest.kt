package app.marmalade.tts.engine.pocket

import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.File
import java.util.zip.GZIPOutputStream
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class PocketPreparedVoiceTest {
    @get:Rule val tmp = TemporaryFolder()
    private val spec = PocketBundle.StateSpec(0, "state_0", "out_state_0",
        PocketBundle.StateDtype.FLOAT32, PocketBundle.StateFill.NAN, longArrayOf(1, 2))
    private val manifest = listOf(spec)
    private fun voice(magic: Int = 0x50505331, rows: Int = 2, slots: Int = 1,
                      dtype: Int = 0, size: Int = 8, bytes: ByteArray = byteArrayOf(0,0,128.toByte(),63,0,0,0,64),
                      trailing: Boolean = false): File {
        val file = tmp.newFile()
        DataOutputStream(GZIPOutputStream(file.outputStream())).use { out ->
            out.writeInt(magic); out.writeInt(rows); out.writeInt(slots)
            out.writeInt(0); out.writeInt(dtype); out.writeInt(2)
            out.writeLong(1); out.writeLong(2); out.writeInt(size); out.write(bytes)
            if (trailing) out.write(42)
        }
        return file
    }
    private fun rejects(file: File) {
        try { PocketPreparedVoice.validate(file, manifest); fail("Invalid state was accepted") }
        catch (_: IllegalArgumentException) { }
        catch (_: java.io.EOFException) { }
    }
    @Test fun `official prompt bytes and row count are preserved`() {
        val file = voice(); val result = ByteArrayOutputStream()
        PocketPreparedVoice.read(file, manifest) { entry, input, count ->
            assertEquals(spec, entry); result.write(ByteArray(count).also(input::readFully))
        }
        assertEquals(2, PocketPreparedVoice.rows(file))
        assertArrayEquals(byteArrayOf(0,0,128.toByte(),63,0,0,0,64), result.toByteArray())
    }
    @Test fun `valid state passes complete and lightweight checks`() {
        val file = voice(); PocketPreparedVoice.validate(file, manifest); PocketPreparedVoice.validateHeader(file, manifest)
    }
    @Test fun `foreign header is rejected`() = rejects(voice(magic = 1))
    @Test fun `zero or overflowing prompt length is rejected`() { rejects(voice(rows = 0)); rejects(voice(rows = 1000)) }
    @Test fun `different transformer depth is rejected`() = rejects(voice(slots = 72))
    @Test fun `different dtype is rejected`() = rejects(voice(dtype = 1))
    @Test fun `incorrect tensor length is rejected`() = rejects(voice(size = 4))
    @Test fun `truncated data is rejected`() = rejects(voice(bytes = byteArrayOf(1,2)))
    @Test fun `trailing data cannot silently become another state`() = rejects(voice(trailing = true))
    @Test fun `different cache shape is rejected`() {
        val file = voice()
        try { PocketPreparedVoice.validate(file, listOf(spec.copy(shape = longArrayOf(2, 1)))); fail() }
        catch (_: IllegalArgumentException) { }
    }
}
