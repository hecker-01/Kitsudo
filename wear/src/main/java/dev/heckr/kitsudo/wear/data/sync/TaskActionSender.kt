package dev.heckr.kitsudo.wear.data.sync

import android.content.Context
import android.util.Log
import com.google.android.gms.wearable.Wearable
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.heckr.kitsudo.data.sync.WearSyncPaths
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Sends task-completion actions from the watch to the phone.
 *
 * Discovers the phone node at call time via [NodeClient], then sends a
 * [MessageClient] message. If no phone is reachable the call is a no-op
 * (the optimistic local DB update done by the ViewModel is the only effect
 * until the phone reconnects and sends a fresh snapshot).
 */
@Singleton
class TaskActionSender @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val messageClient by lazy { Wearable.getMessageClient(context) }
    private val nodeClient by lazy { Wearable.getNodeClient(context) }

    /**
     * Tells the phone to cascade-complete [taskId] with state [isCompleted].
     * Payload format: `"<taskId>:<true|false>"`.
     */
    suspend fun toggleComplete(taskId: String, isCompleted: Boolean) {
        try {
            val nodes = nodeClient.connectedNodes.await()
            Log.d(TAG, "toggleComplete: ${nodes.size} connected nodes")
            val payload = "$taskId:$isCompleted".toByteArray(Charsets.UTF_8)
            nodes.forEach { node ->
                Log.d(TAG, "toggleComplete: sending to node ${node.displayName} (${node.id})")
                messageClient.sendMessage(node.id, WearSyncPaths.TOGGLE_COMPLETE, payload).await()
            }
        } catch (e: Exception) {
            Log.e(TAG, "toggleComplete: failed", e)
        }
    }

    /** Asks the phone to re-publish the current snapshot (called on first launch). */
    suspend fun requestSync(): String {
        return try {
            val nodes = nodeClient.connectedNodes.await()
            Log.d(TAG, "requestSync: ${nodes.size} connected nodes: ${nodes.map { it.displayName }}")
            if (nodes.isEmpty()) return "no nodes connected"
            nodes.forEach { node ->
                Log.d(TAG, "requestSync: sending to node ${node.displayName} (${node.id})")
                messageClient.sendMessage(node.id, WearSyncPaths.REQUEST_SYNC, ByteArray(0)).await()
            }
            "sent to ${nodes.size} nodes"
        } catch (e: Exception) {
            Log.e(TAG, "requestSync: failed", e)
            "error: ${e.message}"
        }
    }

    companion object {
        private const val TAG = "TaskActionSender"
    }
}
