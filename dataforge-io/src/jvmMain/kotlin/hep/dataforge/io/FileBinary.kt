package hep.dataforge.io

import kotlinx.io.core.ByteReadPacket
import kotlinx.io.core.Input
import java.nio.channels.FileChannel
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import kotlin.math.min

@ExperimentalUnsignedTypes
class FileBinary(val path: Path, private val offset: UInt = 0u, size: ULong? = null) : RandomAccessBinary {

    override val size: ULong = size ?: (Files.size(path).toULong() - offset).toULong()

    override fun <R> read(from: UInt, size: UInt, block: Input.() -> R): R {
        FileChannel.open(path, StandardOpenOption.READ).use {
            val theSize: UInt = min(size, Files.size(path).toUInt() - offset)
            val buffer = it.map(FileChannel.MapMode.READ_ONLY, (from + offset).toLong(), theSize.toLong())
            return ByteReadPacket(buffer).block()
        }
    }
}