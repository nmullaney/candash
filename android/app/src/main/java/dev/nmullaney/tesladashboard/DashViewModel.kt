package dev.nmullaney.tesladashboard

import android.content.SharedPreferences
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DashViewModel @Inject constructor(private val dashRepository: DashRepository, private val sharedPreferences: SharedPreferences) : ViewModel() {
    private val TAG = DashViewModel::class.java.simpleName

    private lateinit var careStateData: MutableLiveData<CarState>
    private var viewToShowData: MutableLiveData<String> = MutableLiveData()
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
        careStateData = MutableLiveData()
        startCarStateJob()
        return careStateData
    }

    fun cancelCarStateJob() {
        carStateJob.cancel()
    }

    fun startCarStateJob() {
        carStateJob = viewModelScope.launch {
            dashRepository.carState().collect {
                careStateData.postValue(it)
            }
        }
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
}