package ir.sysfail.chatguard.core.web_content_extractor.implementation

import android.webkit.JavascriptInterface
import android.webkit.WebView
import ir.sysfail.chatguard.core.web_content_extractor.abstraction.WebContentExtractor
import ir.sysfail.chatguard.core.web_content_extractor.abstraction.PlatformExtractionStrategy
import ir.sysfail.chatguard.core.web_content_extractor.models.*
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

    override fun mapElementsToMessages(elements: List<ElementData>): List<ExtractedElementMessage> {
        val processing = strategy.getProcessingConfig()

        return elements.mapNotNull { element ->
            if (processing.checkIsOwnMessage == null || processing.findMessageId == null) return@mapNotNull null

            ExtractedElementMessage(
                message = element.text,
                id = processing.findMessageId(element) ?: return@mapNotNull null,
                isMyMessage = processing.checkIsOwnMessage(element)
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

            callbacks[callbackId] = { json ->
                val messages = parseDetailedData(JSONArray(json), config, processing)
                onMessagesChanged(messages)
            }

            val script = buildObserverScript(config, callbackId)
            webView.post { webView.evaluateJavascript(script, null) }
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
          var timeout = 5000;
          var resolved = false;
        
          function check() {
            return !!(
              document.querySelector('${config.chatHeader}') &&
              document.querySelector('${config.inputFieldSelector}')
            );
          }
        
          return new Promise(function (resolve) {
            if (check()) {
              resolve(true);
              return;
            }
       
            var timer = setTimeout(function () {
              if (resolved) return;
              resolved = true;
              observer.disconnect();
              resolve(false);
            }, timeout);
        
            var observer = new MutationObserver(function () {
              if (resolved) return;
        
              if (check()) {
                resolved = true;
                clearTimeout(timer);
                observer.disconnect();
                resolve(true);
              }
            });
        
            observer.observe(document.body, {
              childList: true,
              subtree: true
            });
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
            var target = document.querySelector('$targetSelector');
            if (!target) return '';
            
            var existingButton = document.querySelector('[data-chatguard-public-key-input]');
            if (existingButton) return '';
            
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
                'margin: 0 8px;';
            
            button.innerHTML = '<svg xmlns="http://www.w3.org/2000/svg" height="24px" viewBox="0 -960 960 960" width="24px" fill="#e3e3e3"><path d="M280-400q-33 0-56.5-23.5T200-480q0-33 23.5-56.5T280-560q33 0 56.5 23.5T360-480q0 33-23.5 56.5T280-400Zm0 160q-100 0-170-70T40-480q0-100 70-170t170-70q67 0 121.5 33t86.5 87h352l120 120-180 180-80-60-80 60-85-60h-47q-32 54-86.5 87T280-240Zm0-80q56 0 98.5-34t56.5-86h125l58 41 82-61 71 55 75-75-40-40H435q-14-52-56.5-86T280-640q-66 0-113 47t-47 113q0 66 47 113t113 47Z"/></svg>';
            
            button.addEventListener('click', function(e) {
                e.preventDefault();
                e.stopPropagation();
                e.stopImmediatePropagation();
                
                $BRIDGE_NAME.onContentExtracted('$callbackId', '');
            });
            
            target.parentNode.insertBefore(button, target.nextSibling);
            
            return '';
        })();
    """.trimIndent()
    }

    private fun buildUpdateMessageTextScript(
        messageId: String,
        newText: String,
        config: SelectorConfig
    ): String {
        val messageIdAttribute = config.messageIdData

        return """
        var messageId = '$messageId';
        var messageIdAttr = ${JSONObject.quote(messageIdAttribute)};
        var newText = ${JSONObject.quote(newText)};
        
        var messageParent = document.querySelector(
            '${config.messageParentSelector}[' + messageIdAttr + '="' + messageId + '"]'
        );
        
        if (!messageParent) {
            console.error('Message not found with ID:', messageId);
            return false;
        }
        
        var allMessageElements = messageParent.querySelectorAll('${config.messageSelector}');
        
        if (allMessageElements.length === 0) {
            console.error('No message text elements found');
            return false;
        }
        
        for (var i = 1; i < allMessageElements.length; i++) {
            allMessageElements[i].remove();
        }
        
        var firstMessage = allMessageElements[0];
        
        if ('value' in firstMessage) {
            firstMessage.value = newText;
        } else if ('innerText' in firstMessage) {
            firstMessage.innerText = newText;
        } else {
            firstMessage.textContent = newText;
        }
        
        firstMessage.dispatchEvent(new Event('input', { bubbles: true }));
        firstMessage.dispatchEvent(new Event('change', { bubbles: true }));
        
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
            console.error('Message not found:', messageId);
            return false;
        }
        
        var targetElement = messageParent.querySelector(injectionTarget);
        if (!targetElement) {
            return false;
        }
        
        var existingButton = messageParent.querySelector('[data-chatguard-button-id="' + buttonId + '"]');
        if (existingButton) {
            return true;
        }
        
        var textContent = messageParent.querySelector('${config.messageSelector}');
        
        var button = document.createElement('button');
        button.className = '$BUTTON_CLASS';
        button.textContent = ${JSONObject.quote(button.text)};
        button.setAttribute('data-chatguard-button-id', buttonId);
        button.setAttribute('data-chatguard-button-type', ${JSONObject.quote(button.buttonType.name)});
        button.disabled = ${!button.enabled};
        button.setAttribute('style', ${JSONObject.quote(defaultStyle)});

        button.addEventListener('click', function(e) {
            e.preventDefault();
            e.stopPropagation();
            
            $BUTTON_BRIDGE_NAME.onButtonClick(
                ${JSONObject.quote(button.buttonType.name)},
                messageId
            );
        });
        
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
                if (lastColor) clearInterval(interval);
            }, 100);
        })();
    """.trimIndent()

    private fun buildSendMessageScript(message: String, config: SelectorConfig): String {
        return """
            var inputs = document.querySelectorAll('${config.inputFieldSelector}');
            if (inputs.length === 0) return false;
        
            inputs.forEach(function(input) {
                var valueSet = false;
            
                try {
                    var setter;
                    if (input.tagName === 'TEXTAREA') {
                        setter = Object.getOwnPropertyDescriptor(
                            window.HTMLTextAreaElement.prototype,
                            'value'
                        ).set;
                    } else {
                        setter = Object.getOwnPropertyDescriptor(
                            window.HTMLInputElement.prototype,
                            'value'
                        ).set;
                    }
            
                    setter.call(input, ${JSONObject.quote(message)});
                    valueSet = true;
                } catch (e) { }
            
                if (!valueSet) {
                    if ('value' in input) {
                        input.value = ${JSONObject.quote(message)};
                    } else {
                        input.innerText = ${JSONObject.quote(message)};
                    }
                }
            
                input.dispatchEvent(new Event('input', { bubbles: true }));
                input.dispatchEvent(new Event('change', { bubbles: true }));
            });
        
            setTimeout(function () {
                var sendBtn = document.querySelector('${config.sendButtonSelector}');
                if (sendBtn) {
                    sendBtn.__chatguardIsSending = true;
                    var clicked = sendBtn.click();
                    
                    if (clicked === false || clicked === undefined) {
                        sendBtn.dispatchEvent(new MouseEvent('mousedown', {
                            bubbles: true,
                            cancelable: true,
                            view: window
                        }));
                        sendBtn.dispatchEvent(new MouseEvent('mouseup', {
                            bubbles: true,
                            cancelable: true,
                            view: window
                        }));
                    }
                }
            }, 200);
        
            return true;
        """.trimIndent()
    }

    private fun buildObserveSendActionScript(
        config: SelectorConfig,
        callbackId: String
    ) = """
        (function() {
            var input = document.querySelector('${config.inputFieldSelector}');
            if (!input) return false;
            
            function attachListener(btn) {
                if (btn.__chatguardSendObserverAttached) return;
                btn.__chatguardSendObserverAttached = true;
                btn.addEventListener('click', function(event) {
                    if (btn.__chatguardIsSending) {
                        btn.__chatguardIsSending = false;
                        return;
                    }
                    event.stopImmediatePropagation();
                    event.preventDefault();
                    var messageText = input.value || input.innerText || input.textContent || '';
                    $BRIDGE_NAME.onContentExtracted(
                        '$callbackId',
                        messageText
                    );
                }, true);
            }
            
            var existingBtn = document.querySelector('${config.sendButtonSelector}');
            if (existingBtn) attachListener(existingBtn);
            
            var observer = new MutationObserver(function(mutations) {
                mutations.forEach(function(mutation) {
                    if (mutation.type === 'childList') {
                        var btn = document.querySelector('${config.sendButtonSelector}');
                        if (btn) attachListener(btn);
                    }
                });
            });
            observer.observe(document.body, { childList: true, subtree: true });
            
            return true;
        })();
    """.trimIndent()


    private fun buildRemoveSendActionObserverScript(config: SelectorConfig) = """
        (function() {
            var sendBtn = document.querySelector('${config.sendButtonSelector}');
            if (!sendBtn) return;
    
            if (sendBtn.__chatguardSendHandler) {
                sendBtn.removeEventListener('click', sendBtn.__chatguardSendHandler, true);
                sendBtn.__chatguardSendHandler = null;
                sendBtn.__chatguardSendObserverAttached = false;
            }
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
        
            var callback = function() {
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
                }
            };
        
            var observer = new MutationObserver(function(mutations) {
                var hasRelevantChange = mutations.some(function(mutation) {
                    if (mutation.type === 'childList' && mutation.addedNodes.length > 0) {
                        return Array.from(mutation.addedNodes).some(function(node) {
                            return node.nodeType === 1 && (
                                node.matches && (
                                    node.matches('${config.messageSelector}') ||
                                    node.matches('${config.messageMetaSelector}')
                                ) ||
                                node.querySelector && (
                                    node.querySelector('${config.messageSelector}') ||
                                    node.querySelector('${config.messageMetaSelector}')
                                )
                            );
                        });
                    }
                    return mutation.type === 'characterData' || mutation.type === 'attributes';
                });
        
                if (hasRelevantChange) {
                    callback();
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
                attributes: true
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
        var elements = document.querySelectorAll('${config.messageSelector}');
        var contents = [];
        var ignoreSelectors = [${(config.ignoreSelectors + ignoreSelectors).joinToString { "'$it'" }}];
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
            var parent = el.closest('${config.messageParentSelector}');
            if (!parent) return null;
        
            var messageElements = parent.querySelectorAll('${config.messageSelector}');
            var textParts = [];
            var htmlParts = [];
            
            messageElements.forEach(function(msgEl) {
                var clone = msgEl.cloneNode(true);
            
                ignoreSelectors.forEach(function(sel) {
                    clone.querySelectorAll(sel).forEach(function(m) { m.remove(); });
                });
            
                clone.querySelectorAll('div, p, br, li').forEach(function(n) {
                    n.insertAdjacentText('afterend', '\n');
                });
            
                var rawText = clone.textContent || '';
                var text = rawText
                    .replace(/\n+/g, ' ')
                    .replace(/\s+/g, ' ')
                    .trim();
            
                var html = clone.innerHTML || '';
                
                if (text) textParts.push(text);
                if (html) htmlParts.push(html);
            });
            
            var finalText = textParts.join(' ');
            var finalHtml = htmlParts.join(' ');
        
            if (finalText || finalHtml) {
                var item = {
                    text: finalText,
                    html: finalHtml,$detailedAttrs
                };
        
                attributes.forEach(function(attr) {
                    item[attr] = parent.getAttribute(attr) || '';
                });
        
                return item;
            }
            return null;
        }
        
        var processedParents = new Set();
        
        elements.forEach(function(el) {
            var parent = el.closest('${config.messageParentSelector}');
            if (parent && !processedParents.has(parent)) {
                processedParents.add(parent);
                
                if ($visibilityCheck) {
                    var item = extractContent(el);
                    if (item) contents.push(item);
                }
            }
        });
        
        $BRIDGE_NAME.onContentExtracted('$callbackId', JSON.stringify(contents));
    """.trimIndent()
    }
}