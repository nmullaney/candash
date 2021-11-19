package dev.nmullaney.tesladashboard

import android.content.Context
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import java.net.*
import java.util.*
import java.util.concurrent.Executors
import kotlin.collections.HashMap
import kotlin.invoke
import kotlin.math.sign

@ExperimentalCoroutinesApi
class PandaServiceImpl(val sharedPreferences: SharedPreferences, val context: Context) : PandaService {
    private val TAG = PandaServiceImpl::class.java.simpleName

    @ExperimentalCoroutinesApi
    private val carStateFlow = MutableStateFlow(CarState())
    private val carState: CarState = CarState()
    private val port = 1338
    private var shutdown = false
    private val heartbeat = "ehllo"
    private var lastHeartbeatTimestamp = 0L
    private val heartBeatIntervalMs = 5_000
    private val signalHelper = CANSignalHelper()
    private val pandaContext = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
    private lateinit var socket: DatagramSocket

    @ExperimentalCoroutinesApi
    override fun carState(): Flow<CarState> {
        return carStateFlow
    }

    private fun getSocket(): DatagramSocket {
        if (!this::socket.isInitialized) {
            socket = DatagramSocket(null)
            socket.soTimeout = heartBeatIntervalMs
            socket.reuseAddress = true
        }
        return socket
    }

    private fun twosComplement(s: String): Long {
        if (s[0].equals('0'))
            return s.toLong(radix = 2)
        var seenOne: Boolean = false
        val chars = s.toCharArray()
        for (i in s.length - 1 downTo 0) {
            if (seenOne == false) {
                if (chars[i].equals('1')) {
                    seenOne = true;
                }
            } else {
                if (chars[i].equals('1')) {
                    chars[i] = '0'
                } else {
                    chars[i] = '1'
                }
            }
        }
        return (String(chars).toLong(radix = 2)) * -1
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
                        Log.d(TAG, "Sending heartbeat")
                        sendHello(getSocket())
                    }

                    val buf = ByteArray(16)
                    val packet = DatagramPacket(buf, buf.size, serverAddress())
                    Log.d(TAG, "C: Waiting to receive...")

                    try {
                        getSocket().receive(packet)
                    } catch (socketTimeoutException: SocketTimeoutException) {
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
                    } else {
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
                Log.d(
                    TAG,
                    channel.name + " mux startbit:" + channel.startBit + " bitlength:" + channel.bitLength + " serviceIndex:" + channel.serviceIndex
                )

                binaryPayloadString = frame.getPayloadValue(

                    channel.startBit,
                    channel.bitLength,
                    channel.serviceIndex,
                    channel.muxIndex
                )
            }
            if (!binaryPayloadString.equals("")) {
                var value: Float = 0.0f
                if (channel.signed != true) {
                    binaryPayloadString = "0" + binaryPayloadString
                    Log.d(TAG, channel.name + "is unsigned" + binaryPayloadString)
                    value = binaryPayloadString.toLong(2) * channel.factor + channel.offset
                } else {
                    Log.d(TAG, channel.name + "is signed" + binaryPayloadString)
                    value = twosComplement(binaryPayloadString) * channel.factor + channel.offset
                }
                // Log.d(TAG, channel.name + " = " + value)
                carState.updateValue(channel.name, value)
                carStateFlow.value = CarState(HashMap(carState.carData))

            } else {
                Log.d(TAG, "skipping payload")

            }
        }
    }

    override suspend fun shutdown() {
        Log.d(TAG, "in shutdown")
        withContext(pandaContext) {
            shutdown = true
            Log.d(TAG, "shutdown true")
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
        try {
            socket.send(packet)
        } catch (socketException: SocketException) {
            Log.e(TAG, "SocketException while sending data.", socketException)
            checkNetwork()
        }
    }

    private fun checkNetwork() {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        if (connectivityManager.activeNetwork != null) {
            Log.d(TAG, "Network is good")
        } else {
            runBlocking {
                restartLater()
            }
        }
    }

    private suspend fun restartLater() {
        Log.d(TAG, "in restartLater")
        withContext(pandaContext) {
            shutdown()
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            connectivityManager.addDefaultNetworkActiveListener {
                async {
                    startRequests()
                }
            }
        }
    }

    private fun serverAddress(): InetSocketAddress =
        InetSocketAddress(InetAddress.getByName(ipAddress()), port)

    private fun ipAddress() =
        sharedPreferences.getString(Constants.ipAddressPrefKey, Constants.ipAddressLocalNetwork)
}