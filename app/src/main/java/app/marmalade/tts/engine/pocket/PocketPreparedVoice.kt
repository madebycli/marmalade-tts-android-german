package app.marmalade.tts.engine.pocket

import java.io.BufferedInputStream
import java.io.DataInputStream
import java.io.File
import java.util.zip.GZIPInputStream

/** Official voice-conditioned KV states, adapted offline to the five-graph manifest.
 * PPS1 stores a big-endian header and little-endian tensor payloads in gzip.
 * Padding is compressed on disk; only the current voice is retained in StateFrame.
 * This is data for the existing ORT graph inputs, never a second inference engine.
 */
internal object PocketPreparedVoice {
    private const val MAGIC = 0x50505331 // PPS1
    private const val IO_BUFFER_BYTES = 64 * 1024
    private fun open(file: File) = DataInputStream(BufferedInputStream(
        GZIPInputStream(file.inputStream(), IO_BUFFER_BYTES), IO_BUFFER_BYTES,
    ))
    fun rows(file: File): Int = open(file).use { input ->
        require(input.readInt() == MAGIC) { "Invalid Pocket voice header" }
        input.readInt().also { require(it in 1..999) { "Invalid Pocket prompt length" } }
    }
    fun validateHeader(file: File, manifest: List<PocketBundle.StateSpec>) {
        open(file).use { input ->
            require(input.readInt() == MAGIC)
            require(input.readInt() in 1..999)
            require(input.readInt() == manifest.size)
        }
    }
    fun validate(file: File, manifest: List<PocketBundle.StateSpec>) {
        read(file, manifest) { _, input, count ->
            var left = count.toLong()
            while (left > 0) {
                val skipped = input.skip(left)
                if (skipped > 0) left -= skipped else {
                    require(input.read() >= 0) { "Truncated Pocket voice state" }; left--
                }
            }
        }
    }
    fun read(
        file: File,
        manifest: List<PocketBundle.StateSpec>,
        checkActive: () -> Unit = {},
        consume: (PocketBundle.StateSpec, DataInputStream, Int) -> Unit,
    ) {
        checkActive()
        open(file).use { input ->
            require(input.readInt() == MAGIC) { "Invalid Pocket voice header" }
            require(input.readInt() in 1..999) { "Invalid Pocket prompt length" }
            require(input.readInt() == manifest.size) { "Pocket voice state count mismatch" }
            var total = 0L
            for (spec in manifest) {
                checkActive()
                require(input.readInt() == spec.index) { "Pocket voice state order mismatch" }
                require(input.readInt() == spec.dtype.ordinal) { "Pocket voice dtype mismatch" }
                require(input.readInt() == spec.shape.size) { "Pocket voice rank mismatch" }
                for (dim in spec.shape) require(input.readLong() == dim) { "Pocket voice shape mismatch" }
                val width = when (spec.dtype) {
                    PocketBundle.StateDtype.FLOAT32 -> 4
                    PocketBundle.StateDtype.INT64 -> 8
                    PocketBundle.StateDtype.BOOL -> 1
                }
                val bytes = spec.shape.fold(width.toLong()) { n, dim ->
                    require(dim in 0..1_000_000); Math.multiplyExact(n, dim)
                }
                require(bytes in 0..64L * 1024 * 1024)
                total += bytes
                require(total <= 256L * 1024 * 1024)
                require(input.readInt().toLong() == bytes) { "Pocket voice byte count mismatch" }
                consume(spec, input, bytes.toInt())
            }
            checkActive()
            require(input.read() == -1) { "Trailing Pocket voice data" }
        }
    }
}
