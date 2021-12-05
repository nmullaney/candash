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
                Constants.battVolts to 390.1,
                Constants.blindSpotLeft to 3.0,
                Constants.blindSpotRight to 3.0,
                Constants.displayBrightnessLev to 11.5,
                Constants.stateOfCharge to 1.0,
                Constants.uiRange to 273.0,
                Constants.vehicleSpeed to 0.0,
                Constants.blindSpotLeft to 3.0,
                Constants.blindSpotRight to 0.0,
                Constants.turnSignalLeft to 1.0,
                Constants.isSunUp to 1.0,
                Constants.autopilotState to 2,
                Constants.rearLeftVehicle to 500,
                Constants.autopilotHands to 1,
                Constants.uiSpeedUnits to 0,
                Constants.gearSelected to Constants.gearPark,
                Constants.battAmps to 20.0

            )),
            CarState(mutableMapOf(
                Constants.battVolts to 390.0,
                Constants.blindSpotLeft to 2.0,
                Constants.blindSpotRight to 2.0,
                Constants.displayBrightnessLev to 10.5,
                Constants.stateOfCharge to 79.0,
                Constants.uiRange to 270.0,
                Constants.vehicleSpeed to 20.0,
                Constants.blindSpotLeft to 3.0,
                Constants.blindSpotRight to 0.0,
                Constants.turnSignalLeft to 2.0,
                Constants.isSunUp to 1.0,
                Constants.autopilotState to 1,
                Constants.autopilotHands to 1,
                Constants.cruiseControlSpeed to 45.0,
                Constants.rearLeftVehicle to 500,
                Constants.uiSpeedUnits to 0,
                Constants.gearSelected to Constants.gearPark,
                Constants.battAmps to 140.0
            )),
            CarState(mutableMapOf(
                Constants.battVolts to 389.9,
                Constants.blindSpotLeft to 1.0,
                Constants.blindSpotRight to 1.0,
                Constants.displayBrightnessLev to 9.5,
                Constants.stateOfCharge to 50.0,
                Constants.uiRange to 268.0,
                Constants.vehicleSpeed to 21.0,
                Constants.blindSpotLeft to 0.0,
                Constants.blindSpotRight to 2.0,
                Constants.turnSignalLeft to 1.0,
                Constants.isSunUp to 1.0,
                Constants.autopilotState to 3,
                Constants.autopilotHands to 4,
                Constants.maxSpeedAP to 45.0,
                Constants.cruiseControlSpeed to 45.0,
                Constants.uiSpeedUnits to 0,
                Constants.gearSelected to Constants.gearDrive,
                Constants.battAmps to 3.0
            )),
            CarState(mutableMapOf(
                Constants.battVolts to 389.8,
                Constants.displayBrightnessLev to 8.5,
                Constants.stateOfCharge to 25.0,
                Constants.uiRange to 265.0,
                Constants.vehicleSpeed to 25.0,
                Constants.blindSpotLeft to 2.0,
                Constants.blindSpotRight to 1.0,
                Constants.turnSignalLeft to 0.0,
                Constants.isSunUp to 1.0,
                Constants.autopilotState to 3,
                Constants.autopilotHands to 1,
                Constants.maxSpeedAP to 45.0,
                Constants.rearLeftVehicle to 200,
                Constants.uiSpeedUnits to 0,
                Constants.cruiseControlSpeed to 45.0,
                Constants.battAmps to 750.0
            )),
            CarState(mutableMapOf(
                Constants.battVolts to 389.7,
                Constants.blindSpotLeft to 2.0,
                Constants.blindSpotRight to 2.0,
                Constants.displayBrightnessLev to 7.5,
                Constants.stateOfCharge to 76.0,
                Constants.uiRange to 264.0,
                Constants.vehicleSpeed to 29.0,
                Constants.blindSpotLeft to 2.0,
                Constants.blindSpotRight to 0.0,
                Constants.turnSignalLeft to 1.0,
                Constants.isSunUp to 0.0,
                Constants.autopilotState to 3,
                Constants.autopilotHands to 1,
                Constants.uiSpeedUnits to 0,
                Constants.rearLeftVehicle to 500,
                Constants.maxSpeedAP to 25.0,
                Constants.cruiseControlSpeed to 23.0,
                Constants.battAmps to -200.0
            )))
}