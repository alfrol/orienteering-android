package ee.taltech.alfrol.hw02.ui.viewmodels

import android.location.Location
import androidx.lifecycle.*
import dagger.hilt.android.lifecycle.HiltViewModel
import ee.taltech.alfrol.hw02.C
import ee.taltech.alfrol.hw02.R
import ee.taltech.alfrol.hw02.data.model.Session
import ee.taltech.alfrol.hw02.data.repositories.LocationPointRepository
import ee.taltech.alfrol.hw02.data.repositories.SessionRepository
import ee.taltech.alfrol.hw02.ui.states.CompassState
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SessionViewModel @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val locationPointRepository: LocationPointRepository
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
     * Save the location point to the database and try to sync to the backend.
     *
     * @param location Location point to save.
     * @param type One of the [C.LOC_TYPE_ID], [C.WP_TYPE_ID], [C.CP_TYPE_ID].
     */
    fun saveLocationPoint(location: Location, type: String) {

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