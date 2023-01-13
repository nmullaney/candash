package app.candash.cluster

import android.content.SharedPreferences

enum class Units(val tag: String) {
    DISTANCE_M(" m"),
    DISTANCE_KM(" km"),
    DISTANCE_MI(" mi"),
    POWER_W(" W"),
    POWER_KW(" kW"),
    POWER_HP(" hp"),
    POWER_PS(" PS"),
    SPEED_KPH(" km/h"),
    SPEED_MPH(" mph"),
    TEMPERATURE_C(" °C"),
    TEMPERATURE_F(" °F"),
    TORQUE_LB_FT(" lb-ft"),
    TORQUE_NM(" Nm"),
}

// Distance and speed conversions
val Float.mToKm: Float
    get() = (this / 1000f)
val Float.mToMi: Float
    get() = (this / 1609f)
val Float.kmToM: Float
    get() = (this * 1000f)
val Float.miToM: Float
    get() = (this * 1609f)
val Float.miToKm: Float
    get() = (this * 1.609f)
val Float.kmToMi: Float
    get() = (this / 1.609f)

// Temperature conversions
val Float.cToF: Float
    get() = ((this * 9f / 5f) + 32f)
val Float.fToC: Float
    get() = ((this - 32f) / 9f * 5f)

// Power conversions
val Float.wToKw: Float
    get() = (this / 1000f)
val Float.wToHp: Float
    get() = (this / 745.7f)
val Float.wToPs: Float
    get() = (this / 735.5f)
val Float.kwToW: Float
    get() = (this * 1000f)
val Float.hpToW: Float
    get() = (this * 745.7f)
val Float.psToW: Float
    get() = (this * 735.5f)

// Torque conversions
val Float.nmToLbfFt: Float
    get() = (this * 0.73756f)
val Float.lbfFtToNm: Float
    get() = (this / 0.73756f)

class UnitConverter(private val prefs: SharedPreferences) {
    private val distanceUnits = listOf(Units.DISTANCE_M, Units.DISTANCE_KM, Units.DISTANCE_MI)
    private val powerUnits = listOf(Units.POWER_W, Units.POWER_KW, Units.POWER_HP, Units.POWER_PS)
    private val speedUnits = listOf(Units.SPEED_KPH, Units.SPEED_MPH)
    private val tempUnits = listOf(Units.TEMPERATURE_C, Units.TEMPERATURE_F)
    private val torqueUnits = listOf(Units.TORQUE_LB_FT, Units.TORQUE_NM)

    fun prefDistanceUnit(): Units {
        return if (prefs.getBooleanPref(Constants.uiSpeedUnitsMPH)) Units.DISTANCE_MI else Units.DISTANCE_KM
    }

    fun prefPowerUnit(): Units {
        return when (prefs.getPref(Constants.powerUnits)) {
            Constants.powerUnitKw -> Units.POWER_KW
            Constants.powerUnitHp -> Units.POWER_HP
            Constants.powerUnitPs -> Units.POWER_PS
            else -> Units.POWER_W
        }
    }

    fun prefSpeedUnit(): Units {
        return if (prefs.getBooleanPref(Constants.uiSpeedUnitsMPH)) Units.SPEED_MPH else Units.SPEED_KPH
    }

    fun prefTempUnit(): Units {
        return if (prefs.getBooleanPref(Constants.tempInF)) Units.TEMPERATURE_F else Units.TEMPERATURE_C
    }

    fun prefTorqueUnit(): Units {
        return if (prefs.getBooleanPref(Constants.torqueInLbfFt)) Units.TORQUE_LB_FT else Units.TORQUE_NM
    }

    fun convertToPreferredUnit(nativeUnit: Units, value: Float): Float {
        return when (nativeUnit) {
            in distanceUnits -> convertDistance(value, nativeUnit)
            in powerUnits -> convertPower(value, nativeUnit)
            in speedUnits -> convertSpeed(value, nativeUnit)
            in tempUnits -> convertTemperature(value, nativeUnit)
            in torqueUnits -> convertTorque(value, nativeUnit)
            else -> value
        }
    }

    private fun convertDistance(value: Float, fromUnit: Units): Float {
        val toUnit = prefDistanceUnit()
        if (fromUnit == toUnit) {
            return value
        }
        val valueMeters = when (fromUnit) {
            Units.DISTANCE_KM -> value.kmToM
            Units.DISTANCE_MI -> value.miToM
            else -> value
        }
        return when (toUnit) {
            Units.DISTANCE_KM -> valueMeters.mToKm
            Units.DISTANCE_MI -> valueMeters.mToMi
            else -> valueMeters
        }
    }

    private fun convertPower(value: Float, fromUnit: Units): Float {
        val toUnit = prefPowerUnit()
        if (fromUnit == toUnit) {
            return value
        }
        val valueWatts = when (fromUnit) {
            Units.POWER_KW -> value.kwToW
            Units.POWER_HP -> value.hpToW
            Units.POWER_PS -> value.psToW
            else -> value
        }
        return when (toUnit) {
            Units.POWER_KW -> valueWatts.wToKw
            Units.POWER_HP -> valueWatts.wToHp
            Units.POWER_PS -> valueWatts.wToPs
            else -> valueWatts
        }
    }

    private fun convertSpeed(value: Float, fromUnit: Units): Float {
        return when (prefSpeedUnit()) {
            fromUnit -> value
            Units.SPEED_MPH -> value.kmToMi
            else -> value.miToKm
        }
    }

    private fun convertTemperature(value: Float, fromUnit: Units): Float {
        return when (prefTempUnit()) {
            fromUnit -> value
            Units.TEMPERATURE_C -> value.fToC
            else -> value.cToF
        }
    }

    private fun convertTorque(value: Float, fromUnit: Units): Float {
        return when (prefTorqueUnit()) {
            fromUnit -> value
            Units.TORQUE_LB_FT -> value.nmToLbfFt
            else -> value.lbfFtToNm
        }
    }
}