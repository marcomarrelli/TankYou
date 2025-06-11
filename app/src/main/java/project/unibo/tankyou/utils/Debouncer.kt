package project.unibo.tankyou.utils

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * A utility class for debouncing actions. Debouncing ensures that a given action
 * is executed only after a certain amount of time has passed without any new calls
 * to the debounce function. This is useful for scenarios like search input fields
 * where you want to trigger a search only after the user has stopped typing.
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
     * @param delayMs The delay in milliseconds before the action is executed. Defaults to 300ms.
     * @param scope The CoroutineScope in which to launch the debounced action.
     * @param highPriority If true, this action is considered high priority.
     *                     High-priority actions will cancel any existing regular or high-priority debounced actions.
     * @param action The suspend function to be executed after the delay.
     */
    fun debounce(
        delayMs: Long = 300L,
        scope: CoroutineScope,
        highPriority: Boolean = false,
        action: suspend () -> Unit
    ) {
        if (highPriority) {
            // Cancel any existing high-priority and regular jobs
            highPriorityJob?.cancel()
            debounceJob?.cancel()
            // Launch the new high-priority job
            highPriorityJob = scope.launch {
                delay(delayMs)
                action()
            }
        } else {
            // Only proceed with a regular job if no high-priority job is currently active
            if (highPriorityJob?.isActive != true) {
                // Cancel any existing regular job
                debounceJob?.cancel()
                // Launch the new regular job
                debounceJob = scope.launch {
                    delay(delayMs)
                    action()
                }
            }
        }
    }

    /**
     * Cancels all currently scheduled debounced actions, both regular and high-priority.
     */
    fun cancelAll() {
        debounceJob?.cancel()
        highPriorityJob?.cancel()
    }
}