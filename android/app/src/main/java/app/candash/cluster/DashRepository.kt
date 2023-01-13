package app.candash.cluster

import kotlinx.coroutines.ExperimentalCoroutinesApi
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

    suspend fun shutdown() {
        getCANService().shutdown()
    }

    fun clearCarState() {
        getCANService().clearCarState()
    }

    fun carState() : CarState {
        return getCANService().carState()
    }

    fun liveCarState() : LiveCarState {
        return getCANService().liveCarState();
    }
}