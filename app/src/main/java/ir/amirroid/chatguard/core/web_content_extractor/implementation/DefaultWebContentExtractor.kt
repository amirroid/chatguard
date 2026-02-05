package ir.amirroid.chatguard.core.web_content_extractor.implementation

import android.webkit.JavascriptInterface
import android.webkit.WebView
import ir.amirroid.chatguard.core.web_content_extractor.abstraction.WebContentExtractor
import ir.amirroid.chatguard.core.web_content_extractor.abstraction.PlatformExtractionStrategy
import ir.amirroid.chatguard.core.web_content_extractor.models.*
import kotlinx.coroutines.suspendCancellableCoroutine
import org.json.JSONArray
import org.json.JSONObject
import kotlin.coroutines.resume

class DefaultWebContentExtractor(
    private val strategy: PlatformExtractionStrategy
) : WebContentExtractor {

    companion object {
        private const val BRIDGE_NAME = "ContentExtractor"
        private const val BUTTON_BRIDGE_NAME = "ButtonBridge"
        private const val VISIBILITY_THRESHOLD_PX = 200
        private const val BUTTON_CLASS = "chatguard-button"
        val ignoreSelectors = listOf(BUTTON_CLASS)
    }

    private var webView: WebView? = null
    private val callbacks = mutableMapOf<String, (String) -> Unit>()
    private val observers = mutableMapOf<String, Boolean>()
    private var isInitialized = false
    private var globalErrorListener: ((String, String) -> Unit)? = null
    private var currentSendActionObserverCallbackId: String? = null
    private var buttonClickListener: ((ButtonClickData) -> Unit)? = null
    private var currentMessagesObserverCallbackId: String? = null

    override fun attachToWebView(webView: WebView) {
        if (this.webView == webView && isInitialized) return

        this.webView = webView
        setupJavaScriptBridge(webView)
        setupButtonBridge(webView)
        isInitialized = true
    }

    override fun detachFromWebView() {
        webView = null
        isInitialized = false
        callbacks.clear()
        observers.clear()
        buttonClickListener = null
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

    private fun setupButtonBridge(webView: WebView) {
        webView.addJavascriptInterface(object {
            @JavascriptInterface
            fun onButtonClick(buttonType: String, messageId: String) {
                val type = ButtonType.valueOf(buttonType)

                val data = ButtonClickData(
                    buttonType = type,
                    messageId = messageId,
                )

                buttonClickListener?.invoke(data)
            }
        }, BUTTON_BRIDGE_NAME)
    }

    override fun setErrorListener(listener: (callbackId: String, error: String) -> Unit) {
        globalErrorListener = listener
    }

    override fun setButtonClickListener(listener: (ButtonClickData) -> Unit) {
        buttonClickListener = listener
    }

    override fun removeButtonClickListener() {
        buttonClickListener = null
    }

    override fun observeSendAction(onSend: (message: String) -> Unit) {
        val webView = webView ?: return

        executeExtraction { config, _, callbackId ->
            currentSendActionObserverCallbackId = callbackId

            observers[callbackId] = true
            callbacks[callbackId] = onSend

            val script = buildObserveSendActionScript(config, callbackId)
            webView.post { webView.evaluateJavascript(script, null) }
        }
    }

    override fun removeSendActionObserver() {
        val callbackId = currentSendActionObserverCallbackId ?: return
        val webView = webView ?: return

        executeExtraction { config, _, _ ->
            val script = buildRemoveSendActionObserverScript(config)
            webView.post { webView.evaluateJavascript(script, null) }
        }

        callbacks.remove(callbackId)
        observers.remove(callbackId)
        currentSendActionObserverCallbackId = null
    }

    override fun mapElementsToMessages(
        currentUrl: String,
        elements: List<ElementData>
    ): List<ExtractedElementMessage> {
        val processing = strategy.getProcessingConfig()

        return elements.mapNotNull { element ->
            if (processing.checkIsOwnMessage == null || processing.findMessageId == null) return@mapNotNull null

            ExtractedElementMessage(
                message = element.text,
                id = processing.findMessageId(element) ?: return@mapNotNull null,
                isMyMessage = processing.checkIsOwnMessage(currentUrl, element)
            )
        }
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
                    var result = (function() { 
                      $script 
                    })();
                
                    if (result && typeof result.then === 'function') {
                      // Promise
                      result.then(function(value) {
                        $BRIDGE_NAME.onContentExtracted(
                          '$callbackId',
                          JSON.stringify(value)
                        );
                      }).catch(function(e) {
                        $BRIDGE_NAME.onError('$callbackId', e?.message || 'Promise failed');
                      });
                    } else {
                      $BRIDGE_NAME.onContentExtracted(
                        '$callbackId',
                        JSON.stringify(result)
                      );
                    }
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

    override fun observeMessages(onMessagesChanged: (List<ElementData>) -> Unit) {
        val webView = webView ?: return

        executeExtraction { config, processing, callbackId ->
            observers[callbackId] = true
            currentMessagesObserverCallbackId = callbackId

            callbacks[callbackId] = { json ->
                val messages = parseDetailedData(JSONArray(json), config, processing)
                onMessagesChanged(messages)
            }

            val script = buildObserverScript(config, callbackId)
            webView.post { webView.evaluateJavascript(script, null) }
        }
    }

    override fun removeMessagesObserver() {
        val callbackId = currentMessagesObserverCallbackId ?: return
        val webView = webView ?: return
        callbacks.remove(callbackId)
        observers.remove(callbackId)
        currentMessagesObserverCallbackId = null
        val script = """
            (function() {
                if (window._chatguard_observers && window._chatguard_observers['$callbackId']) {
                    var obs = window._chatguard_observers['$callbackId'];
                    if (obs && obs.disconnect) {
                        obs.disconnect();
                    }
                    delete window._chatguard_observers['$callbackId'];
                }
            })();
        """.trimIndent()
        webView.post {
            webView.evaluateJavascript(script, null)
        }
    }

    override suspend fun isChatPage(): Boolean =
        executeCustomScript(buildIsChatPageScript(strategy.getSelectorConfig()))
            .let { it.trim() == "true" }

    override fun observeBackgroundColor(onColorChanged: (String) -> Unit) {
        val webView = webView ?: return

        executeExtraction { config, _, callbackId ->
            observers[callbackId] = true

            callbacks[callbackId] = { color ->
                onColorChanged(color)
            }

            val script = buildObserveBackgroundColorScript(config, callbackId)
            webView.post { webView.evaluateJavascript(script, null) }
        }
    }

    override suspend fun sendMessage(message: String, transformer: ((String) -> String)?): Boolean {
        val finalMessage = transformer?.invoke(message) ?: message
        return executeCustomScript(
            buildSendMessageScript(
                finalMessage,
                strategy.getSelectorConfig()
            )
        ).let { it.trim() == "true" }
    }

    override suspend fun injectButton(messageId: String, button: InjectedButton): Boolean {
        val config = strategy.getSelectorConfig()
        val injectionConfig = config.buttonInjectionConfig ?: return false

        val script = buildInjectButtonScript(messageId, button, config, injectionConfig)
        return executeCustomScript(script).trim() == "true"
    }

    override suspend fun removeAllInjectedButtons() {
        executeCustomScript(buildRemoveAllButtonsScript())
    }

    override suspend fun injectInfoMessage(message: InfoMessage): Boolean {
        removeInjectedInfoMessage()

        val config = strategy.getSelectorConfig()
        val infoConfig = config.infoMessageConfig ?: return false

        val script = buildInjectInfoMessageScript(message, infoConfig)
        return executeCustomScript(script).trim() == "true"
    }

    override suspend fun removeInjectedInfoMessage(): Boolean {
        val script = buildRemoveInfoMessageScript()
        return executeCustomScript(script).trim() == "true"
    }

    override fun observeSendPublicKeyButton(onSend: () -> Unit) {
        val webView = webView ?: return

        executeExtraction { config, _, callbackId ->
            observers[callbackId] = true
            callbacks[callbackId] = {
                onSend.invoke()
            }

            val script = buildShowPublicKeyButtonScript(config, callbackId)
            webView.post { webView.evaluateJavascript(script, null) }
        }
    }

    override suspend fun executeInitialScript() {
        strategy.getInitialExecutionScript()?.let {
            executeCustomScript(it)
        }
    }

    override suspend fun clearAllFlags() {
        executeCustomScript(buildClearAllFlagsScript())
    }

    override fun cleanup() {
        webView?.post {
            webView?.evaluateJavascript(
                buildClearAllFlagsScript(),
                null
            )
        }

        callbacks.clear()
        observers.clear()
        globalErrorListener = null
        buttonClickListener = null
        webView = null
        isInitialized = false
    }

    private inline fun <T> executeExtraction(
        block: (SelectorConfig, ProcessingConfig, String) -> T
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

    private suspend fun <T> executeAndProcessSingleObject(
        script: String,
        callbackId: String,
        processor: (JSONObject) -> T
    ): T = suspendCancellableCoroutine { continuation ->
        val webView = webView ?: run {
            continuation.resume(processor(JSONObject()))
            return@suspendCancellableCoroutine
        }

        callbacks[callbackId] = { json ->
            try {
                continuation.resume(processor(JSONObject(json)))
            } catch (e: Exception) {
                e.printStackTrace()
                continuation.resume(processor(JSONObject()))
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
        config.customProcessor?.let { processor -> result = processor(result) }
        return result
    }

    private fun generateCallbackId() = "callback_${System.currentTimeMillis()}"

    override suspend fun updateMessageText(messageId: String, newText: String): Boolean {
        val config = strategy.getSelectorConfig()
        val script = buildUpdateMessageTextScript(messageId, newText, config)
        return executeCustomScript(script).trim() == "true"
    }


    override suspend fun getUserInfo(): ExtractedUserInfo? {
        return executeExtraction { config, _, callbackId ->
            executeAndProcessSingleObject(
                buildGetUserInfoScript(
                    config.userInfoSelector,
                    callbackId
                ), callbackId
            ) { json ->
                val name = json.getString("fullName")
                if (name.isBlank()) return@executeAndProcessSingleObject null

                ExtractedUserInfo(
                    username = name
                )
            }
        }
    }

    private fun buildIsChatPageScript(config: SelectorConfig) = """
        var timeout = 10000;
        var resolved = false;
        var startTime = Date.now();
        
        function check() {
            return !!(
                document.querySelector('${config.chatHeader}') &&
                document.querySelector('${config.inputFieldSelector}')
            );
        }
        
        return new Promise(function(resolve) {
            if (check()) {
                resolve(true);
                return;
            }
            
            var interval = setInterval(function() {
                if (resolved) return;
                
                if (check()) {
                    resolved = true;
                    clearInterval(interval);
                    resolve(true);
                } else if (Date.now() - startTime >= timeout) {
                    resolved = true;
                    clearInterval(interval);
                    resolve(false);
                }
            }, 100);
        });
    """.trimIndent()

    private fun buildInjectInfoMessageScript(
        message: InfoMessage,
        config: InfoMessageConfig
    ): String {
        val bgColor = when (message.type) {
            InfoMessageType.INFO -> "#3b82f6"
            InfoMessageType.ERROR -> "#ef4444"
        }

        return """
            var target = document.querySelector('${config.targetSelector}');
            if (!target) return false;
    
            var computedStyle = window.getComputedStyle(target);
            if (computedStyle.position === 'static') {
                target.style.position = 'relative';
            }
    
            var div = document.createElement('div');
            div.className = 'chatguard-info-message';
            div.style.cssText =
                'position:absolute;' +
                'top:100%;' +
                'left:0;' +
                'background:$bgColor;' +
                'color:#fff;' +
                'width:100%;' +
                'box-sizing:border-box;' +
                'padding:6px 8px;' +
                'font-size:14px;' +
                'z-index:9999;';
    
            div.textContent = ${JSONObject.quote(message.text)};
    
            target.appendChild(div);
    
            return true;
        """.trimIndent()
    }

    private fun buildRemoveInfoMessageScript(): String {
        return """
            try {
                var nodes = document.querySelectorAll('.chatguard-info-message');
                if (!nodes || !nodes.length) return false;

                nodes.forEach(function(el) {
                    el.remove();
                });

                return true;
            } catch (e) {
                return false;
            }
    """.trimIndent()
    }

    private fun buildGetUserInfoScript(
        config: UserInfoSelector,
        callbackId: String
    ): String = """
        (function() {
            var el = document.querySelector('${config.fullNameSelector}');
            var fullName = el ? (el.textContent || '').trim() : '';
            $BRIDGE_NAME.onContentExtracted('$callbackId', JSON.stringify({ fullName: fullName }));
        })();
    """.trimIndent()


    private fun buildShowPublicKeyButtonScript(
        config: SelectorConfig,
        callbackId: String
    ): String {
        val targetSelector = config.beforeSendPublicKeySelector
        return """
            (function() {
                function waitForTarget(retries) {
                    var target = document.querySelector('$targetSelector');
                    
                    if (!target && retries > 0) {
                        setTimeout(function() {
                            waitForTarget(retries - 1);
                        }, 100);
                        return;
                    }
                    
                    if (!target) {
                        return '';
                    }
                    
                    var existingButton = document.querySelector('[data-chatguard-public-key-input]');
                    if (existingButton) {
                        return '';
                    }
                    
                    createButton(target);
                }
                
                function createButton(target) {
                    var button = document.createElement('button');
                    button.setAttribute('data-chatguard-public-key-input', 'true');
                    button.className = '$BUTTON_CLASS';
                    
                    button.style.cssText = 
                        'width: 40px;' +
                        'height: 40px;' +
                        'border-radius: 50%;' +
                        'background: #3b82f6;' +
                        'border: none;' +
                        'cursor: pointer;' +
                        'display: flex;' +
                        'align-items: center;' +
                        'justify-content: center;' +
                        'padding: 0;' +
                        'margin: 0 8px;' +
                        'position: relative;' +
                        'z-index: 99999;' +
                        'pointer-events: auto;' +
                        'touch-action: manipulation;' +
                        '-webkit-tap-highlight-color: transparent;';
                    
                    button.innerHTML = '<svg xmlns="http://www.w3.org/2000/svg" height="24px" viewBox="0 -960 960 960" width="24px" fill="#e3e3e3" style="pointer-events: none;"><path d="M280-400q-33 0-56.5-23.5T200-480q0-33 23.5-56.5T280-560q33 0 56.5 23.5T360-480q0 33-23.5 56.5T280-400Zm0 160q-100 0-170-70T40-480q0-100 70-170t170-70q67 0 121.5 33t86.5 87h352l120 120-180 180-80-60-80 60-85-60h-47q-32 54-86.5 87T280-240Zm0-80q56 0 98.5-34t56.5-86h125l58 41 82-61 71 55 75-75-40-40H435q-14-52-56.5-86T280-640q-66 0-113 47t-47 113q0 66 47 113t113 47Z"/></svg>';
                    
                    var clickHandler = function(e) {
                        if (!e.isTrusted) {
                            return false;
                        }
                        
                        e.preventDefault();
                        e.stopPropagation();
                        e.stopImmediatePropagation();
                        
                        button.disabled = true;
                        button.style.opacity = '0.6';
                        
                        $BRIDGE_NAME.onContentExtracted('$callbackId', 'clicked');
                        
                        setTimeout(function() {
                            button.disabled = false;
                            button.style.opacity = '1';
                        }, 500);
                        
                        return false;
                    };
                    
                    button.addEventListener('click', clickHandler, true);
                    button.addEventListener('touchstart', clickHandler, true);
                    button.addEventListener('mousedown', clickHandler, true);
                    
                    button.__chatguardClickHandler = clickHandler;
                    
                    target.parentNode.insertBefore(button, target.nextSibling);
                    
                    return 'success';
                }
                
                waitForTarget(10);
            })();
        """.trimIndent()
    }

    private fun buildUpdateMessageTextScript(
        messageId: String,
        newText: String,
        config: SelectorConfig
    ): String {
        val escapedMessageId = JSONObject.quote(messageId)
        val escapedAttribute = JSONObject.quote(config.messageIdData)
        val escapedText = JSONObject.quote(newText)
        val escapedParentSelector = JSONObject.quote(config.messageParentSelector)
        val escapedMessageSelector = JSONObject.quote(config.messageSelector)
        val escapedMetaSelector = JSONObject.quote(config.messageMetaSelector)

        return """
            const messageId = $escapedMessageId;
            const messageIdAttr = $escapedAttribute;
            const newText = $escapedText;
            const parentSelector = $escapedParentSelector;
            const messageSelector = $escapedMessageSelector;
            const metaSelector = $escapedMetaSelector;
            
            const messageParent = document.querySelector(
                `${'$'}{parentSelector}[${'$'}{messageIdAttr}="${'$'}{messageId}"]`
            );
            
            if (!messageParent) return false;
            
            const messageElements = messageParent.querySelectorAll(messageSelector);
            
            if (messageElements.length === 0) return false;
            
            for (let i = messageElements.length - 1; i > 0; i--) {
                const element = messageElements[i];
                if (!element.matches(metaSelector) && !element.closest(metaSelector)) {
                    element.remove();
                }
            }
            
            const firstElement = messageElements[0];
            
            if (firstElement instanceof HTMLInputElement || 
                firstElement instanceof HTMLTextAreaElement) {
                firstElement.value = newText;
            } else {
                const metaElements = firstElement.querySelectorAll(metaSelector);
                const savedMetas = Array.from(metaElements).map(meta => meta.cloneNode(true));
               
                firstElement.textContent = newText;
                
                savedMetas.forEach(meta => {
                    firstElement.appendChild(meta);
                });
            }
            
            firstElement.dispatchEvent(new Event('input', { bubbles: true }));
            firstElement.dispatchEvent(new Event('change', { bubbles: true }));
            
            return true;
    """.trimIndent()
    }

    private fun buildInjectButtonScript(
        messageId: String,
        button: InjectedButton,
        config: SelectorConfig,
        injectionConfig: ButtonInjectionConfig
    ): String {
        val messageIdAttribute = config.messageIdData

        val defaultStyle =
            "font-size:13px;" +
                    "padding:6px 10px;" +
                    "line-height:1.3;" +
                    "background:#3b82f6;" +
                    "color:#fff;" +
                    "border:none;" +
                    "border-radius:6px;" +
                    "cursor:pointer;" +
                    "min-height:28px;" +
                    "margin-left:auto;" +
                    "display:block;"

        return """
            var messageId = '$messageId';
            var messageIdAttr = ${JSONObject.quote(messageIdAttribute)};
            var injectionTarget = ${JSONObject.quote(injectionConfig.targetSelector)};
            var insertPosition = ${JSONObject.quote(injectionConfig.insertPosition.name)};
            var buttonId = ${JSONObject.quote(button.id)};
            
            var messageParent = document.querySelector('${config.messageParentSelector}[' + messageIdAttr + '="' + messageId + '"]');
            
            if (!messageParent) {
                return false;
            }
            
            var targetElement = messageParent.querySelector(injectionTarget);
            if (!targetElement) {
                return false;
            }
            
            var existingButton = messageParent.querySelector('[data-chatguard-button-id="' + buttonId + '"]');
            if (existingButton) {
                existingButton.remove();
            }
            
            var button = document.createElement('button');
            button.className = '$BUTTON_CLASS';
            button.textContent = ${JSONObject.quote(button.text)};
            button.setAttribute('data-chatguard-button-id', buttonId);
            button.setAttribute('data-chatguard-button-type', ${JSONObject.quote(button.buttonType.name)});
            button.disabled = ${!button.enabled};
            button.setAttribute('style', ${JSONObject.quote(defaultStyle)});
    
            button.onclick = function(e) {
                e.preventDefault();
                e.stopPropagation();
                e.stopImmediatePropagation();
                
                try {
                    $BUTTON_BRIDGE_NAME.onButtonClick(
                        ${JSONObject.quote(button.buttonType.name)},
                        messageId
                    );
                } catch(err) {
                    console.error('Button click error:', err);
                }
                
                return false;
            };
            
            button.ontouchstart = function(e) {
                e.preventDefault();
                e.stopPropagation();
                e.stopImmediatePropagation();
                
                try {
                    $BUTTON_BRIDGE_NAME.onButtonClick(
                        ${JSONObject.quote(button.buttonType.name)},
                        messageId
                    );
                } catch(err) {
                    console.error('Button touch error:', err);
                }
                
                return false;
            };
            
            switch(insertPosition) {
                case 'BEFORE':
                    targetElement.parentNode.insertBefore(button, targetElement);
                    break;
                case 'AFTER':
                    targetElement.parentNode.insertBefore(button, targetElement.nextSibling);
                    break;
                case 'PREPEND':
                    targetElement.insertBefore(button, targetElement.firstChild);
                    break;
                case 'APPEND':
                default:
                    targetElement.appendChild(button);
                    break;
            }
            
            return true;
    """.trimIndent()
    }

    private fun buildRemoveAllButtonsScript(): String {
        return """
            var buttons = document.querySelectorAll('.$BUTTON_CLASS');
            buttons.forEach(function(button) {
                button.remove();
            });
            return buttons.length;
        """.trimIndent()
    }

    private fun buildObserveBackgroundColorScript(
        config: SelectorConfig,
        callbackId: String
    ) = """
        (function() {
            function rgbToHex(c) {
                if (!c) return '#000000';
                if (c[0] === '#') return c.toLowerCase();
                var m = c.match(/\d+/g);
                if (!m || m.length < 3) return '#000000';
                return '#' + m.slice(0,3).map(n => ('0' + parseInt(n).toString(16)).slice(-2)).join('');
            }
        
            function getBg() {
                var root = document.documentElement;
                var cssVar = getComputedStyle(root).getPropertyValue('${config.backgroundColorVariable}').trim();
                if (cssVar) return rgbToHex(cssVar);
        
                var bg = getComputedStyle(root).backgroundColor;
                if (bg && bg !== 'rgba(0, 0, 0, 0)') return rgbToHex(bg);
        
                return rgbToHex(getComputedStyle(document.body).backgroundColor);
            }
        
            var lastColor = null;
            var maxAttempts = 50;
            var attempts = 0;
        
            function emit() {
                var c = getBg();
                if (c && c !== lastColor) {
                    lastColor = c;
                    $BRIDGE_NAME.onContentExtracted('$callbackId', c);
                }
            }
        
            if (!window.__chatguardColorObserver) {
                window.__chatguardColorObserver = new MutationObserver(emit);
                [document.documentElement, document.body].forEach(el =>
                    window.__chatguardColorObserver.observe(el, {attributes:true, attributeFilter:['class','style'], subtree:true})
                );
            }
        
            var interval = setInterval(function(){
                emit();
                attempts++;
                if (lastColor || attempts >= maxAttempts) {
                    clearInterval(interval);
                }
            }, 100);
        })();
    """.trimIndent()

    private fun buildSendMessageScript(message: String, config: SelectorConfig): String {
        return """
            var input = document.querySelector('${config.inputFieldSelector}');
            if (!input) return false;
        
            var valueSet = false;
        
            try {
                var setter = null;
                if (input.tagName === 'TEXTAREA') {
                    var desc = Object.getOwnPropertyDescriptor(window.HTMLTextAreaElement.prototype, 'value');
                    setter = desc ? desc.set : null;
                } else if (input.tagName === 'INPUT') {
                    var desc = Object.getOwnPropertyDescriptor(window.HTMLInputElement.prototype, 'value');
                    setter = desc ? desc.set : null;
                } else if (input.isContentEditable) {
                    var desc = Object.getOwnPropertyDescriptor(window.HTMLDivElement.prototype, 'textContent');
                    setter = desc ? desc.set : null;
                }
        
                if (setter) {
                    setter.call(input, ${JSONObject.quote(message)});
                    valueSet = true;
                }
            } catch (e) { }
        
            if (!valueSet) {
                if ('value' in input) {
                    input.value = ${JSONObject.quote(message)};
                } else if (input.isContentEditable) {
                    input.textContent = ${JSONObject.quote(message)};
                } else {
                    input.innerText = ${JSONObject.quote(message)};
                }
            }
        
            input.dispatchEvent(new Event('input', { bubbles: true }));
            input.dispatchEvent(new Event('change', { bubbles: true }));
        
            window.__chatguardIsSending = true;
            
            var attempts = 0;
            var messageSent = false;
            
            var checkAndClick = function() {
                if (messageSent) return;
                
                var sendBtn = document.querySelector('${config.sendButtonSelector}');
                
                if (sendBtn && sendBtn.offsetParent !== null) {
                    messageSent = true;
                    
                    ${generateClickEvents(config.submitSendClick)}
                    
                    setTimeout(function() {
                        window.__chatguardIsSending = false;
                    }, 1000);
                } else if (attempts < 10) {
                    attempts++;
                    setTimeout(checkAndClick, 100);
                } else {
                    window.__chatguardIsSending = false;
                }
            };
            
            setTimeout(checkAndClick, 200);
        
            return true;
        """.trimIndent()
    }

    private fun generateClickEvents(submitType: String?): String {
        return when (submitType) {
            "click" -> "sendBtn.click();"
            "mousedown" -> "sendBtn.dispatchEvent(new MouseEvent('mousedown', { bubbles: true, cancelable: true }));"
            "mouseup" -> "sendBtn.dispatchEvent(new MouseEvent('mouseup', { bubbles: true, cancelable: true }));"
            "all" -> """
            sendBtn.dispatchEvent(new MouseEvent('mousedown', { bubbles: true, cancelable: true }));
            sendBtn.dispatchEvent(new MouseEvent('mouseup', { bubbles: true, cancelable: true }));
            sendBtn.dispatchEvent(new MouseEvent('click', { bubbles: true, cancelable: true }));
            sendBtn.click();
        """.trimIndent()

            else -> """
            sendBtn.dispatchEvent(new MouseEvent('mousedown', { bubbles: true, cancelable: true }));
            sendBtn.dispatchEvent(new MouseEvent('mouseup', { bubbles: true, cancelable: true }));
            sendBtn.dispatchEvent(new MouseEvent('click', { bubbles: true, cancelable: true }));
            sendBtn.click();
        """.trimIndent()
        }
    }

    private fun getObserverEventType(submitType: String?): String {
        return when (submitType) {
            "mousedown" -> "mousedown"
            "mouseup" -> "mouseup"
            else -> "click"
        }
    }

    private fun buildObserveSendActionScript(
        config: SelectorConfig,
        callbackId: String
    ) = """
        (function() {
            var input = document.querySelector('${config.inputFieldSelector}');
            if (!input) return false;
            
            if (window.__chatguardSendObserver) {
                window.__chatguardSendObserver.disconnect();
                delete window.__chatguardSendObserver;
            }
            
            var eventType = '${getObserverEventType(config.submitSendClick)}';
            var eventTypes = ['click', 'mousedown', 'mouseup'];
            
            var buttons = document.querySelectorAll('${config.sendButtonSelector}');
            buttons.forEach(function(btn) {
                if (btn.__chatguardSendHandler) {
                    eventTypes.forEach(function(type) {
                        btn.removeEventListener(type, btn.__chatguardSendHandler, true);
                    });
                    delete btn.__chatguardSendHandler;
                }
            });
            
            window.__chatguardIsSending = false;
            
            function attachListener(btn) {
                if (btn.__chatguardSendHandler) {
                    eventTypes.forEach(function(type) {
                        btn.removeEventListener(type, btn.__chatguardSendHandler, true);
                    });
                    delete btn.__chatguardSendHandler;
                }
                
                btn.__chatguardSendHandler = function(event) {
                    if (window.__chatguardIsSending) {
                        window.__chatguardIsSending = false;
                        return;
                    }
                    
                    if (!event.isTrusted) {
                        return;
                    }
                    
                    event.stopImmediatePropagation();
                    event.preventDefault();
                    
                    var messageText = input.value || input.textContent || input.innerText || '';
                    if (messageText && messageText.trim()) {
                        $BRIDGE_NAME.onContentExtracted('$callbackId', messageText.trim());
                    }
                };
                
                btn.addEventListener(eventType, btn.__chatguardSendHandler, true);
            }
            
            var existingBtn = document.querySelector('${config.sendButtonSelector}');
            if (existingBtn) attachListener(existingBtn);
            
            var observer = new MutationObserver(function() {
                var btn = document.querySelector('${config.sendButtonSelector}');
                if (btn && !btn.__chatguardSendHandler) {
                    attachListener(btn);
                }
            });
            
            observer.observe(document.body, { 
                childList: true, 
                subtree: true, 
                attributes: true, 
                attributeFilter: ['class', 'style', 'disabled'] 
            });
            
            window.__chatguardSendObserver = observer;
            
            return true;
        })();
    """.trimIndent()

    private fun buildRemoveSendActionObserverScript(config: SelectorConfig) = """
        (function() {
            if (window.__chatguardSendObserver) {
                window.__chatguardSendObserver.disconnect();
                delete window.__chatguardSendObserver;
            }
            
            var eventTypes = ['click', 'mousedown', 'mouseup'];
            var buttons = document.querySelectorAll('${config.sendButtonSelector}');
            
            buttons.forEach(function(btn) {
                if (btn.__chatguardSendHandler) {
                    eventTypes.forEach(function(eventType) {
                        btn.removeEventListener(eventType, btn.__chatguardSendHandler, true);
                    });
                    delete btn.__chatguardSendHandler;
                }
            });
            
            delete window.__chatguardIsSending;
            
            return true;
        })();
    """.trimIndent()

    private fun buildClearAllFlagsScript() = """
        (function() {
            if (window.__chatguardSendObserver) {
                window.__chatguardSendObserver.disconnect();
                delete window.__chatguardSendObserver;
            }
            
            if (window.__chatguardColorObserver) {
                window.__chatguardColorObserver.disconnect();
                delete window.__chatguardColorObserver;
            }
            
            if (window._chatguard_observers) {
                Object.keys(window._chatguard_observers).forEach(function(key) {
                    var obs = window._chatguard_observers[key];
                    if (obs && obs.disconnect) {
                        obs.disconnect();
                    }
                });
                delete window._chatguard_observers;
            }
            
            var eventTypes = ['click', 'mousedown', 'mouseup'];
            var buttons = document.querySelectorAll('${strategy.getSelectorConfig().sendButtonSelector}');
            buttons.forEach(function(btn) {
                eventTypes.forEach(function(eventType) {
                    if (btn.__chatguardSendHandler) {
                        btn.removeEventListener(eventType, btn.__chatguardSendHandler, true);
                    }
                });
                delete btn.__chatguardSendHandler;
            });
            
            var customButtons = document.querySelectorAll('.$BUTTON_CLASS');
            customButtons.forEach(function(btn) {
                if (btn.__chatguardClickHandler) {
                    btn.removeEventListener('click', btn.__chatguardClickHandler, true);
                    btn.removeEventListener('touchstart', btn.__chatguardClickHandler, true);
                    btn.removeEventListener('mousedown', btn.__chatguardClickHandler, true);
                    delete btn.__chatguardClickHandler;
                }
                btn.remove();
            });
            
            var infoMessages = document.querySelectorAll('.chatguard-info-message');
            infoMessages.forEach(function(msg) {
                msg.remove();
            });
            
            delete window.__chatguardIsSending;
            
            return true;
        })();
""".trimIndent()

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
            if (!window._chatguard_observers) {
                window._chatguard_observers = {};
            }
            
            var lastProcessedCount = 0;
            var lastMessageIds = new Set();
            var messageContents = new Map();
            var debounceTimer = null;
            var isProcessing = false;
            
            var callback = function() {
                if (isProcessing) return;
                isProcessing = true;
                
                try {
                    ${
        generateExtractionLogic(
            config,
            callbackId,
            includeDetailedAttrs = true,
            skipVisibilityCheck = true
        )
    }
                } catch(e) {
                    $BRIDGE_NAME.onError('$callbackId', e.message || 'Observer error');
                } finally {
                    isProcessing = false;
                }
            };
            
            var getMessageContent = function(parent) {
                var messageEl = parent.querySelector('${config.messageSelector}');
                if (!messageEl) return null;
                return (messageEl.textContent || messageEl.value || '').trim();
            };
            
            var hasChanges = function() {
                var messages = document.querySelectorAll('${config.messageSelector}');
                var currentCount = messages.length;
                
                if (currentCount !== lastProcessedCount) {
                    return true;
                }
                
                var currentIds = new Set();
                var currentContents = new Map();
                var parents = document.querySelectorAll('${config.messageParentSelector}[${config.messageIdData}]');
                
                for (var i = 0; i < parents.length; i++) {
                    var id = parents[i].getAttribute('${config.messageIdData}');
                    if (id) {
                        currentIds.add(id);
                        var content = getMessageContent(parents[i]);
                        if (content) {
                            currentContents.set(id, content);
                        }
                    }
                }
                
                if (currentIds.size !== lastMessageIds.size) {
                    return true;
                }
               
                for (var id of currentIds) {
                    if (!lastMessageIds.has(id)) {
                        return true;
                    }
                }
                
                for (var id of lastMessageIds) {
                    if (!currentIds.has(id)) {
                        return true;
                    }
                }
                
                for (var [id, content] of currentContents) {
                    if (messageContents.has(id) && messageContents.get(id) !== content) {
                        return true;
                    }
                }
                
                for (var [currentId, currentContent] of currentContents) {
                    if (currentContent && currentContent.startsWith('🔒')) {
                        var foundMatch = false;
                        for (var [oldId, oldContent] of messageContents) {
                            if (oldContent === currentContent && oldId !== currentId && !currentIds.has(oldId)) {
                                return true;
                            }
                        }
                    }
                }
                
                return false;
            };
            
            var updateState = function() {
                lastProcessedCount = document.querySelectorAll('${config.messageSelector}').length;
                
                lastMessageIds.clear();
                messageContents.clear();
                
                var parents = document.querySelectorAll('${config.messageParentSelector}[${config.messageIdData}]');
                for (var i = 0; i < parents.length; i++) {
                    var id = parents[i].getAttribute('${config.messageIdData}');
                    if (id) {
                        lastMessageIds.add(id);
                        var content = getMessageContent(parents[i]);
                        if (content) {
                            messageContents.set(id, content);
                        }
                    }
                }
            };
            
            var triggerCallback = function() {
                clearTimeout(debounceTimer);
                debounceTimer = setTimeout(function() {
                    if (hasChanges()) {
                        updateState();
                        callback();
                    }
                }, 150);
            };
            
            var observer = new MutationObserver(function(mutations) {
                var shouldTrigger = false;
                var relevantSelectors = [
                    '${config.messageSelector}',
                    '${config.messageParentSelector}'
                ];
                
                for (var i = 0; i < mutations.length && !shouldTrigger; i++) {
                    var mutation = mutations[i];
                    
                    if (mutation.type === 'childList' && mutation.addedNodes.length > 0) {
                        for (var j = 0; j < mutation.addedNodes.length; j++) {
                            var node = mutation.addedNodes[j];
                            if (node.nodeType === 1) {
                                for (var k = 0; k < relevantSelectors.length; k++) {
                                    if ((node.matches && node.matches(relevantSelectors[k])) ||
                                        (node.querySelector && node.querySelector(relevantSelectors[k]))) {
                                        shouldTrigger = true;
                                        break;
                                    }
                                }
                            }
                            if (shouldTrigger) break;
                        }
                    }
                    
                    if (mutation.type === 'attributes') {
                        if (mutation.attributeName === '${config.messageIdData}') {
                            shouldTrigger = true;
                        } else if (mutation.target.matches) {
                            for (var k = 0; k < relevantSelectors.length; k++) {
                                if (mutation.target.matches(relevantSelectors[k])) {
                                    shouldTrigger = true;
                                    break;
                                }
                            }
                        }
                    }
                    
                    if (mutation.type === 'characterData') {
                        var parent = mutation.target.parentElement;
                        while (parent && !shouldTrigger) {
                            for (var k = 0; k < relevantSelectors.length; k++) {
                                if (parent.matches && parent.matches(relevantSelectors[k])) {
                                    shouldTrigger = true;
                                    break;
                                }
                            }
                            parent = parent.parentElement;
                        }
                    }
                }
                
                if (shouldTrigger) {
                    triggerCallback();
                }
            });
            
            window._chatguard_observers['$callbackId'] = observer;
            
            var container = document.querySelector('${config.containerSelector}');
            if (!container) {
                var firstMessage = document.querySelector('${config.messageSelector}');
                if (firstMessage) {
                    container = firstMessage.closest('${config.messageParentSelector}') || firstMessage.parentElement;
                }
            }
            if (!container) {
                container = document.body;
            }
            
            observer.observe(container, { 
                childList: true, 
                subtree: true,
                characterData: true,
                attributes: true,
                attributeFilter: ['${config.messageIdData}']
            });
            
            updateState();
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
                className: parent.className || '',
                id: parent.id || '',
                dir: parent.getAttribute('dir') || '',"""
        } else {
            ""
        }

        val visibilityCheck = if (skipVisibilityCheck) {
            "true"
        } else {
            "isElementVisible(parent)"
        }

        return """
                var parents = new Set();
                var firstMessages = document.querySelectorAll('${config.messageSelector}');
                
                firstMessages.forEach(function(el) {
                    var parent = el.closest('${config.messageParentSelector}');
                    if (parent) parents.add(parent);
                });
                
                var contents = [];
                var ignoreSelectorsJoined = [${(config.ignoreSelectors + ignoreSelectors).joinToString { "'$it'" }}].join(',');
                var attributes = [${config.attributesToExtract.joinToString { "'$it'" }}];
                
                function isElementVisible(el) {
                    var rect = el.getBoundingClientRect();
                    var windowHeight = window.innerHeight || document.documentElement.clientHeight;
                    var threshold = $VISIBILITY_THRESHOLD_PX;
                    return rect.top < windowHeight + threshold && rect.bottom > -threshold;
                }
                
                function cleanText(node) {
                    var clone = node.cloneNode(true);
                    
                    if (ignoreSelectorsJoined) {
                        clone.querySelectorAll(ignoreSelectorsJoined).forEach(function(el) {
                            el.remove();
                        });
                    }
                    
                    var textNodes = [];
                    var walker = document.createTreeWalker(clone, NodeFilter.SHOW_TEXT, null, false);
                    var textNode;
                    
                    while (textNode = walker.nextNode()) {
                        var text = textNode.nodeValue.trim();
                        if (text) textNodes.push(text);
                    }
                    
                    return textNodes.join(' ').replace(/\s+/g, ' ');
                }
                
                parents.forEach(function(parent) {
                    if (!$visibilityCheck) return;
                    
                    var messageElements = parent.querySelectorAll('${config.messageSelector}');
                    if (messageElements.length === 0) return;
                    
                    var textParts = [];
                    var htmlParts = [];
                    
                    messageElements.forEach(function(msgEl) {
                        var text = cleanText(msgEl);
                        if (text) textParts.push(text);
                        
                        var html = msgEl.innerHTML || '';
                        if (html) htmlParts.push(html);
                    });
                    
                    var finalText = textParts.join(' ');
                    var finalHtml = htmlParts.join(' ');
                    
                    if (!finalText && !finalHtml) return;
                    
                    var item = {
                        text: finalText,
                        html: finalHtml,$detailedAttrs
                    };
                    
                    attributes.forEach(function(attr) {
                        item[attr] = parent.getAttribute(attr) || '';
                    });
                    
                    contents.push(item);
                });
                
                $BRIDGE_NAME.onContentExtracted('$callbackId', JSON.stringify(contents));
        """.trimIndent()
    }
}