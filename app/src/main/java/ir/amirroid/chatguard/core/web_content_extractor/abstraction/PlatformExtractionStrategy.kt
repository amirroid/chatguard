package ir.amirroid.chatguard.core.web_content_extractor.abstraction

import ir.amirroid.chatguard.core.messanger.models.MessengerPlatform
import ir.amirroid.chatguard.core.web_content_extractor.models.ElementData
import ir.amirroid.chatguard.core.web_content_extractor.models.ProcessingConfig
import ir.amirroid.chatguard.core.web_content_extractor.models.SelectorConfig

/**
 * Advanced extraction strategy for each platform
 * Provides flexible configuration for DOM selectors and text processing
 */
interface PlatformExtractionStrategy {
    val platform: MessengerPlatform
    
    /**
     * Returns the selector configuration for message extraction
     */
    fun getSelectorConfig(): SelectorConfig
    
    /**
     * Returns the processing configuration for text cleanup
     */
    fun getProcessingConfig(): ProcessingConfig = ProcessingConfig()
    
    /**
     * Custom validation for extracted elements
     * @return true if the element should be included in results
     */
    fun validateElement(text: String, html: String, attributes: Map<String, String>): Boolean = true
    
    /**
     * Transform extracted data before returning
     */
    fun transformData(data: ElementData): ElementData = data
    
    /**
     * Optional: Custom JavaScript code to inject after load completed
     */
    fun getInitialExecutionScript(): String? = null

    /**
     * Check if the given URL belongs to a chat page
     */
    fun isChatUrl(url: String): Boolean = true
}
