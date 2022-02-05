package app.candash.cluster

import android.util.Log

class CANSignalHelper {
    private val TAG = CANSignalHelper::class.java.simpleName

    companion object {
        const val MAX_FILTERS = 48
        const val CLEAR_FILTERS_BYTE : Byte = 0x18
        const val ADD_FILTERS_BYTE : Byte = 0x0F
    }

    private val mNameSignalMap = HashMap<String, CANSignal>()
    private val mFrameSignalMap : MutableMap<Hex, MutableList<CANSignal>>  = mutableMapOf()

    fun clearFiltersPacket() : ByteArray {
        return byteArrayOf(CLEAR_FILTERS_BYTE);
    }

    fun addFilterPackets(signalNamesToUse: List<String>) : List<ByteArray> {
        return signalsToUse(signalNamesToUse).chunked(MAX_FILTERS).map { CANSignals ->
            CANSignals.fold(mutableListOf(ADD_FILTERS_BYTE)) { byteList, CANSignal ->
                byteList.add(CANSignal.busId.toByte())
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

    fun getSignalsForFrame(frame: Hex) : List<CANSignal> {
        return mFrameSignalMap[frame] ?: listOf()
    }
    fun insertCANSignal(name:String, busId:Int, frameId:Hex, startBit:Int, bitLength:Int, factor:Float, offset:Float,serviceIndex:Int = 0, muxIndex:Int = 0, signed:Boolean = false){
        val CANSignal = CANSignal(name, busId, frameId, startBit, bitLength, factor, offset, serviceIndex, muxIndex, signed)
        mNameSignalMap.put(CANSignal.name, CANSignal)
        addToMapList(mFrameSignalMap, CANSignal.frameId, CANSignal)
    }
    fun createCANSignals() {

        insertCANSignal(Constants.stateOfCharge, -1, Hex(0x33A), 48, 7, 1f, 0f)
        insertCANSignal(Constants.battVolts, -1, Hex(0x132), 0, 16, 0.01f, 0f)
        insertCANSignal(Constants.battAmps, -1, Hex(0x132), 16, 16, -.1f, 0f, signed=true)

        insertCANSignal(Constants.uiSpeed, -1, Hex(0x257), 24, 9, 1f, 0f)
        //insertCANSignal(Constants.uiSpeedHighSpeed, -1, Hex(0x257), 34, 9, 1f, 0f)
        insertCANSignal(Constants.uiSpeedUnits, -1, Hex(0x257), 33, 1, 1f, 0f)
        insertCANSignal(Constants.blindSpotLeft, -1, Hex(0x399), 4, 2, 1f, 0f)
        insertCANSignal(Constants.blindSpotRight, -1, Hex(0x399), 6, 2, 1f, 0f)
        //insertCANSignal(Constants.displayBrightnessLev, 0, Hex(0x273), 32, 8, .5f, 0)
        insertCANSignal(Constants.uiRange, -1, Hex(0x33A), 0, 10, 1f, 0f)
        insertCANSignal(Constants.turnSignalLeft, -1, Hex(0x3F5), 0, 2, 1f, 0f)
        insertCANSignal(Constants.turnSignalRight, -1, Hex(0x3F5), 2, 2, 1f, 0f)
        insertCANSignal(Constants.isSunUp, -1, Hex(0x2D3), 25, 2, 1f, 0f)
        //insertCANSignal(Constants.rearLeftVehicle, 1, Hex(0x22E), 36, 9, 1f, 0f)
        //insertCANSignal(Constants.rearRightVehicle, 1, Hex(0x22E), 9, 9, 1f, 0f)
        insertCANSignal(Constants.leftVehicle, -1, Hex(0x22E), 45, 9, 1f, 0f)
        insertCANSignal(Constants.rightVehicle, -1, Hex(0x22E), 0, 9, 1f, 0f)
        insertCANSignal(Constants.autopilotState, -1, Hex(0x399), 0, 4, 1f, 0f)
        insertCANSignal(Constants.autopilotHands, -1, Hex(0x399), 42, 4, 1f, 0f)
        insertCANSignal(Constants.cruiseControlSpeed, -1, Hex(0x368), 32, 9, .5f, 0f)
        insertCANSignal(Constants.maxSpeedAP, -1, Hex(0x368), 32, 9, .5f, 0f)
        insertCANSignal(Constants.liftgateState, -1, Hex(0x103), 56, 4, 1f, 0f)
        insertCANSignal(Constants.frunkState, -1, Hex(0x2E1), 3, 4, 1f, 0f, 3, 0)
        //insertCANSignal(Constants.conditionalSpeedLimit, -1, Hex(0x3D9), 56, 5, 5f, 0f)
        //insertCANSignal(Constants.solarAngle, 0, Hex(0x2D3), 32, 8, 1f, 0, signed = true)
        insertCANSignal(Constants.gearSelected, -1, Hex(0x118), 21, 3, 1f, 0f)
        insertCANSignal(Constants.frontLeftDoorState, -1, Hex(0x102), 0, 4, 1f, 0f)
        insertCANSignal(Constants.frontRightDoorState, -1, Hex(0x103), 0, 4, 1f, 0f)
        insertCANSignal(Constants.rearLeftDoorState, -1, Hex(0x102), 4, 4, 1f, 0f)
        insertCANSignal(Constants.rearRightDoorState, -1, Hex(0x103), 4, 4, 1f, 0f)
        insertCANSignal(Constants.steeringAngle, -1, Hex(0x129), 16, 14, .1f, -819.2f)
        insertCANSignal(Constants.frontTorque, -1, Hex(0x1D5), 21, 13, .222f, 0f, signed = true)
        insertCANSignal(Constants.rearTorque, -1, Hex(0x1D8), 21, 13, .222f, 0f, signed = true)
        insertCANSignal(Constants.battBrickMin, -1, Hex(0x332), 24, 8, .5f, -40f, 2, 0)
        insertCANSignal(Constants.driveConfig, -1, Hex(0x7FF), 10, 1, 1f, 0f, 8, 1)
        insertCANSignal(Constants.frontTemp, -1, Hex(0x396), 24, 8, 1f, -40f)
        insertCANSignal(Constants.rearTemp, -1, Hex(0x395), 24, 8, 1f, -40f)
        insertCANSignal(Constants.coolantFlow, -1, Hex(0x241), 22, 9, .1f, 0f)
        insertCANSignal(Constants.chargeStatus, -1, Hex(0x204), 4, 2, 1f, 0f)
        insertCANSignal(Constants.brakeTempFL, -1, Hex(0x3FE), 0, 10, 1f, -40f)
        insertCANSignal(Constants.brakeTempFR, -1, Hex(0x3FE), 10, 10, 1f, -40f)
        insertCANSignal(Constants.brakeTempRL, -1, Hex(0x3FE), 20, 10, 1f, -40f)
        insertCANSignal(Constants.brakeTempRR, -1, Hex(0x3FE), 30, 10, 1f, -40f)

    }

    private fun addToMapList(map: MutableMap<Hex, MutableList<CANSignal>>, key: Hex, value: CANSignal) {
        if (map.containsKey(key)) {
            map[key]?.add(value)
        } else {
            map[key] = mutableListOf(value)
        }
    }
}