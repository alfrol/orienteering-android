package ee.taltech.alfrol.hw02.ui.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import ee.taltech.alfrol.hw02.data.repositories.SessionRepository
import javax.inject.Inject

@HiltViewModel
class MenuViewModel @Inject constructor(
    private val sessionRepository: SessionRepository
) : ViewModel() {

    val totalDistance: LiveData<Float> =
        sessionRepository.getTotalDistance().asLiveData()

    val averageDistance: LiveData<Float> =
        sessionRepository.getAverageDistance().asLiveData()

    val averageDuration: LiveData<Long> =
        sessionRepository.getAverageDuration().asLiveData()

    val averagePace: LiveData<Float> =
        sessionRepository.getAveragePace().asLiveData()
}