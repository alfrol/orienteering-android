package ee.taltech.alfrol.hw02.ui.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import ee.taltech.alfrol.hw02.C
import ee.taltech.alfrol.hw02.data.repositories.LocationPointRepository
import ee.taltech.alfrol.hw02.data.repositories.SessionRepository
import javax.inject.Inject

@HiltViewModel
class MenuViewModel @Inject constructor(
    sessionRepository: SessionRepository,
    locationPointRepository: LocationPointRepository
) : ViewModel() {

    val sessionsCount: LiveData<Long> =
        sessionRepository.getSessionsCount().asLiveData()

    val totalDistance: LiveData<Float> =
        sessionRepository.getTotalDistance().asLiveData()

    val checkpointCount: LiveData<Long> =
        locationPointRepository.findPointCountByType(C.CP_TYPE_ID).asLiveData()

    val averageDistance: LiveData<Float> =
        sessionRepository.getAverageDistance().asLiveData()

    val averageDuration: LiveData<Long> =
        sessionRepository.getAverageDuration().asLiveData()

    val averagePace: LiveData<Float> =
        sessionRepository.getAveragePace().asLiveData()
}