package dev.nmullaney.tesladashboard

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class DashRepository @ExperimentalCoroutinesApi
@Inject constructor(val pandaService: PandaService) {

    @ExperimentalCoroutinesApi
    fun speed() : Flow<Int> {
        return pandaService.speed()
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