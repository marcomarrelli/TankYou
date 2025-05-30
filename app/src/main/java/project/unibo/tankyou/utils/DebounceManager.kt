package project.unibo.tankyou.utils

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class DebounceManager {
    private var debounceJob: Job? = null

    fun debounce(
        delayMs: Long = 300L,
        scope: CoroutineScope,
        action: suspend () -> Unit
    ) {
        debounceJob?.cancel()
        debounceJob = scope.launch {
            delay(delayMs)
            action()
        }
    }
}