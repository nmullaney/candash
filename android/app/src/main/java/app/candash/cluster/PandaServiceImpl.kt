package app.candash.cluster

import android.content.Context
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import java.io.IOException
import java.net.*
import java.util.*
import java.util.concurrent.Executors


@ExperimentalCoroutinesApi
class PandaServiceImpl(val sharedPreferences: SharedPreferences, val context: Context) :
    PandaService {
    private val TAG = PandaServiceImpl::class.java.simpleName

    @ExperimentalCoroutinesApi
    private val carStateFlow = MutableStateFlow(CarState())
    private val carState: CarState = CarState()
    private val port = 1338
    private var shutdown = false
    private var inShutdown = false
    private val heartbeat = "ehllo"
    private val goodbye = "bye"
    private var lastHeartbeatTimestamp = 0L
    private val heartBeatIntervalMs = 4_000
    private val socketTimeoutMs = 1_000
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

    private fun createSocket(): DatagramSocket {
        socket = DatagramSocket(null)
        socket.soTimeout = heartBeatIntervalMs
        socket.reuseAddress = true
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
        withContext(pandaContext) {
            Log.d(TAG, "Starting requests on thread: ${Thread.currentThread().name}")
            while (inShutdown) {
                yield()
            }
            shutdown = false
            try {

                Log.d(TAG, "Sending heartbeat on thread: ${Thread.currentThread().name}")
                sendHello(createSocket())

                while (!shutdown) {

                    if (System.currentTimeMillis() > (lastHeartbeatTimestamp + heartBeatIntervalMs)) {
                        Log.d(TAG, "Sending heartbeat on thread: ${Thread.currentThread().name}")
                        sendHello(getSocket())
                    }
                    // up to 512 frames which are 16 bytes each
                    val buf = ByteArray(16 * 512)
                    val packet = DatagramPacket(buf, buf.size, serverAddress())
                    Log.d(TAG, "C: Waiting to receive... on thread: ${Thread.currentThread().name}")

                    try {
                        getSocket().receive(packet)
                    } catch (socketTimeoutException: SocketTimeoutException) {
                        Log.w(
                            TAG,
                            "Socket timed out without receiving a packet on thread: ${Thread.currentThread().name}"
                        )
                        sendBye(getSocket())
                        yield()
                        continue
                    }

                    //Log.d(TAG, "Packet from: " + packet.address + ":" + packet.port)
                    for (i in buf.indices step 16){

                        val newPandaFrame = NewPandaFrame(buf.sliceArray(i..i+15))
                        // Log.d(TAG, "bufindex = " + i.toString()+ " pandaFrame :" + newPandaFrame.frameId.toString())
                        if (newPandaFrame.frameId == 0L){
                            break
                        } else if (newPandaFrame.frameId == 6L && newPandaFrame.busId == 15L) {
                            // It's an ack
                            sendFilter(getSocket())
                        } else {
                            handleFrame(newPandaFrame)
                        }
                    }

                    /*
                    Log.d(TAG, "Binary = " + buf.getPayloadBinaryString())
                    Log.d(TAG, "FrameId = " + newPandaFrame.frameIdHex.hexString)
                    Log.d(TAG, "BusId = " + newPandaFrame.busId)
                    Log.d(TAG, "FrameLength = " + newPandaFrame.frameLength)
                     */

                    yield()
                }
                sendBye(getSocket())
                Log.d(
                    TAG,
                    "End while loop: shutdown requests received on thread: ${Thread.currentThread().name}"
                )
                getSocket().disconnect()
                Log.d(TAG, "Socket disconnected")
                getSocket().close()
                Log.d(TAG, "Socket closed")
                inShutdown = false
            } catch (exception: Exception) {
                inShutdown = false
                Log.e(TAG, "Exception while sending or receiving data", exception)
            }
        }
        Log.d(TAG, "Stopping requests on thread: ${Thread.currentThread().name}")
    }

    private fun handleFrame(frame: NewPandaFrame) {
        var binaryPayloadString = ""
        val updateState = CarState(HashMap())

        signalHelper.getSignalsForFrame(frame.frameIdHex).forEach { channel ->
            if (frame.getCANValue(channel) != null){
                carState.updateValue(channel.name, frame.getCANValue(channel)!!)
                carStateFlow.value = CarState(HashMap(carState.carData))
            }
        }
    }

    override suspend fun shutdown() {
        Log.d(TAG, "in shutdown on thread: ${Thread.currentThread().name}")
        withContext(pandaContext) {
            inShutdown = true
            shutdown = true
            Log.d(TAG, "shutdown true on thread: ${Thread.currentThread().name}")
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

    private fun sendBye(socket: DatagramSocket) {
        // prepare data to be sent
        val udpOutputData = goodbye

        // prepare data to be sent
        val buf: ByteArray = udpOutputData.toByteArray()

        sendData(socket, buf, true)
    }


    private fun sendFilter(socket: DatagramSocket) {
        sendData(socket, signalHelper.socketFilterToInclude())
        // Uncomment this to send all data
        //sendData(socket, byteArrayOf(0x0C))
    }

    private fun sendData(socket: DatagramSocket, buf: ByteArray, isBye: Boolean = false) {
        // create a UDP packet with data and its destination ip & port
        val packet = DatagramPacket(buf, buf.size, serverAddress())
        Log.d(TAG, "C: Sending: '" + String(buf) + "'")

        // send the UDP packet
        try {
            socket.send(packet)
        } catch (ioException: IOException) {
            Log.e(TAG, "IOException while sending data.", ioException)
            if (!isBye) checkNetwork()
        } catch (socketException: SocketException) {
            Log.e(TAG, "SocketException while sending data.", socketException)
            if (!isBye) checkNetwork()
        }
    }

    private fun checkNetwork() {
        //val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        CoroutineScope(pandaContext).launch {
            restartLater()

        }
    }


    private suspend fun restartLater() {
        Log.d(TAG, "in restartLater on thread: ${Thread.currentThread().name}")
        withContext(pandaContext) {
            shutdown()
            Log.d(TAG, "in restartLater after shutdown on thread: ${Thread.currentThread().name}")
            val connectivityManager =
                context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val networkRequest = NetworkRequest.Builder()
                .build()
            connectivityManager.registerNetworkCallback(networkRequest, object :
                ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    super.onAvailable(network)
                    Log.d(TAG, "in network callback, on available")
                    doRestart()
                }

                override fun onCapabilitiesChanged(
                    network: Network,
                    networkCapabilities: NetworkCapabilities
                ) {
                    super.onCapabilitiesChanged(network, networkCapabilities)
                    Log.d(TAG, "in network callback, capabilities changed")
                }
            })
        }
    }

    private fun doRestart() {
        CoroutineScope(pandaContext).launch {
            Log.d(
                TAG,
                "in doRestart, restarting requests on thread: ${Thread.currentThread().name}"
            )
            startRequests()
        }
    }

    private fun serverAddress(): InetSocketAddress =
        InetSocketAddress(InetAddress.getByName(ipAddress()), port)

    private fun ipAddress() =
        sharedPreferences.getString(Constants.ipAddressPrefKey, Constants.ipAddressLocalNetwork)
}