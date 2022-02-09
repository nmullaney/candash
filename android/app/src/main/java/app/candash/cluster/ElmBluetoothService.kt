package app.candash.cluster

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.lifecycle.lifecycleScope
import app.candash.cluster.bluetooth.BluetoothService
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext
import java.nio.charset.Charset
import java.util.*
import java.util.concurrent.Executors

class ElmBluetoothService(val context: Context) :
    CANService {
    private val carStateFlow = MutableStateFlow(CarState())
    private val TAG = ElmBluetoothService::class.java.simpleName
    private val STANDARD_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    private var bluetoothAdapter: BluetoothAdapter? = null
    private lateinit var socket: BluetoothSocket
    private lateinit var connectedDevice: BluetoothDevice
    private val elmContext = Executors.newSingleThreadExecutor().asCoroutineDispatcher()

    override suspend fun startRequests(signalNamesToRequest: List<String>) {
        val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()

        val pairedDevices: Set<BluetoothDevice>? = bluetoothAdapter?.bondedDevices
        pairedDevices?.forEach { device ->
            val deviceName = device.name
            val deviceHardwareAddress = device.address // MAC address
            Log.d(TAG, "BT: "+deviceName)
            /*
            if (device.uuids != null){
                for (uuid in device.uuids){
                    Log.d(TAG, "deviceName "+device.name+" uuid: "+uuid.toString())
                }
            }

             */
            if (device.name == "OBDII"){
                connectedDevice = device
            }
        }
        Log.d(TAG, "BTloop")
        var charset: Charset = Charsets.UTF_8
        if (this::connectedDevice.isInitialized){
            withContext(elmContext) {
                BluetoothService.connectDevice(connectedDevice)
                BluetoothService.sendData(("ATZ"+"\r").toByteArray(charset), byteArrayOf(), byteArrayOf() )
                BluetoothService.sendData(("AT E0"+"\r").toByteArray(charset), byteArrayOf(), byteArrayOf() )
                BluetoothService.sendData(("AT CRA 7D5"+"\r").toByteArray(charset), byteArrayOf(), byteArrayOf() )
                BluetoothService.sendData(("AT H1"+"\r").toByteArray(charset), byteArrayOf(), byteArrayOf() )


                BluetoothService.sendData(("0100"+"\r").toByteArray(charset), byteArrayOf(), byteArrayOf() )
            }

        }    }

    override suspend fun shutdown() {
        Log.d(TAG, "ElmShutdown")
    }

    override fun carState(): Flow<CarState> {
        Log.d(TAG, "ElmCarState")
        return carStateFlow
    }

    override fun isRunning(): Boolean {
        Log.d(TAG, "ElmIsRunning")
        return true
    }

    override fun getType(): CANServiceType {
        return CANServiceType.ELM_BLUETOOTH
    }
}