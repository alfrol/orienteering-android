package ee.taltech.alfrol.hw02.ui.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import ee.taltech.alfrol.hw02.C
import ee.taltech.alfrol.hw02.data.SettingsManager
import ee.taltech.alfrol.hw02.data.repositories.LocationPointRepository
import ee.taltech.alfrol.hw02.data.repositories.SessionRepository
import ee.taltech.alfrol.hw02.data.repositories.UserRepository
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MenuViewModel @Inject constructor(
    sessionRepository: SessionRepository,
    locationPointRepository: LocationPointRepository,
    private val userRepository: UserRepository,
    private val settingsManager: SettingsManager,
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

    /**
     * Log the user our.
     * Will remove the associated token and user id from the datastore.
     */
    fun logout() {
        viewModelScope.launch {
            settingsManager.removeValue(SettingsManager.LOGGED_IN_USER_ID_KEY)
            settingsManager.removeValue(SettingsManager.TOKEN_KEY)
        }
    }

    /**
     * Delete the user account.
     */
    fun deleteAccount() {
        viewModelScope.launch {
            settingsManager.getValue(SettingsManager.LOGGED_IN_USER_ID_KEY).firstOrNull()?.also {
                userRepository.deleteById(it)
                logout()
            }
        }
    }

    fun changeTrackColor(color: Int) {
        viewModelScope.launch {
            settingsManager.setValue(SettingsManager.POLYLINE_COLOR_KEY, color)
        }
    }

    fun changeTrackWidth(width: Float) {
        viewModelScope.launch {
            settingsManager.setValue(SettingsManager.POLYLINE_WIDTH_KEY, width)
        }
    }
}