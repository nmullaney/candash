package dev.nmullaney.tesladashboard

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class DashViewModel @Inject constructor(private val dashRepository: DashRepository) : ViewModel() {
    private lateinit var speedData: MutableLiveData<Int>

    fun speed() : LiveData<Int> {
        speedData = MutableLiveData()
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                dashRepository.speed().collect {
                    speedData.postValue(it)
                }
            }
        }
        return speedData
    }

    fun shutdown() {
        dashRepository.shutdown()
    }
}