package project.unibo.tankyou.data.database.model

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import project.unibo.tankyou.data.database.entities.UserSavedGasStation
import project.unibo.tankyou.utils.Constants
import java.time.Instant

/**
 * ViewModel responsible for managing saved gas stations functionality.
 *
 * This class handles operations related to user's saved gas stations, including
 * loading, saving, removing, and updating station information. It maintains
 * the current list of saved stations and provides reactive streams for UI
 * components to observe changes.
 */
class SavedGasStationsModel : ViewModel() {
    private val _savedStations = MutableStateFlow<List<UserSavedGasStation>>(emptyList())
    private val _isLoading = MutableStateFlow(false)

    /**
     * StateFlow that emits the current list of saved gas stations.
     * The list is sorted by save date in descending order (most recent first).
     */
    val savedStations: StateFlow<List<UserSavedGasStation>> = _savedStations.asStateFlow()

    /**
     * StateFlow that indicates whether a loading operation is in progress.
     * UI components can observe this to show loading indicators.
     */
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        loadSavedStations()
    }

    /**
     * Loads the user's saved gas stations from the repository.
     *
     * This method fetches all saved stations and sorts them by save date
     * in descending order. The loading state is managed during the operation.
     */
    fun loadSavedStations() {
        Log.d(Constants.App.LOG_TAG, "Loading saved gas stations")

        viewModelScope.launch {
            _isLoading.value = true

            try {
                val stations = Constants.App.USER_REPOSITORY.getUserSavedStations()
                Log.i(
                    Constants.App.LOG_TAG,
                    "Successfully loaded ${stations.size} saved gas stations"
                )

                _savedStations.value = stations.sortedByDescending { station ->
                    try {
                        Instant.parse(station.savedAt)
                    } catch (e: Exception) {
                        Log.e(
                            Constants.App.LOG_TAG,
                            "Invalid date format for station ${station.stationId}: ${station.savedAt}",
                            e
                        )
                        Instant.MIN
                    }
                }
            } catch (e: Exception) {
                Log.e(Constants.App.LOG_TAG, "Failed to load saved gas stations", e)
                _savedStations.value = emptyList()
            } finally {
                _isLoading.value = false
                Log.d(Constants.App.LOG_TAG, "Finished loading saved gas stations!")
            }
        }
    }

    /**
     * Forces a refresh of the saved gas stations list.
     *
     * This method is equivalent to calling loadSavedStations() and can be used
     * when you need to explicitly refresh the data from the repository.
     */
    fun refreshSavedStations() {
        Log.i(Constants.App.LOG_TAG, "Refreshing saved gas stations...")
        loadSavedStations()
    }
}