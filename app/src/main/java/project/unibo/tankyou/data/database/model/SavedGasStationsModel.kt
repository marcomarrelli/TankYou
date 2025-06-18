package project.unibo.tankyou.data.database.model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import project.unibo.tankyou.data.database.entities.UserSavedGasStation
import project.unibo.tankyou.data.repositories.UserRepository

class SavedGasStationsModel : ViewModel() {
    private val userRepository = UserRepository.getInstance()

    private val _savedStations = MutableStateFlow<List<UserSavedGasStation>>(emptyList())
    val savedStations: StateFlow<List<UserSavedGasStation>> = _savedStations.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        loadSavedStations()
    }

    fun loadSavedStations() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val stations = userRepository.getUserSavedStations()
                _savedStations.value = stations
            } catch (e: Exception) {
                e.printStackTrace()
                _savedStations.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun saveGasStation(stationId: Long, notes: String? = null, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                val success = userRepository.saveGasStation(stationId, notes)
                if (success) {
                    loadSavedStations() // Refresh the list
                }
                onResult(success)
            } catch (e: Exception) {
                e.printStackTrace()
                onResult(false)
            }
        }
    }

    fun removeSavedGasStation(stationId: Long, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                val success = userRepository.removeSavedGasStation(stationId)
                if (success) {
                    loadSavedStations() // Refresh the list
                }
                onResult(success)
            } catch (e: Exception) {
                e.printStackTrace()
                onResult(false)
            }
        }
    }

    fun updateStationNotes(stationId: Long, notes: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                val success = userRepository.updateGasStationNotes(stationId, notes)
                if (success) {
                    loadSavedStations()
                }
                onResult(success)
            } catch (e: Exception) {
                e.printStackTrace()
                onResult(false)
            }
        }
    }

    fun isStationSaved(stationId: Long): Boolean {
        return _savedStations.value.any { it.stationId == stationId }
    }
}