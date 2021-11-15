package dev.nmullaney.tesladashboard

import android.util.Log

class CANSignalHelper {
    private val TAG = CANSignalHelper::class.java.simpleName

    private val mNameSignalMap = HashMap<String, CANSignal>()
    private val mFrameSignalMap : MutableMap<Hex, MutableList<CANSignal>>  = mutableMapOf()

    fun socketFilterToInclude() : ByteArray {
        val filters = mutableListOf<Byte>()
        // This first byte indicates we are including these
        filters.add(0x0F)
        signalsToUse().forEach { canSignal ->
            filters.add(canSignal.busId.toByte())
            filters.addAll(canSignal.frameId.byteArray.asList())
            Log.d(TAG, "frame id in socket filter:" + canSignal.frameId.hexString)
        }
        return filters.toByteArray()
    }

    fun signalsToUse() : List<CANSignal> {
        return signalNamesToUse().mapNotNull {
            getALLCANSignals().get(it)
        }
    }

    fun signalNamesToUse() : List<String> {
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
    fun insertCANSignal(name:String, busId:Int, frameId:Hex, startBit:Int, bitLength:Int, factor:Float, offset:Float,serviceIndex:Int = 0, muxIndex:Int = 0, signed:Boolean? = false){
        val CANSignal = CANSignal(name, busId, frameId, startBit, bitLength, factor, offset, serviceIndex, muxIndex, signed)
        mNameSignalMap.put(CANSignal.name, CANSignal)
        addToMapList(mFrameSignalMap, CANSignal.frameId, CANSignal)
    }
    fun createCANSignals() {
        insertCANSignal(Constants.stateOfCharge, 0, Hex(0x33A), 48, 7, 1f, 0f)
        //insertCANSignal(Constants.battVolts, 0, Hex(0x132), 0, 16, 0.01f, 0f)
        //insertCANSignal(Constants.battAmps, 0, Hex(0x132), 16, 16, -.1f, 0f, signed=true)

        insertCANSignal(Constants.uiSpeed, 0, Hex(0x257), 24, 9, 1f, 0f)
        // insertCANSignal(Constants.uiSpeedHighSpeed, 0, Hex(0x257), 34, 9, 1f, 0)

        insertCANSignal(Constants.uiSpeedUnits, 0, Hex(0x257), 33, 1, 1f, 0f)
        insertCANSignal(Constants.blindSpotLeft, 1, Hex(0x399), 4, 2, 1f, 0f)
        insertCANSignal(Constants.blindSpotRight, 1, Hex(0x399), 6, 2, 1f, 0f)
        //insertCANSignal(Constants.displayBrightnessLev, 0, Hex(0x273), 32, 8, .5f, 0)
        insertCANSignal(Constants.uiRange, 1, Hex(0x33A), 0, 10, 1f, 0f)
        insertCANSignal(Constants.turnSignalLeft, 0, Hex(0x3F5), 0, 2, 1f, 0f)
        insertCANSignal(Constants.turnSignalRight, 0, Hex(0x3F5), 2, 2, 1f, 0f)
        insertCANSignal(Constants.isSunUp, 0, Hex(0x2D3), 25, 2, 1f, 0f)
        insertCANSignal(Constants.rearLeftVehicle, 1, Hex(0x22E), 36, 9, 1f, 0f)
        insertCANSignal(Constants.rearRightVehicle, 1, Hex(0x22E), 9, 9, 1f, 0f)
        insertCANSignal(Constants.leftVehicle, 1, Hex(0x22E), 45, 9, 1f, 0f)
        insertCANSignal(Constants.rightVehicle, 1, Hex(0x22E), 0, 9, 1f, 0f)
        insertCANSignal(Constants.autopilotState, 1, Hex(0x399), 0, 4, 1f, 0f)
        insertCANSignal(Constants.autopilotHands, 1, Hex(0x399), 42, 4, 1f, 0f)
        //insertCANSignal(Constants.cruiseControlSpeed, 1, Hex(0x389), 0, 10, .2f, 0)
        //insertCANSignal(Constants.maxSpeedAP, 1, Hex(0x2B9), 0, 12, .062f, 0)
        insertCANSignal(Constants.liftgateState, 0, Hex(0x103), 56, 4, 1f, 0f)
        insertCANSignal(Constants.frunkState, 0, Hex(0x2E1), 3, 4, 1f, 0f, 3, 0)
        //insertCANSignal(Constants.visionSpeedLimit, 1, Hex(0x399), 8, 5, 1f, 0)
        //insertCANSignal(Constants.solarAngle, 0, Hex(0x2D3), 32, 8, 1f, 0, signed = true)
        insertCANSignal(Constants.gearSelected, 0, Hex(0x118), 21, 3, 1f, 0f)
        insertCANSignal(Constants.frontLeftDoorState, 0, Hex(0x102), 0, 4, 1f, 0f)
        insertCANSignal(Constants.frontRightDoorState, 0, Hex(0x103), 0, 4, 1f, 0f)
        insertCANSignal(Constants.rearLeftDoorState, 0, Hex(0x102), 4, 4, 1f, 0f)
        insertCANSignal(Constants.rearRightDoorState, 0, Hex(0x103), 4, 4, 1f, 0f)
        insertCANSignal(Constants.steeringAngle, 0, Hex(0x129), 16, 14, .1f, -819.2f)





    }

    private fun addToMapList(map: MutableMap<Hex, MutableList<CANSignal>>, key: Hex, value: CANSignal) {
        if (map.containsKey(key)) {
            map[key]?.add(value)
        } else {
            map[key] = mutableListOf(value)
        }
    }
}