package app.candash.cluster

import kotlinx.coroutines.flow.Flow

interface PandaService {
    suspend fun startRequests(signalNamesToRequest: List<String>)
    suspend fun shutdown()
    fun carState() : Flow<CarState>
    fun isRunning() : Boolean
}