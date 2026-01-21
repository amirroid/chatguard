package ir.sysfail.chatguard.core.messanger.models

enum class MessengerPlatform(val packageName: String, val url: String) {
    BALE("ir.nasim", ""),
    SOROUSH("mobi.mmdt.ottplus", "https://web.splus.ir/"),
    EITAA("ir.eitaa.messenger", "https://web.eitaa.com/")
}