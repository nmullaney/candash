package app.candash.cluster

object SName {
    const val accActive = "accActive"
    const val accSpeedLimit = "accSpeedLimit"
    const val accState = "accState"
    const val autoHighBeamEnabled = "autoHighBeamEnabled"
    const val autopilotHands = "AutopilotHands"
    const val autopilotState = "AutopilotState"
    const val battAmps = "BattAmps"
    const val battBrickMin = "battBrickMin"
    const val battVolts = "BattVolts"
    const val blindSpotLeft = "BSL"
    const val blindSpotRight = "BSR"
    const val brakeApplied = "brakeApplied"
    const val brakeHold = "brakeHold"
    const val brakePark = "brakePark"
    const val brakeTempFL = "brakeTempFL"
    const val brakeTempFR = "brakeTempFR"
    const val brakeTempRL = "brakeTempRL"
    const val brakeTempRR = "brakeTempRR"
    const val chargeStatus = "chargeStatus"
    const val conditionalSpeedLimit = "conditionalSpeedLimit"
    const val coolantFlow = "coolantFlow"
    const val courtesyLightRequest = "courtesyLightRequest"
    const val cruiseControlSpeed = "CruiseControlSpeed"
    const val dc12vAmps = "dc12vAmps"
    const val dc12vPower = "dc12vPower"
    const val dc12vVolts = "dc12vVolts"
    const val displayBrightnessLev = "displayBrightnessLev"
    const val displayOn = "displayOn"
    const val driveConfig = "driveConfig"
    const val driverOrientation = "driverOrientation"
    const val driverUnbuckled = "driverUnbuckled"
    const val forwardCollisionWarning = "FCW"
    const val frontFogStatus = "frontFogStatus"
    const val frontLeftDoorState = "FrontLeftDoor"
    const val frontOccupancy = "frontOccupancy"
    const val frontRightDoorState = "FrontRightDoor"
    const val frontTemp = "frontTemp"
    const val frontTorque = "frontTorque"
    const val frunkState = "frunkState"
    const val fusedSpeedLimit = "fusedSpeedLimit"
    const val gearSelected = "GearSelected"
    const val heatBattery = "heatBattery"
    const val highBeamRequest = "highBeamRequest"
    const val highBeamStatus = "highBeamStatus"
    const val hvacAirDistribution = "hvacAirDistribution"
    const val insideTemp = "insideTemp"
    const val insideTempReq = "insideTempReq"
    const val isDarkMode = "isDarkMode"
    const val keepClimateReq = "keepClimateReq"
    const val kwhChargeTotal = "kwhChargeTotal"
    const val kwhDischargeTotal = "kwhDischargeTotal"
    const val leftVehicle = "leftVehDetected"
    const val liftgateState = "liftgateState"
    const val lightingState = "lightingState"
    const val lightSwitch = "lightSwitch"
    const val limRegen = "limRegen"
    const val lockStatus = "lockStatus"
    const val mapRegion = "mapRegion"
    const val maxDischargePower = "maxDischargePower"
    const val maxHeatPower = "maxHeatPower"
    const val maxRegenPower = "maxRegenPower"
    const val maxSpeedAP = "maxSpeedAP"
    const val nominalFullPackEnergy = "nominalFullPackEnergy"
    const val odometer = "odometer"
    const val outsideTemp = "outsideTemp"
    const val partyHoursLeft = "partyHoursLeft"
    const val passengerUnbuckled = "passengerUnbuckled"
    const val PINenabled = "PINenabled"
    const val PINpassed = "PINpassed"
    const val rearFogStatus = "rearFogStatus"
    const val rearLeftDoorState = "RearLeftDoor"
    const val rearLeftVehicle = "rearLeftVehDetected"
    const val rearRightDoorState = "RearRightDoor"
    const val rearRightVehicle = "rearRightVehDetected"
    const val rearTemp = "rearTemp"
    const val rearTorque = "rearTorque"
    const val rightVehicle = "rightVehDetected"
    const val slowPower = "slowPower"
    const val stateOfCharge = "UI_SOC"
    const val steeringAngle = "SteeringAngle"
    const val tpmsHard = "tpmsHard"
    const val tpmsSoft = "tpmsSoft"
    const val turnSignalLeft = "VCFRONT_indicatorLef"
    const val turnSignalRight = "VCFRONT_indicatorRig"
    const val uiRange = "UI_Range"
    const val uiSpeed = "UISpeed"
    const val uiSpeedHighSpeed = "UISpeedHighSpeed"
    const val uiSpeedTestBus0 = "uiSpeedTestBus0"
    const val uiSpeedUnits = "UISpeedUnits"
    const val vehicleSpeed = "Vehicle Speed"

    // Augmented signals:
    const val power = "power"
    const val smoothBattAmps = "smoothBattAmps"
    const val l1Distance = "l1Distance"
    const val l2Distance = "l2Distance"
}

object SGroup {
    val closures = listOf(
        SName.frontLeftDoorState,
        SName.frontRightDoorState,
        SName.rearLeftDoorState,
        SName.rearRightDoorState,
        SName.liftgateState,
        SName.frunkState
    )

    val lights = listOf(
        SName.lightingState,
        SName.lightSwitch,
        SName.highBeamRequest,
        SName.highBeamStatus,
        SName.autoHighBeamEnabled,
        SName.frontFogStatus,
        SName.rearFogStatus,
        SName.courtesyLightRequest,
        SName.mapRegion
    )
}