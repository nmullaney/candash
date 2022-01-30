package app.candash.cluster

import android.content.Context
import kotlinx.coroutines.flow.Flow

class ElmBluetoothService(val context: Context) :
    CANService {

    override suspend fun startRequests(signalNamesToRequest: List<String>) {
        TODO("Not yet implemented")
    }

    override suspend fun shutdown() {
        TODO("Not yet implemented")
    }

    override fun carState(): Flow<CarState> {
        TODO("Not yet implemented")
    }

    override fun isRunning(): Boolean {
        TODO("Not yet implemented")
    }

    override fun getType(): CANServiceType {
        return CANServiceType.ELM_BLUETOOTH
    }
}