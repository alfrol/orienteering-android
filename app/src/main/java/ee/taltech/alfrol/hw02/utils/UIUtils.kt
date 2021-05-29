package ee.taltech.alfrol.hw02.utils

import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.widget.Toast
import androidx.annotation.ColorRes
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import com.google.android.gms.maps.model.PolylineOptions
import ee.taltech.alfrol.hw02.R
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

/**
 * A collection of utility functions for operating with the UI.
 */
object UIUtils {

    /**
     * Show a small informational message to the user.
     * Mostly meant for displaying an error message.
     */
    fun showToast(context: Context, @StringRes message: Int) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }

    /**
     * Construct a [TextWatcher] instance with the provided [callback].
     *
     * @param callback A function that will be executed on [TextWatcher.afterTextChanged].
     */
    fun getTextChangeListener(callback: () -> Unit): TextWatcher {
        return object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (s?.length != 0 || s.isEmpty() && before > 0) {
                    callback()
                }
            }

            override fun afterTextChanged(s: Editable?) {
            }
        }
    }

    /**
     * Construct a [PolylineOptions] with the given presets.
     *
     * @param context Context to use for color extraction.
     * @param color Color to apply.
     * @param width Width to apply.
     */
    fun getPolylineOptions(context: Context, @ColorRes color: Int, width: Float) =
        PolylineOptions().apply {
            color(ContextCompat.getColor(context, color))
            width(width)
        }

    /**
     * Get the correct formatted string for the given distance value.
     *
     * @param context The context to use for requesting a string resource.
     * @param distance Distance in meters.
     * @return Formatted distance as string.
     */
    fun formatDistance(context: Context, distance: Float) = when (distance < 1000.0f) {
        true -> context.getString(R.string.distance_meters, distance.toInt())
        false -> context.getString(R.string.distance_kilometers, distance / 1000.0f)
    }

    /**
     * Format the given duration in milliseconds to stopwatch compatible format.
     *
     * @param context The context to use for requesting a string resource.
     * @param ms Time in milliseconds to format.
     * @param withMillis Whether the format should be precise (with millis)
     * @return Formatted time as string.
     */
    fun formatDuration(context: Context, ms: Long, withMillis: Boolean = true): String {
        var remaining = ms
        val hours = TimeUnit.MILLISECONDS.toHours(remaining)
        remaining -= TimeUnit.HOURS.toMillis(hours)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(remaining)
        remaining -= TimeUnit.MINUTES.toMillis(minutes)
        val seconds = TimeUnit.MILLISECONDS.toSeconds(remaining)
        remaining -= TimeUnit.SECONDS.toMillis(seconds)
        remaining /= 10

        return when (withMillis) {
            true -> context.getString(R.string.duration_millis, hours, minutes, seconds, remaining)
            else -> context.getString(R.string.duration, hours, minutes, seconds)
        }
    }

    /**
     * Calculate the pace.
     *
     * pace (min/km) = duration (min) / distance (km).
     *
     * @param duration Duration in milliseconds.
     * @param distance Distance in meters.
     */
    fun calculatePace(duration: Long, distance: Float): Float =
        when (duration > 0L && distance > 0.0f) {
            true -> TimeUnit.MILLISECONDS.toMinutes(duration) / (distance / 1000.0f)
            false -> 0.0f
        }

    /**
     * Get the time in milliseconds formatted ans ISO-8601 with offset.
     *
     * @param ms Time in milliseconds to format.
     * @return Time formatted as string.
     */
    fun timeMillisToIsoOffset(ms: Long): String =
        OffsetDateTime.ofInstant(Instant.ofEpochMilli(ms), ZoneId.systemDefault()).format(
            DateTimeFormatter.ISO_OFFSET_DATE_TIME
        )
}