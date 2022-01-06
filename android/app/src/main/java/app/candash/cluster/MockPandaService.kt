package app.candash.cluster

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
    private var count: AtomicInteger = AtomicInteger(0)

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
                Constants.autopilotState to 1f,
                Constants.isSunUp to 1f,
                Constants.autopilotHands to 1f,
                Constants.driveConfig to 0f,
                Constants.gearSelected to Constants.gearPark.toFloat(),
                Constants.stateOfCharge to 70f,
                Constants.battAmps to -20f,
                Constants.battVolts to 390f


                )),
            CarState(mutableMapOf(
                Constants.autopilotState to 3f,
                Constants.isSunUp to 1f,
                Constants.autopilotHands to 1f,
                Constants.driveConfig to 0f,
                Constants.gearSelected to Constants.gearPark.toFloat(),
                Constants.stateOfCharge to 70f,
                Constants.battAmps to -23f,
                Constants.battVolts to 390f


            )),
            CarState(mutableMapOf(
                Constants.autopilotState to 3f,
                Constants.isSunUp to 1f,
                Constants.driveConfig to 1f,
                Constants.stateOfCharge to 70f,
                Constants.battAmps to -10f,
                Constants.battVolts to 390f


            )),
            CarState(mutableMapOf(
                Constants.autopilotState to 1f,
                Constants.isSunUp to 1f
            )))
}
/*
    private fun mockCarStates() : List<CarState> =
        listOf(
            CarState(mutableMapOf(
                Constants.battVolts to 390.1f,
                Constants.frontLeftDoorState to 1.0f,
                Constants.displayBrightnessLev to 11.5f,
                Constants.stateOfCharge to 1.0f,
                Constants.uiRange to 273.0f,
                Constants.uiSpeed to 0.0f,
                //Constants.blindSpotLeft to 3.0f,
                Constants.blindSpotRight to 0.0f,
                Constants.turnSignalLeft to 1.0f,
                Constants.isSunUp to 1.0f,
                Constants.autopilotState to 2f,
                Constants.rearLeftVehicle to 100f,
                Constants.autopilotHands to 1f,
                Constants.uiSpeedUnits to 0f,
                Constants.gearSelected to Constants.gearPark.toFloat(),
                Constants.battAmps to 20.0f

            )),
            CarState(mutableMapOf(
                Constants.battVolts to 390.0f,
                Constants.frontLeftDoorState to 1.0f,

                Constants.displayBrightnessLev to 10.5f,
                Constants.stateOfCharge to 79.0f,
                Constants.uiRange to 270.0f,
                Constants.uiSpeed to 20.0f,
                //Constants.blindSpotLeft to 3.0f,
                Constants.blindSpotRight to 0.0f,
                Constants.turnSignalLeft to 2.0f,
                Constants.turnSignalRight to 1.0f,
                Constants.isSunUp to 1.0f,
                Constants.autopilotState to 1f,
                Constants.autopilotHands to 3f,
                Constants.cruiseControlSpeed to 45.0f,
                Constants.rearLeftVehicle to 100f,
                Constants.uiSpeedUnits to 0f,
                Constants.gearSelected to Constants.gearPark.toFloat(),
                Constants.battAmps to 140.0f
            )),
            CarState(mutableMapOf(
                Constants.battVolts to 389.9f,
                //Constants.blindSpotLeft to 1.0f,
                //Constants.blindSpotRight to 1.0f,
                Constants.displayBrightnessLev to 9.5f,
                Constants.stateOfCharge to 50.0f,
                Constants.uiRange to 268.0f,
                Constants.uiSpeed to 21.0f,
                Constants.blindSpotLeft to 0.0f,
                //Constants.blindSpotRight to 2.0f,
                Constants.turnSignalLeft to 1.0f,
                Constants.turnSignalRight to 1.0f,
                Constants.isSunUp to 1.0f,
                Constants.autopilotState to 3f,
                Constants.autopilotHands to 3f,
                Constants.maxSpeedAP to 45.0f,
                Constants.cruiseControlSpeed to 45.0f,
                Constants.rearLeftVehicle to 100f,
                Constants.uiSpeedUnits to 0f,
                Constants.gearSelected to Constants.gearDrive.toFloat(),
                Constants.battAmps to 3.0f
            )),
            CarState(mutableMapOf(
                Constants.battVolts to 389.8f,
                Constants.displayBrightnessLev to 8.5f,
                Constants.stateOfCharge to 25.0f,
                Constants.uiRange to 265.0f,
                Constants.uiSpeed to 25.0f,

                Constants.turnSignalLeft to 0.0f,
                Constants.turnSignalRight to 1.0f,
                Constants.isSunUp to 1.0f,
                Constants.autopilotState to 3f,
                Constants.autopilotHands to 3f,
                Constants.maxSpeedAP to 45.0f,
                Constants.rearLeftVehicle to 200f,
                Constants.uiSpeedUnits to 0f,
                Constants.cruiseControlSpeed to 45.0f,
                Constants.battAmps to 750.0f
            )),
            CarState(mutableMapOf(
                Constants.battVolts to 389.7f,

                Constants.displayBrightnessLev to 7.5f,
                Constants.stateOfCharge to 76.0f,
                Constants.uiRange to 264.0f,
                Constants.uiSpeed to 29.0f,

                Constants.turnSignalLeft to 0.0f,
                Constants.turnSignalRight to 1.0f,
                Constants.isSunUp to 0.0f,
                Constants.autopilotState to 3f,
                Constants.autopilotHands to 1f,
                Constants.uiSpeedUnits to 0f,
                Constants.rearLeftVehicle to 100f,
                Constants.maxSpeedAP to 25.0f,
                Constants.cruiseControlSpeed to 23.0f,
                Constants.battAmps to -200.0f
            )))
}

 */