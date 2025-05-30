package project.unibo.tankyou.utils

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class DebounceManager {
    private var debounceJob: Job? = null
    private var highPriorityJob: Job? = null

    fun debounce(
        delayMs: Long = 300L,
        scope: CoroutineScope,
        highPriority: Boolean = false,
        action: suspend () -> Unit
    ) {
        if (highPriority) {
            // Le azioni ad alta priorità cancellano tutto
            highPriorityJob?.cancel()
            debounceJob?.cancel()
            highPriorityJob = scope.launch {
                delay(delayMs)
                action()
            }
        } else {
            // Solo se non c'è un'azione ad alta priorità in corso
            if (highPriorityJob?.isActive != true) {
                debounceJob?.cancel()
                debounceJob = scope.launch {
                    delay(delayMs)
                    action()
                }
            }
        }
    }

    fun cancelAll() {
        debounceJob?.cancel()
        highPriorityJob?.cancel()
    }
}