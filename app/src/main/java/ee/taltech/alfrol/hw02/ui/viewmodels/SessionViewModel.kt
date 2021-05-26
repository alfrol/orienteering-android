package ee.taltech.alfrol.hw02.ui.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.android.gms.maps.model.LatLng
import ee.taltech.alfrol.hw02.R
import ee.taltech.alfrol.hw02.ui.states.CompassState

class SessionViewModel : ViewModel() {

    private var _compassState = MutableLiveData<CompassState>()
    val compassState: LiveData<CompassState> = _compassState

    private var _checkpoints = MutableLiveData<MutableList<LatLng>>(mutableListOf())
    val checkpoints: LiveData<MutableList<LatLng>> = _checkpoints

    private var _waypoint = MutableLiveData<LatLng>()
    val waypoint: LiveData<LatLng> = _waypoint

    private var _duration = MutableLiveData(0L)
    val duration: LiveData<Long> = _duration

    /**
     * Add a new checkpoint to the checkpoints list.
     * Also save the checkpoint to the db and try to backend server.
     */
    fun addCheckpoint(location: LatLng) {
        _checkpoints.value = _checkpoints.value?.apply {
            add(location)
        } ?: mutableListOf(location)
    }

    /**
     * Set a new waypoint.
     */
    fun addWaypoint(location: LatLng) {
        _waypoint.value = location
    }

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
}