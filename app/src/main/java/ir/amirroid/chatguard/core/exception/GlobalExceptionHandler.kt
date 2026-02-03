package ir.amirroid.chatguard.core.exception

import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import ir.amirroid.chatguard.BuildConfig
import ir.amirroid.chatguard.features.crash.DisplayCrashActivity
import ir.amirroid.chatguard.utils.Constants
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.lang.Thread.UncaughtExceptionHandler
import kotlin.system.exitProcess

class GlobalExceptionHandler(
    private val applicationContext: Context,
    private val oldHandler: UncaughtExceptionHandler?,
    private val onCrash: (ApplicationCrash) -> Unit
) : UncaughtExceptionHandler {

    override fun uncaughtException(t: Thread, e: Throwable) {
        try {
            handleUncaughtException(t, e)
        } catch (handlerException: Exception) {
            Log.e("GlobalExceptionHandler", "Failed to handle crash", handlerException)
        }

        oldHandler?.uncaughtException(t, e) ?: run {
            exitProcess(1)
        }
    }

    private fun handleUncaughtException(t: Thread, e: Throwable) {
        val message = e.message.orEmpty()
        val exceptionClass = e::class.qualifiedName.orEmpty()
        val stackTrace = e.stackTraceToString()

        val firstStacktrace = e.stackTrace.firstOrNull()
        val fileName = firstStacktrace?.fileName.orEmpty()
        val lineNumber = firstStacktrace?.lineNumber ?: 0

        val crash = ApplicationCrash(
            stacktrace = stackTrace,
            message = message,
            exception = exceptionClass,
            fileName = fileName,
            lineNumber = lineNumber,
            appVersion = BuildConfig.VERSION_NAME,
            device = ApplicationCrash.Device(
                apiVersion = Build.VERSION.SDK_INT,
                androidVersion = Build.VERSION.RELEASE,
                model = "${Build.MANUFACTURER} ${Build.BRAND} ${Build.MODEL} (${Build.DEVICE})"
            )
        )

        onCrash.invoke(crash)

        val intent = Intent(applicationContext, DisplayCrashActivity::class.java).apply {
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP
            )
            putExtra(Constants.CRASH_DATA_KEY, Json.encodeToString(crash))
        }

        applicationContext.startActivity(intent)

        Thread.sleep(500)
    }
}