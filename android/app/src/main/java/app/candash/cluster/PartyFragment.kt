package app.candash.cluster

import android.content.Context
import android.content.SharedPreferences
import android.media.AudioAttributes
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import app.candash.cluster.databinding.FragmentPartyBinding
import kotlin.math.round

class PartyFragment : Fragment() {
    private lateinit var binding: FragmentPartyBinding
    private lateinit var viewModel: DashViewModel
    private lateinit var unitConverter: UnitConverter
    private lateinit var prefs: SharedPreferences
    private lateinit var alarmPlayer: Ringtone

    private var blackoutAfter = System.currentTimeMillis()
    private var tempDP = 0

    /**
     * This converts a float to the correct unit of measurement specified in sharedPreferences,
     * then rounds it to the provided decimal places and returns a string
     * which is formatted to show that many decimal places.
     *
     * @param nativeUnit Specify the original unit of measurement of the value
     * @param dp If null, decimal places are automatically determined by size of the Float
     */
    private fun Float.convertAndRoundToString(nativeUnit: Units, dp: Int? = null): String {
        return unitConverter.convertToPreferredUnit(nativeUnit, this, party=true).roundToString(dp)
    }

    /**
     * Get or set the view's visibility as a Boolean.
     * Returns true if visibility == View.VISIBLE
     *
     * Set true/false to show/hide the view.
     * If set to false, it will set the view to INVISIBLE unless this view is in
     * viewsToSetGone() in which case it will set it to GONE.
     */
    private var View.visible: Boolean
        get() = (this.visibility == View.VISIBLE)
        set(visible) {
            this.visibility = when {
                visible -> View.VISIBLE
                this in viewsToSetGone() -> View.GONE
                else -> View.INVISIBLE
            }
        }

    /**
     * Shows the view if the signal equals a value, hides it otherwise
     */
    private fun View.showWhen(signalName: String, isValue: Float?) {
        this.visible = (viewModel.carState[signalName] == isValue)
    }

    /**
     * These are views which should be set to View.GONE when hidden.
     * If not in this list, will be set to View.INVISIBLE.
     * Note that this should only be used when you want Views to shift based on constraints
     * to other views.
     *
     * Don't use for rapidly changing Views
     */
    private fun viewsToSetGone(): Set<View> =
        setOf(
            binding.battHeat,
            binding.battCharge,
            binding.batteryPercent,
            binding.lockOpen,
            binding.lockClosed,
            binding.blackout,
        ) + doorViews()

    private fun doorViews(): Set<View> =
        setOf(
            binding.modely,
            binding.frontleftdoor,
            binding.frontrightdoor,
            binding.rearleftdoor,
            binding.rearrightdoor,
            binding.hood,
            binding.hatch
        )

    private fun doorHiddenView(): Set<View> =
        setOf(
            binding.bigSoc,
            binding.bigSocPercent,
        )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentPartyBinding.inflate(inflater, container, false)
        prefs = requireContext().getSharedPreferences("dash", Context.MODE_PRIVATE)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Ensure the alarm is stopped when the user exits the view
        alarmPlayer.stop()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        prefs = requireContext().getSharedPreferences("dash", Context.MODE_PRIVATE)
        unitConverter = UnitConverter(prefs)

        // Use default alarm tone
        val alert = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        alarmPlayer = RingtoneManager.getRingtone(this.context, alert)
        alarmPlayer.audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()

        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(requireActivity()).get(DashViewModel::class.java)

        // On launch, set temp units to F if main preference is already F
        if (prefs.getBooleanPref(Constants.tempInF)) {
            prefs.setBooleanPref(Constants.partyTempInF, true)
        }

        if (prefs.getPref(Constants.partyTimeTarget) == 0f) {
            prefs.setPref(Constants.partyTimeTarget, 20f)
        }

        binding.blackout.visible = false

        // This is executed now to kick-start some logic even before we get car state data
        setGaugeVisibility()
        setUnits()

        setOf(
            binding.outsideTempUnit,
            binding.insideTempUnit,
            binding.battTempUnit,
            binding.insideTempReqUnit,
        ).forEach {
            it.setOnClickListener {
                prefs.setBooleanPref(
                    Constants.partyTempInF,
                    !prefs.getBooleanPref(Constants.partyTempInF)
                )
                setUnits()
                updateTemps()
                binding.infoToast.text = "Temp unit:" + unitConverter.prefTempUnit(party = true).tag
                binding.infoToast.visible = true
                binding.infoToast.startAnimation(fadeOut(5000))
            }
        }

        binding.partyTime.setOnClickListener {
            val newTarget = prefs.getPref(Constants.partyTimeTarget) + 5f
            if (newTarget > 90f || newTarget >= (viewModel.carState[SName.stateOfCharge]
                    ?: 90f)
            ) {
                prefs.setPref(Constants.partyTimeTarget, 20f)
            } else {
                prefs.setPref(Constants.partyTimeTarget, newTarget)
            }
            updateAlarm()
            binding.infoToast.text = "Alarm at: " + prefs.getPref(Constants.partyTimeTarget).roundToString(0) + "%"
            binding.infoToast.visible = true
            binding.infoToast.startAnimation(fadeOut(5000))
        }

        binding.blackout.setOnClickListener {
            // wake screen on tap, then eventually sleep again
            binding.blackout.visible = false
            blackoutAfterDelay(Constants.partyBlackoutDelaySeconds * 1000L)
        }

        binding.batteryPercent.setOnClickListener {
            prefs.setBooleanPref(Constants.showBattRange, !prefs.getBooleanPref(Constants.showBattRange))
            processBattery()
            binding.infoToast.text = if (prefs.getBooleanPref(Constants.showBattRange)) "Showing battery range" else "Showing battery SOC"
            binding.infoToast.visible = true
            binding.infoToast.startAnimation(fadeOut(5000))
        }

        binding.root.setOnLongClickListener {
            viewModel.switchToInfoFragment()
            return@setOnLongClickListener true
        }

        binding.bigSoc.setOnLongClickListener {
            prefs.setBooleanPref(Constants.forceDarkMode, !prefs.getBooleanPref(Constants.forceDarkMode))
            viewModel.updateTheme()
            binding.infoToast.text = if (prefs.getBooleanPref(Constants.forceDarkMode)) "Force dark mode" else "Auto dark mode"
            binding.infoToast.visible = true
            binding.infoToast.startAnimation(fadeOut(5000))
            return@setOnLongClickListener true
        }

        /**
         * Add signal observers and logic below.
         * Use one of viewModel.onSignal or onSomeSignals
         * Remember that it will only run when the value of the signal(s) change
         */

        viewModel.startThemeListener(viewLifecycleOwner)
        viewModel.startBrightnessListener(viewLifecycleOwner)

        viewModel.onSignal(viewLifecycleOwner, SName.keepClimateReq) {
            if (it != SVal.keepClimateParty) {
                viewModel.switchToDashFragment()
            }
        }

        viewModel.onSomeSignals(viewLifecycleOwner, SGroup.closures) { updateDoorStateUI() }

        viewModel.onSomeSignals(viewLifecycleOwner, listOf(SName.frontOccupancy) + SGroup.closures) {
            if (anyDoorOpen() || it[SName.frontOccupancy] == 2f) {
                updateBlackout()
            } else {
                blackoutAfterDelay(Constants.partyBlackoutDelaySeconds * 1000L)
            }
        }

        viewModel.onSomeSignals(
            viewLifecycleOwner,
            listOf(
                SName.outsideTemp,
                SName.insideTemp,
                SName.battBrickMin,
                SName.insideTempReq,
                SName.hvacAirDistribution
            )
        ) {
            updateTemps()
        }

        viewModel.onSignal(viewLifecycleOwner, SName.power) {
            if (it != null) {
                binding.dcPower.text = it.wToKw.roundToString()
                binding.dcPowerGauge.setGauge(it / 5000f)
            } else {
                binding.dcPower.text = ""
                binding.dcPowerGauge.setGauge(0f)
            }
        }

        viewModel.onSignal(viewLifecycleOwner, SName.dc12vPower) {
            if (it != null) {
                binding.dc12vPower.text = it.wToKw.roundToString()
                binding.dc12vPowerGauge.setGauge(it / 2000f)
            } else {
                binding.dc12vPower.text = ""
                binding.dc12vPowerGauge.setGauge(0f)
            }
        }

        viewModel.onSignal(viewLifecycleOwner, SName.partyHoursLeft) {
            updatePartyTime()
        }

        viewModel.onSomeSignals(viewLifecycleOwner, listOf(SName.uiRange, SName.stateOfCharge, SName.chargeStatus)) {
            processBattery()
            updateAlarm()
        }

        setOf(
            Triple(binding.battHeat, SName.heatBattery, 1f),
            Triple(binding.battCharge, SName.chargeStatus, SVal.chargeStatusActive),
            Triple(binding.lockOpen, SName.lockStatus, 1f),
            Triple(binding.lockClosed, SName.lockStatus, 2f)
        ).forEach { triple ->
            viewModel.onSignal(viewLifecycleOwner, triple.second) {
                triple.first.showWhen(triple.second, triple.third)
            }
        }
    }

    /**
     * Waits for [delayMs] then updates the blackout based on the future conditions.
     * If this is called multiple times within [delayMs], it will only update after the latest call + [delayMs]
     */
    private fun blackoutAfterDelay(delayMs: Long) {
        blackoutAfter = System.currentTimeMillis() + delayMs
        view?.postDelayed(
            { if (System.currentTimeMillis() >= blackoutAfter && context != null) updateBlackout() },
            delayMs
        )
    }

    private fun updateBlackout() {
        // the displayOn signal is always true in party mode, so instead we look at doors and butts
        if (!prefs.getBooleanPref(Constants.disableDisplaySync)
            && !anyDoorOpen()
            && viewModel.carState[SName.frontOccupancy] != 2f
            && !alarmPlayer.isPlaying
        ) {
            binding.blackout.visible = true
            binding.infoToast.text =
                "Display sleeping.\nTap screen to wake."
            // workaround for race condition of potential simultaneous calls
            if (activity != null) {
                binding.infoToast.visible = true
                binding.infoToast.startAnimation(fadeOut(5000))
            }
        } else {
            binding.blackout.visible = false
        }
    }

    private fun setGaugeVisibility() {
        doorHiddenView().forEach { it.visible = !anyDoorOpen() }
    }

    private fun updateDoorStateUI() {
        binding.modely.visible = anyDoorOpen()
        displayOpenDoors()
        setGaugeVisibility()
    }

    private fun anyDoorOpen(): Boolean {
        return (SGroup.closures.any { closureIsOpen(it) })
    }

    private fun closureIsOpen(signalName: String): Boolean {
        return (viewModel.carState[signalName] in setOf(1f, 4f, 5f))
    }

    private fun displayOpenDoors() {
        mapOf(
            SName.liftgateState to binding.hatch,
            SName.frunkState to binding.hood,
            SName.frontLeftDoorState to binding.frontleftdoor,
            SName.frontRightDoorState to binding.frontrightdoor,
            SName.rearLeftDoorState to binding.rearleftdoor,
            SName.rearRightDoorState to binding.rearrightdoor
        ).forEach {
            it.value.visible = closureIsOpen(it.key)
        }
    }

    private fun updatePartyTime() {
        val partyHours = viewModel.carState[SName.partyHoursLeft]
        if (alarmPlayer.isPlaying) {
            binding.partyTime.text = "Tap to dismiss alarm"
        } else if (partyHours != null && partyHours > 0 && (viewModel.carState[SName.slowPower]
                ?: 0f) > 100
        ) {
            val remaining = when {
                partyHours >= 48 -> (partyHours / 24).roundToString(1) + " days"
                else -> partyHours.roundToString(1) + " hours"
            }
            binding.partyTime.text =
                remaining + " to " + prefs.getPref(Constants.partyTimeTarget)
                    .roundToString(0) + "%"
        } else {
            binding.partyTime.text = ""
        }
    }

    private fun updateAlarm() {
        if (round(
                viewModel.carState[SName.stateOfCharge] ?: 100f
            ) <= prefs.getPref(Constants.partyTimeTarget) && !carIsCharging()
        ) {
            if (!alarmPlayer.isPlaying) {
                alarmPlayer.play()
                updatePartyTime()
                updateBlackout()
            }
        } else if (alarmPlayer.isPlaying) {
            alarmPlayer.stop()
            updatePartyTime()
            updateBlackout()
        }
    }

    private fun processBattery() {
        val socVal = viewModel.carState[SName.stateOfCharge]
        if (prefs.getBooleanPref(Constants.showBattRange)) {
            val range = viewModel.carState[SName.uiRange]
            binding.batteryPercent.text = if (range != null) range.convertAndRoundToString(
                Units.DISTANCE_MI,
                0
            ) + unitConverter.prefDistanceUnit().tag else ""
        } else {
            binding.batteryPercent.text = if (socVal != null) socVal.roundToString(0) + " %" else ""
        }
        binding.battery.setGauge(socVal ?: 0f)
        binding.battery.setChargeMode(carIsCharging())
        binding.battery.visible = (socVal != null)

        if (socVal != null) {
            binding.socGauge.setGauge(socVal / 100f, 4f, carIsCharging())
            binding.bigSoc.text = socVal.roundToString(0)
        } else {
            binding.socGauge.setGauge(0f, 4f)
            binding.bigSoc.text = ""
        }
    }

    private fun carIsCharging(): Boolean {
        val chargeStatus = viewModel.carState[SName.chargeStatus] ?: SVal.chargeStatusInactive
        return chargeStatus != SVal.chargeStatusInactive
    }

    private fun updateTemps() {
        val outsideTemp = viewModel.carState[SName.outsideTemp]
        if (outsideTemp != null) {
            binding.outsideTemp.text = outsideTemp.convertAndRoundToString(Units.TEMPERATURE_C, tempDP)
            binding.outsideTempGauge.setGauge((outsideTemp + 40f) / 128)
        } else {
            binding.outsideTemp.text = ""
            binding.outsideTempGauge.setGauge(0f)
        }

        val insideTemp = viewModel.carState[SName.insideTemp]
        if (insideTemp != null) {
            binding.insideTemp.text = insideTemp.convertAndRoundToString(Units.TEMPERATURE_C, tempDP)
            binding.insideTempGauge.setGauge((insideTemp + 40f) / 128)
        } else {
            binding.insideTemp.text = ""
            binding.insideTempGauge.setGauge(0f)
        }

        val battTemp = viewModel.carState[SName.battBrickMin]
        if (battTemp != null) {
            binding.battTemp.text = battTemp.convertAndRoundToString(Units.TEMPERATURE_C, tempDP)
            binding.battTempGauge.setGauge((battTemp + 40f) / 128)
        } else {
            binding.battTemp.text = ""
            binding.battTempGauge.setGauge(0f)
        }

        val reqTemp = viewModel.carState[SName.insideTempReq]
        val stateTag =
            if (viewModel.carState[SName.hvacAirDistribution] == SVal.hvacAirDistributionAuto) "(Auto)" else "(Manual)"
        if (reqTemp != null) {
            binding.insideTempReq.text = when (reqTemp) {
                15f -> "LO"
                28f -> "HI"
                else -> reqTemp.convertAndRoundToString(Units.TEMPERATURE_C, tempDP)
            }
            binding.insideTempReqAutoLabel.text = stateTag
            binding.insideTempReqGauge.setGauge((reqTemp - 15f) / 13)
        } else {
            binding.insideTempReq.text = ""
            binding.insideTempReqAutoLabel.text = ""
            binding.insideTempReqGauge.setGauge(0f)
        }
    }

    private fun setUnits() {
        if (prefs.getBooleanPref(Constants.partyTempInF)) {
            binding.outsideTempUnit.text = "°F"
            binding.insideTempUnit.text = "°F"
            binding.battTempUnit.text = "°F"
            binding.insideTempReqUnit.text = "°F"
            tempDP = 0
        } else {
            binding.outsideTempUnit.text = "°C"
            binding.insideTempUnit.text = "°C"
            binding.battTempUnit.text = "°C"
            binding.insideTempReqUnit.text = "°C"
            tempDP = 1
        }
    }

    private fun fadeOut(duration: Long = 500L): Animation {
        val fadeOut = AnimationUtils.loadAnimation(activity, R.anim.fade_out)
        fadeOut.duration = duration
        return fadeOut
    }

    // If the discoveryService finds a different ip address, save the new
    // address and restart
    private fun setupZeroConfListener() {
        viewModel.zeroConfIpAddress.observe(viewLifecycleOwner) { ipAddress ->
            if (viewModel.serverIpAddress() != ipAddress && !ipAddress.equals("0.0.0.0")) {
                viewModel.saveSettings(ipAddress)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        setupZeroConfListener()
        binding.root.postDelayed({
            viewModel.startDiscoveryService()
        }, 2000)
    }

    override fun onDestroy() {
        viewModel.stopDiscoveryService()
        super.onDestroy()
    }

    override fun onPause() {
        viewModel.stopDiscoveryService()
        super.onPause()
    }
}