package ee.taltech.alfrol.hw02.ui.viewmodels

import android.app.Application
import android.content.Intent
import androidx.lifecycle.*
import com.android.volley.Request
import com.google.android.gms.maps.model.LatLng
import dagger.hilt.android.lifecycle.HiltViewModel
import ee.taltech.alfrol.hw02.C
import ee.taltech.alfrol.hw02.R
import ee.taltech.alfrol.hw02.api.AuthorizedJsonObjectRequest
import ee.taltech.alfrol.hw02.api.RestHandler
import ee.taltech.alfrol.hw02.data.SettingsManager
import ee.taltech.alfrol.hw02.data.dao.SessionDao
import ee.taltech.alfrol.hw02.data.model.Session
import ee.taltech.alfrol.hw02.service.LocationService
import ee.taltech.alfrol.hw02.ui.states.CompassState
import ee.taltech.alfrol.hw02.ui.states.SessionState
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import org.json.JSONObject
import javax.inject.Inject

@HiltViewModel
class SessionViewModel @Inject constructor(
    private val app: Application,
    private val sessionDao: SessionDao,
    private val restHandler: RestHandler,
    private val settingsManager: SettingsManager
) : AndroidViewModel(app) {

    companion object {
        private const val OBSERVE_LOCATION_UPDATES = "LOCATION_UPDATES"
        private const val OBSERVE_CURRENT_LOCATION = "CURRENT_LOCATION"
    }

    private var _sessionState = MutableLiveData<SessionState>()
    val sessionState: LiveData<SessionState> = _sessionState

    private var _locationState = MutableLiveData<MutableList<LatLng>>(mutableListOf())
    val locationState: LiveData<MutableList<LatLng>> = _locationState

    private var _currentLocation = MutableLiveData<LatLng>()
    val currentLocation: LiveData<LatLng> = _currentLocation

    private var _compassState = MutableLiveData<CompassState>()
    val compassState: LiveData<CompassState> = _compassState

    override fun onCleared() {
        stopObserving(OBSERVE_CURRENT_LOCATION)
        stopObserving(OBSERVE_LOCATION_UPDATES)
        super.onCleared()
    }

    /**
     * Start a new session.
     *
     * First, the method saves session to the database.
     * Next updates the state and notifies the observers that
     * the session has started.
     *
     * Second, it tries to create a session in the backend.
     */
    fun startSession() {
        viewModelScope.launch {
            // Save session to the db and "start" it
            val session = Session()
            sessionDao.insert(session)

            _sessionState.postValue(
                SessionState(
                    isRunning = true,
                    buttonColor = R.color.red,
                    buttonIcon = R.drawable.ic_stop
                )
            )

            startLocationService(C.ACTION_START_SERVICE, OBSERVE_LOCATION_UPDATES)
            saveSessionToBackend(session)
        }
    }

    /**
     * Stop the currently running session.
     */
    fun stopSession() {
        startLocationService(C.ACTION_STOP_SERVICE, OBSERVE_LOCATION_UPDATES)
        _sessionState.value = SessionState()

        viewModelScope.launch {
            settingsManager.removeSessionId()
        }
    }

    fun getCurrentLocation() =
        startLocationService(C.ACTION_GET_CURRENT_LOCATION, OBSERVE_CURRENT_LOCATION)

    /**
     * Check whether there is a session that is currently running.
     */
    fun isSessionRunning(): Boolean = _sessionState.value?.isRunning == true

    /**
     * Toggle the compass enabled state.
     */
    fun toggleCompass() {
        _compassState.value = when (isCompassEnabled()) {
            true -> CompassState()
            false -> CompassState(isEnabled = true, compassButtonIcon = R.drawable.ic_compass_off)
        }
    }

    /**
     * Check whether the compass is in enabled state.
     */
    fun isCompassEnabled(): Boolean = _compassState.value?.isEnabled == true

    /**
     * Try to save the new session to the backend.
     * Doesn't do anything on failure.
     */
    private suspend fun saveSessionToBackend(session: Session) {
        val token = settingsManager.token.firstOrNull() ?: return

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
                viewModelScope.launch {
                    settingsManager.saveSessionId(id)
                }
            },
            {},
            token
        )
        restHandler.addRequest(sessionRequest)
    }

    /**
     * Start (or stop) the [LocationService].
     *
     * @param action One of the
     * [C.ACTION_START_SERVICE],
     * [C.ACTION_STOP_SERVICE] or
     * [C.ACTION_GET_CURRENT_LOCATION].
     */
    private fun startLocationService(action: String, which: String) =
        Intent(app.applicationContext, LocationService::class.java).also {
            it.action = action
            app.applicationContext.startService(it)

            when (action) {
                in listOf(C.ACTION_START_SERVICE, C.ACTION_GET_CURRENT_LOCATION) -> startObserving(
                    which
                )
                C.ACTION_STOP_SERVICE -> stopObserving(which)
            }
        }

    /**
     * Register an observer for [LocationService.points] or [LocationService.currentLocation].
     *
     * @param which Which livedata to start observing.
     */
    private fun startObserving(which: String) {
        when (which) {
            OBSERVE_LOCATION_UPDATES -> LocationService.points.observeForever(locationObserver)
            OBSERVE_CURRENT_LOCATION -> LocationService.currentLocation.observeForever(
                currentLocationObserver
            )
        }
    }

    /**
     * Unregister the observer for [LocationService.points] or [LocationService.currentLocation].
     *
     * @param which From which livedata to remove the observer.
     */
    private fun stopObserving(which: String) {
        when (which) {
            OBSERVE_LOCATION_UPDATES -> LocationService.points.removeObserver(locationObserver)
            OBSERVE_CURRENT_LOCATION -> LocationService.currentLocation.removeObserver(
                currentLocationObserver
            )
        }
    }

    /**
     * An observer for changes in [LocationService.points].
     */
    private val locationObserver = Observer<MutableList<LatLng>> { points ->
        _locationState.value = points
    }

    /**
     * An observer for changes in [LocationService.currentLocation].
     */
    private val currentLocationObserver = Observer<LatLng> { location ->
        _currentLocation.value = location
    }
}