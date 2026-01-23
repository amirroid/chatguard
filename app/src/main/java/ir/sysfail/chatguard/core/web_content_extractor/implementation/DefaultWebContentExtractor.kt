package ir.sysfail.chatguard.core.web_content_extractor.implementation

import android.webkit.JavascriptInterface
import android.webkit.WebView
import ir.sysfail.chatguard.core.web_content_extractor.abstraction.WebContentExtractor
import ir.sysfail.chatguard.core.web_content_extractor.abstraction.PlatformExtractionStrategy
import ir.sysfail.chatguard.core.web_content_extractor.models.ElementData
import ir.sysfail.chatguard.core.web_content_extractor.models.ProcessingConfig
import ir.sysfail.chatguard.core.web_content_extractor.models.SelectorConfig
import kotlinx.coroutines.suspendCancellableCoroutine
import org.json.JSONArray
import org.json.JSONObject
import kotlin.coroutines.resume

class DefaultWebContentExtractor(
    private val strategy: PlatformExtractionStrategy
) : WebContentExtractor {

    companion object {
        private const val BRIDGE_NAME = "ContentExtractor"
        private const val VISIBILITY_THRESHOLD_PX = 200
    }

    private var webView: WebView? = null
    private val callbacks = mutableMapOf<String, (String) -> Unit>()
    private val observers = mutableMapOf<String, Boolean>()
    private var isInitialized = false
    private var globalErrorListener: ((String, String) -> Unit)? = null

    override fun attachToWebView(webView: WebView) {
        if (this.webView == webView && isInitialized) return

        this.webView = webView
        setupJavaScriptBridge(webView)
        isInitialized = true

        strategy.getPreExtractionScript()?.let { script ->
            webView.post {
                webView.evaluateJavascript("(function() { $script })();", null)
            }
        }
    }

    override fun detachFromWebView() {
        webView = null
        isInitialized = false
        callbacks.clear()
        observers.clear()
    }

    private fun setupJavaScriptBridge(webView: WebView) {
        webView.addJavascriptInterface(object {
            @JavascriptInterface
            fun onContentExtracted(callbackId: String, content: String) {
                callbacks[callbackId]?.invoke(content)

                if (!observers.containsKey(callbackId)) {
                    callbacks.remove(callbackId)
                }
            }

            @JavascriptInterface
            fun onError(callbackId: String, errorMessage: String) {
                globalErrorListener?.invoke(callbackId, errorMessage)

                if (!observers.containsKey(callbackId)) {
                    callbacks.remove(callbackId)
                }
            }
        }, BRIDGE_NAME)
    }

    override fun setErrorListener(listener: (callbackId: String, error: String) -> Unit) {
        globalErrorListener = listener
    }

    override suspend fun extractMessages(): List<String> =
        executeExtraction { config, processing, callbackId ->
            executeAndProcess(buildExtractionScript(config, callbackId), callbackId) { json ->
                parseMessages(json, config, processing)
            }
        }

    override suspend fun extractHTML(): List<String> =
        executeExtraction { config, _, callbackId ->
            executeAndProcess(buildExtractionScript(config, callbackId), callbackId) { json ->
                parseSimpleArray(json)
            }
        }

    override suspend fun extractDetailedContent(): List<ElementData> =
        executeExtraction { config, processing, callbackId ->
            executeAndProcess(buildDetailedScript(config, callbackId), callbackId) { json ->
                parseDetailedData(json, config, processing)
            }
        }

    override suspend fun executeCustomScript(script: String): String =
        suspendCancellableCoroutine { continuation ->
            val webView = webView ?: run {
                continuation.resume("")
                return@suspendCancellableCoroutine
            }

            val callbackId = generateCallbackId()
            callbacks[callbackId] = { continuation.resume(it) }

            val wrapped = """
                (function() {
                    try {
                        var result = (function() { $script })();
                        $BRIDGE_NAME.onContentExtracted('$callbackId', JSON.stringify(result || ''));
                    } catch(e) {
                        $BRIDGE_NAME.onError('$callbackId', e.message || 'Script execution failed');
                    }
                })();
            """.trimIndent()

            webView.post { webView.evaluateJavascript(wrapped, null) }
            continuation.invokeOnCancellation {
                callbacks.remove(callbackId)
                observers.remove(callbackId)
            }
        }

    override fun observeMessages(onMessagesChanged: (List<String>) -> Unit) {
        val webView = webView ?: return
        val config = strategy.getSelectorConfig()
        val processing = strategy.getProcessingConfig()
        val callbackId = generateCallbackId()

        observers[callbackId] = true

        callbacks[callbackId] = { json ->
            val messages = parseMessages(JSONArray(json), config, processing)
            onMessagesChanged(messages)
        }

        val script = buildObserverScript(config, callbackId)
        webView.post { webView.evaluateJavascript(script, null) }
    }

    override suspend fun isChatPage(): Boolean =
        executeCustomScript(strategy.getIsChatPageScript())
            .let { it.trim() == "true" }

    override suspend fun sendMessage(message: String, transformer: ((String) -> String)?): Boolean {
        val finalMessage = transformer?.invoke(message) ?: message
        return executeCustomScript(strategy.getSendMessageScript(finalMessage))
            .let { it.trim() == "true" }
    }

    override fun cleanup() {
        observers.keys.forEach { callbackId ->
            webView?.post {
                webView?.evaluateJavascript(
                    "if (window._chatguard_observers && window._chatguard_observers['$callbackId']) { window._chatguard_observers['$callbackId'].disconnect(); delete window._chatguard_observers['$callbackId']; }",
                    null
                )
            }
        }

        callbacks.clear()
        observers.clear()
        globalErrorListener = null
        webView = null
        isInitialized = false
    }

    private suspend fun <T> executeExtraction(
        block: suspend (SelectorConfig, ProcessingConfig, String) -> T
    ): T {
        if (webView == null) return block(
            strategy.getSelectorConfig(),
            strategy.getProcessingConfig(),
            ""
        )

        return block(
            strategy.getSelectorConfig(),
            strategy.getProcessingConfig(),
            generateCallbackId()
        )
    }

    private suspend fun <T> executeAndProcess(
        script: String,
        callbackId: String,
        processor: (JSONArray) -> T
    ): T = suspendCancellableCoroutine { continuation ->
        val webView = webView ?: run {
            continuation.resume(processor(JSONArray()))
            return@suspendCancellableCoroutine
        }

        callbacks[callbackId] = { json ->
            try {
                continuation.resume(processor(JSONArray(json)))
            } catch (e: Exception) {
                e.printStackTrace()
                continuation.resume(processor(JSONArray()))
            }
        }

        webView.post { webView.evaluateJavascript(script, null) }
        continuation.invokeOnCancellation {
            callbacks.remove(callbackId)
            observers.remove(callbackId)
        }
    }

    private fun parseMessages(
        json: JSONArray,
        config: SelectorConfig,
        processing: ProcessingConfig
    ): List<String> {
        val result = mutableListOf<String>()
        for (i in 0 until json.length()) {
            val obj = json.getJSONObject(i)
            val text = obj.getString("text")
            val html = obj.getString("html")
            val attrs = extractAttributes(obj, config)

            if (strategy.validateElement(text, html, attrs)) {
                processText(text, processing).takeIf { it.isNotEmpty() }?.let(result::add)
            }
        }
        return result
    }

    private fun parseSimpleArray(json: JSONArray): List<String> {
        return (0 until json.length()).mapNotNull { i ->
            json.getJSONObject(i).getString("html").takeIf { it.isNotBlank() }
        }
    }

    private fun parseDetailedData(
        json: JSONArray,
        config: SelectorConfig,
        processing: ProcessingConfig
    ): List<ElementData> {
        val result = mutableListOf<ElementData>()
        for (i in 0 until json.length()) {
            val obj = json.getJSONObject(i)
            val text = obj.getString("text")
            val html = obj.getString("html")
            val attrs = extractAttributes(obj, config)

            if (strategy.validateElement(text, html, attrs)) {
                val processed = processText(text, processing)
                if (processed.isNotEmpty()) {
                    val data = ElementData(
                        text = processed,
                        html = html,
                        className = obj.optString("className", ""),
                        id = obj.optString("id", ""),
                        dir = obj.optString("dir", ""),
                        customAttributes = attrs
                    )
                    result.add(strategy.transformData(data))
                }
            }
        }
        return result
    }

    private fun extractAttributes(obj: JSONObject, config: SelectorConfig) =
        config.attributesToExtract.associateWith { obj.optString(it, "") }

    private fun processText(text: String, config: ProcessingConfig): String {
        var result = text
        if (config.trimWhitespace) result = result.trim()
        if (config.normalizeSpaces) result = result.replace(Regex("\\s+"), " ")
        if (config.removeEmptyLines) {
            result = result.lines().filter { it.isNotBlank() }.joinToString("\n")
        }
        if (config.removeUrls) result = result.replace(Regex("https?://\\S+"), "")
        config.customProcessor?.let { result = it(result) }
        return result
    }

    private fun generateCallbackId() = "callback_${System.currentTimeMillis()}"

    private fun buildExtractionScript(config: SelectorConfig, callbackId: String) =
        buildBaseScript(config, callbackId, includeDetailedAttrs = false)

    private fun buildDetailedScript(config: SelectorConfig, callbackId: String) =
        buildBaseScript(config, callbackId, includeDetailedAttrs = true)

    private fun buildBaseScript(
        config: SelectorConfig,
        callbackId: String,
        includeDetailedAttrs: Boolean
    ) = """
        (function() {
            try {
                ${generateExtractionLogic(config, callbackId, includeDetailedAttrs)}
            } catch(e) {
                $BRIDGE_NAME.onError('$callbackId', e.message || 'Unknown error');
            }
        })();
    """.trimIndent()

    private fun buildObserverScript(config: SelectorConfig, callbackId: String) = """
        (function() {
            var targets = document.querySelectorAll('${config.messageSelector}');
            if (targets.length === 0) return;
            
            if (!window._chatguard_observers) {
                window._chatguard_observers = {};
            }
            
            var callback = function() {
                try {
                    ${generateExtractionLogic(config, callbackId, includeDetailedAttrs = false, skipVisibilityCheck = true)}
                } catch(e) {
                    $BRIDGE_NAME.onError('$callbackId', e.message || 'Observer error');
                }
            };
            
            var observer = new MutationObserver(callback);
            window._chatguard_observers['$callbackId'] = observer;
            
            targets.forEach(function(node) {
                observer.observe(node, { 
                    childList: true, 
                    subtree: true, 
                    characterData: true 
                });
            });
            
            callback();
        })();
    """.trimIndent()

    private fun generateExtractionLogic(
        config: SelectorConfig,
        callbackId: String,
        includeDetailedAttrs: Boolean,
        skipVisibilityCheck: Boolean = false
    ): String {
        val detailedAttrs = if (includeDetailedAttrs) {
            """
                    className: el.className || '',
                    id: el.id || '',
                    dir: el.getAttribute('dir') || '',"""
        } else {
            ""
        }

        val visibilityCheck = if (skipVisibilityCheck) {
            "true"
        } else {
            "isElementVisible(el)"
        }

        return """
            var elements = document.querySelectorAll('${config.messageSelector}');
            var contents = [];
            var ignoreSelectors = [${config.ignoreSelectors.joinToString { "'$it'" }}];
            var attributes = [${config.attributesToExtract.joinToString { "'$it'" }}];
            
            function isElementVisible(el) {
                var rect = el.getBoundingClientRect();
                var windowHeight = window.innerHeight || document.documentElement.clientHeight;
                var threshold = $VISIBILITY_THRESHOLD_PX;
                
                return (
                    rect.top < windowHeight + threshold &&
                    rect.bottom > -threshold
                );
            }
            
            function extractContent(el) {
                var clone = el.cloneNode(true);
                ignoreSelectors.forEach(function(sel) {
                    clone.querySelectorAll(sel).forEach(function(m) { m.remove(); });
                });
                
                var text = (clone.textContent || clone.innerText || '').trim();
                var html = clone.innerHTML || '';
                
                if (text || html) {
                    var item = {
                        text: text,
                        html: html,$detailedAttrs
                    };
                    
                    attributes.forEach(function(attr) {
                        item[attr] = el.getAttribute(attr) || '';
                    });
                    
                    return item;
                }
                return null;
            }
            
            elements.forEach(function(el) {
                if ($visibilityCheck) {
                    var item = extractContent(el);
                    if (item) {
                        contents.push(item);
                    }
                }
            });
            
            $BRIDGE_NAME.onContentExtracted('$callbackId', JSON.stringify(contents));
        """.trimIndent()
    }
}