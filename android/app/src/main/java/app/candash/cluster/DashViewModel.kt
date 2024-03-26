package app.candash.cluster

import android.content.Context
import android.content.SharedPreferences
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.util.DisplayMetrics
import android.util.Log
import android.view.WindowManager
import androidx.lifecycle.*
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.launch
import java.net.InetAddress
import javax.inject.Inject

@HiltViewModel
class DashViewModel @Inject constructor(private val dashRepository: DashRepository, private val sharedPreferences: SharedPreferences, @ApplicationContext context: Context) : ViewModel() {
    private val TAG = DashViewModel::class.java.simpleName

    var carState = dashRepository.carState()
    private var liveCarState = dashRepository.liveCarState()
    private var viewToShowData: MutableLiveData<String> = MutableLiveData()
    private var nsdManager = (context.getSystemService(Context.NSD_SERVICE) as NsdManager?)!!
    private var windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var isSplitScreen = MutableLiveData<Boolean>()
    private val _zeroConfIpAddress = MutableLiveData("0.0.0.0")
    private val _themeUpdate = MutableLiveData<Boolean>()
    val themeUpdate: LiveData<Boolean> = _themeUpdate
    private val _brightnessUpdate = MutableLiveData<Boolean>()
    val brightnessUpdate: LiveData<Boolean> = _brightnessUpdate
    val zeroConfIpAddress : LiveData<String>
        get() = _zeroConfIpAddress
    private var discoveryListener : NsdManager.DiscoveryListener? = null
    private var signalsToRequest : List <String> = arrayListOf()

    fun updateTheme() {
        _themeUpdate.value = true
    }

    fun updateBrightness() {
        _brightnessUpdate.value = true
    }

    fun getCurrentCANServiceIndex() : Int {
        return dashRepository.getCANServiceType().ordinal
    }

    fun getCANServiceOptions(context: Context) : List<String> {
        return CANServiceType.values().map { context.getString(it.nameResId) }
    }

    private fun setCANServiceByIndex(selectedPosition: Int) {
        val type = CANServiceType.values()[selectedPosition]
        dashRepository.setCANServiceType(type)
    }

    fun serverIpAddress() : String? = sharedPreferences.getString(Constants.ipAddressPrefKey, Constants.ipAddressLocalNetwork)

    private fun setServerIpAddress(ipAddress: String) {
        sharedPreferences.edit().putString(Constants.ipAddressPrefKey, ipAddress).apply()
    }

    fun saveSettings(ipAddress: String) {
        saveSettings(CANServiceType.values().indexOf(CANServiceType.PANDA), ipAddress)
    }

    fun saveSettings(selectedServicePosition: Int, ipAddress: String) {
        shutdown()
        setCANServiceByIndex(selectedServicePosition)
        setServerIpAddress(ipAddress)
        restart()
    }

    // An empty list will return all defined signals
    fun startUp(signalNamesToRequest: List<String> = arrayListOf()) {
        carState = dashRepository.carState()
        liveCarState = dashRepository.liveCarState()
        signalsToRequest = signalNamesToRequest
        viewModelScope.launch {
            dashRepository.startRequests(signalNamesToRequest)
        }
    }

    fun restart(){
        if (isRunning()) {
            shutdown()
        }
        startUp(signalsToRequest)
    }

    fun isRunning() : Boolean{
        return dashRepository.isRunning()
    }

    fun shutdown() {
        viewModelScope.launch {
            dashRepository.shutdown()
        }
    }

    fun clearCarState() {
        dashRepository.clearCarState()
    }

    /**
     * This observes a single CAN signal, running the provided unit when the signal values changes
     * from it's previous value.
     *
     * @param owner Provide the view's lifecycle owner
     * @param signal The signal name from `SName` to watch for changes
     * @param onChanged The unit to execute when the signal value changes. The value of the observed
     * signal is provided to the unit.
     */
    fun onSignal(owner: LifecycleOwner, signal: String, onChanged: (value: Float?) -> Unit) {
        liveCarState[signal]!!.observe(owner) { onChanged(it) }
    }

    /**
     * This observes a list of CAN signals, running the provided unit when ANY of the signal values
     * change from it's previous value.
     *
     * @param owner Provide the view's lifecycle owner
     * @param signals A list of signal names from `SName` to watch for changes
     * @param onChanged The unit to execute when a signal value changes. The carState with all
     * signals is provided to the unit.
     */
    fun onSomeSignals(owner: LifecycleOwner, signals: List<String>, onChanged: (carState: CarState) -> Unit) {
        signals.forEach { signal ->
            liveCarState[signal]!!.observe(owner) { onChanged(carState) }
        }
    }

    /**
     * This observes ALL CAN signals, running the provided unit when ANY of the signal values
     * change from it's previous value.
     *
     * CAUTION: try to use `onSomeSignals` instead, `onAllSignals` will cause your unit to execute at ridiculous rates.
     *
     * @param owner Provide the view's lifecycle owner
     * @param onChanged The unit to execute when a signal value changes. The carState with all
     * signals is provided to the unit.
     */
    fun onAllSignals(owner: LifecycleOwner, onChanged: (carState: CarState) -> Unit) {
        liveCarState.forEach {
            it.value.observe(owner) { onChanged(carState) }
        }
    }

    /**
     * Returns a sorted list of all signal names available
     */
    fun allSignalNames(): List<String> {
        val result = mutableListOf<String>()
        liveCarState.forEach {
            result.add(it.key)
        }
        // Sort by name
        result.sort()
        return result.toList()
    }

    private fun getScreenWidth(): Int {
        val displayMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(displayMetrics)
        return displayMetrics.widthPixels
    }

    private fun getRealScreenWidth(): Int {
        val displayMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getRealMetrics(displayMetrics)
        return displayMetrics.widthPixels
    }

    fun isSplitScreen(): Boolean {
        return getRealScreenWidth() > getScreenWidth() * 2
    }

    fun setSplitScreen(){
        with(isSplitScreen) { postValue(isSplitScreen())}
    }

    fun getSplitScreen(): LiveData<Boolean>{
        return isSplitScreen
    }

    fun fragmentNameToShow() : LiveData<String> = viewToShowData

    fun switchToDashFragment() {
        viewToShowData.value = "dash"
    }

    fun switchToSettingsFragment() {
        viewToShowData.value = "settings"
    }

    fun switchToInfoFragment() {
        viewToShowData.value = "info"
    }

    fun switchToPartyFragment() {
        viewToShowData.value = "party"
    }

    override fun onCleared() {
        super.onCleared()
        Log.d(TAG, "DashViewModel is cleared")
    }

    fun startDiscoveryService() {
        try {
            nsdManager.discoverServices(
                "_panda._udp",
                NsdManager.PROTOCOL_DNS_SD,
                createDiscoveryListener()
            )
        } catch (iae: java.lang.IllegalArgumentException) {
            Log.e(TAG, "Unable to start discovery service", iae)
        }
    }

    fun stopDiscoveryService() {
        try {
            if (discoveryListener != null) {

                nsdManager.apply {
                    stopServiceDiscovery(discoveryListener)
                }

            }
        }catch (iae: java.lang.IllegalArgumentException) {
            Log.e(TAG, "Unable to stop discovery service", iae)
        }
    }


    private fun createDiscoveryListener() : NsdManager.DiscoveryListener =
        NsdDiscoveryListener(nsdManager, _zeroConfIpAddress)


    fun startThemeListener(owner: LifecycleOwner) {
        this.onSignal(owner, SName.isDarkMode) {
            if (it != null) {
                if (it != sharedPreferences.getPref(Constants.lastDarkMode)) {
                    sharedPreferences.setPref(Constants.lastDarkMode, it)
                    this.updateTheme()
                }
            }
        }

        this.onSignal(owner, SName.isSunUp) {
            if (it != null) {
                if (it != sharedPreferences.getPref(Constants.lastSunUp)) {
                    sharedPreferences.setPref(Constants.lastSunUp, it)
                    this.updateTheme()
                }
            }
        }
    }

    fun startBrightnessListener(owner: LifecycleOwner) {
        this.onSomeSignals(
            owner, listOf(
                SName.displayBrightnessLev,
                SName.solarBrightnessFactor
            )
        ) {
            val solarBrightnessFactor = it[SName.solarBrightnessFactor]
            if (solarBrightnessFactor != null) {
                sharedPreferences.setPref(Constants.lastSolarBrightnessFactor, solarBrightnessFactor)
            }
            this.updateBrightness()
        }
    }
}

class NsdDiscoveryListener(
    private val nsdManager: NsdManager,
    private val ipAddressLiveData: MutableLiveData<String>
) : NsdManager.DiscoveryListener {

    companion object {
        const val TAG = "NsdDiscoveryListener"
    }

    private var discoveryStarted = false

    // Called as soon as service discovery begins.
    override fun onDiscoveryStarted(regType: String) {
        Log.d(TAG, "Service discovery started")
        discoveryStarted = true
    }


    override fun onServiceFound(service: NsdServiceInfo) {
        // A service was found! Do something with it.
        Log.d(TAG, "Service discovery success$service")
        Log.d(TAG, "Service Type: ${service.serviceType} Service Name: ${service.serviceName}")
        nsdManager.resolveService(service, NsdResolveListener(ipAddressLiveData))
        discoveryStarted = false
    }

    override fun onServiceLost(service: NsdServiceInfo) {
        // When the network service is no longer available.
        // Internal bookkeeping code goes here.
        Log.e(TAG, "service lost: $service")
    }

    override fun onDiscoveryStopped(serviceType: String) {
        Log.i(TAG, "Discovery stopped: $serviceType")
    }

    override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
        Log.e(TAG, "Discovery failed: Error code:$errorCode")
        
        //do nothing if we're at the max limit, just wait it out
        /*when (errorCode) {
            NsdManager.FAILURE_MAX_LIMIT -> return
        }*/
        try {
            nsdManager.stopServiceDiscovery(this)
        }catch (iae: java.lang.IllegalArgumentException){
            Log.e(TAG, "Unable to stop discovery service", iae)
        }
    }

    override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
        Log.e(TAG, "Discovery failed: Error code:$errorCode")
        try {
            nsdManager.stopServiceDiscovery(this)
        }catch (iae: java.lang.IllegalArgumentException){
            Log.e(TAG, "Unable to stop discovery service", iae)
        }
    }
}

class NsdResolveListener(private val ipAddressLiveData: MutableLiveData<String>) :
    NsdManager.ResolveListener {

    companion object {
        const val TAG = "NsdResolveListener"
    }

    override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
        // Called when the resolve fails. Use the error code to debug.
        Log.e(TAG, "Resolve failed: $errorCode")
    }

    override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
        Log.e(TAG, "Resolve Succeeded. $serviceInfo")
        val host: InetAddress = serviceInfo.host
        ipAddressLiveData.postValue(host.hostAddress)
        Log.e(TAG, "IP Succeeded. $host")

    }
}