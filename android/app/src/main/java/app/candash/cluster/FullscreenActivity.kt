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
import kotlin.math.pow

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
@AndroidEntryPoint
class FullscreenActivity : AppCompatActivity() {
    private var handler: Handler = Handler()
    private var runnable: Runnable? = null
    private var delay = 1000
    private var currentTheme = 0
    
    private var currentFragmentName: String = "dash"

    private lateinit var viewModel: DashViewModel
    private lateinit var prefs: SharedPreferences

    @RequiresApi(Build.VERSION_CODES.N)
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        // Checks the orientation of the screen
        if (isInMultiWindowMode) {
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
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(this).get(DashViewModel::class.java)
        prefs = getSharedPreferences("dash", Context.MODE_PRIVATE)

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
        applyTheme(getDashTheme())

        setContentView(R.layout.activity_fullscreen)
        setStatusBarColor()
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

        viewModel.isSplitScreen()
        viewModel.fragmentNameToShow().observe(this) {
            currentFragmentName = it
            applyTheme(getDashTheme())
            switchToFragment(it)
        }

        // Listen for carstate theme changes
        viewModel.onSignal(this, SName.isDarkMode) {
            if (it != null) {
                if (it != prefs.getPref(Constants.lastDarkMode)) {
                    prefs.setPref(Constants.lastDarkMode, it)
                    applyThemeAndReload(getDashTheme())
                }
            }
        }
        viewModel.onSignal(this, SName.isSunUp) {
            if (it != null) {
                if (it != prefs.getPref(Constants.lastSunUp)) {
                    prefs.setPref(Constants.lastSunUp, it)
                    applyThemeAndReload(getDashTheme())
                }
            }
        }

        // Listen for manual theme changes
        viewModel.themeUpdate.observe(this) {
            applyThemeAndReload(getDashTheme())
        }

        // Listen for display brightness changes
        viewModel.onSignal(this, SName.displayBrightnessLev) {
            setBrightness()
        }
    }

    private fun setBrightness() {
        val displayBrightnessLev = viewModel.carState[SName.displayBrightnessLev]
        if (displayBrightnessLev != null) {
            // if user has forced dark mode but car is in light mode, add 0.3 to level to compensate
            window.attributes.screenBrightness =
                if (prefs.getBooleanPref(Constants.forceDarkMode) && prefs.getPref(Constants.lastDarkMode) == 0f) {
                    (displayBrightnessLev.pow(0.5f)).coerceIn(0f, 1f)
                } else {
                    displayBrightnessLev.coerceIn(0f, 1f)
                }
        }
    }

    private fun isDarkMode(): Boolean {
        val dark = viewModel.carState[SName.isDarkMode] ?: prefs.getPref(Constants.lastDarkMode)
        return prefs.getBooleanPref(Constants.forceDarkMode) || dark == 1f
    }

    private fun isSunUp(): Boolean {
        val sunUp = viewModel.carState[SName.isSunUp] ?: prefs.getPref(Constants.lastSunUp)
        return sunUp == 1f
    }

    private fun getDashTheme(): Int {
        val dark = isDarkMode()
        val sunUp = isSunUp()
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
    
    private fun applyTheme(theme: Int) {
        if (currentTheme != theme) {
            currentTheme = theme
            setTheme(theme)
            setStatusBarColor()
        }
        setBrightness()
    }

    private fun applyThemeAndReload(theme: Int) {
        applyTheme(theme)
        // Don't recreate the activity, just recreate the fragment
        switchToFragment(currentFragmentName)
    }

    private fun setStatusBarColor() {
        // this could fail if content view is not set yet
        if (findViewById<View>(android.R.id.content ) == null) {
            return
        }
        val color = if (isDarkMode() && isSunUp()) {
            resources.getColor(R.color.dark_background)
        } else if (isDarkMode()) {
            resources.getColor(R.color.night_background)
        } else {
            resources.getColor(R.color.day_background)
        }
        window.statusBarColor = color
        window.navigationBarColor = color

        // Set status bar text color to match background to try to hide it
        if (isDarkMode()) {
            // For Android 11 and above
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                window.insetsController?.setSystemBarsAppearance(WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS, WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS)
            } else {
                // For earlier versions
                @Suppress("DEPRECATION")
                window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            }
        } else {
            // For Android 11 and above
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                window.insetsController?.setSystemBarsAppearance(0, WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS)
            } else {
                // For earlier versions
                @Suppress("DEPRECATION")
                window.decorView.systemUiVisibility = 0
            }
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