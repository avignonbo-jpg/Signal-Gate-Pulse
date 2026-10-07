package com.signalgate.pulse.diagnostics

import android.util.Log
import com.signalgate.pulse.BuildConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.ArrayDeque
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicLong

/** One Timber event held in the debug-only in-app log buffer. */
data class AppLogEntry(
    val id: Long,
    val timestampMillis: Long,
    val priority: Int,
    val tag: String,
    val threadName: String,
    val message: String
) {
    val priorityLabel: String
        get() = when (priority) {
            Log.VERBOSE -> "V"
            Log.DEBUG -> "D"
            Log.INFO -> "I"
            Log.WARN -> "W"
            Log.ERROR -> "E"
            Log.ASSERT -> "A"
            else -> priority.toString()
        }

    fun displayLine(): String {
        val time = SimpleDateFormat("MM-dd HH:mm:ss.SSS", Locale.US).format(Date(timestampMillis))
        return "$time $priorityLabel/$tag [$threadName]: $message"
    }
}

/**
 * Captures in-process Timber events in a bounded, observable buffer and mirrors each
 * stored message to Android Logcat. Installing this tree instead of Timber.DebugTree
 * avoids double-writing the same event while keeping the existing Logcat reader useful.
 */
internal class AppDiagnosticsTree(
    private val capacity: Int = AppLogCapture.BUFFER_SIZE,
    private val maxMessageChars: Int = DEFAULT_MESSAGE_CHARS,
    private val logcatSink: (priority: Int, tag: String, message: String) -> Unit = { priority, tag, message ->
        Log.println(priority, tag, message)
    },
    private val clockMillis: () -> Long = { System.currentTimeMillis() }
) : Timber.Tree() {
    private val lock = Any()
    private val buffer = ArrayDeque<AppLogEntry>(capacity)
    private val nextId = AtomicLong(0L)
    private val mutableEntries = MutableStateFlow<List<AppLogEntry>>(emptyList())
    val entries: StateFlow<List<AppLogEntry>> = mutableEntries.asStateFlow()

    init {
        require(capacity > 0) { "capacity must be positive" }
        require(maxMessageChars > TRUNCATION_SUFFIX.length) { "maxMessageChars is too small" }
    }

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        append(priority, tag, message, t)
    }

    internal fun append(priority: Int, tag: String?, message: String, throwable: Throwable? = null) {
        val resolvedTag = (tag?.takeIf(String::isNotBlank) ?: inferTag()).take(MAX_TAG_CHARS)
        val fullMessage = if (throwable == null) message else "$message\n${stackTraceOf(throwable)}"
        val storedMessage = if (fullMessage.length <= maxMessageChars) {
            fullMessage
        } else {
            fullMessage.take(maxMessageChars - TRUNCATION_SUFFIX.length) + TRUNCATION_SUFFIX
        }
        val entry = AppLogEntry(
            id = nextId.incrementAndGet(),
            timestampMillis = clockMillis(),
            priority = priority,
            tag = resolvedTag,
            threadName = Thread.currentThread().name.take(MAX_THREAD_NAME_CHARS),
            message = storedMessage
        )

        synchronized(lock) {
            buffer.addLast(entry)
            while (buffer.size > capacity) buffer.removeFirst()
            mutableEntries.value = buffer.toList()
        }

        // A broken platform log sink must not prevent the in-app buffer from recording the event.
        try {
            logcatSink(priority, resolvedTag, storedMessage)
        } catch (_: Exception) {
            // Deliberately best-effort; the in-process buffer remains available.
        }
    }

    fun clear() {
        synchronized(lock) {
            buffer.clear()
            mutableEntries.value = emptyList()
        }
    }

    private fun inferTag(): String = Throwable().stackTrace
        .firstOrNull { frame ->
            !frame.className.startsWith("timber.log.") &&
                !frame.className.startsWith(AppDiagnosticsTree::class.java.name)
        }
        ?.className
        ?.substringAfterLast('.')
        ?.take(MAX_TAG_CHARS)
        ?: "Pulse"

    private fun stackTraceOf(throwable: Throwable): String = StringWriter().also { writer ->
        PrintWriter(writer).use { printer -> throwable.printStackTrace(printer) }
    }.toString()

    companion object {
        const val DEFAULT_MESSAGE_CHARS = 8_000
        const val MAX_TAG_CHARS = 80
        const val MAX_THREAD_NAME_CHARS = 80
        const val TRUNCATION_SUFFIX = "… [truncated]"
    }
}

/** Debug-build-only singleton that installs one capture tree and exposes its live feed. */
internal object AppLogCapture {
    const val BUFFER_SIZE = 1_000

    private val tree = AppDiagnosticsTree(capacity = BUFFER_SIZE)
    private var installed = false

    val entries: StateFlow<List<AppLogEntry>>
        get() = tree.entries

    @Synchronized
    fun install() {
        if (!BuildConfig.DEBUG) return
        if (!installed) {
            Timber.plant(tree)
            installed = true
        }
    }

    fun clear() = tree.clear()
}
