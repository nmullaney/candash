package app.candash.cluster.bluetooth

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.util.Log
import com.google.common.primitives.Bytes
import kotlinx.coroutines.*
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.*


object BluetoothService {
    private const val TAG = "BTCOMService"
    private val uuid: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    private lateinit var outputStream: OutputStream
    private lateinit var inputStream: InputStream
    private lateinit var socket: BluetoothSocket


    suspend fun sendData(data: ByteArray, startBytes: ByteArray, untilBytes: ByteArray) =
        coroutineScope {
            withContext(Dispatchers.IO) {
                outputStream.write(data)
                listenData(startBytes, untilBytes = untilBytes)
            }
        }

    public fun connected() = this::socket.isInitialized && socket.isConnected

    private suspend fun listenData(
        startBytes: ByteArray,
        untilBytes: ByteArray
    ): ByteArray {
        var buffer = byteArrayOf()
        withContext(Dispatchers.IO) {
            var startReady = false
            while (true) {
                val bytes = inputStream.available()
                if (bytes != 0) {
                    var tempBuffer = ByteArray(bytes)
                    inputStream.read(tempBuffer)
                    val index = Bytes.indexOf(tempBuffer, startBytes)
                    if (index != -1) {
                        startReady = true
                        buffer = byteArrayOf()
                        tempBuffer = tempBuffer.sliceArray(index until bytes - 1)
                    } else if (!startReady) {
                        continue
                    }
                    buffer = Bytes.concat(buffer, tempBuffer)
                    val i = Bytes.indexOf(tempBuffer, untilBytes)
                    if (i != -1) {
                        buffer = Bytes.concat(
                            buffer,
                            tempBuffer.sliceArray(0 until i + untilBytes.size)
                        )
                        break
                    } else {
                        buffer = Bytes.concat(buffer, tempBuffer)
                    }
                }
                delay(300L)
            }
        }
        return buffer
    }


    suspend fun connectDevice(device: BluetoothDevice) {
        BluetoothAdapter.getDefaultAdapter()?.cancelDiscovery()
        withContext(Dispatchers.IO) {
            socket = device.createInsecureRfcommSocketToServiceRecord(uuid)
            try {
                if (socket.isConnected) {
                    socket.close()
                }
                socket.connect()
                outputStream = socket.outputStream
                inputStream = socket.inputStream
                Log.d(TAG, socket.isConnected.toString())
            } catch (e: IOException) {
                // Error
            }
        }
    }
}