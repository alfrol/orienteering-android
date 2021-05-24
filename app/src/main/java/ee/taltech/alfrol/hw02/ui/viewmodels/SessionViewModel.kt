package ee.taltech.alfrol.hw02.ui.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.volley.Request
import dagger.hilt.android.lifecycle.HiltViewModel
import ee.taltech.alfrol.hw02.C
import ee.taltech.alfrol.hw02.R
import ee.taltech.alfrol.hw02.api.AuthorizedJsonObjectRequest
import ee.taltech.alfrol.hw02.api.RestHandler
import ee.taltech.alfrol.hw02.data.SettingsManager
import ee.taltech.alfrol.hw02.data.dao.SessionDao
import ee.taltech.alfrol.hw02.data.model.Session
import ee.taltech.alfrol.hw02.ui.other.CompassState
import ee.taltech.alfrol.hw02.ui.other.SessionState
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import org.json.JSONObject
import javax.inject.Inject

@HiltViewModel
class SessionViewModel @Inject constructor(
    private val sessionDao: SessionDao,
    private val restHandler: RestHandler,
    private val settingsManager: SettingsManager
) : ViewModel() {

    private var _sessionState = MutableLiveData<SessionState>()
    val sessionState: LiveData<SessionState> = _sessionState

    private var _compassState = MutableLiveData<CompassState>()
    val compassState: LiveData<CompassState> = _compassState

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

            // Try to create a session in the backend
            val token = settingsManager.token.firstOrNull() ?: return@launch

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
    }

    fun stopSession() {
        _sessionState.value = SessionState()
    }

    /**
     * Check whether there is a session that is currently running.
     */
    fun isSessionRunning(): Boolean = _sessionState.value?.isRunning == true

    /**
     * Toggle the compass enabled state.
     */
    fun enableDisableCompass() {
        _compassState.value = when (isCompassEnabled()) {
            true -> CompassState()
            false -> CompassState(isEnabled = true, compassButtonIcon = R.drawable.ic_compass_off)
        }
    }

    /**
     * Check whether the compass is in enabled state.
     */
    fun isCompassEnabled(): Boolean = _compassState.value?.isEnabled == true
}