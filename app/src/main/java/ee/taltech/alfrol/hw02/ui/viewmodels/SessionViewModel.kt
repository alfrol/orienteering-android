package ee.taltech.alfrol.hw02.ui.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import ee.taltech.alfrol.hw02.C
import ee.taltech.alfrol.hw02.R
import ee.taltech.alfrol.hw02.data.SettingsManager
import ee.taltech.alfrol.hw02.data.model.Session
import ee.taltech.alfrol.hw02.data.model.SessionWithLocationPoints
import ee.taltech.alfrol.hw02.data.repositories.SessionRepository
import ee.taltech.alfrol.hw02.ui.states.CompassState
import ee.taltech.alfrol.hw02.ui.states.PolylineState
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SessionViewModel @Inject constructor(
    private val settingsManager: SettingsManager,
    private val sessionRepository: SessionRepository
) : ViewModel() {

    private var _polylineState = MutableLiveData<PolylineState>()
    val polylineState: LiveData<PolylineState> = _polylineState

    private var _compassState = MutableLiveData<CompassState>()
    val compassState: LiveData<CompassState> = _compassState

    private var _previewSession = MutableLiveData<SessionWithLocationPoints>()
    val previewSession: LiveData<SessionWithLocationPoints> = _previewSession

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

    /**
     * Load the session for preview.
     * Preview session must come with all info and all the location points.
     *
     * @param id ID of the session to load from the db.
     */
    fun loadPreviewSession(id: Long) {
        viewModelScope.launch {
            _previewSession.postValue(sessionRepository.findByIdWithLocationPoints(id).first())
        }
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
}