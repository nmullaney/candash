package app.candash.cluster

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import app.candash.cluster.bluetooth.BluetoothService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.nio.charset.Charset
import java.util.*
import java.util.concurrent.Executors

class ElmBluetoothService(val context: Context) :
    CANService {
    private val carStateFlow = MutableStateFlow(CarState())
    private val carState: CarState = CarState()
    private val TAG = ElmBluetoothService::class.java.simpleName
    private val STANDARD_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    private var bluetoothAdapter: BluetoothAdapter? = null
    private lateinit var socket: BluetoothSocket
    private lateinit var connectedDevice: BluetoothDevice
    private val elmContext = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
    private val signalHelper = CANSignalHelper()
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
            if (device.name == "OBDLink LX"){
                connectedDevice = device
            }
        }
        Log.d(TAG, "BTloop")
        var charset: Charset = Charsets.UTF_8
        signalHelper.getALLCANSignals()
        if (this::connectedDevice.isInitialized){
            withContext(elmContext) {
                BluetoothService.connectDevice(connectedDevice)
                var output = BluetoothService.sendData(("ATZ"+"\r").toByteArray(charset))
                Log.d(TAG, "BToutput: "+output.toString(charset))
                output = BluetoothService.sendData(("ATD"+"\r").toByteArray(charset))
                Log.d(TAG, "BToutput: "+output.toString(charset))
                output = BluetoothService.sendData(("AT E0"+"\r").toByteArray(charset))
                Log.d(TAG, "BToutput: "+output.toString(charset))
                output = BluetoothService.sendData(("STP 31"+"\r").toByteArray(charset))
                Log.d(TAG, "BToutput: "+output.toString(charset))
                //output = BluetoothService.sendData(("ATL0"+"\r").toByteArray(charset))
                //Log.d(TAG, "BToutput: "+output.toString(charset))
                output = BluetoothService.sendData(("ATL1"+"\r").toByteArray(charset))
                Log.d(TAG, "BToutput: "+output.toString(charset))
                output = BluetoothService.sendData(("ATAL"+"\r").toByteArray(charset))
                Log.d(TAG, "BToutput: "+output.toString(charset))

                val signals = signalHelper.getALLCANSignals()
                signals.forEach() {
                    output = BluetoothService.sendData(("STFPA "+it.value.frameId.string+", 7FF"+"\r").toByteArray(charset))
                }
                output = BluetoothService.sendData(("AT H1"+"\r").toByteArray(charset))
                Log.d(TAG, "BToutput: "+output.toString(charset))

                BluetoothService.requestData(("STM"+"\r").toByteArray(charset))
                BluetoothService.getData(this).collect {
                    Log.d(TAG, "BToutput: $it")
                    if (it.split(" ")[0].length == 3){
                        val frame = ElmFrame(it.toString())
                        handleFrame(frame)
                    } else {
                        // BluetoothService.requestData(("STM"+"\r").toByteArray(charset))
                    }
                    //val frame = ElmFrame(it.toString())
                    //handleFrame(frame)
                    if (it == "0 00 00") {
                        BluetoothService.requestData(("STM"+"\r").toByteArray(charset))
                    }
                }
            }

        }    }

    override suspend fun shutdown() {
        Log.d(TAG, "ElmShutdown")
        BluetoothService.shutdown()
    }
    private fun handleFrame(frame: ElmFrame) {
        Log.d(TAG, "BTframeID:"+frame.frameIdHex.string)
        signalHelper.getSignalsForFrame(frame.frameIdHex).forEach { channel ->
            // since I am using the 'any bus' filter, 0x399 exists on both buses with different data, so hard coding
            // a filter to remove the undesirable frame.
            if (frame.frameIdHex == Hex(0x399) && (frame.frameLength() == 3)){
                return
            }
            if (frame.frameIdHex == Hex(0x3FE) && (frame.frameLength() == 8)){
                return
            }
            // all bytes in the wrong  bus 0x395 frame are 0 except for the first
            if (frame.frameIdHex == Hex(0x395) && (frame.isZero(1))){
                return
            }


            if (frame.getCANValue(channel) != null){
                carState.updateValue(channel.name, frame.getCANValue(channel)!!)
                carStateFlow.value = CarState(HashMap(carState.carData))
            }
        }
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