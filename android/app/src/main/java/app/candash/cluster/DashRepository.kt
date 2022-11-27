package app.candash.cluster

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class DashRepository @ExperimentalCoroutinesApi
@Inject constructor(private val canServiceFactory: CANServiceFactory) {

    private fun getCANService() : CANService {
        return canServiceFactory.getService()
    }

    fun getCANServiceType() : CANServiceType {
        return canServiceFactory.getService().getType()
    }

    fun setCANServiceType(type: CANServiceType) {
        canServiceFactory.setServiceType(type)
    }

    suspend fun startRequests(signalNamesToRequest: List<String>) {
        getCANService().startRequests(signalNamesToRequest)
    }

    fun isRunning() : Boolean {
        return getCANService().isRunning()
    }

    @ExperimentalCoroutinesApi
    suspend fun shutdown() {
        getCANService().shutdown()
    }

    @ExperimentalCoroutinesApi
    fun carState() : Flow<CarState> {
        return getCANService().carState();
    }
}