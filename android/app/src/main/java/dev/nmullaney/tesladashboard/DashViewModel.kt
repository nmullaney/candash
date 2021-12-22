package dev.nmullaney.tesladashboard

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

    private lateinit var carStateJob: Job

    fun useMockServer() : Boolean = sharedPreferences.getBoolean(Constants.useMockServerPrefKey, false)

    fun setUseMockServer(useMockServer: Boolean) {
        sharedPreferences.edit().putBoolean(Constants.useMockServerPrefKey, useMockServer).apply()
    }

    fun serverIpAddress() : String? = sharedPreferences.getString(Constants.ipAddressPrefKey, Constants.ipAddressLocalNetwork)

    fun setServerIpAddress(ipAddress: String) {
        sharedPreferences.edit().putString(Constants.ipAddressPrefKey, ipAddress).apply()
    }

    fun saveSettings(useMockServer: Boolean, ipAddress: String) {
        shutdown()
        cancelCarStateJob()
        setServerIpAddress(ipAddress)
        setUseMockServer(useMockServer)
        startUp()
        startCarStateJob()
    }

    fun startUp() {
        viewModelScope.launch {
            dashRepository.startRequests()
        }
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
                for ((k, v) in it.carData){
                    var ts = TimestampedValue(k, v.toFloat(), Timestamp(System.currentTimeMillis()) )
                    carStateHistory()[k] = ts
                    Log.d(TAG, "Appending to history:"+k + v.toString())
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
        viewToShowData.value = "dash"
    }

    fun switchToInfoFragment() {
        viewToShowData.value = "info"
    }

    override fun onCleared() {
        super.onCleared()
        Log.d(TAG, "DashViewModel is cleared")
    }

    fun getZeroconfHost(): LiveData<String> {
        nsdManager?.discoverServices("_panda._udp", NsdManager.PROTOCOL_DNS_SD, getDiscoveryListener())
        return zeroconfHost
    }

    fun getResolveListener() : NsdManager.ResolveListener {
        val resolveListener = object : NsdManager.ResolveListener {
            override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                // Called when the resolve fails. Use the error code to debug.
                Log.e(TAG, "Resolve failed: $errorCode")
            }

            override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
                Log.e(TAG, "Resolve Succeeded. $serviceInfo")
                val host: InetAddress = serviceInfo.host
                zeroconfHost.postValue(host.hostAddress)
                Log.e(TAG, "IP Succeeded. $zeroconfHost")

            }
        }
        return resolveListener
    }
    fun getDiscoveryListener() : NsdManager.DiscoveryListener {
        val discoveryListener = object : NsdManager.DiscoveryListener {

            // Called as soon as service discovery begins.
            override fun onDiscoveryStarted(regType: String) {
                Log.d(TAG, "Service discovery started")
            }

            override fun onServiceFound(service: NsdServiceInfo) {
                // A service was found! Do something with it.
                Log.d(TAG, "Service discovery success$service")
                Log.d(TAG, "Service Type: ${service.serviceType} Service Name: ${service.serviceName}")

                nsdManager.resolveService(service, getResolveListener())
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

                nsdManager?.stopServiceDiscovery(this)
            }

            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
                Log.e(TAG, "Discovery failed: Error code:$errorCode")
                nsdManager?.stopServiceDiscovery(this)
            }
        }
        return discoveryListener
    }
}