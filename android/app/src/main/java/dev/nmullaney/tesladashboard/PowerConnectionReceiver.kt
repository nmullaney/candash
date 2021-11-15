package dev.nmullaney.tesladashboard

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.BatteryManager

class PowerConnectionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        val batteryStatus: Intent? = IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { ifilter ->
            context?.registerReceiver(null, ifilter)
        }

        var status : Int? = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        val isCharging: Boolean = status == BatteryManager.BATTERY_STATUS_CHARGING
                || status == BatteryManager.BATTERY_STATUS_FULL


        if (isCharging) {
            var pm : PackageManager? = context?.packageManager

            var i : Intent? = context?.let { pm?.getLaunchIntentForPackage(it.packageName) }
            //i.setClassName("dev.nmullaney", "android.intent.action.MAIN");
            //i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (context != null) {
                context.startActivity(i)
            }
        }

    }
}