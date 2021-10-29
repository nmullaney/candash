package dev.nmullaney.tesladashboard

import kotlinx.coroutines.flow.Flow

interface PandaService {
    suspend fun startRequests()
    suspend fun shutdown()
    fun carState() : Flow<CarState>
}