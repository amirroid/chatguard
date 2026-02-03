package ir.amirroid.chatguard.core.exception

import kotlinx.serialization.Serializable

@Serializable
data class ApplicationCrash(
    val stacktrace: String,
    val message: String,
    val exception: String,
    val fileName: String,
    val lineNumber: Int,
    val appVersion: String,
    val device: Device
) {
    @Serializable
    data class Device(
        val apiVersion: Int,
        val androidVersion: String,
        val model: String
    )
}