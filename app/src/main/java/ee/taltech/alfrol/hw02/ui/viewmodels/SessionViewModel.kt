package ee.taltech.alfrol.hw02.ui.viewmodels

import android.annotation.SuppressLint
import android.location.Location
import android.os.Build
import androidx.lifecycle.*
import com.android.volley.Request
import com.google.android.gms.maps.model.Polyline
import dagger.hilt.android.lifecycle.HiltViewModel
import ee.taltech.alfrol.hw02.C
import ee.taltech.alfrol.hw02.R
import ee.taltech.alfrol.hw02.api.AuthorizedJsonObjectRequest
import ee.taltech.alfrol.hw02.api.RestHandler
import ee.taltech.alfrol.hw02.data.SettingsManager
import ee.taltech.alfrol.hw02.data.model.LocationPoint
import ee.taltech.alfrol.hw02.data.model.Session
import ee.taltech.alfrol.hw02.data.repositories.LocationPointRepository
import ee.taltech.alfrol.hw02.data.repositories.SessionRepository
import ee.taltech.alfrol.hw02.ui.states.CompassState
import ee.taltech.alfrol.hw02.ui.states.PolylineState
import ee.taltech.alfrol.hw02.utils.UIUtils
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import javax.inject.Inject

@HiltViewModel
class SessionViewModel @Inject constructor(
    private val restHandler: RestHandler,
    private val settingsManager: SettingsManager,
    private val sessionRepository: SessionRepository,
    private val locationPointRepository: LocationPointRepository
) : ViewModel() {

    private var _polylineState = MutableLiveData<PolylineState>()
    val polylineState: LiveData<PolylineState> = _polylineState

    private var _compassState = MutableLiveData<CompassState>()
    val compassState: LiveData<CompassState> = _compassState

    private var activeSession: Session? = null

    init {
        viewModelScope.launch {
            val color = settingsManager.getValue(
                SettingsManager.POLYLINE_COLOR_KEY,
                C.DEFAULT_POLYLINE_COLOR
            ).first()
            val width = settingsManager.getValue(
                SettingsManager.POLYLINE_WIDTH_KEY,
                C.DEFAULT_POLYLINE_WIDTH
            ).first()

            _polylineState.postValue(PolylineState(color!!, width!!))

            settingsManager.getValue(SettingsManager.ACTIVE_SESSION_ID_KEY).first()?.let {
                activeSession = sessionRepository.findById(it).first()
            }
        }
    }

    fun getSessionWithLocationPoints(id: Long) =
        sessionRepository.findByIdWithLocationPoints(id).asLiveData()

    /**
     * Create and save a new session.
     */
    fun createSession() {
        runBlocking {
            val session = Session()
            val id = sessionRepository.insertSession(session)
            activeSession = Session(id, recordedAt = session.recordedAt)

            // Persist the session id until it's stopped
            settingsManager.setValue(SettingsManager.ACTIVE_SESSION_ID_KEY, id)
        }

        viewModelScope.launch {
            createSessionInBackend()
        }
    }

    /**
     * Update the stopped session details in the db.
     *
     * @param distance Distance travelled during the session.
     * @param duration Duration of the session in milliseconds.
     * @param pace Average pace in minutest per kilometer.
     */
    fun updateStoppedSession(distance: Float, duration: Long, pace: Float) {
        val session = activeSession ?: return

        viewModelScope.launch {
            val updatedSession = Session(
                id = session.id,
                externalId = session.externalId,
                recordedAt = session.recordedAt,
                distance = distance,
                duration = duration,
                pace = pace
            )
            sessionRepository.updateSession(updatedSession)
        }
    }

    /**
     * Save the location point to the database and try to sync to the backend.
     *
     * @param location Location point to save.
     * @param type One of the [C.LOC_TYPE_ID], [C.WP_TYPE_ID], [C.CP_TYPE_ID].
     */
    fun saveLocationPoint(location: Location, type: String) {
        viewModelScope.launch {
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
     * Update and save polyline settings to the datastore.
     *
     * @param polyline Polyline containing the new width, color values to save.
     */
    fun savePolyline(polyline: Polyline) {
        val color = polyline.color
        val width = polyline.width

        viewModelScope.launch {
            settingsManager.setValue(SettingsManager.POLYLINE_COLOR_KEY, color)
            settingsManager.setValue(SettingsManager.POLYLINE_WIDTH_KEY, width)
        }

        _polylineState.postValue(PolylineState(color, width))
    }

    /**
     * Toggle the state of the compass.
     */
    fun toggleCompass() {
        _compassState.value = _compassState.value?.let {
            when (it.isEnabled) {
                true -> CompassState()
                false -> CompassState(
                    isEnabled = true,
                    compassButtonIcon = R.drawable.ic_compass_off
                )
            }
        } ?: CompassState(isEnabled = true, compassButtonIcon = R.drawable.ic_compass_off)
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

                viewModelScope.launch {
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

        viewModelScope.launch {
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
}