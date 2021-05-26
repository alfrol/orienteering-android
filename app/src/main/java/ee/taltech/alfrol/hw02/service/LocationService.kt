package ee.taltech.alfrol.hw02.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
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
import javax.inject.Inject

@AndroidEntryPoint
class LocationService : LifecycleService() {

    companion object {
        val isRunning = MutableLiveData(false)
        val pathPoints = MutableLiveData<MutableList<LatLng>>(mutableListOf())
        val currentLocation = MutableLiveData<LatLng>()
        val duration = MutableLiveData(0L)

        private const val MAX_RETRIES = 3
    }

    @Inject
    lateinit var settingsManager: SettingsManager

    @Inject
    lateinit var sessionDao: SessionDao

    @Inject
    lateinit var restHandler: RestHandler

    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var notificationManager: NotificationManager

    private val durationMillis = MutableLiveData(0L)

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

    private fun startService() {
        val session: Session

        runBlocking {
            session = Session()
            sessionDao.insert(session)
        }
        saveSessionToBackend(session)

        isRunning.value = true
        startForeground(C.NOTIFICATION_ID, createNotification())
        requestLocationUpdates()
    }

    private fun stopService() {
        lifecycleScope.launchWhenCreated {
            settingsManager.removeSessionId()
        }

        isRunning.value = false
        stopSelf()
        fusedLocationProviderClient.removeLocationUpdates(callback)
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
            }
        }
    }

    /**
     * Create a new notification.
     */
    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, C.NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(getString(R.string.app_name))
            .setContentText("")
            .setContentIntent(createPendingIntent())
            .build()
    }

    /**
     * Create a pending intent for use in notification.
     * It will be used for opening the [SessionFragment]
     * when the user clicks on the notification.
     */
    private fun createPendingIntent() = NavDeepLinkBuilder(this)
        .setGraph(R.navigation.nav_graph)
        .setDestination(R.id.sessionFragment)
        .createPendingIntent()
}