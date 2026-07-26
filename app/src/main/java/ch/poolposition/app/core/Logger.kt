package ch.poolposition.app.core

import android.content.Context
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Tiny append-only diagnostic log written to a capped file in private storage.
 * Lets the user see whether the periodic job runs, what each fetch returned, and
 * whether a watch fired — without a logcat cable.
 */
object Logger {

    private const val FILE_NAME = "poolposition.log"
    private const val MAX_BYTES = 64 * 1024
    private val lock = Any()
    private val timeFmt = SimpleDateFormat("MM-dd HH:mm:ss", Locale.US)

    fun log(context: Context, message: String) {
        synchronized(lock) {
            runCatching {
                val file = File(context.filesDir, FILE_NAME)
                file.appendText("${timeFmt.format(Date())}  $message\n")
                if (file.length() > MAX_BYTES) {
                    // Keep the most recent half to bound growth.
                    file.writeText(file.readText().takeLast(MAX_BYTES / 2))
                }
            }
        }
    }

    fun read(context: Context): String = synchronized(lock) {
        val file = File(context.filesDir, FILE_NAME)
        if (file.exists()) file.readText() else ""
    }

    fun clear(context: Context) {
        synchronized(lock) {
            runCatching { File(context.filesDir, FILE_NAME).writeText("") }
        }
    }
}
