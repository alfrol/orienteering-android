package ee.taltech.alfrol.hw02.service

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.location.Location
import android.os.Looper
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.MutableLiveData
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import dagger.hilt.android.AndroidEntryPoint
import ee.taltech.alfrol.hw02.C
import ee.taltech.alfrol.hw02.data.SettingsManager
import ee.taltech.alfrol.hw02.utils.LocationUtils
import ee.taltech.alfrol.hw02.utils.PermissionUtils
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@AndroidEntryPoint
class LocationService : LifecycleService() {

    companion object {
        val isTracking = MutableLiveData<Boolean>()
        val currentLocation = MutableLiveData<Location>()
        val pathPoints = MutableLiveData<MutableList<Location>>()
        val checkpoints = MutableLiveData<MutableList<Location>>()
        val waypoint = MutableLiveData<Location>()

        // In meters
        private const val MAX_ACCURACY = 30.0f
        private const val MAX_DISTANCE = 50.0f
        private const val MIN_DISTANCE = 3.0f
        private const val PROVIDER = "fused"
    }

    @Inject
    lateinit var settingsManager: SettingsManager

    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    private val intentFilter = IntentFilter(C.ACTION_LOCATION_ACTION).apply {
        addAction(C.ACTION_ADD_CHECKPOINT)
        addAction(C.ACTION_ADD_WAYPOINT)
    }
    private val notificationBroadcastReceiver = LocationActionReceiver()

    override fun onCreate() {
        super.onCreate()
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
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
    }

    /**
     * Start tracking the device location.
     */
    private fun startTracking() {
        isTracking.postValue(true)
        requestLocationUpdates()
        registerReceiver(notificationBroadcastReceiver, intentFilter)
        startForeground(C.NOTIFICATION_ID, LocationUtils.createNotification(this, ""))
    }

    /**
     * Stop tracking the device location.
     */
    private fun stopTracking() {
        postInitialValues()
        fusedLocationProviderClient.removeLocationUpdates(callback)
        unregisterReceiver(notificationBroadcastReceiver)
        stopForeground(true)
        stopSelf()
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
            if (location.provider != PROVIDER) {
                return
            }

            addPoint(pathPoints, location)
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
    }

    /**
     * Broadcast receiver for the service notification actions.
     */
    inner class LocationActionReceiver : BroadcastReceiver() {

        override fun onReceive(context: Context?, intent: Intent?) {
            intent?.let {
                val lastPoint = pathPoints.value?.last() ?: return

                when (it.action) {
                    C.ACTION_ADD_CHECKPOINT -> addPoint(checkpoints, lastPoint)
                    C.ACTION_ADD_WAYPOINT -> waypoint.postValue(lastPoint)
                }
            }
        }
    }
}