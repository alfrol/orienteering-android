package ee.taltech.alfrol.hw02.service

import android.annotation.SuppressLint
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.location.Location
import android.location.LocationManager
import android.net.ConnectivityManager
import android.os.Build
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.lifecycle.*
import androidx.lifecycle.Observer
import androidx.navigation.NavDeepLinkBuilder
import com.android.volley.Request
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import dagger.hilt.android.AndroidEntryPoint
import ee.taltech.alfrol.hw02.C
import ee.taltech.alfrol.hw02.R
import ee.taltech.alfrol.hw02.api.AuthorizedJsonObjectRequest
import ee.taltech.alfrol.hw02.api.RestHandler
import ee.taltech.alfrol.hw02.data.SettingsManager
import ee.taltech.alfrol.hw02.data.model.LocationPoint
import ee.taltech.alfrol.hw02.data.model.Session
import ee.taltech.alfrol.hw02.data.repositories.LocationPointRepository
import ee.taltech.alfrol.hw02.data.repositories.SessionRepository
import ee.taltech.alfrol.hw02.ui.fragments.SessionFragment
import ee.taltech.alfrol.hw02.ui.utils.UIUtils
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import java.util.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@AndroidEntryPoint
class LocationService : LifecycleService() {

    companion object {
        val isRunning = MutableLiveData<Boolean>()
        val pathPoints = MutableLiveData<MutableList<Location>>()
        val checkpoints = MutableLiveData<MutableList<Location>>()
        val waypoint = MutableLiveData<Location?>()
        val currentLocation = MutableLiveData<Location>()

        val totalDistance = MutableLiveData<Float>()
        val totalAveragePace = MutableLiveData<Float>()

        val checkpointDistance = MutableLiveData<Float>()
        val checkpointAveragePace = MutableLiveData<Float>()

        val waypointDistance = MutableLiveData<Float>()
        val waypointAveragePace = MutableLiveData<Float>()

        private const val MAX_RETRIES = 3

        // In meters
        private const val LOCATION_DISTANCE_THRESHOLD = 50.0f
        private const val LOCATION_ACCURACY_THRESHOLD = 15.0f
    }

    @Inject
    lateinit var settingsManager: SettingsManager

    @Inject
    lateinit var sessionRepository: SessionRepository

    @Inject
    lateinit var locationPointRepository: LocationPointRepository

    @Inject
    lateinit var restHandler: RestHandler

    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var notificationManager: NotificationManager
    private lateinit var connectivityManager: ConnectivityManager
    private lateinit var session: Session

    private var duration = 0L
    private var checkpointDuration = 0L
    private var waypointDuration = 0L
    private var lastWholeSecond = 0L

    override fun onCreate() {
        super.onCreate()
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            when (it.action) {
                C.ACTION_START_SERVICE -> startService()
                C.ACTION_STOP_SERVICE -> stopService()
                C.ACTION_GET_CURRENT_LOCATION -> getCurrentLocation()
                C.ACTION_ADD_CHECKPOINT -> addNewCheckpoint()
                C.ACTION_ADD_WAYPOINT -> addNewWaypoint()
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        super.onDestroy()
        fusedLocationProviderClient.removeLocationUpdates(callback)
    }

    private fun postInitialValues() {
        pathPoints.value = mutableListOf()
        checkpoints.value = mutableListOf()
        waypoint.value = null

        totalDistance.value = 0.0f
        totalAveragePace.value = 0.0f

        checkpointDistance.value = 0.0f
        checkpointAveragePace.value = 0.0f

        waypointDistance.value = 0.0f
        waypointAveragePace.value = 0.0f
    }

    private fun startService() {
        runBlocking {
            val rawSession = Session()
            val id = sessionRepository.insertSession(rawSession)

            // All other values remain default
            session = Session(id = id, recordedAt = rawSession.recordedAt)
        }
        saveSessionToBackend()

        postInitialValues()
        startForeground(C.NOTIFICATION_ID, createNotification())
        requestLocationUpdates()
        startObserving()
        isRunning.value = true
    }

    private fun stopService() {
        fusedLocationProviderClient.removeLocationUpdates(callback)
        updateSessionInDb()
        isRunning.value = false
        postInitialValues()
        stopForeground(true)
        stopSelf()
    }

    private fun startObserving() {
        checkpoints.observe(this, checkpointsObserver)
        waypoint.observe(this, waypointObserver)

        StopwatchService.total.observe(this, stopwatchObserver)
        StopwatchService.checkpoint.observe(this, checkpointStopwatchObserver)
        StopwatchService.waypoint.observe(this, waypointStopwatchObserver)
    }

    /**
     * Observer for changes in [checkpoints].
     * Used for saving the last checkpoint when updated.
     */
    private val checkpointsObserver = Observer<MutableList<Location>> {
        it?.let { ckpts ->
            if (ckpts.isNotEmpty()) {
                val lastCheckpoint = ckpts.last()
                saveLocation(lastCheckpoint, C.CP_TYPE_ID)
            }
        }
    }

    /**
     * Observer for changes in [waypoint].
     * Used for saving the waypoint when it's updated.
     */
    private val waypointObserver = Observer<Location?> {
        it?.let { wp ->
            saveLocation(wp, C.WP_TYPE_ID)
        }
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
     * Observer for changes in the checkpoint duration.
     * Only needed for calculating the pace from the last checkpoint.
     */
    private val checkpointStopwatchObserver = Observer<Long> {
        checkpointDuration = it
    }

    /**
     * Observer for changes in the waypoint duration.
     * Only needed for calculating the pace from the waypoint.
     */
    private val waypointStopwatchObserver = Observer<Long> {
        waypointDuration = it
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
                    currentLocation.value = it
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
            val location = locationResult.lastLocation ?: return

            // Validate whether the new location is good enough.
            if (location.provider !in arrayOf("fused", LocationManager.GPS_PROVIDER)) {
                return
            }
            if (location.hasAccuracy() && location.accuracy > LOCATION_ACCURACY_THRESHOLD) {
                return
            }
            if (pathPoints.value?.isNotEmpty() == true) {
                val lastPoint = pathPoints.value?.last()
                if (lastPoint != null) {
                    val distance = UIUtils.calculateDistance(lastPoint, location)

                    if (distance > LOCATION_DISTANCE_THRESHOLD) {
                        return
                    }
                }
            }

            addPathPoint(location)
            saveLocation(location, C.LOC_TYPE_ID)
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
    private fun addPathPoint(location: Location) {
        pathPoints.value?.apply {
            add(location)
            pathPoints.value = this

            updateDistance()
            updatePace()

            updateCheckpointDistance()
            updateCheckpointPace()

            updateWaypointDistance()
            updateWaypointPace()
        }
    }

    /**
     * Add a new checkpoint to the checkpoints list.
     */
    private fun addNewCheckpoint() {
        if (isRunning.value != true) {
            return
        }

        pathPoints.value?.let { points ->
            if (points.isNotEmpty()) {
                val last = points.last()

                checkpoints.value = checkpoints.value?.apply {
                    add(last)
                } ?: mutableListOf(last)

                val location = Location("").apply {
                    latitude = last.latitude
                    longitude = last.longitude
                }
                saveLocation(location, C.CP_TYPE_ID)
            }
        }
    }

    /**
     * Set a new waypoint.
     *
     * @param location Waypoint location.
     */
    private fun addNewWaypoint() {
        if (isRunning.value != true) {
            return
        }

        pathPoints.value?.let { points ->
            if (points.isNotEmpty()) {
                val last = points.last()

                waypoint.value = last

                val location = Location("").apply {
                    latitude = last.latitude
                    longitude = last.longitude
                }
                saveLocation(location, C.WP_TYPE_ID)
            }
        }
    }

    /**
     * Try to save the new session to the backend.
     * Doesn't do anything on failure.
     */
    private fun saveSessionToBackend() {
        lifecycleScope.launchWhenCreated {
            val token = settingsManager.token.firstOrNull() ?: return@launchWhenCreated

            val requestBody = JSONObject().apply {
                put(C.JSON_NAME_KEY, session.name)
                put(C.JSON_DESCRIPTION_KEY, session.description)
                put(C.JSON_RECORDED_AT_KEY, session.recordedAtIso)
                put(C.JSON_PACE_MIN_KEY, 420)
                put(C.JSON_PACE_MAX_KEY, 600)
            }

            val sessionRequest = AuthorizedJsonObjectRequest(
                Request.Method.POST, C.API_SESSIONS_URL, requestBody,
                { response ->
                    val externalId = response.getString(C.JSON_ID_KEY)
                    session = Session(
                        id = session.id,
                        externalId = externalId,
                        recordedAt = session.recordedAt
                    )

                    // Save backend session id for later use
                    lifecycleScope.launch {
                        sessionRepository.updateSession(session)
                    }
                },
                {},
                token
            )
            restHandler.addRequest(sessionRequest)
        }
    }

    /**
     * Update session in the database.
     */
    private fun updateSessionInDb() {
        val updatedSession = Session(
            id = session.id,
            externalId = session.externalId,
            recordedAt = session.recordedAt,
            distance = totalDistance.value ?: 0.0f,
            duration = duration,
            pace = totalAveragePace.value ?: 0.0f
        )

        lifecycleScope.launchWhenCreated {
            sessionRepository.updateSession(updatedSession)
        }
    }

    /**
     * Try to save the location to database and the backend server.
     *
     * @param location Location to save.
     * @param type Type of the location. One of the [C.LOC_TYPE_ID], [C.WP_TYPE_ID], [C.CP_TYPE_ID].
     */
    @SuppressLint("NewApi")
    fun saveLocation(location: Location, type: String) {
        val recordedAt = UIUtils.timeMillisToIsoOffset(location.time)

        // Don't save waypoints to database.
        if (type != C.WP_TYPE_ID) {
            val locationPoint = LocationPoint(
                sessionId = session.id,
                recordedAt = location.time,
                latitude = location.latitude,
                longitude = location.longitude,
                altitude = location.altitude,
                accuracy = location.accuracy,
                type = type
            )
            saveLocationToDb(locationPoint)
        }

        val locationJson = JSONObject().apply {
            put(C.JSON_RECORDED_AT_KEY, recordedAt)
            put(C.JSON_LATITUDE_KEY, location.latitude)
            put(C.JSON_LONGITUDE_KEY, location.longitude)
            put(C.JSON_GPS_SESSION_ID, session.externalId)
            put(C.JSON_GPS_LOCATION_TYPE_ID, type)

            // Only save altitude and accuracy in case of regular location point
            if (type == C.LOC_TYPE_ID) {
                put(C.JSON_ALTITUDE_KEY, location.altitude)
                put(C.JSON_ACCURACY_KEY, location.accuracy)

                val verticalAcc = when (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    true -> location.verticalAccuracyMeters
                    false -> location.accuracy
                }
                put(C.JSON_VERTICAL_ACCURACY, verticalAcc)
            }
        }
        saveLocationToBackend(locationJson)
    }

    /**
     * Save the given location to the db.
     *
     * @param location The location to save to the db.
     */
    private fun saveLocationToDb(location: LocationPoint) {
        lifecycleScope.launchWhenCreated {
            locationPointRepository.insertLocationPoint(location)
        }
    }

    /**
     * Save the given location to the backend server.
     *
     * @param location The location to send to the backend.
     */
    private fun saveLocationToBackend(location: JSONObject) {
        lifecycleScope.launchWhenCreated {
            val token = settingsManager.token.firstOrNull() ?: return@launchWhenCreated

            val locationRequest = AuthorizedJsonObjectRequest(
                Request.Method.POST, C.API_LOCATIONS_URL, location,
                {},
                {
                    // TODO: Do something when saving fails
                },
                token
            )
            restHandler.addRequest(locationRequest)
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
            val distance = UIUtils.calculateDistance(last, preLast)

            totalDistance.value = totalDistance.value?.plus(distance) ?: 0.0f
        }
    }

    /**
     * Update the average pace.
     */
    private fun updatePace() {
        totalDistance.value?.let {
            totalAveragePace.value = UIUtils.calculatePace(duration, it)
        }
    }

    /**
     * Update distance from the last checkpoint.
     */
    private fun updateCheckpointDistance() {
        checkpoints.value?.let { ckpts ->
            pathPoints.value?.let { points ->
                if (ckpts.isNotEmpty() && points.isNotEmpty()) {
                    val lastPoint = points.last()
                    val lastCheckpoint = ckpts.last()
                    val distance = UIUtils.calculateDistance(lastPoint, lastCheckpoint)

                    checkpointDistance.value = distance
                }
            }
        }
    }

    /**
     * Update average pace from the last checkpoint.
     */
    private fun updateCheckpointPace() {
        checkpointDistance.value?.let {
            checkpointAveragePace.value = UIUtils.calculatePace(checkpointDuration, it)
        }
    }

    /**
     * Update distance from the waypoint.
     */
    private fun updateWaypointDistance() {
        waypoint.value?.let { wp ->
            pathPoints.value?.let { points ->
                if (points.isNotEmpty()) {
                    val lastPoint = points.last()
                    val distance = UIUtils.calculateDistance(lastPoint, wp)

                    waypointDistance.value = distance
                }
            }
        }
    }

    /**
     * Update average pace from the waypoint.
     */
    private fun updateWaypointPace() {
        waypointDistance.value?.let {
            waypointAveragePace.value = UIUtils.calculatePace(waypointDuration, it)
        }
    }

    /**
     * Create a new notification.
     */
    private fun createNotification(text: String = "") =
        NotificationCompat.Builder(this, C.NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(getString(R.string.text_total_statistics))
            .setContentText(text)
            .setContentIntent(createPendingIntent())
            .addAction(
                R.drawable.ic_checkpoint,
                getString(R.string.title_add_checkpoint),
                createActionPendingIntent(C.ACTION_ADD_CHECKPOINT)
            )
            .addAction(
                R.drawable.ic_waypoint,
                getString(R.string.title_add_waypoint),
                createActionPendingIntent(C.ACTION_ADD_WAYPOINT)
            )
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
     * Create a pending intent for use in notification as action.
     *
     * The intent is constructed such that when called, it will start
     * this service with the necessary action.
     *
     * @param action Expected to be one of the [C.ACTION_ADD_CHECKPOINT] or [C.ACTION_ADD_WAYPOINT].
     */
    private fun createActionPendingIntent(action: String) = PendingIntent.getService(
        this,
        0,
        Intent(this, this::class.java).apply {
            this.action = action
        },
        PendingIntent.FLAG_UPDATE_CURRENT
    )

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