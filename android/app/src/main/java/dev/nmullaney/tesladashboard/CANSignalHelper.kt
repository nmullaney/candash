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
    fun insertCANSignal(name:String, busId:Int, frameId:Hex, startBit:Int, bitLength:Int, factor:Float, offset:Int, signed:Boolean? = false){
        val CANSignal = CANSignal(name, busId, frameId, startBit, bitLength, factor, offset, signed)
        mNameSignalMap.put(CANSignal.name, CANSignal)
        addToMapList(mFrameSignalMap, CANSignal.frameId, CANSignal)
    }
    fun createCANSignals() {
        insertCANSignal(Constants.stateOfCharge, 0, Hex(0x33A), 48, 7, 1f, 0)
        insertCANSignal(Constants.battVolts, 0, Hex(0x132), 0, 16, 0.01f, 0)
        insertCANSignal(Constants.uiSpeed, 0, Hex(0x257), 24, 9, 1f, 0)
        insertCANSignal(Constants.blindSpotLeft, 1, Hex(0x399), 4, 2, 1f, 0)
        insertCANSignal(Constants.blindSpotRight, 1, Hex(0x399), 6, 2, 1f, 0)
        insertCANSignal(Constants.displayBrightnessLev, 0, Hex(0x273), 32, 8, .5f, 0)
        insertCANSignal(Constants.uiRange, 1, Hex(0x33A), 0, 10, 1f, 0)
        insertCANSignal(Constants.turnSignalLeft, 0, Hex(0x3F5), 0, 2, 1f, 0)
        insertCANSignal(Constants.turnSignalRight, 0, Hex(0x3F5), 2, 2, 1f, 0)
        insertCANSignal(Constants.isSunUp, 0, Hex(0x2D3), 25, 2, 1f, 0)
        insertCANSignal(Constants.rearLeftVehicle, 1, Hex(0x24A), 63, 1, 1f, 0)
        insertCANSignal(Constants.rearRightVehicle, 1, Hex(0x24A), 62, 1, 1f, 0)





    }

    private fun addToMapList(map: MutableMap<Hex, MutableList<CANSignal>>, key: Hex, value: CANSignal) {
        if (map.containsKey(key)) {
            map[key]?.add(value)
        } else {
            map[key] = mutableListOf(value)
        }
    }
}