package ir.sysfail.chatguard.core.web_content_extractor.implementation.strategy

import ir.sysfail.chatguard.core.messanger.models.MessengerPlatform
import ir.sysfail.chatguard.core.web_content_extractor.abstraction.PlatformExtractionStrategy
import ir.sysfail.chatguard.core.web_content_extractor.models.*

class BaleExtractionStrategy : PlatformExtractionStrategy {
    override val platform = MessengerPlatform.BALE

    override fun getSelectorConfig() = SelectorConfig(
        containerSelector = ".JL_RzJ",
        messageParentSelector = ".message-item",
        messageSelector = ".KTwPFW.AiYtbO .p",
        messageIdData = MESSAGE_ID_DATA,
        ignoreSelectors = listOf(
            ".time",
        ),
        attributesToExtract = listOf(MESSAGE_ID_DATA),
        sendButtonSelector = ".FRqrfO.RaTWwR",
        submitSendClick = "click",
        inputFieldSelector = "#main-message-input",
        messageMetaSelector = ".time",
        backgroundColorVariable = "--color-neutrals-n-00",
        buttonInjectionConfig = ButtonInjectionConfig(
            targetSelector = ".IheVs2.KgorAF.QMmcaY",
            insertPosition = InsertPosition.AFTER,
        ),
        beforeSendPublicKeySelector = ".tf8V56",
        userInfoSelector = UserInfoSelector(
            fullNameSelector = ".nMlHDG"
        ),
        infoMessageConfig = InfoMessageConfig(
            targetSelector = ".kvGVCY"
        ),
        chatHeader = ".kvGVCY"
    )

    override fun getProcessingConfig() = ProcessingConfig(
        trimWhitespace = true,
        removeEmptyLines = true,
        normalizeSpaces = true,
        findMessageId = { data ->
            data.customAttributes[MESSAGE_ID_DATA].takeIf { !it.isNullOrBlank() }
        },
        checkIsOwnMessage = { data ->
            data.className.split(" ").contains("LtdUd1")
        }
    )

    override fun validateElement(text: String, html: String, attributes: Map<String, String>) =
        text.length > 2

    override fun transformData(data: ElementData) =
        data.copy(text = data.text.replace(Regex("\\s+"), " ").trim())

    companion object {
        private const val MESSAGE_ID_DATA = "data-sid"
    }
}