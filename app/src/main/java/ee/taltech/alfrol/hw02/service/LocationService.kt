package ee.taltech.alfrol.hw02.service

import android.annotation.SuppressLint
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.lifecycle.*
import androidx.navigation.NavDeepLinkBuilder
import com.android.volley.Request
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.LatLng
import dagger.hilt.android.AndroidEntryPoint
import ee.taltech.alfrol.hw02.C
import ee.taltech.alfrol.hw02.R
import ee.taltech.alfrol.hw02.api.AuthorizedJsonObjectRequest
import ee.taltech.alfrol.hw02.api.RestHandler
import ee.taltech.alfrol.hw02.data.SettingsManager
import ee.taltech.alfrol.hw02.data.dao.SessionDao
import ee.taltech.alfrol.hw02.data.model.Session
import ee.taltech.alfrol.hw02.ui.fragments.SessionFragment
import ee.taltech.alfrol.hw02.ui.utils.UIUtils
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@AndroidEntryPoint
class LocationService : LifecycleService() {

    companion object {
        val isRunning = MutableLiveData(false)
        val pathPoints = MutableLiveData<MutableList<LatLng>>(mutableListOf())
        val checkpoints = MutableLiveData<MutableList<LatLng>>(mutableListOf())
        val waypoint = MutableLiveData<LatLng>()
        val currentLocation = MutableLiveData<LatLng>()

        val totalDistance = MutableLiveData(0.0f)
        val totalAveragePace = MutableLiveData(0.0f)

        val checkpointDistance = MutableLiveData(0.0f)
        val checkpointAveragePace = MutableLiveData(0.0f)

        val waypointDistance = MutableLiveData(0.0f)
        val waypointAveragePace = MutableLiveData(0.0f)

        private const val MAX_RETRIES = 3

        /**
         * Add a new checkpoint to the checkpoints list.
         *
         * @param location Checkpoint location.
         */
        fun addNewCheckpoint(location: LatLng) {
            checkpoints.value = checkpoints.value?.apply {
                add(location)
            } ?: mutableListOf(location)
        }

        /**
         * Set a new waypoint.
         *
         * @param location Waypoint location.
         */
        fun addNewWaypoint(location: LatLng) {
            waypoint.value = location
        }
    }

    @Inject
    lateinit var settingsManager: SettingsManager

    @Inject
    lateinit var sessionDao: SessionDao

    @Inject
    lateinit var restHandler: RestHandler

    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var notificationManager: NotificationManager

    private var duration = 0L
    private var lastWholeSecond = 0L

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            when (it.action) {
                C.ACTION_START_SERVICE -> startService()
                C.ACTION_STOP_SERVICE -> stopService()
                C.ACTION_GET_CURRENT_LOCATION -> getCurrentLocation()
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        super.onDestroy()
        fusedLocationProviderClient.removeLocationUpdates(callback)
        StopwatchService.total.removeObserver(stopwatchObserver)
    }

    private fun startService() {
        val session: Session

        runBlocking {
            session = Session()
            sessionDao.insert(session)
        }
        // TODO: Uncomment this
        //saveSessionToBackend(session)

        isRunning.value = true
        startForeground(C.NOTIFICATION_ID, createNotification())
        requestLocationUpdates()
        StopwatchService.total.observeForever(stopwatchObserver)
    }

    private fun stopService() {
        lifecycleScope.launchWhenCreated {
            settingsManager.removeSessionId()
        }

        isRunning.value = false
        stopSelf()
        fusedLocationProviderClient.removeLocationUpdates(callback)
        StopwatchService.total.removeObserver(stopwatchObserver)
    }

    /**
     * Observer for changes in the total duration.
     * Updates the notification with the new duration,
     * but does that only each second.
     */
    private val stopwatchObserver = Observer<Long> {
        if (it >= lastWholeSecond + TimeUnit.SECONDS.toMillis(1)) {
            lastWholeSecond = it
            val notificationText = getNotificationText()
            val notification = createNotification(notificationText)
            notificationManager.notify(C.NOTIFICATION_ID, notification)
        }

        // Save for calculating the pace
        duration = it
    }

    /**
     * Try to save the new session to the backend.
     * Doesn't do anything on failure.
     */
    private fun saveSessionToBackend(session: Session) {
        lifecycleScope.launchWhenCreated {
            val token = settingsManager.token.firstOrNull() ?: return@launchWhenCreated

            val requestBody = JSONObject()
            requestBody.put(C.JSON_NAME_KEY, session.name)
            requestBody.put(C.JSON_DESCRIPTION_KEY, session.description)
            requestBody.put(C.JSON_RECORDED_AT_KEY, session.recordedAtIso)
            requestBody.put(C.JSON_PACE_MIN_KEY, 420)
            requestBody.put(C.JSON_PACE_MAX_KEY, 600)

            val sessionRequest = AuthorizedJsonObjectRequest(
                Request.Method.POST, C.API_SESSIONS_URL, requestBody,
                { response ->
                    val id = response.getString(C.JSON_ID_KEY)

                    // Save backend session id for later use
                    lifecycleScope.launch {
                        settingsManager.saveSessionId(id)
                    }
                },
                {},
                token
            )
            restHandler.addRequest(sessionRequest)
        }
    }

    /**
     * Request the current location and notify observers.
     *
     * @param retries The number of times to try to query the location
     * if previous request fails.
     */
    @SuppressLint("MissingPermission")
    private fun getCurrentLocation(retries: Int = 0) {
        if (retries == MAX_RETRIES) {
            return
        }

        if (UIUtils.hasLocationPermission(this)) {
            fusedLocationProviderClient.lastLocation.addOnSuccessListener {
                it?.apply {
                    val latLng = LatLng(latitude, longitude)
                    currentLocation.value = latLng
                }
            }

            fusedLocationProviderClient.lastLocation.addOnFailureListener {
                if (currentLocation.value == null) {
                    getCurrentLocation(retries + 1)
                } else {
                    currentLocation.value = currentLocation.value
                }
            }
        }
    }

    /**
     * Callback for receiving and processing the location updates.
     */
    private val callback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            addPoint(locationResult.lastLocation)
        }
    }

    /**
     * Start requesting the location updates.
     */
    @SuppressLint("MissingPermission")
    private fun requestLocationUpdates() {
        if (UIUtils.hasLocationPermission(this)) {
            val interval: Long
            val fastestInterval: Long

            runBlocking {
                interval = settingsManager.locationUpdateInterval.first()
                fastestInterval = settingsManager.locationUpdateFastestInterval.first()
            }

            val request = UIUtils.getLocationRequest(interval, fastestInterval)

            fusedLocationProviderClient.requestLocationUpdates(
                request,
                callback,
                Looper.getMainLooper()
            )
        }
    }

    /**
     * Add a new location point to the path point list.
     */
    private fun addPoint(location: Location?) {
        location?.let {
            val latLng = LatLng(it.latitude, it.longitude)
            pathPoints.value?.apply {
                add(latLng)
                pathPoints.value = this

                updateDistance()
                updatePace()
            }
        }
    }

    /**
     * Update the total tracked distance.
     */
    private fun updateDistance() {
        pathPoints.value?.let {
            if (it.size < 2) {
                return@let
            }

            val last = it.last()
            val preLast = it[it.lastIndex - 1]

            val result = FloatArray(1)
            Location.distanceBetween(
                preLast.latitude,
                preLast.longitude,
                last.latitude,
                last.longitude,
                result
            )

            totalDistance.value = totalDistance.value?.plus(result[0]) ?: 0.0f
        }
    }

    /**
     * Update the average pace.
     */
    private fun updatePace() {
        totalDistance.value?.let {
            if (it > 0.0f && duration > 0L) {
                totalAveragePace.value = TimeUnit.MILLISECONDS.toMinutes(duration) / (it / 1000.0f)
            }
        }
    }

    /**
     * Create a new notification.
     */
    private fun createNotification(text: String = "") =
        NotificationCompat.Builder(this, C.NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(text)
            .setContentIntent(createPendingIntent())
            .build()

    /**
     * Create a pending intent for use in notification.
     * It will be used for opening the [SessionFragment]
     * when the user clicks on the notification.
     */
    private fun createPendingIntent() = NavDeepLinkBuilder(this)
        .setGraph(R.navigation.nav_graph)
        .setDestination(R.id.sessionFragment)
        .createPendingIntent()

    /**
     * Construct a notification text.
     */
    private fun getNotificationText(): String {
        val durationText = UIUtils.formatDuration(this, lastWholeSecond, false)
        val distanceText = UIUtils.formatDistance(this, totalDistance.value ?: 0.0f)
        val paceText = getString(R.string.pace, totalAveragePace.value ?: 0.0f)
        return "$distanceText | $durationText | $paceText"
    }
}