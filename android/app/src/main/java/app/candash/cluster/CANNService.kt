package app.candash.cluster

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import okhttp3.*
import okio.ByteString
import javax.inject.Inject
import kotlin.random.Random

class CANNService @Inject constructor(val okHttpClient: OkHttpClient) {
    private val TAG = CANNService::class.java.simpleName

    private var shutdown = false

    init {
        GlobalScope.launch(Dispatchers.IO) {
            setup()
        }
    }

    fun setup() {
        var request = Request.Builder()
                .url("http://canserver.local/ws")
                .get().build()
        var websocket = okHttpClient.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket?, response: Response?) {
                super.onOpen(webSocket, response)
                Log.d(TAG, "onOpen: " + response)
            }

            override fun onMessage(webSocket: WebSocket?, text: String?) {
                super.onMessage(webSocket, text)
                Log.d(TAG, "onMessage: " + text)
            }

            override fun onMessage(webSocket: WebSocket?, bytes: ByteString?) {
                super.onMessage(webSocket, bytes)
                Log.d(TAG, "onMessage: " + bytes)
            }

            override fun onClosing(webSocket: WebSocket?, code: Int, reason: String?) {
                super.onClosing(webSocket, code, reason)
                Log.d(TAG, "onClosing: " + code + ", reason: " + reason)
            }

            override fun onClosed(webSocket: WebSocket?, code: Int, reason: String?) {
                super.onClosed(webSocket, code, reason)
                Log.d(TAG, "onClosed: " + code + ", reason: " + reason)
            }

            override fun onFailure(webSocket: WebSocket?, t: Throwable?, response: Response?) {
                super.onFailure(webSocket, t, response)
                Log.d(TAG, "onFailure: " + t.toString())
            }
        })


    }

    fun shutdown() {
        shutdown = true
    }
}