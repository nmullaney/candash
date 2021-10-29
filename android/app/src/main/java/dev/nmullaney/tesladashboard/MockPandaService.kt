package dev.nmullaney.tesladashboard

import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

class MockPandaService : PandaService {
    private val MS_BETWEEN_REQUESTS = 2_000L
    private val carStateFlow = MutableStateFlow(CarState())
    private val pandaContext = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
    private var shutdown = false
    private var count : AtomicInteger = AtomicInteger(0)

    override suspend fun startRequests() {
        withContext(pandaContext) {
            shutdown = false
            while (!shutdown) {
                delay(MS_BETWEEN_REQUESTS)
                carStateFlow.value = mockCarStates()[count.getAndAdd(1) % mockCarStates().size]
                yield()
            }
        }
    }

    override suspend fun shutdown() {
        withContext(pandaContext) {
            shutdown = true
        }
    }

    override fun carState(): Flow<CarState> {
        return carStateFlow
    }

    private fun mockCarStates() : List<CarState> =
        listOf(
            CarState(mutableMapOf(
                ChannelConstants.battVolts to 390.1,
                ChannelConstants.blindSpotLeft to 3.0,
                ChannelConstants.blindSpotRight to 3.0,
                ChannelConstants.displayBrightnessLev to 11.5,
                ChannelConstants.stateOfCharge to 80.0,
                ChannelConstants.uiRange to 273.0,
                ChannelConstants.uiSpeed to 0.0
            )),
            CarState(mutableMapOf(
                ChannelConstants.battVolts to 390.0,
                ChannelConstants.blindSpotLeft to 2.0,
                ChannelConstants.blindSpotRight to 2.0,
                ChannelConstants.displayBrightnessLev to 10.5,
                ChannelConstants.stateOfCharge to 79.0,
                ChannelConstants.uiRange to 270.0,
                ChannelConstants.uiSpeed to 10.0
            )),
            CarState(mutableMapOf(
                ChannelConstants.battVolts to 389.9,
                ChannelConstants.blindSpotLeft to 1.0,
                ChannelConstants.blindSpotRight to 1.0,
                ChannelConstants.displayBrightnessLev to 9.5,
                ChannelConstants.stateOfCharge to 78.0,
                ChannelConstants.uiRange to 268.0,
                ChannelConstants.uiSpeed to 20.0
            )),
            CarState(mutableMapOf(
                ChannelConstants.battVolts to 389.8,
                ChannelConstants.blindSpotLeft to 0.0,
                ChannelConstants.blindSpotRight to 0.0,
                ChannelConstants.displayBrightnessLev to 8.5,
                ChannelConstants.stateOfCharge to 77.0,
                ChannelConstants.uiRange to 265.0,
                ChannelConstants.uiSpeed to 30.0
            )),
            CarState(mutableMapOf(
                ChannelConstants.battVolts to 389.7,
                ChannelConstants.blindSpotLeft to 2.0,
                ChannelConstants.blindSpotRight to 2.0,
                ChannelConstants.displayBrightnessLev to 7.5,
                ChannelConstants.stateOfCharge to 76.0,
                ChannelConstants.uiRange to 264.0,
                ChannelConstants.uiSpeed to 40.0
            )))
}