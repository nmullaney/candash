package dev.nmullaney.tesladashboard

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class DashRepository @ExperimentalCoroutinesApi
@Inject constructor(val pandaService: PandaService) {

    suspend fun startRequests() {
        pandaService.startRequests()
    }

    @ExperimentalCoroutinesApi
    fun shutdown() {
        pandaService.shutdown()
    }

    @ExperimentalCoroutinesApi
    fun carState() : Flow<CarState> {
        return pandaService.carState();
    }
}