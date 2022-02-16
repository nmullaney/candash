package app.candash.cluster.bluetooth

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.app.ActivityCompat
import com.google.common.primitives.Bytes
import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.shareIn
import java.io.*
import java.nio.charset.Charset
import java.util.*
import java.util.concurrent.Flow


object BluetoothService {
    private const val TAG = "BTCOMService"
    private val uuid: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    private lateinit var outputStream: OutputStream
    private lateinit var inputStream: InputStream
    private lateinit var socket: BluetoothSocket
    private var shutdown: Boolean = false
    private var lastHeartbeatTimestamp = 0L
    private val heartBeatIntervalMs = 1_000
    private val socketTimeoutMs = 1_000
    private val charset: Charset = Charsets.UTF_8

    suspend fun sendData(data: ByteArray) =
        coroutineScope {
            Log.d(TAG, "BTCommand:"+data.toString(charset))
            shutdown = false
            withContext(Dispatchers.IO) {
                outputStream.write(data)
                listenData()
            }
        }

    public fun connected() = this::socket.isInitialized && socket.isConnected

    public fun shutdown(){
        shutdown = true
    }
    private suspend fun listenData(

    ): ByteArray {
        var buffer = byteArrayOf()
        withContext(Dispatchers.IO) {
            var startReady = false
            while (true) {
                val bytes = inputStream.available()
                if (bytes != 0) {
                    var tempBuffer = ByteArray(bytes)
                    inputStream.read(tempBuffer)
                    buffer = Bytes.concat(buffer,tempBuffer)
                    break
                }

                delay(500L)
            }
        }
        return buffer
    }

    suspend fun requestData(data: ByteArray) =
        coroutineScope {
            withContext(Dispatchers.IO) {
                lastHeartbeatTimestamp = System.currentTimeMillis()
                outputStream.write(data)
            }
        }

    suspend fun getData(scope: CoroutineScope) = flow<String> {
            while(true) {
                if (shutdown){
                    Log.d(TAG, "BTShutdown")
                    break
                }
                val buffer = BufferedReader(InputStreamReader(inputStream))
                val line = buffer.readLine()
                emit(line)
                /*
                    if (System.currentTimeMillis() > (lastHeartbeatTimestamp + heartBeatIntervalMs)) {
                        outputStream.write(("STM"+"\r").toByteArray(charset))
                        lastHeartbeatTimestamp = System.currentTimeMillis()
                    }

                 */
                }
    }.flowOn(Dispatchers.IO).shareIn(scope, SharingStarted.Eagerly, 1)

    suspend fun connectDevice(device: BluetoothDevice) {
        BluetoothAdapter.getDefaultAdapter()?.cancelDiscovery()
        withContext(Dispatchers.IO) {
            socket = device.createRfcommSocketToServiceRecord(uuid)
            try {
                if (socket.isConnected) {
                    socket.close()
                }
                socket.connect()
                outputStream = socket.outputStream
                inputStream = socket.inputStream
                Log.d(TAG, "BT is connected: "+ socket.isConnected.toString())
            } catch (e: IOException) {
                // Error
            }
        }
    }
}