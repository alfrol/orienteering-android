package ee.taltech.alfrol.hw02.ui.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import ee.taltech.alfrol.hw02.data.model.Session
import ee.taltech.alfrol.hw02.data.repositories.SessionRepository
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
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
}