package app.candash.cluster

import android.content.*
import android.content.pm.PackageManager
import android.os.BatteryManager

class PowerConnectionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        val TAG = DashViewModel::class.java.simpleName

        val batteryStatus: Intent? = IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { ifilter ->
            context?.registerReceiver(null, ifilter)
        }


// How are we charging?
        val chargePlug: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1) ?: -1
        val usbCharge: Boolean = chargePlug == BatteryManager.BATTERY_PLUGGED_USB
        val acCharge: Boolean = chargePlug == BatteryManager.BATTERY_PLUGGED_AC
        val isPlugged: Boolean = chargePlug == BatteryManager.BATTERY_PLUGGED_USB
                || chargePlug == BatteryManager.BATTERY_PLUGGED_AC
                || chargePlug == BatteryManager.BATTERY_PLUGGED_WIRELESS

        if (isPlugged) {
            var pm : PackageManager? = context?.packageManager

            var i : Intent? = context?.let { pm?.getLaunchIntentForPackage(it.packageName) }
            //i.setClassName("dev.nmullaney", "android.intent.action.MAIN");
            //i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        }

    }
}

class RunOnStartup : BroadcastReceiver() {
    private lateinit var prefs: SharedPreferences

    override fun onReceive(context: Context, intent: Intent) {
        val i = Intent(
            context,
            app.candash.cluster.FullscreenActivity::class.java
        )
        prefs = context.getSharedPreferences("dash", Context.MODE_PRIVATE)
        if (prefs.getBooleanPref(Constants.StartOnBoot)) {
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(i)
        }
    }
}