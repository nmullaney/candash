package app.candash.cluster

import android.util.Log

class CANSignalHelper {
    private val TAG = CANSignalHelper::class.java.simpleName

    private val mNameSignalMap = HashMap<String, CANSignal>()
    private val mNameTestSignalMap = HashMap<String, CANSignal>()

    private val mFrameSignalMap : MutableMap<Hex, MutableList<CANSignal>>  = mutableMapOf()
    private val mFrameTestSignalMap : MutableMap<Hex, MutableList<CANSignal>>  = mutableMapOf()


    fun socketFilterToInclude(flip:Boolean = false) : ByteArray {
        val filters = mutableListOf<Byte>()
        // This first byte indicates we are including these
        filters.add(0x0F)
        signalsToUse().forEach { canSignal ->
            // flip means we have to reverse bus 0 and 1 because Chassis Bus is on 0
            if (flip){
                if(canSignal.busId == 0){
                    filters.add(1.toByte())
                }else {
                    filters.add(0.toByte())
                }
            }
            else {
                filters.add(canSignal.busId.toByte())
            }
            filters.addAll(canSignal.frameId.byteArray.asList())
            //Log.v(TAG, "frame id in socket filter:" + canSignal.frameId.hexString)
        }
        return filters.toByteArray()
    }

    fun testSocketFilterToInclude() : ByteArray{
        val filters = mutableListOf<Byte>()
        // This first byte indicates we are including these
        filters.add(0x0F)
        testSignalsToUse().forEach { canSignal ->
            filters.add(0.toByte())
            filters.addAll(canSignal.frameId.byteArray.asList())
            //Log.v(TAG, "frame id in socket filter:" + canSignal.frameId.hexString)
        }
        return filters.toByteArray()
    }
    fun signalsToUse() : List<CANSignal> {
        return signalNamesToUse().mapNotNull {
            getALLCANSignals().get(it)
        }
    }
    fun testSignalsToUse() : List<CANSignal> {
        return testSignalNamesToUse().mapNotNull {
            getALLTestCANSignals().get(it)
        }
    }
    fun signalNamesToUse() : List<String> {
        return getALLCANSignals().keys.toList()

    }
    fun testSignalNamesToUse() : List<String> {
        return getALLTestCANSignals().keys.toList()

    }
    fun getALLCANSignals() : Map<String, CANSignal> {

            if (mNameSignalMap.isEmpty()) {
                createCANSignals()
            }


        return mNameSignalMap
    }
    fun getALLTestCANSignals() : Map<String, CANSignal> {

        if (mNameTestSignalMap.isEmpty()) {
            createTestCANSignals()
        }


        return mNameTestSignalMap
    }

    fun getSignalsForFrame(frame: Hex) : List<CANSignal> {
        return mFrameSignalMap[frame] ?: listOf()
    }
    fun getTestSignalsForFrame(frame: Hex) : List<CANSignal> {
        return mFrameTestSignalMap[frame] ?: listOf()
    }
    fun insertCANSignal(name:String, busId:Int, frameId:Hex, startBit:Int, bitLength:Int, factor:Float, offset:Float,serviceIndex:Int = 0, muxIndex:Int = 0, signed:Boolean = false){
        val CANSignal = CANSignal(name, busId, frameId, startBit, bitLength, factor, offset, serviceIndex, muxIndex, signed)
        mNameSignalMap.put(CANSignal.name, CANSignal)
        addToMapList(mFrameSignalMap, CANSignal.frameId, CANSignal)
    }
    fun insertTestCANSignal(name:String, busId:Int, frameId:Hex, startBit:Int, bitLength:Int, factor:Float, offset:Float,serviceIndex:Int = 0, muxIndex:Int = 0, signed:Boolean = false){
        val CANSignal = CANSignal(name, busId, frameId, startBit, bitLength, factor, offset, serviceIndex, muxIndex, signed)
        mNameTestSignalMap.put(CANSignal.name, CANSignal)
        addToMapList(mFrameTestSignalMap, CANSignal.frameId, CANSignal)
    }
    fun createTestCANSignals() {
        insertTestCANSignal(Constants.battVolts, 0, Hex(0x132), 0, 16, 0.01f, 0f)
        insertTestCANSignal(Constants.leftVehicle, 0, Hex(0x22E), 45, 9, 1f, 0f)

    }
    fun createCANSignals() {

        insertCANSignal(Constants.stateOfCharge, 0, Hex(0x33A), 48, 7, 1f, 0f)
        insertCANSignal(Constants.battVolts, 0, Hex(0x132), 0, 16, 0.01f, 0f)
        insertCANSignal(Constants.battAmps, 0, Hex(0x132), 16, 16, -.1f, 0f, signed=true)

        insertCANSignal(Constants.uiSpeed, 0, Hex(0x257), 24, 9, 1f, 0f)
        //insertCANSignal(Constants.uiSpeedTestBus0, 0, Hex(0x257), 24, 9, 1f, 0f)

        insertCANSignal(Constants.uiSpeedHighSpeed, 0, Hex(0x257), 34, 9, 1f, 0f)
        //insertCANSignal(Constants.indicatorLeft, 0, Hex(0x3E2), 4, 2, 1f, 0f)
        //insertCANSignal(Constants.indicatorRight, 0, Hex(0x3E3), 4, 2, 1f, 0f)
        insertCANSignal(Constants.uiSpeedUnits, 0, Hex(0x257), 33, 1, 1f, 0f)
        insertCANSignal(Constants.blindSpotLeft, 1, Hex(0x399), 4, 2, 1f, 0f)
        insertCANSignal(Constants.blindSpotRight, 1, Hex(0x399), 6, 2, 1f, 0f)
        //insertCANSignal(Constants.displayBrightnessLev, 0, Hex(0x273), 32, 8, .5f, 0)
        insertCANSignal(Constants.uiRange, 0, Hex(0x33A), 0, 10, 1f, 0f)
        insertCANSignal(Constants.turnSignalLeft, 0, Hex(0x3F5), 0, 2, 1f, 0f)
        insertCANSignal(Constants.turnSignalRight, 0, Hex(0x3F5), 2, 2, 1f, 0f)
        insertCANSignal(Constants.isSunUp, 0, Hex(0x2D3), 25, 2, 1f, 0f)
        //insertCANSignal(Constants.rearLeftVehicle, 1, Hex(0x22E), 36, 9, 1f, 0f)
        //insertCANSignal(Constants.rearRightVehicle, 1, Hex(0x22E), 9, 9, 1f, 0f)
        insertCANSignal(Constants.leftVehicle, 1, Hex(0x22E), 45, 9, 1f, 0f)
        insertCANSignal(Constants.rightVehicle, 1, Hex(0x22E), 0, 9, 1f, 0f)
        insertCANSignal(Constants.autopilotState, 1, Hex(0x399), 0, 4, 1f, 0f)
        insertCANSignal(Constants.autopilotHands, 1, Hex(0x399), 42, 4, 1f, 0f)
        insertCANSignal(Constants.cruiseControlSpeed, 0, Hex(0x368), 32, 9, .5f, 0f)
        insertCANSignal(Constants.maxSpeedAP, 1, Hex(0x368), 32, 9, .5f, 0f)
        insertCANSignal(Constants.liftgateState, 0, Hex(0x103), 56, 4, 1f, 0f)
        insertCANSignal(Constants.frunkState, 0, Hex(0x2E1), 3, 4, 1f, 0f, 3, 0)
        insertCANSignal(Constants.conditionalSpeedLimit, 1, Hex(0x3D9), 56, 5, 5f, 0f)
        //insertCANSignal(Constants.solarAngle, 0, Hex(0x2D3), 32, 8, 1f, 0, signed = true)
        insertCANSignal(Constants.gearSelected, 0, Hex(0x118), 21, 3, 1f, 0f)
        insertCANSignal(Constants.frontLeftDoorState, 0, Hex(0x102), 0, 4, 1f, 0f)
        insertCANSignal(Constants.frontRightDoorState, 0, Hex(0x103), 0, 4, 1f, 0f)
        insertCANSignal(Constants.rearLeftDoorState, 0, Hex(0x102), 4, 4, 1f, 0f)
        insertCANSignal(Constants.rearRightDoorState, 0, Hex(0x103), 4, 4, 1f, 0f)
        insertCANSignal(Constants.steeringAngle, 0, Hex(0x129), 16, 14, .1f, -819.2f)
        insertCANSignal(Constants.frontTorque, 0, Hex(0x1D5), 21, 13, .222f, 0f, signed = true)
        insertCANSignal(Constants.rearTorque, 0, Hex(0x1D8), 21, 13, .222f, 0f, signed = true)
        insertCANSignal(Constants.battBrickMin, 0, Hex(0x332), 24, 8, .5f, -40f, 2, 0)
        insertCANSignal(Constants.driveConfig, 0, Hex(0x7FF), 10, 1, 1f, 0f, 8, 1)
        insertCANSignal(Constants.frontTemp, 0, Hex(0x396), 24, 8, 1f, -40f)
        insertCANSignal(Constants.rearTemp, 0, Hex(0x395), 24, 8, 1f, -40f)
        insertCANSignal(Constants.coolantFlow, 0, Hex(0x241), 22, 9, .11f, 0f)
        insertCANSignal(Constants.chargeStatus, 0, Hex(0x204), 4, 2, 1f, 0f)
        insertCANSignal(Constants.brakeTempFL, 0, Hex(0x3FE), 0, 10, 1f, -40f)
        insertCANSignal(Constants.brakeTempFR, 0, Hex(0x3FE), 10, 10, 1f, -40f)
        insertCANSignal(Constants.brakeTempRL, 0, Hex(0x3FE), 20, 10, 1f, -40f)
        insertCANSignal(Constants.brakeTempRR, 0, Hex(0x3FE), 30, 10, 1f, -40f)



        //insertCANSignal(Constants.vehicleSpeed, 1, Hex(0x175), 42, 10, .5f, 0f)



    }
    fun createCANSignals2() {

        insertCANSignal(Constants.stateOfCharge, 1, Hex(0x33A), 48, 7, 1f, 0f)
        insertCANSignal(Constants.battVolts, 1, Hex(0x132), 0, 16, 0.01f, 0f)
        insertCANSignal(Constants.battAmps, 1, Hex(0x132), 16, 16, -.1f, 0f, signed=true)

        insertCANSignal(Constants.uiSpeed, 1, Hex(0x257), 24, 9, 1f, 0f)
        //insertCANSignal(Constants.uiSpeedTestBus0, 1, Hex(0x257), 24, 9, 1f, 0f)

        insertCANSignal(Constants.uiSpeedHighSpeed, 1, Hex(0x257), 34, 9, 1f, 0f)
        //insertCANSignal(Constants.indicatorLeft, 1, Hex(0x3E2), 4, 2, 1f, 0f)
        //insertCANSignal(Constants.indicatorRight, 1, Hex(0x3E3), 4, 2, 1f, 0f)
        insertCANSignal(Constants.uiSpeedUnits, 1, Hex(0x257), 33, 1, 1f, 0f)
        insertCANSignal(Constants.blindSpotLeft, 0, Hex(0x399), 4, 2, 1f, 0f)
        insertCANSignal(Constants.blindSpotRight, 0, Hex(0x399), 6, 2, 1f, 0f)
        //insertCANSignal(Constants.displayBrightnessLev, 1, Hex(0x273), 32, 8, .5f, 0)
        insertCANSignal(Constants.uiRange, 0, Hex(0x33A), 0, 10, 1f, 0f)
        insertCANSignal(Constants.turnSignalLeft, 1, Hex(0x3F5), 0, 2, 1f, 0f)
        insertCANSignal(Constants.turnSignalRight, 1, Hex(0x3F5), 2, 2, 1f, 0f)
        insertCANSignal(Constants.isSunUp, 1, Hex(0x2D3), 25, 2, 1f, 0f)
        //insertCANSignal(Constants.rearLeftVehicle, 0, Hex(0x22E), 36, 9, 1f, 0f)
        //insertCANSignal(Constants.rearRightVehicle, 0, Hex(0x22E), 9, 9, 1f, 0f)
        insertCANSignal(Constants.leftVehicle, 0, Hex(0x22E), 45, 9, 1f, 0f)
        insertCANSignal(Constants.rightVehicle, 0, Hex(0x22E), 0, 9, 1f, 0f)
        insertCANSignal(Constants.autopilotState, 0, Hex(0x399), 0, 4, 1f, 0f)
        insertCANSignal(Constants.autopilotHands, 0, Hex(0x399), 42, 4, 1f, 0f)
        insertCANSignal(Constants.cruiseControlSpeed, 1, Hex(0x368), 32, 9, .5f, 0f)
        insertCANSignal(Constants.maxSpeedAP, 0, Hex(0x368), 32, 9, .5f, 0f)
        insertCANSignal(Constants.liftgateState, 1, Hex(0x103), 56, 4, 1f, 0f)
        insertCANSignal(Constants.frunkState, 1, Hex(0x2E1), 3, 4, 1f, 0f, 3, 0)
        insertCANSignal(Constants.conditionalSpeedLimit, 0, Hex(0x3D9), 56, 5, 5f, 0f)
        //insertCANSignal(Constants.solarAngle, 1, Hex(0x2D3), 32, 8, 1f, 1, signed = true)
        insertCANSignal(Constants.gearSelected, 1, Hex(0x118), 21, 3, 1f, 0f)
        insertCANSignal(Constants.frontLeftDoorState, 1, Hex(0x102), 0, 4, 1f, 0f)
        insertCANSignal(Constants.frontRightDoorState, 1, Hex(0x103), 0, 4, 1f, 0f)
        insertCANSignal(Constants.rearLeftDoorState, 1, Hex(0x102), 4, 4, 1f, 0f)
        insertCANSignal(Constants.rearRightDoorState, 1, Hex(0x103), 4, 4, 1f, 0f)
        insertCANSignal(Constants.steeringAngle, 1, Hex(0x129), 16, 14, .1f, -819.2f)
        insertCANSignal(Constants.frontTorque, 1, Hex(0x1D5), 21, 13, .222f, 0f, signed = true)
        insertCANSignal(Constants.rearTorque, 1, Hex(0x1D8), 21, 13, .222f, 0f, signed = true)
        insertCANSignal(Constants.battBrickMin, 1, Hex(0x332), 24, 8, .5f, -40f, 2, 0)
        insertCANSignal(Constants.driveConfig, 1, Hex(0x7FF), 10, 1, 1f, 0f, 8, 1)
        insertCANSignal(Constants.frontTemp, 1, Hex(0x396), 24, 8, 1f, -40f)
        insertCANSignal(Constants.rearTemp, 1, Hex(0x395), 24, 8, 1f, -40f)
        insertCANSignal(Constants.coolantFlow, 1, Hex(0x241), 22, 9, .11f, 0f)
        insertCANSignal(Constants.chargeStatus, 1, Hex(0x204), 4, 2, 1f, 0f)
        insertCANSignal(Constants.brakeTempFL, 1, Hex(0x3FE), 0, 10, 1f, -40f)
        insertCANSignal(Constants.brakeTempFR, 1, Hex(0x3FE), 10, 10, 1f, -40f)
        insertCANSignal(Constants.brakeTempRL, 1, Hex(0x3FE), 20, 10, 1f, -40f)
        insertCANSignal(Constants.brakeTempRR, 1, Hex(0x3FE), 30, 10, 1f, -40f)



        //insertCANSignal(Constants.vehicleSpeed, 1, Hex(0x175), 42, 10, .5f, 0f)



    }

    private fun addToMapList(map: MutableMap<Hex, MutableList<CANSignal>>, key: Hex, value: CANSignal) {
        if (map.containsKey(key)) {
            map[key]?.add(value)
        } else {
            map[key] = mutableListOf(value)
        }
    }
}