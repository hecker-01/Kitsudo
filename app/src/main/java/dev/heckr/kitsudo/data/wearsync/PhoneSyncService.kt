package dev.heckr.kitsudo.data.wearsync

import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import dev.heckr.kitsudo.data.sync.WearSyncPaths
import dev.heckr.kitsudo.domain.usecase.CascadeCompleteUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

/**
 * Receives messages from the paired Wear OS app.
 *
 * - [WearSyncPaths.TOGGLE_COMPLETE]: runs [CascadeCompleteUseCase] so
 *   parent/subtask cascade logic is applied on the phone's source-of-truth DB.
 * - [WearSyncPaths.REQUEST_SYNC]: watch came online; publish a fresh snapshot
 *   immediately via [PhoneTaskPublisher.publishOnce].
 *
 * NOTE: [WearableListenerService] has its own internal binder that conflicts
 * with Hilt's [dagger.hilt.android.AndroidEntryPoint] instrumentation. We use
 * [EntryPointAccessors] instead to safely reach the Hilt singletons.
 *
 * [runBlocking] is safe here: [onMessageReceived] is called on a background
 * thread by Google Play Services, and the work (one Room write + one DataClient
 * put) completes in well under the system's ~10 s kill window.
 */
class PhoneSyncService : WearableListenerService() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface PhoneSyncEntryPoint {
        fun cascadeCompleteUseCase(): CascadeCompleteUseCase
        fun phoneTaskPublisher(): PhoneTaskPublisher
    }

    private val entryPoint by lazy {
        EntryPointAccessors.fromApplication<PhoneSyncEntryPoint>(applicationContext)
    }

    override fun onMessageReceived(messageEvent: MessageEvent) {
        when (messageEvent.path) {
            WearSyncPaths.TOGGLE_COMPLETE -> handleToggle(messageEvent.data)
            WearSyncPaths.REQUEST_SYNC    -> runBlocking(Dispatchers.IO) {
                entryPoint.phoneTaskPublisher().publishOnce()
            }
        }
    }

    private fun handleToggle(data: ByteArray) {
        val payload = String(data, Charsets.UTF_8)
        val sep = payload.lastIndexOf(':')
        if (sep < 0) return
        val taskId = payload.substring(0, sep)
        val isCompleted = payload.substring(sep + 1).toBoolean()
        runBlocking(Dispatchers.IO) {
            entryPoint.cascadeCompleteUseCase()(taskId, isCompleted)
        }
    }
}
