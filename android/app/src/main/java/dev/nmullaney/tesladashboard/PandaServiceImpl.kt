package dev.nmullaney.tesladashboard

import android.content.SharedPreferences
import android.util.Log
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import java.net.*
import java.util.concurrent.Executors

@ExperimentalCoroutinesApi
class PandaServiceImpl(val sharedPreferences: SharedPreferences) : PandaService {
    private val TAG = PandaServiceImpl::class.java.simpleName

    @ExperimentalCoroutinesApi
    private val carStateFlow = MutableStateFlow(CarState())
    private val carState : CarState = CarState()
    private val port = 1338
    private var shutdown = false
    private val heartbeat = "ehllo"
    private var lastHeartbeatTimestamp = 0L
    private val heartBeatIntervalMs = 5_000
    private val signalHelper = CANSignalHelper()
    private val pandaContext = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
    private lateinit var socket : DatagramSocket

    @ExperimentalCoroutinesApi
    override fun carState() : Flow<CarState> {
        return carStateFlow
    }

    private fun getSocket() : DatagramSocket {
        if (!this::socket.isInitialized) {
            socket = DatagramSocket(null)
            socket.soTimeout = heartBeatIntervalMs
            socket.reuseAddress = true
        }
        return socket
    }

    @ExperimentalCoroutinesApi
    override suspend fun startRequests() {
        Log.d(TAG, "Starting requests")
        withContext(pandaContext) {
            shutdown = false
            try {

                Log.d(TAG, "Sending heartbeat")
                sendHello(getSocket())

                while (!shutdown) {

                    if (System.currentTimeMillis() > (lastHeartbeatTimestamp + heartBeatIntervalMs)) {
                        //Log.d(TAG, "Sending heartbeat")
                        sendHello(getSocket())
                    }

                    val buf = ByteArray(16)
                    val packet = DatagramPacket(buf, buf.size, serverAddress())
                    //Log.d(TAG, "C: Waiting to receive...")

                    try {
                        getSocket().receive(packet)
                    } catch (socketTimeoutException : SocketTimeoutException) {
                        Log.w(TAG, "Socket timed out without receiving a packet")
                        yield()
                        continue
                    }

                    //Log.d(TAG, "Packet from: " + packet.address + ":" + packet.port)

                    val pandaFrame = PandaFrame(buf)
                    /*Log.d(TAG, "FrameId = " + pandaFrame.frameIdHex.hexString)
                    Log.d(TAG, "BusId = " + pandaFrame.busId)
                    Log.d(TAG, "FrameLength = " + pandaFrame.frameLength())

                     */

                    if (pandaFrame.frameId == 6L && pandaFrame.busId == 15L) {
                        // It's an ack
                        sendFilter(getSocket())
                    }
                    else {
                        handleFrame(pandaFrame)
                    }
                    yield()
                }
                Log.d(TAG, "End while loop: shutdown requests received")

                getSocket().disconnect()
                Log.d(TAG, "Socket disconnected")
                // For some reason, closing the socket doesn't allow for reconnecting later, so for now
                // we're just never closing it
                //getSocket().close()
                //Log.d(TAG, "Socket closed")
            } catch (exception: Exception) {
                Log.e(TAG, "Exception while sending or receiving data", exception)
            }
        }
        Log.d(TAG, "Stopping requests")
    }

    private fun handleFrame(frame: PandaFrame) {
        var binaryPayloadString = ""
        signalHelper.getSignalsForFrame(frame.frameIdHex).forEach { channel ->
            if (channel.serviceIndex == 0) {
                //Log.d(TAG, channel.name + "no mux")

                binaryPayloadString = frame.getPayloadValue(
                    channel.startBit,
                    channel.bitLength
                )
            } else {
                Log.d(TAG, channel.name + " mux startbit:" + channel.startBit + " bitlength:" + channel.bitLength + " serviceIndex:" + channel.serviceIndex )

                binaryPayloadString = frame.getPayloadValue(

                    channel.startBit,
                    channel.bitLength,
                    channel.serviceIndex,
                    channel.muxIndex
                )
            }
            if (!binaryPayloadString.equals("")){
                val value = binaryPayloadString.toLong(2) * channel.factor + channel.offset
                // Log.d(TAG, channel.name + " = " + value)
                carState.updateValue(channel.name, value)
                carStateFlow.value = CarState(HashMap(carState.carData))
            }else{
                Log.d(TAG, "skipping payload")

            }
        }
    }

    override suspend fun shutdown() {
        withContext(pandaContext) {
            shutdown = true
        }
    }

    private fun sendHello(socket: DatagramSocket) {
        // prepare data to be sent
        val udpOutputData = heartbeat

        // prepare data to be sent
        val buf: ByteArray = udpOutputData.toByteArray()

        sendData(socket, buf)
        lastHeartbeatTimestamp = System.currentTimeMillis()
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
        InetSocketAddress(InetAddress.getByName(ipAddress()), port)

    private fun ipAddress() = sharedPreferences.getString(Constants.ipAddressPrefKey, Constants.ipAddressLocalNetwork)
}