package ee.taltech.alfrol.hw02.service

import android.annotation.SuppressLint
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.location.Location
import android.os.Build
import android.os.Looper
import androidx.lifecycle.*
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.android.volley.Request
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import dagger.hilt.android.AndroidEntryPoint
import ee.taltech.alfrol.hw02.C
import ee.taltech.alfrol.hw02.api.AuthorizedJsonObjectRequest
import ee.taltech.alfrol.hw02.api.RestHandler
import ee.taltech.alfrol.hw02.data.SettingsManager
import ee.taltech.alfrol.hw02.data.model.LocationPoint
import ee.taltech.alfrol.hw02.data.model.Session
import ee.taltech.alfrol.hw02.data.repositories.LocationPointRepository
import ee.taltech.alfrol.hw02.data.repositories.SessionRepository
import ee.taltech.alfrol.hw02.utils.LocationUtils
import ee.taltech.alfrol.hw02.utils.PermissionUtils
import ee.taltech.alfrol.hw02.utils.UIUtils
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@AndroidEntryPoint
class LocationService : LifecycleService() {

    companion object {
        val isTracking = MutableLiveData<Boolean>()
        val currentLocation = MutableLiveData<Location>()
        val pathPoints = MutableLiveData<MutableList<Location>>()
        val checkpoints = MutableLiveData<MutableList<Location>>()
        val waypoint = MutableLiveData<Location?>()

        val totalDistance = MutableLiveData<Float>()
        val totalAveragePace = MutableLiveData<Float>()

        val checkpointDistance = MutableLiveData<Float>()
        val checkpointAveragePace = MutableLiveData<Float>()

        val waypointDistance = MutableLiveData<Float>()
        val waypointAveragePace = MutableLiveData<Float>()


        // In meters
        private const val MAX_ACCURACY = 30.0f
        private const val MAX_DISTANCE = 50.0f
        private const val MIN_DISTANCE = 3.0f
    }

    @Inject
    lateinit var restHandler: RestHandler

    @Inject
    lateinit var settingsManager: SettingsManager

    @Inject
    lateinit var sessionRepository: SessionRepository

    @Inject
    lateinit var locationPointRepository: LocationPointRepository

    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var notificationManager: NotificationManager

    private var duration = 0L
    private var checkpointDuration = 0L
    private var waypointDuration = 0L
    private var lastWholeSecond = 0L

    private var activeSession: Session? = null

    private val intentFilter = IntentFilter().apply {
        addAction(C.ACTION_ADD_CHECKPOINT)
        addAction(C.ACTION_ADD_WAYPOINT)
    }
    private val locationActionReceiver = LocationActionReceiver()

    override fun onCreate() {
        super.onCreate()
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            when (it.action) {
                C.ACTION_START_SERVICE -> startTracking()
                C.ACTION_STOP_SERVICE -> stopTracking()
                C.ACTION_GET_CURRENT_LOCATION -> getCurrentLocation()
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isTracking.value == true) {
            stopTracking()
        }
    }

    /**
     * Post the initial values for the livedata.
     */
    private fun postInitialValues() {
        isTracking.postValue(false)
        pathPoints.postValue(mutableListOf())
        checkpoints.postValue(mutableListOf())
        waypoint.postValue(null)

        totalDistance.postValue(0.0f)
        totalAveragePace.postValue(0.0f)
        duration = 0L
        lastWholeSecond = 0L

        checkpointDistance.postValue(0.0f)
        checkpointAveragePace.postValue(0.0f)
        checkpointDuration = 0L

        waypointDistance.postValue(0.0f)
        waypointAveragePace.postValue(0.0f)
        waypointDuration = 0L
    }

    /**
     * Start observing for [StopwatchService] to get the durations for calculations.
     */
    private fun startObserving() {
        StopwatchService.total.observe(this, stopwatchObserver)
        StopwatchService.checkpoint.observe(this, Observer {
            waypointDuration = it ?: 0L
        })
        StopwatchService.waypoint.observe(this, Observer {
            checkpointDuration = it ?: 0L
        })
    }

    /**
     * Observer for changes in the total duration.
     * Updates the notification with the new duration,
     * but does that only each second.
     */
    private val stopwatchObserver = Observer<Long> {
        if (it >= lastWholeSecond + TimeUnit.SECONDS.toMillis(1)) {
            lastWholeSecond = it
            val notificationText = LocationUtils.getNotificationText(
                this,
                lastWholeSecond,
                totalDistance.value ?: 0.0f,
                totalAveragePace.value ?: 0.0f
            )
            val notification = LocationUtils.createNotification(this, notificationText)
            notificationManager.notify(C.NOTIFICATION_ID, notification)
        }

        // Save for calculating the pace
        duration = it
    }


    /**
     * Start tracking the device location.
     */
    private fun startTracking() {
        isTracking.postValue(true)
        createSession()
        startObserving()
        requestLocationUpdates()
        registerLocationActionReceiver()
        startForeground(C.NOTIFICATION_ID, LocationUtils.createNotification(this))
    }

    /**
     * Stop tracking the device location.
     */
    private fun stopTracking() {
        updateStoppedSession()
        postInitialValues()
        fusedLocationProviderClient.removeLocationUpdates(callback)
        unregisterLocationActionReceiver()
        stopForeground(true)
        stopSelf()
    }

    /**
     * Register the [LocationActionReceiver] for both outside and local broadcasts.
     */
    private fun registerLocationActionReceiver() {
        registerReceiver(locationActionReceiver, intentFilter)
        LocalBroadcastManager.getInstance(this)
            .registerReceiver(locationActionReceiver, intentFilter)
    }

    /**
     * Unregister the [LocationActionReceiver] from both outside and local broadcasts.
     */
    private fun unregisterLocationActionReceiver() {
        unregisterReceiver(locationActionReceiver)
        LocalBroadcastManager.getInstance(this)
            .unregisterReceiver(locationActionReceiver)
    }

    /**
     * Get current user location.
     */
    @SuppressLint("MissingPermission")
    private fun getCurrentLocation() {
        if (!PermissionUtils.hasLocationPermission(this)) {
            return
        }

        val isTrackingValue = isTracking.value == true
        val pathPointsValue = pathPoints.value ?: mutableListOf()

        // When tracking then current location is essentially the last location in path points list
        if (isTrackingValue && pathPointsValue.isNotEmpty()) {
            currentLocation.postValue(pathPointsValue.last())
        } else {
            fusedLocationProviderClient.lastLocation.addOnSuccessListener {
                currentLocation.postValue(it)
            }
        }
    }

    /**
     * Start requesting location updates.
     */
    @SuppressLint("MissingPermission")
    private fun requestLocationUpdates() {
        if (!PermissionUtils.hasLocationPermission(this)) {
            return
        }

        val interval: Long
        val fastestInterval: Long

        runBlocking {
            interval = settingsManager.getValue(
                SettingsManager.LOCATION_UPDATE_INTERVAL_LEY,
                C.DEFAULT_LOCATION_UPDATE_INTERVAL
            ).first()!!
            fastestInterval = settingsManager.getValue(
                SettingsManager.LOCATION_UPDATE_FASTEST_INTERVAL_LEY,
                C.DEFAULT_LOCATION_UPDATE_FASTEST_INTERVAL
            ).first()!!
        }

        val locationRequest = LocationUtils.getLocationRequest(interval, fastestInterval)
        fusedLocationProviderClient.requestLocationUpdates(
            locationRequest,
            callback,
            Looper.getMainLooper()
        )
    }

    /**
     * Callback for receiving the location updates.
     */
    private val callback = object : LocationCallback() {

        override fun onLocationResult(locationResult: LocationResult) {
            val location = locationResult.lastLocation ?: return

            val pathPointsValue = pathPoints.value ?: mutableListOf()
            if (pathPointsValue.isEmpty()) {
                addPoint(pathPoints, location)
                saveLocationPoint(location, C.LOC_TYPE_ID)
                return
            }

            val distance = LocationUtils.calculateDistance(location, pathPointsValue.last())

            // Make some filtering of undesired points.
            if (location.accuracy > MAX_ACCURACY) {
                return
            }
            if (distance > MAX_DISTANCE || distance < MIN_DISTANCE) {
                return
            }
            if (location.provider != C.PROVIDER_FUSED) {
                return
            }

            addPoint(pathPoints, location)
            saveLocationPoint(location, C.LOC_TYPE_ID)
        }
    }

    /**
     * Add a new point to the list.
     *
     * @param points Points list where to add the new one.
     * @param location New location point to add.
     */
    private fun addPoint(points: MutableLiveData<MutableList<Location>>, location: Location) {
        points.value?.apply {
            add(location)
            points.postValue(this)
        } ?: points.postValue(mutableListOf(location))
        updateData()
    }

    /**
     * Update all distances and pace values.
     */
    private fun updateData() {
        updateDistance()
        updatePace()

        updateCheckpointDistance()
        updateCheckpointPace()

        updateWaypointDistance()
        updateWaypointPace()
    }

    /**
     * Update the total tracked distance.
     */
    private fun updateDistance() = pathPoints.value?.let {
        if (it.size < 2) {
            return@let
        }

        val last = it.last()
        val preLast = it[it.lastIndex - 1]
        val distance = LocationUtils.calculateDistance(last, preLast)

        totalDistance.value = totalDistance.value?.plus(distance) ?: 0.0f
    }

    /**
     * Update the average pace.
     */
    private fun updatePace() = totalDistance.value?.let {
        totalAveragePace.value = LocationUtils.calculatePace(duration, it)
    }

    /**
     * Update distance from the last checkpoint.
     */
    private fun updateCheckpointDistance() = checkpoints.value?.let { ckpts ->
        pathPoints.value?.let { points ->
            if (ckpts.isNotEmpty() && points.isNotEmpty()) {
                val lastPoint = points.last()
                val lastCheckpoint = ckpts.last()
                val distance = LocationUtils.calculateDistance(lastPoint, lastCheckpoint)

                checkpointDistance.value = distance
            }
        }
    }

    /**
     * Update average pace from the last checkpoint.
     */
    private fun updateCheckpointPace() = checkpointDistance.value?.let {
        checkpointAveragePace.value = LocationUtils.calculatePace(checkpointDuration, it)
    }

    /**
     * Update distance from the waypoint.
     */
    private fun updateWaypointDistance() = waypoint.value?.let { wp ->
        pathPoints.value?.let { points ->
            if (points.isNotEmpty()) {
                val lastPoint = points.last()
                val distance = LocationUtils.calculateDistance(lastPoint, wp)

                waypointDistance.value = distance
            }
        }
    }

    /**
     * Update average pace from the waypoint.
     */
    private fun updateWaypointPace() = waypointDistance.value?.let {
        waypointAveragePace.value = LocationUtils.calculatePace(waypointDuration, it)
    }

    /**
     * Create and save a new session.
     */
    private fun createSession() {
        runBlocking {
            val session = Session()
            val id = sessionRepository.insertSession(session)
            activeSession = Session(id, recordedAt = session.recordedAt)

            // Persist the session id until it's stopped
            settingsManager.setValue(SettingsManager.ACTIVE_SESSION_ID_KEY, id)
        }

        lifecycleScope.launch {
            createSessionInBackend()
        }
    }

    /**
     * Update the stopped session details in the db.
     */
    private fun updateStoppedSession() {
        val session = activeSession ?: return

        lifecycleScope.launch {
            val updatedSession = Session(
                id = session.id,
                externalId = session.externalId,
                recordedAt = session.recordedAt,
                distance = totalDistance.value ?: 0.0f,
                duration = duration,
                pace = totalAveragePace.value ?: 0.0f
            )
            sessionRepository.updateSession(updatedSession)

            settingsManager.removeValue(SettingsManager.ACTIVE_SESSION_ID_KEY)
        }
    }

    /**
     * Try to create a new session in the backend.
     */
    private suspend fun createSessionInBackend() {
        val token = settingsManager.getValue(SettingsManager.TOKEN_KEY, null).first() ?: return
        val session = activeSession ?: return
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
                // Save the id of the session that is used in the backend server.
                val externalId = response.getString(C.JSON_ID_KEY)
                activeSession =
                    Session(id = session.id, externalId, recordedAt = session.recordedAt)

                lifecycleScope.launch {
                    sessionRepository.updateSession(activeSession!!)
                }
            },
            {
                // TODO: Do something here?
            },
            token
        )
        restHandler.addRequest(sessionRequest)
    }

    /**
     * Save the location point to the database and try to sync to the backend.
     *
     * @param location Location point to save.
     * @param type One of the [C.LOC_TYPE_ID], [C.WP_TYPE_ID], [C.CP_TYPE_ID].
     */
    fun saveLocationPoint(location: Location, type: String) {
        lifecycleScope.launch {
            val session = activeSession ?: return@launch
            val token =
                settingsManager.getValue(SettingsManager.TOKEN_KEY, null).first() ?: return@launch

            if (type != C.WP_TYPE_ID) {
                saveLocationToDb(session.id, location, type)
            }
            saveLocationToBackend(session.externalId, token, location, type)
        }
    }

    /**
     * Save the given location point to the database.
     *
     * @param sessionId ID of the session to associate this point with.
     * @param location Location that represents the point to save.
     * @param type Type of the location point. One of the [C.LOC_TYPE_ID], [C.CP_TYPE_ID].
     */
    private fun saveLocationToDb(sessionId: Long, location: Location, type: String) {
        val locationPoint = LocationPoint(
            sessionId = sessionId,
            recordedAt = location.time,
            latitude = location.latitude,
            longitude = location.longitude,
            altitude = location.altitude,
            accuracy = location.accuracy,
            type = type
        )

        lifecycleScope.launch {
            locationPointRepository.insertLocationPoint(locationPoint)
        }
    }

    /**
     * Save the given location point to the backend server.
     *
     * @param sessionExternalId Backend ID of the session to associate this point with.
     * @param location Location that represents the point to save.
     * @param type Type of the location point. One of the [C.LOC_TYPE_ID], [C.CP_TYPE_ID].
     */
    @SuppressLint("NewApi")
    private fun saveLocationToBackend(
        sessionExternalId: String?,
        token: String,
        location: Location,
        type: String
    ) {
        val recordedAt = UIUtils.timeMillisToIsoOffset(location.time)
        val requestBody = JSONObject().apply {
            put(C.JSON_RECORDED_AT_KEY, recordedAt)
            put(C.JSON_LATITUDE_KEY, location.latitude)
            put(C.JSON_LONGITUDE_KEY, location.longitude)
            put(C.JSON_GPS_SESSION_ID, sessionExternalId)
            put(C.JSON_GPS_LOCATION_TYPE_ID, type)
            put(C.JSON_ALTITUDE_KEY, location.altitude)
            put(C.JSON_ACCURACY_KEY, location.accuracy)

            // Vertical accuracy is only available on devices with version O or higher.
            val verticalAccuracy = when (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                true -> location.verticalAccuracyMeters
                false -> location.accuracy
            }
            put(C.JSON_VERTICAL_ACCURACY, verticalAccuracy)
        }

        val sessionRequest = AuthorizedJsonObjectRequest(
            Request.Method.POST, C.API_LOCATIONS_URL, requestBody,
            { },
            {
                // TODO: Do something here?
            },
            token
        )
        restHandler.addRequest(sessionRequest)
    }

    /**
     * Broadcast receiver for the service notification actions.
     */
    inner class LocationActionReceiver : BroadcastReceiver() {

        override fun onReceive(context: Context?, intent: Intent?) {
            intent?.let {
                val lastPoint = pathPoints.value?.last() ?: return

                when (it.action) {
                    C.ACTION_ADD_CHECKPOINT -> addPoint(
                        checkpoints,
                        lastPoint
                    ).also { saveLocationPoint(lastPoint, C.CP_TYPE_ID) }
                    C.ACTION_ADD_WAYPOINT -> waypoint.postValue(lastPoint)
                }
            }
        }
    }
}