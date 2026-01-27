package ir.sysfail.chatguard.core.web_content_extractor.abstraction

import android.webkit.WebView
import ir.sysfail.chatguard.core.web_content_extractor.models.ElementData
import ir.sysfail.chatguard.core.web_content_extractor.models.ExtractedElementMessage
import ir.sysfail.chatguard.core.web_content_extractor.models.InjectedButton
import ir.sysfail.chatguard.core.web_content_extractor.models.ButtonClickData
import ir.sysfail.chatguard.core.web_content_extractor.models.ExtractedUserInfo
import ir.sysfail.chatguard.core.web_content_extractor.models.InfoMessage

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
    fun observeMessages(onMessagesChanged: (List<ElementData>) -> Unit)

    /**
     * Check if the current page is a chat page
     */
    suspend fun isChatPage(): Boolean

    /**
     * Observe background color changes of the current page
     */
    fun observeBackgroundColor(onColorChanged: (String) -> Unit)

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

    /**
     * Observe send button click events
     */
    fun observeSendAction(onSend: (message: String) -> Unit)

    /**
     * Remove send action observer
     */
    fun removeSendActionObserver()

    /**
     * Convert extracted elements to message models
     */
    fun mapElementsToMessages(elements: List<ElementData>): List<ExtractedElementMessage>

    /**
     * Inject a button into a specific message
     * @param messageId Message ID to find the message
     * @param button The button to inject
     * @return true if injection was successful
     */
    suspend fun injectButton(messageId: Long, button: InjectedButton): Boolean

    /**
     * Set up listener for button clicks
     */
    fun setButtonClickListener(listener: (ButtonClickData) -> Unit)

    /**
     * Remove button click listener
     */
    fun removeButtonClickListener()

    /**
     * Updates the text content of a specific message identified by its ID.
     */
    suspend fun updateMessageText(messageId: Long, newText: String): Boolean

    /**
     * Extracts user information from the web page using the configured CSS selectors.
     */
    suspend fun getUserInfo(): ExtractedUserInfo?

    /**
     * Inject an info into a specific element
     */
    suspend fun injectInfoMessage(message: InfoMessage): Boolean

    /**
     * Remove injected current info message
     */
    suspend fun removeInjectedInfoMessage(): Boolean

    /**
     * Show public key input button and observe user interaction
     */
    fun observeSendPublicKeyButton(onSend: () -> Unit)
}