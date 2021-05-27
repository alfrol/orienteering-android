package ee.taltech.alfrol.hw02.ui.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.google.android.gms.maps.model.LatLng
import ee.taltech.alfrol.hw02.R
import ee.taltech.alfrol.hw02.ui.states.CompassState

class SessionViewModel(private val savedStateHandle: SavedStateHandle) : ViewModel() {

    companion object {
        private const val CHECKPOINTS_KEY = "CHECKPOINTS"
        private const val WAYPOINT_KEY = "WAYPOINT"
    }

    private var _compassState = MutableLiveData<CompassState>()
    val compassState: LiveData<CompassState> = _compassState

    val checkpoints: LiveData<MutableList<LatLng>>
    val waypoint: LiveData<LatLng>

    init {
        with(savedStateHandle) {
            checkpoints = getLiveData(CHECKPOINTS_KEY, mutableListOf())
            waypoint = getLiveData(WAYPOINT_KEY)
        }
    }

    /**
     * Add a new checkpoint to the checkpoints list.
     * Also save the checkpoint to the db and try to backend server.
     */
    fun addCheckpoint(location: LatLng) {
        savedStateHandle[CHECKPOINTS_KEY] = checkpoints.value?.apply {
            add(location)
        } ?: mutableListOf(location)
    }

    /**
     * Set a new waypoint.
     */
    fun addWaypoint(location: LatLng) {
        savedStateHandle[WAYPOINT_KEY] = location
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