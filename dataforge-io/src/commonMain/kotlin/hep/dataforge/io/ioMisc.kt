package hep.dataforge.io

import kotlinx.io.*
import kotlin.math.min

public fun Output.writeRawString(str: String) {
    str.forEach { writeByte(it.toByte()) }
}

public fun Input.readRawString(size: Int): String {
    val array = CharArray(size) { readByte().toChar() }
    return array.concatToString()
}

public inline fun buildByteArray(expectedSize: Int = 16, block: Output.() -> Unit): ByteArray =
    ByteArrayOutput(expectedSize).apply(block).toByteArray()

@Suppress("FunctionName")
public inline fun Binary(expectedSize: Int = 16, block: Output.() -> Unit): Binary =
    buildByteArray(expectedSize, block).asBinary()

/**
 * View section of a [Binary] as an independent binary
 */
public class BinaryView(private val source: Binary, private val start: Int, override val size: Int) : Binary {

    init {
        require(start > 0)
        require(start + size <= source.size) { "View boundary is outside source binary size" }
    }

    override fun <R> read(offset: Int, atMost: Int, block: Input.() -> R): R {
        return source.read(start + offset, min(size, atMost), block)
    }
}

public fun Binary.view(start: Int, size: Int): BinaryView = BinaryView(this, start, size)

public operator fun Binary.get(range: IntRange): BinaryView = view(range.first, range.last - range.first)