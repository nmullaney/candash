package app.candash.cluster

import android.util.Log

class CANSignalHelper {
    private val TAG = CANSignalHelper::class.java.simpleName

    companion object {
        const val MAX_FILTERS = 43

        const val CLEAR_FILTERS_BYTE : Byte = 0x18
        const val ADD_FILTERS_BYTE : Byte = 0x0F
    }

    private val mNameSignalMap = HashMap<String, CANSignal>()
    private val mBusFrameSignalMap : MutableMap<Int, MutableMap<Hex, MutableList<CANSignal>>>  = mutableMapOf()

    fun clearFiltersPacket() : ByteArray {
        return byteArrayOf(CLEAR_FILTERS_BYTE);
    }

    fun addFilterPackets(signalNamesToUse: List<String>, flipBus: Boolean) : List<ByteArray> {
        return signalsToUse(signalNamesToUse).chunked(MAX_FILTERS).map { CANSignals ->
            CANSignals.fold(mutableListOf(ADD_FILTERS_BYTE)) { byteList, CANSignal ->
                if (flipBus){
                    val busID = when (CANSignal.busId){
                        Constants.chassisBus -> Constants.vehicleBus
                        Constants.vehicleBus -> Constants.chassisBus
                        else -> Constants.anyBus
                    }
                    byteList.add(busID.toByte())
                } else {
                    byteList.add(CANSignal.busId.toByte())
                }
                byteList.addAll(CANSignal.frameId.byteArray.asList())
                Log.v(TAG, "frame id in socket filter:" + CANSignal.frameId.hexString)
                byteList
            }.toByteArray()
        }
    }

    fun signalsToUse(signalNamesToUse: List<String>) : List<CANSignal> {
        val names = signalNamesToUse.ifEmpty { defaultSignalNamesToUse() }
        return names.mapNotNull {
            getALLCANSignals().get(it)
        }
    }

    fun defaultSignalNamesToUse() : List<String> {
        return getALLCANSignals().keys.toList()
    }

    fun getALLCANSignals() : Map<String, CANSignal> {
        if (mNameSignalMap.isEmpty()) {
            createCANSignals()
        }
        return mNameSignalMap
    }

    fun getSignalsForFrame(bus: Int, frame: Hex) : List<CANSignal> {
        // Specific bus should take precedence
        if (mBusFrameSignalMap[bus]?.containsKey(frame) == true){
            return mBusFrameSignalMap[bus]?.get(frame) ?: listOf()
        }
        // Otherwise try the "anyBus" (-1) map
        return mBusFrameSignalMap[Constants.anyBus]?.get(frame) ?: listOf()
    }
    fun insertCANSignal(name:String, busId:Int, frameId:Hex, startBit:Int, bitLength:Int, factor:Float, offset:Float,serviceIndex:Int = 0, muxIndex:Int = 0, signed:Boolean = false, sna:Float? = null){
        val CANSignal = CANSignal(name, busId, frameId, startBit, bitLength, factor, offset, serviceIndex, muxIndex, signed, sna)
        mNameSignalMap.put(CANSignal.name, CANSignal)
        addToMapList(mBusFrameSignalMap, CANSignal.busId, CANSignal.frameId, CANSignal)
    }
    fun createCANSignals() {

        // Technically uSOE (usable State Of Energy), this better matches what the car shows.
        insertCANSignal(Constants.stateOfCharge, Constants.vehicleBus, Hex(0x33A), 56, 7, 1f, 0f)
        insertCANSignal(Constants.battVolts, Constants.vehicleBus, Hex(0x132), 0, 16, 0.01f, 0f)
        insertCANSignal(Constants.battAmps, Constants.vehicleBus, Hex(0x132), 16, 16, -.1f, 0f, signed=true, sna=3276.7f)

        insertCANSignal(Constants.uiSpeed, Constants.vehicleBus, Hex(0x257), 24, 9, 1f, 0f, sna=511f)
        //insertCANSignal(Constants.uiSpeedHighSpeed, Constants.vehicleBus, Hex(0x257), 34, 9, 1f, 0f)
        insertCANSignal(Constants.fusedSpeedLimit, Constants.chassisBus, Hex(0x399), 8, 5, 5f, 0f, sna=0f)
        insertCANSignal(Constants.uiSpeedUnits, Constants.vehicleBus, Hex(0x257), 33, 1, 1f, 0f)
        insertCANSignal(Constants.blindSpotLeft, Constants.chassisBus, Hex(0x399), 4, 2, 1f, 0f, sna=3f)
        insertCANSignal(Constants.blindSpotRight, Constants.chassisBus, Hex(0x399), 6, 2, 1f, 0f, sna=3f)
        //insertCANSignal(Constants.displayBrightnessLev, Constants.vehicleBus, Hex(0x273), 32, 8, .5f, 0)
        insertCANSignal(Constants.uiRange, Constants.vehicleBus, Hex(0x33A), 0, 10, 1f, 0f)
        insertCANSignal(Constants.turnSignalLeft, Constants.vehicleBus, Hex(0x3F5), 0, 2, 1f, 0f)
        insertCANSignal(Constants.turnSignalRight, Constants.vehicleBus, Hex(0x3F5), 2, 2, 1f, 0f)

        insertCANSignal(Constants.lowBeamLeft, Constants.vehicleBus, Hex(0x3F5), 28, 2, 1f, 0f, sna=3f)
        insertCANSignal(Constants.highBeamRequest, Constants.vehicleBus, Hex(0x3F5), 58, 1, 1f, 0f)
        insertCANSignal(Constants.autoHighBeamEnabled, Constants.vehicleBus, Hex(0x273), 41, 1, 1f, 0f)
        insertCANSignal(Constants.highLowBeamDecision, Constants.vehicleBus, Hex(0x3E9), 11, 2, 1f, 0f, sna=3f)
        insertCANSignal(Constants.highBeamStalkStatus, Constants.vehicleBus, Hex(0x249), 12, 2, 1f, 0f, sna=3f)
        insertCANSignal(Constants.frontFogSwitch, Constants.vehicleBus, Hex(0x273), 3, 1, 1f, 0f)
        insertCANSignal(Constants.rearFogSwitch, Constants.vehicleBus, Hex(0x273), 23, 1, 1f, 0f)

        insertCANSignal(Constants.isSunUp, Constants.chassisBus, Hex(0x2D3), 25, 2, 1f, 0f, sna=3f)
        //insertCANSignal(Constants.rearLeftVehicle, Constants.chassisBus, Hex(0x22E), 36, 9, 1f, 0f)
        //insertCANSignal(Constants.rearRightVehicle, Constants.chassisBus, Hex(0x22E), 9, 9, 1f, 0f)
        insertCANSignal(Constants.leftVehicle, Constants.chassisBus, Hex(0x22E), 45, 9, 1f, 0f, sna=511f)
        insertCANSignal(Constants.rightVehicle, Constants.chassisBus, Hex(0x22E), 0, 9, 1f, 0f, sna=511f)
        insertCANSignal(Constants.autopilotState, Constants.chassisBus, Hex(0x399), 0, 4, 1f, 0f, sna=15f)
        insertCANSignal(Constants.autopilotHands, Constants.chassisBus, Hex(0x399), 42, 4, 1f, 0f, sna=15f)
        // These don't exist anymore. 0x368 is absent on chassis bus, and is something different on vehicle bus:
        //insertCANSignal(Constants.cruiseControlSpeed, Constants.chassisBus, Hex(0x368), 32, 9, .5f, 0f)
        //insertCANSignal(Constants.maxSpeedAP, Constants.chassisBus, Hex(0x368), 32, 9, .5f, 0f)
        insertCANSignal(Constants.liftgateState, Constants.vehicleBus, Hex(0x103), 56, 4, 1f, 0f, sna=0f)
        insertCANSignal(Constants.frunkState, Constants.vehicleBus, Hex(0x2E1), 3, 4, 1f, 0f, 3, 0, sna=0f)
        //insertCANSignal(Constants.conditionalSpeedLimit, Constants.chassisBus, Hex(0x3D9), 56, 5, 5f, 0f)
        //insertCANSignal(Constants.solarAngle, Constants.chassisBus, Hex(0x2D3), 32, 8, 1f, 0, signed = true)
        insertCANSignal(Constants.gearSelected, Constants.vehicleBus, Hex(0x118), 21, 3, 1f, 0f, sna=7f)
        insertCANSignal(Constants.frontLeftDoorState, Constants.vehicleBus, Hex(0x102), 0, 4, 1f, 0f, sna=0f)
        insertCANSignal(Constants.frontRightDoorState, Constants.vehicleBus, Hex(0x103), 0, 4, 1f, 0f, sna=0f)
        insertCANSignal(Constants.rearLeftDoorState, Constants.vehicleBus, Hex(0x102), 4, 4, 1f, 0f, sna=0f)
        insertCANSignal(Constants.rearRightDoorState, Constants.vehicleBus, Hex(0x103), 4, 4, 1f, 0f, sna=0f)
        insertCANSignal(Constants.steeringAngle, Constants.vehicleBus, Hex(0x129), 16, 14, .1f, -819.2f)
        insertCANSignal(Constants.frontTorque, Constants.vehicleBus, Hex(0x1D5), 21, 13, .222f, 0f, signed = true)
        insertCANSignal(Constants.rearTorque, Constants.vehicleBus, Hex(0x1D8), 21, 13, .222f, 0f, signed = true)
        insertCANSignal(Constants.battBrickMin, Constants.vehicleBus, Hex(0x332), 24, 8, .5f, -40f, 2, 0)
        insertCANSignal(Constants.driveConfig, Constants.vehicleBus, Hex(0x7FF), 10, 1, 1f, 0f, 8, 1)
        insertCANSignal(Constants.mapRegion, Constants.vehicleBus, Hex(0x7FF), 8, 4, 1f, 0f, 8, 3)
        insertCANSignal(Constants.frontTemp, Constants.vehicleBus, Hex(0x396), 24, 8, 1f, -40f)
        insertCANSignal(Constants.rearTemp, Constants.vehicleBus, Hex(0x395), 24, 8, 1f, -40f)
        insertCANSignal(Constants.coolantFlow, Constants.vehicleBus, Hex(0x241), 22, 9, .1f, 0f)
        insertCANSignal(Constants.chargeStatus, Constants.vehicleBus, Hex(0x212), 29, 1, 1f, 0f)
        insertCANSignal(Constants.brakeTempFL, Constants.vehicleBus, Hex(0x3FE), 12, 10, 1f, -40f, sna=983f)
        insertCANSignal(Constants.brakeTempFR, Constants.vehicleBus, Hex(0x3FE), 22, 10, 1f, -40f, sna=983f)
        insertCANSignal(Constants.brakeTempRL, Constants.vehicleBus, Hex(0x3FE), 32, 10, 1f, -40f, sna=983f)
        insertCANSignal(Constants.brakeTempRR, Constants.vehicleBus, Hex(0x3FE), 42, 10, 1f, -40f, sna=983f)
        insertCANSignal(Constants.displayOn, Constants.vehicleBus, Hex(0x353), 5, 1, 1f, 0f)

        insertCANSignal(Constants.drlMode, Constants.vehicleBus, Hex(0x381), 5, 2, 1f, 0f, 5, 18 )
        insertCANSignal(Constants.driverUnbuckled, Constants.vehicleBus, Hex(0x3A1), 32, 2, 1f, 0f, 1, 0, sna=2f)
        insertCANSignal(Constants.passengerUnbuckled, Constants.vehicleBus, Hex(0x3A1), 34, 2, 1f, 0f, 1, 0, sna=2f)
        insertCANSignal(Constants.heatBattery, Constants.vehicleBus, Hex(0x2E1), 63, 1, 1f, 0f, 3, 0 )

        // EPBR_telltaleLocal: 0 "LAMP_OFF" 1 "LAMP_RED_ON" 2 "LAMP_AMBER_ON" 3 "LAMP_RED_FLASH" 7 "SNA"
        insertCANSignal(Constants.brakePark, Constants.vehicleBus, Hex(0x228), 39, 3, 1f, 0f, sna=7f)

        insertCANSignal(Constants.brakeHold, Constants.vehicleBus, Hex(0x2B6), 10, 1, 1f, 0f)
        insertCANSignal(Constants.tpmsSoft, Constants.vehicleBus, Hex(0x123), 13, 1, 1f, 0f)
        insertCANSignal(Constants.tpmsHard, Constants.vehicleBus, Hex(0x123), 12, 1, 1f, 0f)

        insertCANSignal(Constants.odometer, Constants.vehicleBus, Hex(0x3B6), 0, 32, 0.001f, 0f, sna=4294967.295f)
    }

    private fun addToMapList(map: MutableMap<Int, MutableMap<Hex, MutableList<CANSignal>>>, bus: Int, frameId: Hex, value: CANSignal) {
        if (!map.containsKey(bus)) {
            map[bus] = mutableMapOf(Pair(frameId, mutableListOf(value)))
        }else if (map[bus]?.containsKey(frameId) == false) {
            map[bus]?.set(frameId, mutableListOf(value))
        } else {
            map[bus]?.get(frameId)?.add(value)
        }
    }
}