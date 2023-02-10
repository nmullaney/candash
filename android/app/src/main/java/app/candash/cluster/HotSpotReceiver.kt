package app.candash.cluster

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.wifi.WifiManager

class HotSpotReceiver(private val viewModel: DashViewModel) : BroadcastReceiver() {
    override fun onReceive(contxt: Context, intent: Intent) {
        if ("android.net.wifi.WIFI_AP_STATE_CHANGED" == intent.action) {
            val state = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, 0)
            val prevState = intent.getIntExtra(WifiManager.EXTRA_PREVIOUS_WIFI_STATE, 0)
            if ((WifiManager.WIFI_STATE_ENABLED == state % 10) && (WifiManager.WIFI_STATE_ENABLED != prevState % 10)) {
                viewModel.restart()
            }
        }
    }
}