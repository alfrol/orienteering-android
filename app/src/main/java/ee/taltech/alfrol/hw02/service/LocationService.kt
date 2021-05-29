package ee.taltech.alfrol.hw02.service

import android.annotation.SuppressLint
import android.content.Intent
import android.location.Location
import android.os.Looper
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.MutableLiveData
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import ee.taltech.alfrol.hw02.C
import ee.taltech.alfrol.hw02.data.SettingsManager
import ee.taltech.alfrol.hw02.utils.LocationUtils
import ee.taltech.alfrol.hw02.utils.PermissionUtils
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

class LocationService : LifecycleService() {

    companion object {
        val isTracking = MutableLiveData<Boolean>()
        val pathPoints = MutableLiveData<MutableList<Location>>()
        val currentLocation = MutableLiveData<Location>()
    }

    @Inject
    lateinit var settingsManager: SettingsManager

    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    private var _isTracking = false
    private var _pathPoints: MutableList<Location> = mutableListOf()

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

    /**
     * Post the initial values for the livedata.
     */
    private fun postInitialValues() {
        isTracking.postValue(_isTracking)
        pathPoints.postValue(_pathPoints)
    }

    /**
     * Start tracking the device location.
     */
    private fun startTracking() {

    }

    /**
     * Stop tracking the device location.
     */
    private fun stopTracking() {

    }

    /**
     * Get current user location.
     */
    @SuppressLint("MissingPermission")
    private fun getCurrentLocation() {
        if (!PermissionUtils.hasLocationPermission(this)) {
            return
        }

        // When tracking then current location is essentially the last location in path points list
        if (_isTracking && _pathPoints.isNotEmpty()) {
            currentLocation.postValue(_pathPoints.last())
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
            interval = settingsManager.locationUpdateInterval.first()
            fastestInterval = settingsManager.locationUpdateFastestInterval.first()
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
            super.onLocationResult(locationResult)
        }
    }
}