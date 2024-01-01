package app.candash.cluster

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.res.Configuration
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import dagger.hilt.android.AndroidEntryPoint

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
@AndroidEntryPoint
class FullscreenActivity : AppCompatActivity() {
    private var handler: Handler = Handler()
    private var runnable: Runnable? = null
    private var delay = 1000
    
    private var currentFragmentName: String = "dash"

    private lateinit var viewModel: DashViewModel

    @RequiresApi(Build.VERSION_CODES.N)
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        // Checks the orientation of the screen
        if (isInMultiWindowMode) {
            viewModel = ViewModelProvider(this).get(DashViewModel::class.java)
            // isInMultiWindowMode is always true in versions > R, so only do this for R
            if (Build.VERSION.SDK_INT == Build.VERSION_CODES.R) {
                window.insetsController?.show(WindowInsets.Type.statusBars())
            }
            viewModel.setSplitScreen()
        } else {
            if (Build.VERSION.SDK_INT == Build.VERSION_CODES.R) {
                window.insetsController?.hide(WindowInsets.Type.statusBars())
            }
            window.decorView.systemUiVisibility =
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION xor View.SYSTEM_UI_FLAG_FULLSCREEN xor View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY xor View.SYSTEM_UI_FLAG_LAYOUT_STABLE xor View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN

            viewModel.setSplitScreen()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val prefs = getSharedPreferences("dash", Context.MODE_PRIVATE)
        super.onCreate(savedInstanceState)
        // check every second if battery is connected
        val context = applicationContext

        handler.postDelayed(Runnable {
            handler.postDelayed(runnable!!, delay.toLong())
            // get battery status to decide whether or not to disable screen dimming
            val batteryStatus: Intent? =
                IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { iFilter ->
                    context?.registerReceiver(null, iFilter)
                }
            // How are we charging?
            val chargePlug: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1) ?: -1
            val isPlugged: Boolean = chargePlug == BatteryManager.BATTERY_PLUGGED_USB
                    || chargePlug == BatteryManager.BATTERY_PLUGGED_AC
                    || chargePlug == BatteryManager.BATTERY_PLUGGED_WIRELESS
            //Log.d(TAG, "keep_screen_on" + isPlugged.toString())
            
            val batteryPct: Float? = batteryStatus?.let { intent ->
                val level: Int = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale: Int = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                level * 100f / scale
            }
            val lowBattery: Boolean = batteryPct == null || batteryPct <= 20f

            if (isPlugged || (!lowBattery && !prefs.getBooleanPref(Constants.disableDisplaySync))) {
                this@FullscreenActivity.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                //Log.d(TAG, "keep_screen_on")
            } else {
                this@FullscreenActivity.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                //Log.d(TAG, "do not keep screen on")
            }
        }.also { runnable = it }, delay.toLong())

        // Set initial theme
        val theme = getTheme(prefs)
        setTheme(theme)
        setStatusBarColor(prefs)

        setContentView(R.layout.activity_fullscreen)
        // This is a known unsafe cast, but is safe in the only correct use case:
        // TeslaDashboardApplication extends Hilt_TeslaDashboardApplication
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.systemBarsBehavior =
                WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        } else {
            window.setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
            )
        }

        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION xor View.SYSTEM_UI_FLAG_FULLSCREEN xor View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY xor View.SYSTEM_UI_FLAG_LAYOUT_STABLE xor View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        supportFragmentManager
            .beginTransaction()
            .add(R.id.fragment_container, DashFragment())
            .commit()

        viewModel = ViewModelProvider(this).get(DashViewModel::class.java)
        viewModel.isSplitScreen()
        viewModel.fragmentNameToShow().observe(this) {
            currentFragmentName = it
            setTheme(getTheme(prefs))
            setStatusBarColor(prefs)
            switchToFragment(it)
        }

        // Listen for carstate theme changes
        viewModel.onSignal(this, SName.isDarkMode) {
            if (it != null) {
                if (it != prefs.getPref(Constants.lastDarkMode)) {
                    prefs.setPref(Constants.lastDarkMode, it)
                    updateTheme(getTheme(prefs))
                    setStatusBarColor(prefs)
                }
            }
        }
        viewModel.onSignal(this, SName.isSunUp) {
            if (it != null) {
                if (it != prefs.getPref(Constants.lastSunUp)) {
                    prefs.setPref(Constants.lastSunUp, it)
                    updateTheme(getTheme(prefs))
                    setStatusBarColor(prefs)
                }
            }
        }

        // Listen for manual theme changes
        viewModel.themeUpdate.observe(this) {
            updateTheme(getTheme(prefs))
            setStatusBarColor(prefs)
        }
    }

    private fun getTheme(prefs: SharedPreferences): Int {
        val dark = prefs.getPref(Constants.lastDarkMode) == 1f || prefs.getBooleanPref(Constants.forceDarkMode)
        val sunUp = prefs.getPref(Constants.lastSunUp) == 1f
        val cyber = prefs.getBooleanPref(Constants.cyberMode)
        return when {
            dark && sunUp && cyber -> R.style.Theme_TeslaDashboard_Cyber_Dark
            dark && cyber -> R.style.Theme_TeslaDashboard_Cyber_Night
            cyber -> R.style.Theme_TeslaDashboard_Cyber
            dark && sunUp -> R.style.Theme_TeslaDashboard_Dark
            dark -> R.style.Theme_TeslaDashboard_Night
            else -> R.style.Theme_TeslaDashboard
        }
    }

    private fun updateTheme(theme: Int) {
        setTheme(theme)
        // Don't recreate the activity, just recreate the fragment
        switchToFragment(currentFragmentName)
    }

    private fun setStatusBarColor(prefs: SharedPreferences) {
        val dark = prefs.getPref(Constants.lastDarkMode) == 1f || prefs.getBooleanPref(Constants.forceDarkMode)
        if (dark) {
            window.statusBarColor = resources.getColor(R.color.night_background)
        } else {
            window.statusBarColor = resources.getColor(R.color.day_background)
        }
    }

    private fun switchToFragment(fragmentName: String) {
        val fragment = when (fragmentName) {
            "dash" -> DashFragment()
            "info" -> InfoFragment()
            "settings" -> SettingsFragment()
            "party" -> PartyFragment()
            else -> throw IllegalStateException("Attempting to switch to unknown fragment: $fragmentName")
        }
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }

    override fun onResume() {
        super.onResume()
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION xor View.SYSTEM_UI_FLAG_FULLSCREEN xor View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY xor View.SYSTEM_UI_FLAG_LAYOUT_STABLE xor View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        viewModel.startUp()
    }

    override fun onStart() {
        super.onStart()
        viewModel.startUp()
    }

    override fun onStop() {
        super.onStop()
        viewModel.shutdown()
    }

    override fun onDestroy() {
        handler.removeCallbacks(runnable!!)
        super.onDestroy()
    }
}