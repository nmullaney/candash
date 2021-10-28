package dev.nmullaney.tesladashboard

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DashViewModel @Inject constructor(private val dashRepository: DashRepository) : ViewModel() {
    private lateinit var careStateData: MutableLiveData<CarState>
    private var viewToShowData: MutableLiveData<String> = MutableLiveData()

    fun startUp() {
        viewModelScope.launch {
            dashRepository.startRequests()
        }
    }

    fun shutdown() {
        dashRepository.shutdown()
    }

    fun carState() : LiveData<CarState> {
        careStateData = MutableLiveData()
        val job = viewModelScope.launch {
            dashRepository.carState().collect {
                careStateData.postValue(it)
            }
        }
        return careStateData
    }

    fun fragmentNameToShow() : LiveData<String> = viewToShowData

    fun switchToDashFragment() {
        viewToShowData.value = "dash"
    }

    fun switchToInfoFragment() {
        viewToShowData.value = "info"
    }
}