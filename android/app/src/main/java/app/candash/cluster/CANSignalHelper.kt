package app.candash.cluster

import android.util.Log

class CANSignalHelper {
    private val TAG = CANSignalHelper::class.java.simpleName

    companion object {
        const val MAX_FILTERS = 42
        const val CLEAR_FILTERS_BYTE : Byte = 0x18
        const val ADD_FILTERS_BYTE : Byte = 0x0F
    }

    private val nameToSignal = HashMap<String, CANSignal>()
    private val busToFrameToSignal : MutableMap<Int, MutableMap<Hex, MutableList<CANSignal>>>  = mutableMapOf()
    private val nameToAugSignal: MutableMap<String, AugmentedCANSignal> = mutableMapOf()
    private val augSigDepToName: MutableMap<String, MutableSet<String>> = mutableMapOf()

    private val battAmpsHistory = mutableListOf<Float>()
    private var accActive = 0f

    fun clearFiltersPacket() : ByteArray {
        return byteArrayOf(CLEAR_FILTERS_BYTE)
    }

    fun addFilterPackets(signalNamesToUse: List<String>, flipBus: Boolean): List<ByteArray> {
        // Build a set of bus/frame pairs to remove duplicate filter requests
        val signalsToUse = mutableSetOf<Pair<Int, Hex>>()
        val names = signalNamesToUse.ifEmpty { getRealCANSignalNames() }
        names.forEach {
            signalsToUse.add(Pair(nameToSignal[it]!!.busId, nameToSignal[it]!!.frameId))
        }
        return signalsToUse.chunked(MAX_FILTERS).map {
            it.fold(mutableListOf(ADD_FILTERS_BYTE)) { byteList, busFramePair ->
                if (flipBus) {
                    val busID = when (busFramePair.first) {
                        Constants.chassisBus -> Constants.vehicleBus
                        Constants.vehicleBus -> Constants.chassisBus
                        else -> Constants.anyBus
                    }
                    byteList.add(busID.toByte())
                } else {
                    byteList.add(busFramePair.first.toByte())
                }
                byteList.addAll(busFramePair.second.byteArray.asList())
                Log.v(TAG, "frame id in socket filter:" + busFramePair.second.hexString)
                byteList
            }.toByteArray()
        }
    }

    fun getAllCANSignalNames(): List<String> {
        if (nameToSignal.isEmpty()) {
            createCANSignals()
        }
        return nameToSignal.keys.toList() + nameToAugSignal.keys.toList()
    }

    fun getRealCANSignalNames(): List<String> {
        if (nameToSignal.isEmpty()) {
            createCANSignals()
        }
        return nameToSignal.keys.toList()
    }

    fun getSignalsForFrame(bus: Int, frame: Hex) : List<CANSignal> {
        // Specific bus should take precedence
        if (busToFrameToSignal[bus]?.containsKey(frame) == true){
            return busToFrameToSignal[bus]?.get(frame) ?: listOf()
        }
        // Otherwise try the "anyBus" (-1) map
        return busToFrameToSignal[Constants.anyBus]?.get(frame) ?: listOf()
    }

    private fun insertCANSignal(
        name: String,
        busId: Int,
        frameId: Hex,
        startBit: Int,
        bitLength: Int,
        factor: Float,
        offset: Float,
        serviceIndex: Int = 0,
        muxIndex: Int = 0,
        signed: Boolean = false,
        sna: Float? = null
    ) {
        val CANSignal = CANSignal(
            name,
            busId,
            frameId,
            startBit,
            bitLength,
            factor,
            offset,
            serviceIndex,
            muxIndex,
            signed,
            sna
        )
        nameToSignal.put(CANSignal.name, CANSignal)
        addToMapList(CANSignal.busId, CANSignal.frameId, CANSignal)
    }

    private fun insertAugmentedCANSignal(
        name: String,
        dependsOnSignals: List<String>,
        calculation: (carState: CarState) -> Float?
    ) {
        val augmentedCANSignal = AugmentedCANSignal(name, dependsOnSignals, calculation)
        nameToAugSignal[name] = augmentedCANSignal
        dependsOnSignals.forEach { dep ->
            if (augSigDepToName.contains(dep)) {
                augSigDepToName[dep]!!.add(name)
            } else {
                augSigDepToName[dep] = mutableSetOf(name)
            }
        }
    }

    private fun createCANSignals() {
        // Technically uSOE (usable State Of Energy), this better matches what the car shows.
        insertCANSignal(SName.stateOfCharge, Constants.vehicleBus, Hex(0x33A), 56, 7, 1f, 0f)
        insertCANSignal(SName.battVolts, Constants.vehicleBus, Hex(0x132), 0, 16, 0.01f, 0f)
        insertCANSignal(SName.battAmps, Constants.vehicleBus, Hex(0x132), 16, 16, -.1f, 0f, signed=true, sna=3276.7f)

        insertCANSignal(SName.uiSpeed, Constants.vehicleBus, Hex(0x257), 24, 9, 1f, 0f, sna=511f)
        //insertCANSignal(SName.uiSpeedHighSpeed, Constants.vehicleBus, Hex(0x257), 34, 9, 1f, 0f)
        insertCANSignal(SName.fusedSpeedLimit, Constants.chassisBus, Hex(0x399), 8, 5, 5f, 0f, sna=0f)
        insertCANSignal(SName.uiSpeedUnits, Constants.vehicleBus, Hex(0x257), 33, 1, 1f, 0f)
        insertCANSignal(SName.blindSpotLeft, Constants.chassisBus, Hex(0x399), 4, 2, 1f, 0f, sna=3f)
        insertCANSignal(SName.blindSpotRight, Constants.chassisBus, Hex(0x399), 6, 2, 1f, 0f, sna=3f)
        insertCANSignal(SName.forwardCollisionWarning, Constants.chassisBus, Hex(0x449), 3, 1, 1f, 0f)
        //insertCANSignal(SName.displayBrightnessLev, Constants.vehicleBus, Hex(0x273), 32, 8, .5f, 0)
        insertCANSignal(SName.uiRange, Constants.vehicleBus, Hex(0x33A), 0, 10, 1f, 0f)
        insertCANSignal(SName.turnSignalLeft, Constants.vehicleBus, Hex(0x3F5), 0, 2, 1f, 0f)
        insertCANSignal(SName.turnSignalRight, Constants.vehicleBus, Hex(0x3F5), 2, 2, 1f, 0f)

        // Lights updated for 2022.44.25
        insertCANSignal(SName.lightingState, Constants.vehicleBus, Hex(0x3F5), 32, 4, 1f, 0f, sna=15f)
        insertCANSignal(SName.lightSwitch, Constants.vehicleBus, Hex(0x3B3), 9, 3, 1f, 0f, sna=4f)
        insertCANSignal(SName.highBeamRequest, Constants.vehicleBus, Hex(0x3F5), 28, 2, 1f, 0f)
        insertCANSignal(SName.autoHighBeamEnabled, Constants.vehicleBus, Hex(0x273), 41, 1, 1f, 0f)
        insertCANSignal(SName.highBeamStatus, Constants.vehicleBus, Hex(0x3F6), 4, 2, 1f, 0f, sna=3f)
        insertCANSignal(SName.frontFogStatus, Constants.vehicleBus, Hex(0x3F5), 38, 1, 1f, 0f)
        insertCANSignal(SName.rearFogStatus, Constants.vehicleBus, Hex(0x3E3), 8, 2, 1f, 0f, sna=3f)
        insertCANSignal(SName.courtesyLightRequest, Constants.vehicleBus, Hex(0x3F5), 24, 1, 1f, 0f)

        insertCANSignal(SName.isSunUp, Constants.anyBus, Hex(0x2D3), 25, 2, 1f, 0f, sna=3f)
        //insertCANSignal(SName.rearLeftVehicle, Constants.chassisBus, Hex(0x22E), 36, 9, 1f, 0f)
        //insertCANSignal(SName.rearRightVehicle, Constants.chassisBus, Hex(0x22E), 9, 9, 1f, 0f)
        insertCANSignal(SName.leftVehicle, Constants.chassisBus, Hex(0x22E), 45, 9, 1f, 0f, sna=511f)
        insertCANSignal(SName.rightVehicle, Constants.chassisBus, Hex(0x22E), 0, 9, 1f, 0f, sna=511f)
        insertCANSignal(SName.autopilotState, Constants.chassisBus, Hex(0x399), 0, 4, 1f, 0f, sna=15f)
        insertCANSignal(SName.autopilotHands, Constants.chassisBus, Hex(0x399), 42, 4, 1f, 0f, sna=15f)
        // These don't exist anymore. 0x368 is absent on chassis bus, and is something different on vehicle bus:
        //insertCANSignal(SName.cruiseControlSpeed, Constants.chassisBus, Hex(0x368), 32, 9, .5f, 0f)
        //insertCANSignal(SName.maxSpeedAP, Constants.chassisBus, Hex(0x368), 32, 9, .5f, 0f)
        // This shows SNA when TACC is active, but we want to see that, so we don't set the sna param:
        insertCANSignal(SName.accSpeedLimit, Constants.chassisBus, Hex(0x389), 0, 10, 0.2f, 0f)
        insertCANSignal(SName.accState, Constants.chassisBus, Hex(0x2B9), 12, 4, 1f, 0f, sna=15f)
        insertCANSignal(SName.liftgateState, Constants.vehicleBus, Hex(0x103), 56, 4, 1f, 0f, sna=0f)
        insertCANSignal(SName.frunkState, Constants.vehicleBus, Hex(0x2E1), 3, 4, 1f, 0f, 3, 0, sna=0f)
        //insertCANSignal(SName.conditionalSpeedLimit, Constants.chassisBus, Hex(0x3D9), 56, 5, 5f, 0f)
        //insertCANSignal(SName.solarAngle, Constants.chassisBus, Hex(0x2D3), 32, 8, 1f, 0, signed = true)
        insertCANSignal(SName.gearSelected, Constants.vehicleBus, Hex(0x118), 21, 3, 1f, 0f, sna=7f)
        insertCANSignal(SName.frontLeftDoorState, Constants.vehicleBus, Hex(0x102), 0, 4, 1f, 0f, sna=0f)
        insertCANSignal(SName.frontRightDoorState, Constants.vehicleBus, Hex(0x103), 0, 4, 1f, 0f, sna=0f)
        insertCANSignal(SName.rearLeftDoorState, Constants.vehicleBus, Hex(0x102), 4, 4, 1f, 0f, sna=0f)
        insertCANSignal(SName.rearRightDoorState, Constants.vehicleBus, Hex(0x103), 4, 4, 1f, 0f, sna=0f)
        insertCANSignal(SName.steeringAngle, Constants.vehicleBus, Hex(0x129), 16, 14, .1f, -819.2f)
        insertCANSignal(SName.frontTorque, Constants.vehicleBus, Hex(0x1D5), 21, 13, .222f, 0f, signed = true)
        insertCANSignal(SName.rearTorque, Constants.vehicleBus, Hex(0x1D8), 21, 13, .222f, 0f, signed = true)
        insertCANSignal(SName.battBrickMin, Constants.vehicleBus, Hex(0x332), 24, 8, .5f, -40f, 2, 0)
        insertCANSignal(SName.driveConfig, Constants.vehicleBus, Hex(0x7FF), 56, 4, 1f, 0f, 8, 1)
        insertCANSignal(SName.mapRegion, Constants.vehicleBus, Hex(0x7FF), 8, 4, 1f, 0f, 8, 3)
        insertCANSignal(SName.driverOrientation, Constants.vehicleBus, Hex(0x211), 40, 3, 1f, 0f, sna=7f)
        insertCANSignal(SName.frontTemp, Constants.vehicleBus, Hex(0x396), 24, 8, 1f, -40f)
        insertCANSignal(SName.rearTemp, Constants.vehicleBus, Hex(0x395), 24, 8, 1f, -40f)
        insertCANSignal(SName.coolantFlow, Constants.vehicleBus, Hex(0x241), 22, 9, .1f, 0f)
        insertCANSignal(SName.chargeStatus, Constants.vehicleBus, Hex(0x212), 29, 1, 1f, 0f)
        insertCANSignal(SName.brakeTempFL, Constants.vehicleBus, Hex(0x3FE), 12, 10, 1f, -40f, sna=983f)
        insertCANSignal(SName.brakeTempFR, Constants.vehicleBus, Hex(0x3FE), 22, 10, 1f, -40f, sna=983f)
        insertCANSignal(SName.brakeTempRL, Constants.vehicleBus, Hex(0x3FE), 32, 10, 1f, -40f, sna=983f)
        insertCANSignal(SName.brakeTempRR, Constants.vehicleBus, Hex(0x3FE), 42, 10, 1f, -40f, sna=983f)
        insertCANSignal(SName.displayOn, Constants.vehicleBus, Hex(0x353), 5, 1, 1f, 0f)

        insertCANSignal(SName.driverUnbuckled, Constants.vehicleBus, Hex(0x3A1), 32, 2, 1f, 0f, 1, 0, sna=2f)
        insertCANSignal(SName.passengerUnbuckled, Constants.vehicleBus, Hex(0x3A1), 34, 2, 1f, 0f, 1, 0, sna=2f)
        insertCANSignal(SName.heatBattery, Constants.vehicleBus, Hex(0x2E1), 63, 1, 1f, 0f, 3, 0 )

        // EPBR_telltaleLocal: 0 "LAMP_OFF" 1 "LAMP_RED_ON" 2 "LAMP_AMBER_ON" 3 "LAMP_RED_FLASH" 7 "SNA"
        insertCANSignal(SName.brakePark, Constants.vehicleBus, Hex(0x228), 39, 3, 1f, 0f, sna=7f)

        insertCANSignal(SName.brakeHold, Constants.vehicleBus, Hex(0x2B6), 10, 1, 1f, 0f)
        // TPMS changed in 2022.36, dbc still shows them at this address but they don't work
        // insertCANSignal(SName.tpmsSoft, Constants.vehicleBus, Hex(0x123), 13, 1, 1f, 0f)
        // insertCANSignal(SName.tpmsHard, Constants.vehicleBus, Hex(0x123), 12, 1, 1f, 0f)

        insertCANSignal(SName.odometer, Constants.vehicleBus, Hex(0x3B6), 0, 32, 0.001f, 0f, sna=4294967.295f)
        insertCANSignal(SName.PINenabled, Constants.vehicleBus, Hex(0x3B3), 6, 1, 1f, 0f)
        insertCANSignal(SName.PINpassed, Constants.vehicleBus, Hex(0x3B3), 7, 1, 1f, 0f)
        insertCANSignal(SName.brakeApplied, Constants.chassisBus, Hex(0x39D), 16, 2, 1f, 0f)

        insertCANSignal(SName.limRegen, Constants.vehicleBus, Hex(0x36E), 8, 1, 1f, 0f)
        insertCANSignal(SName.kwhDischargeTotal, Constants.vehicleBus, Hex(0x3D2), 0, 32, 0.001f, 0f, sna=4294967.295f)
        insertCANSignal(SName.kwhChargeTotal, Constants.vehicleBus, Hex(0x3D2), 32, 32, 0.001f, 0f, sna=4294967.295f)


        // Create augmented signals
        insertAugmentedCANSignal(SName.smoothBattAmps, listOf(SName.battAmps)) {
            val amps = it[SName.battAmps]
            if (amps != null) {
                battAmpsHistory.add(amps)
                // Smooth to last 10 of amps signal
                while (battAmpsHistory.size > 10) {
                    battAmpsHistory.removeFirst()
                }
                return@insertAugmentedCANSignal battAmpsHistory.average().toFloat()
            } else {
                battAmpsHistory.clear()
                return@insertAugmentedCANSignal null
            }
        }
        insertAugmentedCANSignal(SName.power, listOf(SName.smoothBattAmps, SName.battVolts)) {
            val smoothAmps = it[SName.smoothBattAmps]
            val volts = it[SName.battVolts]
            if (smoothAmps != null && volts != null) {
                return@insertAugmentedCANSignal volts * smoothAmps
            } else {
                return@insertAugmentedCANSignal null
            }
        }
        insertAugmentedCANSignal(SName.l1Distance, listOf(SName.uiSpeed)) {
            val sensingSpeedLimit = if (it[SName.uiSpeedUnits] == 1f) 55f else 35f
            if ((it[SName.uiSpeed] ?: 0f) >= sensingSpeedLimit) {
                return@insertAugmentedCANSignal Constants.l1DistanceHighSpeed
            } else {
                return@insertAugmentedCANSignal Constants.l1DistanceLowSpeed
            }
        }
        insertAugmentedCANSignal(SName.l2Distance, listOf(SName.uiSpeed)) {
            val sensingSpeedLimit = if (it[SName.uiSpeedUnits] == 1f) 55f else 35f
            if ((it[SName.uiSpeed] ?: 0f) >= sensingSpeedLimit) {
                return@insertAugmentedCANSignal Constants.l2DistanceHighSpeed
            } else {
                return@insertAugmentedCANSignal Constants.l2DistanceLowSpeed
            }
        }
        insertAugmentedCANSignal(SName.accActive, listOf(SName.accState, SName.accSpeedLimit)) {
            // accSpeedLimit is 204.6f (SNA) while TACC is active
            if (it[SName.accState] == 4f && it[SName.accSpeedLimit] == 204.6f) {
                accActive = 1f
            }
            // accActive stays latched on until accState is cancelled
            if (it[SName.accState] in setOf(null, 0f, 13f, 15f)) {
                accActive = 0f
            }
            return@insertAugmentedCANSignal accActive
        }
    }

    private fun addToMapList(bus: Int, frameId: Hex, value: CANSignal) {
        if (!busToFrameToSignal.containsKey(bus)) {
            busToFrameToSignal[bus] = mutableMapOf(Pair(frameId, mutableListOf(value)))
        }else if (busToFrameToSignal[bus]?.containsKey(frameId) == false) {
            busToFrameToSignal[bus]?.set(frameId, mutableListOf(value))
        } else {
            busToFrameToSignal[bus]?.get(frameId)?.add(value)
        }
    }

    fun getAugmentsForDep(dependency: String): List<Pair<String, (carState: CarState) -> Float?>> {
        val augments = mutableListOf<Pair<String, (carState: CarState) -> Float?>>()
        augSigDepToName[dependency]?.forEach {
            augments.add(Pair(it, nameToAugSignal[it]!!.calculation))
        }
        return augments
    }
}