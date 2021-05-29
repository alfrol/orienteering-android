package ee.taltech.alfrol.hw02.utils

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.location.Location
import androidx.core.app.NotificationCompat
import androidx.navigation.NavDeepLinkBuilder
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.maps.model.LatLng
import ee.taltech.alfrol.hw02.C
import ee.taltech.alfrol.hw02.R
import ee.taltech.alfrol.hw02.service.LocationService
import java.util.concurrent.TimeUnit

object LocationUtils {

    /**
     * Construct a new [LocationRequest].
     */
    fun getLocationRequest(interval: Long, fastestInterval: Long): LocationRequest =
        LocationRequest.create().apply {
            this.interval = interval
            this.fastestInterval = fastestInterval
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }

    /**
     * Create a new notification for use in [LocationService].
     *
     * @param text The text to set as notification content text.
     * If null, the default text will be selected.
     * @see LocationUtils.getNotificationText
     */
    fun createNotification(context: Context, text: String? = null) =
        NotificationCompat.Builder(context, C.NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.text_total_statistics))
            .setContentText(text ?: getNotificationText(context, 0L, 0.0f, 0.0f))
            .setContentIntent(createNotificationIntent(context))
            .addAction(
                R.drawable.ic_checkpoint,
                context.getString(R.string.title_add_checkpoint),
                createActionPendingIntent(context, C.ACTION_ADD_CHECKPOINT)
            )
            .addAction(
                R.drawable.ic_waypoint,
                context.getString(R.string.title_add_waypoint),
                createActionPendingIntent(context, C.ACTION_ADD_WAYPOINT)
            )
            .build()

    /**
     * Construct a notification text.
     */
    fun getNotificationText(
        context: Context,
        duration: Long,
        distance: Float,
        averagePace: Float
    ): String {
        val durationText = UIUtils.formatDuration(context, duration, false)
        val distanceText = UIUtils.formatDistance(context, distance)
        val paceText = context.getString(R.string.pace, averagePace)
        return "$distanceText | $durationText | $paceText"
    }


    /**
     * Calculate the distance between two points.
     *
     * @param first Point one.
     * @param second Point two.
     * @return Distance between two points in meters.
     */
    fun calculateDistance(first: Location, second: Location): Float {
        val result = FloatArray(1)
        Location.distanceBetween(
            first.latitude,
            first.longitude,
            second.latitude,
            second.longitude,
            result
        )
        return result[0]
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
     * Map the list of [Location] objects to list of [LatLng] objects.
     */
    fun mapLocationToLatLng(location: List<Location>): MutableList<LatLng> =
        location.map { LatLng(it.latitude, it.longitude) }.toMutableList()

    /**
     * Create a new pending intent for the notification.
     */
    private fun createNotificationIntent(context: Context) =
        NavDeepLinkBuilder(context)
            .setGraph(R.navigation.nav_graph)
            .setDestination(R.id.sessionFragment)
            .createPendingIntent()

    /**
     * Create a new pending intent for the notification action.
     *
     * @param context Context to use for the pending intent.
     * @param action Action to use in the intent
     */
    private fun createActionPendingIntent(context: Context, action: String) =
        PendingIntent.getBroadcast(
            context,
            0,
            Intent(action),
            PendingIntent.FLAG_UPDATE_CURRENT
        )
}