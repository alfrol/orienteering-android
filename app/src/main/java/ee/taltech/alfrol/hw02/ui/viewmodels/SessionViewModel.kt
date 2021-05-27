package ee.taltech.alfrol.hw02.ui.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import ee.taltech.alfrol.hw02.R
import ee.taltech.alfrol.hw02.ui.states.CompassState

class SessionViewModel : ViewModel() {

    private var _compassState = MutableLiveData<CompassState>()
    val compassState: LiveData<CompassState> = _compassState

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