package dev.nmullaney.tesladashboard

import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class DashRepository @Inject constructor(val pandaService: PandaService) {

    fun speed() : Flow<Int> {
        return pandaService.speed()
    }

    fun shutdown() {
        pandaService.shutdown()
    }
}