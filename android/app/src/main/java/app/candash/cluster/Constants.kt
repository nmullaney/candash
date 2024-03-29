package app.candash.cluster

object Constants {
    // PandaService may flip these automatically
    const val chassisBus = 0
    const val vehicleBus = 1
    const val anyBus = -1

    const val ipAddressPrefKey = "ip_address_pref_key"
    const val useMockServerPrefKey = "use_mock_server"
    // For PIWIS-WLAN
    const val ipAddressLocalNetwork = "192.168.4.1"

    const val efficiencyOdoStepKm = 0.001f
    // Smooths to x of the entire chart size
    const val efficiencyChartSmoothing = 0.1f

    // Prefs
    const val uiSpeedUnitsMPH = "uiSpeedUnitsMPH"
    const val forceDarkMode = "forceDarkMode"
    const val gaugeMode = "gaugeMode"
    const val showSimpleGauges = 0f
    const val showRegularGauges = 1f
    const val showFullGauges = 2f
    const val showBattRange = "showBattRange"
    const val hideOdometer = "hideOdometer"
    const val hideBs = "hideBs"
    const val hideSpeedLimit = "hideSpeedLimit"
    const val disableDisplaySync = "disableDisplaySync"
    const val tempInF = "tempInF"
    const val partyTempInF = "partyTempInF"
    const val powerUnits = "powerUnits"
    const val powerUnitKw = 0f
    const val powerUnitHp = 1f
    const val powerUnitPs = 2f
    const val torqueInLbfFt = "torqueInLbfFt"
    const val hideEfficiency = "hideEfficiency"
    const val hideEfficiencyChart = "hideEfficiencyChart"
    const val efficiencyLookBack = "efficiencyLookBack"
    const val detectedRHD = "detectedRHD"
    const val forceRHD = "forceRHD"
    const val lastDarkMode = "lastDarkMode"
    const val lastSunUp = "lastSunUp"
    const val lastSolarBrightnessFactor = "lastSolarBrightnessFactor"
    const val partyTimeTarget = "partyTimeTarget"
    const val cyberMode = "cyberMode"
    const val disableAutoBrightness = "disableAutoBrightness"

    // For efficiency history in prefs
    const val kwhHistory = "kwhHistory"
    const val parkedKwhHistory = "parkedKwhHistory"
    const val parkedStartKwhDischarge = "parkedStartKwhDischarge"
    const val parkedStartKwhCharge = "parkedStartKwhCharge"

    // For sonar arcs
    const val l1DistanceLowSpeed = 300f
    const val l2DistanceLowSpeed = 200f
    const val l1DistanceHighSpeed = 400f
    const val l2DistanceHighSpeed = 250f

    // For solar elevation brightness scaling
    const val solarElevationMax = 7f
    const val solarElevationMin = -7f
    const val solarElevationMinFactor = 0.25f

    const val blackoutOverrideSeconds = 10
    const val partyBlackoutDelaySeconds = 600
}