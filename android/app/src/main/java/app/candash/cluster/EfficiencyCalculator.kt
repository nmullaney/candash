package app.candash.cluster

import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class EfficiencyCalculator(
    private val viewModel: DashViewModel,
    private val prefs: SharedPreferences
) {
    private val gson = Gson()

    // these triples contain (odometerKm, kwhDischarge, kwhCharge)
    private var kwhHistory = mutableListOf<Triple<Float, Float, Float>>()
    private var parkedKwhHistory = mutableListOf<Triple<Float, Float, Float>>()
    private var parkedStartKwh: Pair<Float, Float>? = null
    private val tripleFloatListType =
        object : TypeToken<MutableList<Triple<Float, Float, Float>>>() {}.type

    private val Float.miToKm: Float
        get() = (this / .621371).toFloat()
    private val Float.kmToMi: Float
        get() = (this * .621371).toFloat()

    fun changeLookBack(inMiles: Boolean): String {
        val old = prefs.getPref(Constants.efficiencyLookBack)
        val options = if (inMiles) {
            listOf(0f, 5f.miToKm, 15f.miToKm, 30f.miToKm)
        } else {
            listOf(0f, 10f, 25f, 50f)
        }
        val index = (options.indexOf(old) + 1) % options.size
        prefs.setPref(Constants.efficiencyLookBack, options[index])

        return if (inMiles) {
            "Last %.0f miles".format(options[index].kmToMi)
        } else {
            "Last %.0f kilometers".format(options[index])
        }
    }

    fun updateKwhHistory(odoKm: Float) {
        loadHistoryFromPrefs()
        val kwhDischargeTotal = viewModel.getValue(Constants.kwhDischargeTotal)
        val kwhChargeTotal = viewModel.getValue(Constants.kwhChargeTotal)
        if (kwhDischargeTotal == null || kwhChargeTotal == null) {
            return
        }
        updateParkedHistory(odoKm, kwhDischargeTotal, kwhChargeTotal)
        updateHistory(odoKm, kwhDischargeTotal, kwhChargeTotal)
    }

    fun getEfficiencyText(inMiles: Boolean, power: Float): String? {
        val lookBackKm = prefs.getPref(Constants.efficiencyLookBack)
        return if (lookBackKm == 0f) {
            getInstantEfficiencyText(inMiles, power)
        } else {
            getRecentEfficiencyText(inMiles, lookBackKm)
        }
    }

    private fun getInstantEfficiencyText(inMiles: Boolean, power: Float): String? {
        val speed = viewModel.getValue(Constants.uiSpeed) ?: return null
        val instantEfficiency = power / speed / 1000f
        return if (inMiles) {
            "%.2f kWh/mi".format(instantEfficiency)
        } else {
            "%.2f kWh/km".format(instantEfficiency)
        }
    }

    private fun getRecentEfficiencyText(inMiles: Boolean, lookBackKm: Float): String? {
        val newOdo = viewModel.getValue(Constants.odometer)
        val newDischarge = viewModel.getValue(Constants.kwhDischargeTotal)
        val newCharge = viewModel.getValue(Constants.kwhChargeTotal)
        if (newOdo == null || newDischarge == null || newCharge == null) {
            return null
        }
        val targetKm = newOdo - lookBackKm
        val oldKwhTriple = kwhHistory.lastOrNull { it.first <= targetKm }
            ?: return getCalculatingText(inMiles, lookBackKm, newOdo)

        val odoDelta = newOdo - oldKwhTriple.first
        var dischargeDelta = newDischarge - oldKwhTriple.second
        var chargeDelta = newCharge - oldKwhTriple.third
        // Subtract parked consumption/charges
        for (parkedTriple in parkedKwhHistory.filter { it.first >= targetKm }) {
            dischargeDelta -= parkedTriple.second
            chargeDelta -= parkedTriple.third
        }
        val consumedWh = (dischargeDelta - chargeDelta) * 1000f
        return if (inMiles) {
            "%.0f Wh/mi".format((consumedWh / odoDelta.kmToMi))
        } else {
            "%.0f Wh/km".format(consumedWh / odoDelta)
        }
    }

    private fun getCalculatingText(inMiles: Boolean, lookBackKm: Float, odo: Float): String {
        if (kwhHistory.size > 0) {
            val distanceKm = odo - kwhHistory.first().first
            // Using toInt as a floor rounding
            return if (inMiles) {
                "(%d of %d mi)".format(
                    distanceKm.kmToMi.toInt(),
                    lookBackKm.kmToMi.toInt()
                )
            } else {
                "(%d of %d km)".format(
                    distanceKm.toInt(),
                    lookBackKm.toInt()
                )
            }
        } else {
            return "(calculating)"
        }
    }

    private fun saveHistoryToPrefs() {
        prefs.setStringPref(Constants.kwhHistory, gson.toJson(kwhHistory))
        prefs.setStringPref(Constants.parkedKwhHistory, gson.toJson(parkedKwhHistory))
    }

    private fun loadHistoryFromPrefs() {
        if (kwhHistory.isEmpty() && prefs.getStringPref(Constants.kwhHistory) != null) {
            kwhHistory =
                gson.fromJson(prefs.getStringPref(Constants.kwhHistory), tripleFloatListType)
        }
        if (parkedKwhHistory.isEmpty() && prefs.getStringPref(Constants.parkedKwhHistory) != null) {
            parkedKwhHistory =
                gson.fromJson(prefs.getStringPref(Constants.parkedKwhHistory), tripleFloatListType)
        }
        if (parkedStartKwh == null && prefs.getPref(Constants.parkedStartKwhDischarge) > 0) {
            parkedStartKwh = Pair(
                prefs.getPref(Constants.parkedStartKwhDischarge),
                prefs.getPref(Constants.parkedStartKwhCharge)
            )
        }
    }

    private fun updateParkedHistory(odo: Float, discharge: Float, charge: Float) {
        val gear = viewModel.getValue(Constants.gearSelected) ?: Constants.gearInvalid
        // Edge from Drive to Park
        if (gear in setOf(Constants.gearPark, Constants.gearInvalid) && parkedStartKwh == null) {
            parkedStartKwh = Pair(discharge, charge)
            prefs.setPref(Constants.parkedStartKwhDischarge, discharge)
            prefs.setPref(Constants.parkedStartKwhCharge, charge)
        }

        // Edge from Park to Drive
        if (gear !in setOf(Constants.gearPark, Constants.gearInvalid) && parkedStartKwh != null) {
            parkedKwhHistory.add(
                Triple(
                    odo,
                    discharge - parkedStartKwh!!.first,
                    charge - parkedStartKwh!!.second
                )
            )
            while (parkedKwhHistory.firstOrNull { it.first < odo - 51f } != null) {
                parkedKwhHistory.removeFirst()
            }
            saveHistoryToPrefs()
            parkedStartKwh = null
            prefs.setPref(Constants.parkedStartKwhDischarge, 0f)
            prefs.setPref(Constants.parkedStartKwhCharge, 0f)
        }
    }

    private fun updateHistory(odo: Float, discharge: Float, charge: Float) {
        val histEndOdo = kwhHistory.lastOrNull()?.first ?: 0f
        if (odo - histEndOdo >= 1.0 || odo - histEndOdo < 0f) {
            // We're missing too much data, reset
            kwhHistory.clear()
            kwhHistory.add(
                Triple(
                    odo,
                    discharge,
                    charge
                )
            )
        } else if (odo - histEndOdo >= 0.1) {
            kwhHistory.add(
                Triple(
                    odo,
                    discharge,
                    charge
                )
            )
        } else {
            return
        }
        while (kwhHistory.firstOrNull { it.first < odo - 51f } != null) {
            kwhHistory.removeFirst()
        }
        saveHistoryToPrefs()
    }
}