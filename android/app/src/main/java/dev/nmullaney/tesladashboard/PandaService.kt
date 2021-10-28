package dev.nmullaney.tesladashboard

import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import kotlin.experimental.and
import kotlin.random.Random


class PandaService  {
    private val TAG = PandaService::class.java.simpleName

    private lateinit var speedFlow : Flow<Int>
    private val random = Random
    private val ipAddress = "192.168.2.4"
    private val port = 1338
    private var shutdown = false
    private val heartbeat = "ehllo"
    private val signalHelper = CANSignalHelper()

    init {
        GlobalScope.launch(Dispatchers.IO) {
            setup()
        }
    }

    fun speed() : Flow<Int> {
        speedFlow = flow {
            while(true) {
                emit(random.nextInt(0, 60))
                delay(10000)
            }
        }
        return speedFlow
    }

    private suspend fun setup() {
        withContext(Dispatchers.IO) {
            try {
                val socket = DatagramSocket()

                sendHello(socket)

                while (!shutdown) {
                    val buf = ByteArray(16)
                    val bytes = "\u00ff\u0000\u00cf\tabc".toByteArray()

                    val packet = DatagramPacket(buf, buf.size, serverAddress())
                    Log.d(TAG, "C: Waiting to receive...")
                    socket.receive(packet)
                    //Log.d(TAG, "C: Receiving: '" + String(buf) + "'")

                    val pandaFrame = PandaFrame(buf)
                    Log.d(TAG, "FrameId = " + pandaFrame.frameIdHex.hexString)
                    Log.d(TAG, "BusId = " + pandaFrame.busId)
                    Log.d(TAG, "FrameLength = " + pandaFrame.frameLength())

                    if (pandaFrame.frameId == 6L && pandaFrame.busId == 15L) {
                        // It's an ack
                        sendFilter(socket)
                    }
                    else if (!signalHelper.getSignalsForFrame(pandaFrame.frameIdHex).isEmpty()) {
                        signalHelper.getSignalsForFrame(pandaFrame.frameIdHex).forEach { channel ->
                            val value = pandaFrame.getPayloadValue(
                                channel.startBit,
                                channel.bitLength
                            ) * channel.factor + channel.offset
                            Log.d(TAG, channel.name + " = " + value)
                        }
                    }



                }

                socket.close()
            } catch (exception: Exception) {
                Log.e(TAG, "Exception while sending hello", exception)
            }
        }
    }

    private fun handleFrame(frame: PandaFrame) {
        val signalList = signalHelper.getSignalsForFrame(frame.frameIdHex)
        signalList.forEach {

        }
    }

    fun shutdown() {
        shutdown = true
    }

    private fun sendHello(socket: DatagramSocket) {
        // prepare data to be sent
        val udpOutputData = heartbeat

        // prepare data to be sent
        val buf: ByteArray = udpOutputData.toByteArray()

        sendData(socket, buf)
    }

    private fun sendFilter(socket: DatagramSocket) {
        sendData(socket, signalHelper.socketFilterToInclude())
        // Uncomment this to send all data
        //sendData(socket, byteArrayOf(0x0C))
    }

    private fun sendData(socket: DatagramSocket, buf: ByteArray) {
        // create a UDP packet with data and its destination ip & port
        val packet = DatagramPacket(buf, buf.size, serverAddress())
        Log.d(TAG, "C: Sending: '" + String(buf) + "'")

        // send the UDP packet

        socket.send(packet)
    }

    private fun serverAddress() : InetSocketAddress =
        InetSocketAddress(InetAddress.getByName(ipAddress), port)
}