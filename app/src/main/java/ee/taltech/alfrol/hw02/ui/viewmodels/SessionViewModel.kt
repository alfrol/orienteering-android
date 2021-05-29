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

    fun getSessionWithLocationPoints(id: Long) =
        sessionRepository.findByIdWithLocationPoints(id).asLiveData()

    fun deleteSession(session: Session) =
        viewModelScope.launch { sessionRepository.deleteSession(session) }
}