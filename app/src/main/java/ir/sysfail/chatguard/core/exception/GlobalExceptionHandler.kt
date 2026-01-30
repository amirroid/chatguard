package ir.sysfail.chatguard.core.exception

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import ir.sysfail.chatguard.BuildConfig
import ir.sysfail.chatguard.features.crash.DisplayCrashActivity
import ir.sysfail.chatguard.utils.Constants
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

            try {
                val intent = Intent(applicationContext, DisplayCrashActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)

                    putExtra(Constants.CRASH_DATA_KEY, Json.encodeToString(crash))

                }
                applicationContext.startActivity(intent)

                Thread.sleep(300)
            } catch (activityException: Exception) {
                activityException.printStackTrace()
            }

        } catch (handlerException: Exception) {
            handlerException.printStackTrace()
        } finally {
            if (oldHandler != null) {
                oldHandler.uncaughtException(t, e)
            } else {
                android.os.Process.killProcess(android.os.Process.myPid())
                exitProcess(10)
            }
        }
    }
}