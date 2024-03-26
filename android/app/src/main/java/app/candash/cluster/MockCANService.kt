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
                val mockStates = mockCarStates()
                // val mockStates = mockPartyStates()
                val newState = mockStates[count.getAndAdd(1) % mockStates.size]
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

    private fun mockPartyStates(): List<CarState> =
        listOf(
            createCarState(mutableMapOf(
                SName.keepClimateReq to SVal.keepClimateParty,
                SName.stateOfCharge to 69f,
                SName.outsideTemp to 24f,
                SName.insideTemp to 20f,
                SName.insideTempReq to 20f,
                SName.battBrickMin to 35f,
                SName.dc12vPower to 200f,
                SName.power to 400f,
                SName.slowPower to 400f,
                SName.partyHoursLeft to 2f,
                SName.frontOccupancy to 2f,
            )),
            createCarState(mutableMapOf(
                SName.keepClimateReq to SVal.keepClimateParty,
                SName.stateOfCharge to 60f,
                SName.outsideTemp to 24f,
                SName.insideTemp to 20f,
                SName.insideTempReq to 20f,
                SName.battBrickMin to 35f,
                SName.dc12vPower to 200f,
                SName.power to 400f,
                SName.slowPower to 400f,
                SName.partyHoursLeft to 2f,
                SName.frontOccupancy to 2f,
            )),
        )
}