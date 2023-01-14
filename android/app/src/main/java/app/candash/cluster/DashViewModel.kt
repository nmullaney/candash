package app.candash.cluster

import android.content.Context
import android.content.SharedPreferences
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.util.DisplayMetrics
import android.util.Log
import android.view.WindowManager
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.net.InetAddress
import java.sql.Timestamp
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject

@HiltViewModel
class DashViewModel @Inject constructor(private val dashRepository: DashRepository, private val sharedPreferences: SharedPreferences, @ApplicationContext context: Context) : ViewModel() {
    private val TAG = DashViewModel::class.java.simpleName

    private var carStateData: MutableLiveData<CarState> = MutableLiveData()
    private var viewToShowData: MutableLiveData<String> = MutableLiveData()
    private var zeroconfHost = MutableLiveData<String>()
    private var nsdManager = (context?.getSystemService(Context.NSD_SERVICE) as NsdManager?)!!
    private var carStateHistory: ConcurrentHashMap<String, TimestampedValue> = ConcurrentHashMap()
    private var windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var isSplitScreen = MutableLiveData<Boolean>()
    private var renderWidth : Float = 100f
    private var discoveryStarted = false
    private val _zeroConfIpAddress = MutableLiveData("0.0.0.0")
    val zeroConfIpAddress : LiveData<String>
        get() = _zeroConfIpAddress
    private var discoveryListener : NsdManager.DiscoveryListener? = null
    private var signalsToRequest : List <String> = arrayListOf()

    private lateinit var carStateJob: Job

    fun useMockServer() : Boolean = sharedPreferences.getBoolean(Constants.useMockServerPrefKey, false)

    fun setUseMockServer(useMockServer: Boolean) {
        sharedPreferences.edit().putBoolean(Constants.useMockServerPrefKey, useMockServer).apply()
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

    fun setServerIpAddress(ipAddress: String) {
        sharedPreferences.edit().putString(Constants.ipAddressPrefKey, ipAddress).apply()
    }

    fun saveSettings(ipAddress: String) {
        saveSettings(CANServiceType.values().indexOf(CANServiceType.PANDA), ipAddress)
    }

    fun saveSettings(selectedServicePosition: Int, ipAddress: String) {
        shutdown()
        cancelCarStateJob()
        setCANServiceByIndex(selectedServicePosition)
        setServerIpAddress(ipAddress)
        startUp(arrayListOf())
        startCarStateJob()
    }

    // An empty list will return all defined signals
    fun startUp(signalNamesToRequest: List<String> = arrayListOf()) {
        signalsToRequest = signalNamesToRequest
        viewModelScope.launch {
            dashRepository.startRequests(signalNamesToRequest)
        }
    }

    fun restart(){
        viewModelScope.launch {
            dashRepository.startRequests(signalsToRequest)
        }
    }

    fun isRunning() : Boolean{
        return dashRepository.isRunning()
    }

    fun shutdown() {

        viewModelScope.launch {
            dashRepository.shutdown()
        }
    }

    fun carState() : LiveData<CarState> {
        startCarStateJob()
        return carStateData
    }

    fun clearCarState() {
        if (carStateData.value != null) {
            carStateData.value = CarState()
        }
    }

    fun getValue(key: String): Float? {
        if (carStateHistory[key] != null){
            return carStateHistory[key]?.value
        }else {
            return null
        }
    }

    fun getTimestamp(key: String): Timestamp? {
        if (carStateHistory[key] != null){
            return carStateHistory[key]?.timestamp
        }else {
            return null
        }
    }

    fun cancelCarStateJob() {
        if (::carStateJob.isInitialized) {
            carStateJob.cancel()
        }
    }

    fun startCarStateJob() {
        carStateJob = viewModelScope.launch {
            dashRepository.carState().collect {
                //var oldTimestamp = Timestamp(System.currentTimeMillis())
                //var oldValue = 0f
                for ((k, v) in it.carData){
                    /*
                    carStateHistory()[k]?.let { carStateItemVal ->
                        oldTimestamp = carStateItemVal.timestamp
                        oldValue = carStateItemVal.value
                    }

                     */
                    var ts = TimestampedValue(k, v.toFloat(), Timestamp(System.currentTimeMillis()) )
                    carStateHistory()[k] = ts
                    //Log.d(TAG, "Appending to history:"+k + v.toString())
                    /*
                    if (k == Constants.turnSignalLeft && v != oldValue) {
                        var timeDiff = ts.timestamp.time - oldTimestamp.time
                        Log.d(TAG, "leftTurnSignal: " + v.toString() + "interval (ms): " + timeDiff.toString())

                    }

                     */
                }
                carStateData.postValue(it)

            }
        }
    }
    fun getScreenWidth(): Int {
        var displayMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(displayMetrics)
        return displayMetrics.widthPixels
    }

    fun getRealScreenWidth(): Int {
        var displayMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getRealMetrics(displayMetrics)
        return displayMetrics.widthPixels
    }

    fun isSplitScreen(): Boolean {

        return getRealScreenWidth() > getScreenWidth() * 2
    }
    fun setSplitScreen(ss: Boolean){
        with(isSplitScreen) { postValue(ss)}
    }
    fun getSplitScreen(): LiveData<Boolean>{
        return isSplitScreen
    }

    fun carStateHistory() : ConcurrentHashMap<String, TimestampedValue> {
        return carStateHistory
    }
    fun fragmentNameToShow() : LiveData<String> = viewToShowData

    fun switchToDashFragment() {
        //viewToShowData.value = "dash"
        viewToShowData.value = "track"
    }
    fun switchToSettingsFragment() {
        viewToShowData.value = "settings"
    }

    fun switchToInfoFragment() {
        viewToShowData.value = "info"
    }

    override fun onCleared() {
        super.onCleared()
        Log.d(TAG, "DashViewModel is cleared")
    }

    fun startDiscoveryService() {
        try {
            nsdManager?.discoverServices(
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

        when (errorCode) {
            //do nothing if we're at the max limit, just wait it out
            //NsdManager.FAILURE_MAX_LIMIT -> return
        }
        try {
            nsdManager?.stopServiceDiscovery(this)
        }catch (iae: java.lang.IllegalArgumentException){
            Log.e(TAG, "Unable to stop discovery service", iae)
        }
    }

    override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
        Log.e(TAG, "Discovery failed: Error code:$errorCode")
        try {
            nsdManager?.stopServiceDiscovery(this)
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