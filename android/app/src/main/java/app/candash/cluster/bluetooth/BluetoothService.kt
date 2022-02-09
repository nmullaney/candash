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
import java.nio.charset.Charset
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
                listenData()
            }
        }

    public fun connected() = this::socket.isInitialized && socket.isConnected

    private suspend fun listenData(

    ): ByteArray {
        var buffer = byteArrayOf()
        var charset: Charset = Charsets.UTF_8

        withContext(Dispatchers.IO) {
            var startReady = false
            while (true) {
                val bytes = inputStream.available()
                if (bytes != 0) {
                    var tempBuffer = ByteArray(bytes)
                    inputStream.read(tempBuffer)
                    buffer = Bytes.concat(buffer,tempBuffer)
                    Log.d(TAG, "BToutput: "+buffer.toString(charset))


                }
                delay(500L)
            }
        }
    }


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