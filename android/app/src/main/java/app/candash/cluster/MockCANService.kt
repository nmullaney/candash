package app.candash.cluster

import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

class MockCANService : CANService {
    private val MS_BETWEEN_REQUESTS = 2_000L
    private val pandaContext = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
    private var shutdown = false
    private var count: AtomicInteger = AtomicInteger(0)
    private val carState = createCarState()
    private val liveCarState = createLiveCarState()
    private var clearRequest = false

    override suspend fun startRequests(signalNamesToRequest: List<String>) {
        withContext(pandaContext) {
            shutdown = false
            while (!shutdown) {
                val newState = mockCarStates()[count.getAndAdd(1) % mockCarStates().size]
                carState.clear()
                liveCarState.clear()
                for (state in newState) {
                    carState[state.key] = state.value
                    liveCarState[state.key]!!.postValue(state.value)
                }
                Thread.sleep(MS_BETWEEN_REQUESTS)
                yield()
                if (clearRequest) {
                    carState.clear()
                    liveCarState.clear()
                    clearRequest = false
                }
            }
            // Clear carState after stopping
            carState.clear()
            liveCarState.clear()
        }
    }

    override fun isRunning() : Boolean {
        return !shutdown
    }

    override fun getType(): CANServiceType {
        return CANServiceType.MOCK
    }

    override suspend fun shutdown() {
        withContext(pandaContext) {
            shutdown = true
        }
    }

    override fun clearCarState() {
        clearRequest = true
    }

    override fun carState(): CarState {
        return carState
    }

    override fun liveCarState(): LiveCarState {
        return liveCarState
    }
    
    private fun mockCarStates() : List<CarState> =
        listOf(
            createCarState(mutableMapOf(
                SName.autopilotState to 2f,
                SName.accState to 4f,
                SName.accActive to 0f,
                SName.isDarkMode to 0f,
                SName.autopilotHands to 1f,
                SName.gearSelected to SVal.gearDrive,
                SName.stateOfCharge to 70f,
                SName.turnSignalLeft to 1.0f,
                SName.battAmps to -20f,
                SName.battVolts to 390f,
                SName.uiSpeedUnits to 0f,
                SName.displayOn to 1f,
                SName.blindSpotRight to 0f,

                SName.liftgateState to 2f,
                SName.frunkState to 2f,
                SName.frontLeftDoorState to 2f,
                SName.frontRightDoorState to 2f,
                SName.rearLeftDoorState to 2f,
                SName.rearRightDoorState to 2f,
                SName.lightingState to SVal.lightsOff,
                SName.chargeStatus to SVal.chargeStatusInactive,
                SName.mapRegion to SVal.mapUS,
                SName.fusedSpeedLimit to 65f
                )),
            createCarState(mutableMapOf(
                SName.autopilotState to 3f,
                SName.accState to 4f,
                SName.accActive to 1f,
                SName.turnSignalLeft to 1.0f,
                SName.isDarkMode to 0f,
                SName.autopilotHands to 1f,
                SName.driveConfig to 0f,
                SName.gearSelected to SVal.gearDrive,
                SName.stateOfCharge to 70f,
                SName.battAmps to -23f,
                SName.uiSpeedUnits to 0f,
                SName.blindSpotRight to 1f,

                SName.battVolts to 390f,
                SName.uiSpeed to 0.0f,
                // display should stay on because gear is in drive
                SName.displayOn to 0f,

                SName.frontLeftDoorState to 2f,
                SName.lightingState to SVal.lightDRL,
                SName.passengerUnbuckled to 1f,
                SName.limRegen to 1f,
                SName.brakePark to 1f,
                SName.chargeStatus to SVal.chargeStatusInactive,
                SName.mapRegion to SVal.mapEU,
                SName.fusedSpeedLimit to 100f
            )),
            createCarState(mutableMapOf(
                SName.autopilotState to 1f,
                SName.accState to 2f,
                SName.isDarkMode to 0f,
                SName.driveConfig to 1f,
                SName.stateOfCharge to 70f,
                SName.battAmps to -10f,
                SName.battVolts to 390f,
                SName.uiSpeedUnits to 0f,
                SName.uiSpeed to 22.0f,
                SName.displayOn to 1f,
                SName.blindSpotLeft to 3f,

                SName.frontLeftDoorState to 2f,
                SName.lightingState to SVal.lightsPos,
                SName.passengerUnbuckled to 0f,
                SName.chargeStatus to SVal.chargeStatusActive,
                SName.fusedSpeedLimit to SVal.fusedSpeedNone
            )),
            createCarState(mutableMapOf(
                SName.autopilotState to 1f,
                SName.isDarkMode to 0f,
                SName.uiSpeed to 65.0f,
                SName.uiSpeedUnits to 0f,
                SName.frontLeftDoorState to 1f,
                SName.lightingState to SVal.lightsOn,
                SName.chargeStatus to SVal.chargeStatusActive,
                SName.power to 45000f,
                SName.stateOfCharge to 55f,
                SName.gearSelected to SVal.gearInvalid,
                SName.displayOn to 1f,
            )),
            createCarState(mutableMapOf(
                // display will turn off if the pref is enabled
                SName.gearSelected to SVal.gearPark,
                SName.displayOn to 0f,
            ))
        )


}
/*
    private fun mockCarStates() : List<CarState> =
        listOf(
            createCarState(mutableMapOf(
                SName.battVolts to 390.1f,
                SName.frontLeftDoorState to 1.0f,
                SName.displayBrightnessLev to 11.5f,
                SName.stateOfCharge to 1.0f,
                SName.uiRange to 273.0f,
                SName.uiSpeed to 0.0f,
                //SName.blindSpotLeft to 3.0f,
                SName.blindSpotRight to 0.0f,
                SName.turnSignalLeft to 1.0f,
                SName.isDarkMode to 0.0f,
                SName.autopilotState to 2f,
                SName.autopilotHands to 1f,
                SName.uiSpeedUnits to 0f,
                SName.gearSelected to SVal.gearPark,
                SName.battAmps to 20.0f,
                SName.displayOn to 1f

            )),
            createCarState(mutableMapOf(
                SName.battVolts to 390.0f,
                SName.frontLeftDoorState to 1.0f,

                SName.displayBrightnessLev to 10.5f,
                SName.stateOfCharge to 79.0f,
                SName.uiRange to 270.0f,
                SName.uiSpeed to 20.0f,
                //SName.blindSpotLeft to 3.0f,
                SName.blindSpotRight to 0.0f,
                SName.turnSignalLeft to 2.0f,
                SName.turnSignalRight to 1.0f,
                SName.isDarkMode to 0.0f,
                SName.autopilotState to 1f,
                SName.autopilotHands to 3f,
                SName.cruiseControlSpeed to 45.0f,
                SName.uiSpeedUnits to 0f,
                SName.gearSelected to SVal.gearPark,
                SName.battAmps to 140.0f,
                SName.displayOn to 1f
            )),
            createCarState(mutableMapOf(
                SName.battVolts to 389.9f,
                //SName.blindSpotLeft to 1.0f,
                //SName.blindSpotRight to 1.0f,
                SName.displayBrightnessLev to 9.5f,
                SName.stateOfCharge to 50.0f,
                SName.uiRange to 268.0f,
                SName.uiSpeed to 21.0f,
                SName.blindSpotLeft to 0.0f,
                //SName.blindSpotRight to 2.0f,
                SName.turnSignalLeft to 1.0f,
                SName.turnSignalRight to 1.0f,
                SName.isDarkMode to 0.0f,
                SName.autopilotState to 3f,
                SName.autopilotHands to 3f,
                SName.maxSpeedAP to 45.0f,
                SName.cruiseControlSpeed to 45.0f,
                SName.uiSpeedUnits to 0f,
                SName.gearSelected to SVal.gearDrive,
                SName.battAmps to 3.0f,
                SName.displayOn to 1f
            )),
            createCarState(mutableMapOf(
                SName.battVolts to 389.8f,
                SName.displayBrightnessLev to 8.5f,
                SName.stateOfCharge to 25.0f,
                SName.uiRange to 265.0f,
                SName.uiSpeed to 25.0f,

                SName.turnSignalLeft to 0.0f,
                SName.turnSignalRight to 1.0f,
                SName.isDarkMode to 0.0f,
                SName.autopilotState to 3f,
                SName.autopilotHands to 3f,
                SName.maxSpeedAP to 45.0f,
                SName.uiSpeedUnits to 0f,
                SName.cruiseControlSpeed to 45.0f,
                SName.battAmps to 750.0f,
                SName.displayOn to 1f
            )),
            createCarState(mutableMapOf(
                SName.battVolts to 389.7f,

                SName.displayBrightnessLev to 7.5f,
                SName.stateOfCharge to 76.0f,
                SName.uiRange to 264.0f,
                SName.uiSpeed to 29.0f,

                SName.turnSignalLeft to 0.0f,
                SName.turnSignalRight to 1.0f,
                SName.isDarkMode to 1.0f,
                SName.autopilotState to 3f,
                SName.autopilotHands to 1f,
                SName.uiSpeedUnits to 0f,
                SName.maxSpeedAP to 25.0f,
                SName.cruiseControlSpeed to 23.0f,
                SName.battAmps to -200.0f,
                SName.displayOn to 1f
            )))
}

 */