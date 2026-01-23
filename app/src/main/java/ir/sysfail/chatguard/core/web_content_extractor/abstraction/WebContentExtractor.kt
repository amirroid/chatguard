package ir.sysfail.chatguard.core.web_content_extractor.abstraction

import android.webkit.WebView
import ir.sysfail.chatguard.core.web_content_extractor.models.ElementData

/**
 * Main interface for content extraction from web views
 */
interface WebContentExtractor {
    /**
     * Extract text content from messages
     */
    suspend fun extractMessages(): List<String>

    /**
     * Extract HTML content from messages
     */
    suspend fun extractHTML(): List<String>

    /**
     * Extract detailed information including text, HTML, and attributes
     */
    suspend fun extractDetailedContent(): List<ElementData>

    /**
     * Execute custom JavaScript and return the result
     */
    suspend fun executeCustomScript(script: String): String

    /**
     * Set up real-time observation of message changes
     */
    fun observeMessages(onMessagesChanged: (List<String>) -> Unit)

    /**
     * Check if the current page is a chat page
     */
    suspend fun isChatPage(): Boolean

    /**
     * Send a message through the chat interface
     * @param message The message text to send
     * @param transformer Optional function to transform message before sending
     * @return true if message was sent successfully
     */
    suspend fun sendMessage(message: String, transformer: ((String) -> String)? = null): Boolean

    /**
     * Attach this extractor to a WebView instance
     */
    fun attachToWebView(webView: WebView)

    /**
     * Detach from current WebView
     */
    fun detachFromWebView()

    /**
     * Clean up resources and callbacks
     */
    fun cleanup()

    /**
     * Set an error listener
     */
    fun setErrorListener(listener: (callbackId: String, error: String) -> Unit)
}