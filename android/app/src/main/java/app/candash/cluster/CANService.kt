package app.candash.cluster

import kotlinx.coroutines.flow.Flow

interface CANService {
    suspend fun startRequests(signalNamesToRequest: List<String>)
    suspend fun shutdown()
    fun carState() : Flow<CarState>
    fun isRunning() : Boolean
    fun getType() : CANServiceType
}

enum class CANServiceType(val nameResId: Int) {
    MOCK(R.string.mock_server),
    PANDA(R.string.can_server),
    ELM_BLUETOOTH(R.string.elm_bluetooth_server)
}