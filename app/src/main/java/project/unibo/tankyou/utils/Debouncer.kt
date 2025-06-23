package project.unibo.tankyou.utils

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import project.unibo.tankyou.utils.Constants.App.LOG_TAG

/**
 * A utility class for debouncing actions.
 *
 * Debouncing ensures that a given action is executed only after a certain
 * amount of time has passed without any new calls to the debounce function.
 * This is useful for scenarios like search input fields where you want to
 * trigger a search only after the user has stopped typing.
 *
 * This Debouncer supports two types of jobs: regular and high-priority.
 * High-priority jobs will cancel any existing regular or high-priority jobs.
 * Regular jobs will only execute if no high-priority job is currently active.
 */
class Debouncer {
    private var debounceJob: Job? = null
    private var highPriorityJob: Job? = null

    /**
     * Debounces the execution of the given action.
     *
     * @param delayMs The delay in milliseconds before the action is executed
     * @param scope The CoroutineScope in which to launch the debounced action
     * @param highPriority If true, this action is considered high priority
     * @param action The suspend function to be executed after the delay
     */
    fun debounce(
        delayMs: Long = 300L,
        scope: CoroutineScope,
        highPriority: Boolean = false,
        action: suspend () -> Unit
    ) {
        Log.d(LOG_TAG, "Debouncing action with delay: ${delayMs}ms, highPriority: $highPriority")

        try {
            if (highPriority) {
                Log.d(LOG_TAG, "Canceling existing jobs for high priority action")
                // Cancel any existing high-priority and regular jobs
                highPriorityJob?.cancel()
                debounceJob?.cancel()
                // Launch the new high-priority job
                highPriorityJob = scope.launch {
                    delay(delayMs)
                    Log.d(LOG_TAG, "Executing high priority debounced action")
                    action()
                }
            } else {
                // Only proceed with a regular job if no high-priority job is currently active
                if (highPriorityJob?.isActive != true) {
                    Log.d(LOG_TAG, "Launching regular priority debounced action")
                    // Cancel any existing regular job
                    debounceJob?.cancel()
                    // Launch the new regular job
                    debounceJob = scope.launch {
                        delay(delayMs)
                        Log.d(LOG_TAG, "Executing regular priority debounced action")
                        action()
                    }
                } else {
                    Log.d(LOG_TAG, "Skipping regular action due to active high priority job")
                }
            }
        } catch (e: Exception) {
            Log.e(LOG_TAG, "Error during debounce action setup", e)
        }
    }
}