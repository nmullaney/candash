package app.candash.cluster

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket

sealed class ConnectionState {
    class Connecting(val bluetoothDevice: BluetoothDevice) : ConnectionState()
    class Connected(val socket: BluetoothSocket): ConnectionState()
    class ConnectionFailed(val failureReason: String): ConnectionState()
    object Disconnected: ConnectionState()
}