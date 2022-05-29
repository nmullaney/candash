package app.candash.cluster

import kotlinx.coroutines.flow.Flow

/**
 * To create a different CANService, add your new service to the CANServiceType, create a new
 * class that implements CANService, and add the new class to the CANServiceFactory.
 */
interface CANService {
    suspend fun startRequests(signalNamesToRequest: List<String>)
    suspend fun shutdown()
    fun carState() : Flow<CarState>
    fun isRunning() : Boolean
    fun getType() : CANServiceType
}

enum class CANServiceType(val nameResId: Int) {
    MOCK(R.string.mock_server),
    PANDA(R.string.can_server)
}