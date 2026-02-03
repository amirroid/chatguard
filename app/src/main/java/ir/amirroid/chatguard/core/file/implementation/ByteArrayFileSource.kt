package ir.amirroid.chatguard.core.file.implementation

import ir.amirroid.chatguard.core.file.abstraction.FileSource
import java.io.InputStream

class ByteArrayFileSource(private val data: ByteArray) : FileSource {

    override fun open(): InputStream = data.inputStream()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ByteArrayFileSource) return false
        return data.contentEquals(other.data)
    }

    override fun hashCode(): Int {
        return data.contentHashCode()
    }

    override fun toString(): String {
        return "ByteArrayFileSource(size=${data.size})"
    }
}