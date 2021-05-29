package ee.taltech.alfrol.hw02.ui.viewmodels

import android.location.Location
import androidx.lifecycle.*
import com.google.android.gms.maps.model.Polyline
import dagger.hilt.android.lifecycle.HiltViewModel
import ee.taltech.alfrol.hw02.C
import ee.taltech.alfrol.hw02.R
import ee.taltech.alfrol.hw02.data.SettingsManager
import ee.taltech.alfrol.hw02.data.model.Session
import ee.taltech.alfrol.hw02.data.repositories.LocationPointRepository
import ee.taltech.alfrol.hw02.data.repositories.SessionRepository
import ee.taltech.alfrol.hw02.ui.states.CompassState
import ee.taltech.alfrol.hw02.ui.states.PolylineState
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SessionViewModel @Inject constructor(
    private val settingsManager: SettingsManager,
    private val sessionRepository: SessionRepository,
    private val locationPointRepository: LocationPointRepository
) : ViewModel() {

    private var _polylineState = MutableLiveData<PolylineState>()
    val polylineState: LiveData<PolylineState> = _polylineState

    private var _compassState = MutableLiveData<CompassState>()
    val compassState: LiveData<CompassState> = _compassState

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
        }
    }

    fun getSessionWithLocationPoints(id: Long) =
        sessionRepository.findByIdWithLocationPoints(id).asLiveData()

    fun deleteSession(session: Session) =
        viewModelScope.launch { sessionRepository.deleteSession(session) }

    /**
     * Save the location point to the database and try to sync to the backend.
     *
     * @param location Location point to save.
     * @param type One of the [C.LOC_TYPE_ID], [C.WP_TYPE_ID], [C.CP_TYPE_ID].
     */
    fun saveLocationPoint(location: Location, type: String) {

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

    private fun saveLocationToDb() {

    }

    private fun saveLocationToBackend() {

    }
}