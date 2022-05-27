package app.candash.cluster

object Constants {
    const val uiSpeed = "UISpeed"
    const val battVolts = "BattVolts"
    const val stateOfCharge = "UI_SOC"
    const val blindSpotLeft = "BSL"
    const val blindSpotRight = "BSR"
    const val displayBrightnessLev = "displayBrightnessLev"
    const val uiRange = "UI_Range"
    const val isSunUp = "isSunUp"

    const val ipAddressPrefKey = "ip_address_pref_key"
    // For PIWIS-WLAN
    const val ipAddressLocalNetwork = "192.168.4.1"
    const val ipAddressCANServer = "192.168.4.1"
    const val useMockServerPrefKey = "use_mock_server"
    const val turnSignalLeft = "VCFRONT_indicatorLef"
    const val turnSignalRight = "VCFRONT_indicatorRig"
    const val rearLeftVehicle = "rearLeftVehDetected"
    const val rearRightVehicle = "rearRightVehDetected"
    const val leftVehicle = "leftVehDetected"
    const val rightVehicle = "rightVehDetected"
    const val autopilotState = "AutopilotState"
    const val liftgateState = "liftgateState"
    const val frunkState = "frunkState"
    const val uiSpeedUnits = "UISpeedUnits"
    const val autopilotHands = "AutopilotHands"
    const val maxSpeedAP = "maxSpeedAP"
    const val cruiseControlSpeed = "CruiseControlSpeed"
    const val uiSpeedHighSpeed = "UISpeedHighSpeed"
    const val gearSelected = "GearSelected"
    const val frontLeftDoorState = "FrontLeftDoor"
    const val frontRightDoorState = "FrontRightDoor"
    const val rearLeftDoorState = "RearLeftDoor"
    const val rearRightDoorState = "RearRightDoor"
    const val battAmps = "BattAmps"
    const val steeringAngle = "SteeringAngle"
    const val vehicleSpeed = "Vehicle Speed"
    const val gearPark  = 1
    const val gearReverse = 2
    const val gearNeutral = 3
    const val gearDrive = 4
    const val doorOpen = 1
    const val doorClosed = 2
    const val maxDischargePower = "maxDischargePower"
    const val maxRegenPower = "maxRegenPower"
    const val maxHeatPower = "maxHeatPower"
    const val frontTorque = "frontTorque"
    const val rearTorque = "rearTorque"
    const val battBrickMin = "battBrickMin"
    const val driveConfig = "driveConfig"
    const val awd = 1f
    const val rwd = 0f
    const val frontTemp = "frontTemp"
    const val rearTemp = "rearTemp"
    const val coolantFlow = "coolantFlow"
    const val chargeStatus = "chargeStatus"
    const val chargeStatusConnected = 1f
    const val chargeStatusInactive = 0f
    const val chargeStatusEnabled = 5f
    const val chargeStatusStandby = 2f
    const val chargeStatusEVSETestActive = 3f
    const val chargeStatusEVSETestPassed = 4f
    const val chargeStatusFault = 6f
    const val uiSpeedTestBus0 = "uiSpeedTestBus0"
    const val indicatorLeft = "indicatorLeft"
    const val indicatorRight = "indicatorRight"
    const val conditionalSpeedLimit = "conditionalSpeedLimit"
    const val brakeTempFL = "brakeTempFL"
    const val brakeTempFR = "brakeTempFR"
    const val brakeTempRL = "brakeTempRL"
    const val brakeTempRR = "brakeTempRR"
    const val showPerfGauges = 2f
    const val showPowerGauges = 1f
    const val showSimpleGauges = 0f
    const val gaugeMode = "gaugeMode"
    const val displayOn = "displayOn"
}