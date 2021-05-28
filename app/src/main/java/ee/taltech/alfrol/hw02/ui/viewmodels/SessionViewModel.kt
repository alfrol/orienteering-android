package ee.taltech.alfrol.hw02.ui.viewmodels

import androidx.lifecycle.*
import dagger.hilt.android.lifecycle.HiltViewModel
import ee.taltech.alfrol.hw02.R
import ee.taltech.alfrol.hw02.data.model.Session
import ee.taltech.alfrol.hw02.data.repositories.SessionRepository
import ee.taltech.alfrol.hw02.ui.states.CompassState
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SessionViewModel @Inject constructor(
    private val sessionRepository: SessionRepository
) : ViewModel() {

    val sessionsSortedByRecordedAt: LiveData<List<Session>> =
        sessionRepository.findAllSortedByRecordedAt().asLiveData()

    val sessionsSortedByDistance: LiveData<List<Session>> =
        sessionRepository.findAllSortedByDistance().asLiveData()

    val sessionsSortedByDuration: LiveData<List<Session>> =
        sessionRepository.findAllSortedByDistance().asLiveData()

    val sessionsSortedByPace: LiveData<List<Session>> =
        sessionRepository.findAllSortedByPace().asLiveData()

    val totalDistance: LiveData<Float> =
        sessionRepository.getTotalDistance().asLiveData()

    val averageDistance: LiveData<Float> =
        sessionRepository.getAverageDistance().asLiveData()

    val averageDuration: LiveData<Long> =
        sessionRepository.getAverageDuration().asLiveData()

    val averagePace: LiveData<Float> =
        sessionRepository.getAveragePace().asLiveData()

    private var _compassState = MutableLiveData<CompassState>()
    val compassState: LiveData<CompassState> = _compassState

    fun getSessionWithLocationPoints(id: Long) =
        sessionRepository.findByIdWithLocationPoints(id).asLiveData()

    fun deleteSession(session: Session) =
        viewModelScope.launch { sessionRepository.deleteSession(session) }

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