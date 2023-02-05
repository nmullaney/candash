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

    private var lastSavedAtKm = 0f

    init {
        loadHistoryFromPrefs()
        lastSavedAtKm = kwhHistory.lastOrNull()?.first ?: 0f
    }

    fun changeLookBack(): String {
        val inMiles = prefs.getBooleanPref(Constants.uiSpeedUnitsMPH)
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

    fun clearHistory() {
        kwhHistory.clear()
        parkedKwhHistory.clear()
        saveHistoryToPrefs()
    }

    fun getEfficiencyText(): String? {
        val inMiles = prefs.getBooleanPref(Constants.uiSpeedUnitsMPH)
        val power = viewModel.carState[SName.power] ?: 0f
        val lookBackKm = prefs.getPref(Constants.efficiencyLookBack)
        return if (lookBackKm == 0f) {
            getInstantEfficiencyText(inMiles, power)
        } else {
            getRecentEfficiencyText(inMiles, lookBackKm)
        }
    }

    /**
     * Returns a list of pairs, where first is km from now (e.g. -10), and second
     * is the efficiency in Wh/km. The result is smoothed and length is based on
     * the efficiency look-back pref.
     */
    fun getEfficiencyHistory(): List<Pair<Float, Float>> {
        val lookBack = prefs.getPref(Constants.efficiencyLookBack)
        if (lookBack == 0f) return listOf()
        val nowOdo = kwhHistory.lastOrNull()?.first ?: 0f

        val data = mutableListOf<Pair<Float, Float>>()

        for (i in 1 until kwhHistory.size) {
            val (lastOdo, lastDischarge, lastCharge) = kwhHistory[i-1]
            val (odo, discharge, charge) = kwhHistory[i]
            // Skip parked consumption/charges
            val parked = parkedKwhHistory.firstOrNull { it.first in lastOdo..odo }
            if (parked != null) continue

            val odoDelta = odo - lastOdo
            val dischargeDelta = discharge - lastDischarge
            val chargeDelta = charge - lastCharge
            val consumedWh = (dischargeDelta - chargeDelta) * 1000f
            data.add(Pair(odo - nowOdo, consumedWh / odoDelta))
        }
        // Smooth to 1/50th of the look-back
        val smoothWindowKm = lookBack * Constants.efficiencyChartSmoothing
        return smoothEfficiencyHistory(data.toList(), smoothWindowKm)
    }

    private fun smoothEfficiencyHistory(
        values: List<Pair<Float, Float>>,
        windowSizeKm: Float
    ): List<Pair<Float, Float>> {
        if (windowSizeKm <= 0 || values.size <= 1) return values.toList()
        val result = mutableListOf<Pair<Float, Float>>()
        for ((km, _) in values) {
            val start = values.indexOfFirst { it.first >= km - windowSizeKm / 2 }
            val end = values.indexOfLast { it.first <= km + windowSizeKm / 2 }
            val window = values.slice(start until end + 1)
            val smoothedValue = window.sumOf { it.second.toDouble() } / window.size
            result.add(Pair(km, smoothedValue.toFloat()))
        }
        return result
    }

    fun updateKwhHistory() {
        val odo = viewModel.carState[SName.odometer]
        val kwhDischargeTotal = viewModel.carState[SName.kwhDischargeTotal]
        val kwhChargeTotal = viewModel.carState[SName.kwhChargeTotal]
        if (odo == null || kwhDischargeTotal == null || kwhChargeTotal == null) {
            return
        }
        updateParkedHistory(odo, kwhDischargeTotal, kwhChargeTotal)
        updateHistory(odo, kwhDischargeTotal, kwhChargeTotal)
    }

    private fun updateParkedHistory(odo: Float, discharge: Float, charge: Float) {
        val gear = viewModel.carState[SName.gearSelected] ?: SVal.gearInvalid
        // Switching from D/R/N to Park
        if (gear in setOf(SVal.gearPark, SVal.gearInvalid) && parkedStartKwh == null) {
            parkedStartKwh = Pair(discharge, charge)
            saveHistoryToPrefs()
            return
        }

        // Switching from Park to D/R/N
        if (gear !in setOf(SVal.gearPark, SVal.gearInvalid) && parkedStartKwh != null) {
            parkedKwhHistory.add(
                Triple(
                    odo,
                    discharge - parkedStartKwh!!.first,
                    charge - parkedStartKwh!!.second
                )
            )
            parkedStartKwh = null
            parkedKwhHistory.removeIf { it.first < odo - 51f }
            saveHistoryToPrefs()
            return
        }
    }

    private fun updateHistory(odo: Float, discharge: Float, charge: Float) {
        val lastOdo = kwhHistory.lastOrNull()?.first ?: 0f
        val newTriple = Triple(odo, discharge, charge)
        if (odo - lastOdo >= 8.0 || odo - lastOdo < 0) {
            // We're missing too much data, start over
            clearHistory()
            kwhHistory.add(newTriple)
        } else if (odo - lastOdo >= Constants.efficiencyOdoStepKm) {
            // We have travelled far enough, add to history
            kwhHistory.add(newTriple)
        } else {
            // We haven't travelled far enough yet, return because
            // there is no need to cleanup or save to prefs
            return
        }
        // Cleanup history from more than 51 km ago
        kwhHistory.removeIf { it.first < odo - 51f }
        // Don't spam writing to disk while driving, the app is unlikely to restart in-motion
        if (odo - lastSavedAtKm >= 0.5) {
            saveHistoryToPrefs()
        }
    }

    private fun getInstantEfficiencyText(inMiles: Boolean, power: Float): String? {
        val speed = viewModel.carState[SName.uiSpeed] ?: 0f
        // If speed is 0 (or null) return to prevent "infinity kWh/mi"
        if (speed == 0f) {
            return null
        }
        val instantEfficiency = power / speed / 1000f
        return if (inMiles) {
            "%.2f kWh/mi".format(instantEfficiency)
        } else {
            "%.2f kWh/km".format(instantEfficiency)
        }
    }

    private fun getRecentEfficiencyText(inMiles: Boolean, lookBackKm: Float): String? {
        val newOdo = viewModel.carState[SName.odometer]
        // If parked, use the (dis)charge values from the start of park so display doesn't change
        val newDischarge = parkedStartKwh?.first ?: viewModel.carState[SName.kwhDischargeTotal]
        val newCharge = parkedStartKwh?.second ?: viewModel.carState[SName.kwhChargeTotal]
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
        prefs.setPref(Constants.parkedStartKwhDischarge, parkedStartKwh?.first ?: 0f)
        prefs.setPref(Constants.parkedStartKwhCharge, parkedStartKwh?.second ?: 0f)
        lastSavedAtKm = kwhHistory.lastOrNull()?.first ?: 0f
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
}