package ir.sysfail.chatguard.core.file.abstraction

import java.io.InputStream

/**
 * Abstraction over a file-like source that can provide an InputStream.
 * Implementations can read from assets, raw resources, disk, network, or in-memory data.
 */
interface FileSource {
    /**
     * Open a new InputStream to read the file content.
     * Each call should return a fresh InputStream.
     */
    fun open(): InputStream
}