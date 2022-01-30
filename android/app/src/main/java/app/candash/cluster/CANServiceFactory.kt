package app.candash.cluster

import android.content.SharedPreferences
import javax.inject.Inject

class CANServiceFactory @Inject constructor(
    val sharedPreferences: SharedPreferences,
    private val pandaService: PandaService,
    private val mockPandaService: MockCANService,
    private val elmBluetoothService: ElmBluetoothService
) {

    companion object {
        const val CAN_SERVICE_KEY = "CAN_service_key"
    }

    fun setServiceType(serviceType: CANServiceType) {
        sharedPreferences.edit().putInt(CAN_SERVICE_KEY, serviceType.ordinal).apply()
    }

    fun getService() : CANService {
        return when (CANServiceType.values()[sharedPreferences.getInt(CAN_SERVICE_KEY, 0)]) {
            CANServiceType.MOCK -> mockPandaService
            CANServiceType.PANDA -> pandaService
            CANServiceType.ELM_BLUETOOTH -> elmBluetoothService
        }
    }
}