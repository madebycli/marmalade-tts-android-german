package app.marmalade.tts.engine.pocket

import java.io.ByteArrayInputStream
import java.io.EOFException
import java.util.concurrent.CancellationException
import org.junit.Assert.*
import org.junit.Test

/** No ORT native runtime needed: loading must complete before tensors may be bound. */
class PocketPreparedStateTest {
    private fun slot(elements: Long = 2, initialize: Boolean = false) = PocketStateSlot(
        PocketBundle.StateDtype.FLOAT32, longArrayOf(elements), PocketBundle.StateFill.NAN,
        initialize = initialize,
    )

    @Test fun `ordinary engine state is immediately ready`() {
        assertTrue(slot(initialize = true).hasData)
    }

    @Test fun `prepared state becomes ready only after a complete payload`() {
        val state = slot()
        assertFalse(state.hasData)
        state.loadPreparedBytes(ByteArrayInputStream(ByteArray(8)), 8)
        assertTrue(state.hasData)
    }

    @Test fun `empty prepared state is valid without reading input`() {
        val state = slot(elements = 0)
        val input = ByteArrayInputStream(byteArrayOf(42))
        state.loadPreparedBytes(input, 0)
        assertTrue(state.hasData)
        assertEquals(42, input.read())
    }

    @Test fun `wrong byte count is rejected before consuming input`() {
        val state = slot()
        val input = ByteArrayInputStream(ByteArray(8))
        try { state.loadPreparedBytes(input, 4); fail() }
        catch (_: IllegalArgumentException) { }
        assertEquals(8, input.available())
        assertFalse(state.hasData)
    }

    @Test fun `truncated payload never becomes bindable`() {
        val state = slot()
        try { state.loadPreparedBytes(ByteArrayInputStream(ByteArray(7)), 8); fail() }
        catch (_: EOFException) { }
        assertFalse(state.hasData)
    }

    @Test fun `cancelled request reads no bytes`() {
        val state = slot()
        val input = ByteArrayInputStream(ByteArray(8))
        val stopped = CancellationException("Stopped")
        try { state.loadPreparedBytes(input, 8) { throw stopped }; fail() }
        catch (actual: CancellationException) { assertSame(stopped, actual) }
        assertEquals(8, input.available())
        assertFalse(state.hasData)
    }

    @Test fun `stop during large payload releases the loader between blocks and permits retry`() {
        val bytes = ByteArray(256 * 1024)
        val input = ByteArrayInputStream(bytes)
        val state = slot(elements = bytes.size.toLong() / 4)
        try {
            state.loadPreparedBytes(input, bytes.size) {
                if (input.available() < bytes.size) throw CancellationException("Stopped")
            }
            fail()
        } catch (_: CancellationException) { }
        assertTrue("Cancellation must leave unread data", input.available() > 0)
        assertTrue("At most one 64 KiB block before rechecking", bytes.size - input.available() <= 64 * 1024)
        assertFalse(state.hasData)
        state.loadPreparedBytes(ByteArrayInputStream(bytes), bytes.size)
        assertTrue(state.hasData)
    }

    @Test fun `failed overwrite invalidates a previously complete state`() {
        val state = slot()
        state.loadPreparedBytes(ByteArrayInputStream(ByteArray(8)), 8)
        try { state.loadPreparedBytes(ByteArrayInputStream(ByteArray(3)), 8); fail() }
        catch (_: EOFException) { }
        assertFalse(state.hasData)
    }

    @Test fun `state manifest keeps initialization opt in for prepared loads only`() {
        val manifest = listOf(PocketBundle.StateSpec(0, "state_0", "out_0",
            PocketBundle.StateDtype.FLOAT32, PocketBundle.StateFill.NAN, longArrayOf(2)))
        assertTrue(initStates(manifest).getValue("state_0").hasData)
        assertFalse(initStates(manifest, initialize = false).getValue("state_0").hasData)
    }
}
