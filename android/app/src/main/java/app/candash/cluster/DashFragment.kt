package app.candash.cluster

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.content.Context
import android.content.SharedPreferences
import android.content.res.Resources
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.GradientDrawable.Orientation
import android.os.Build
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.animation.doOnEnd
import androidx.core.view.setMargins
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import app.candash.cluster.databinding.FragmentDashBinding
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.pow

class DashFragment : Fragment() {
    private lateinit var binding: FragmentDashBinding
    private lateinit var viewModel: DashViewModel
    private lateinit var unitConverter: UnitConverter
    private lateinit var prefs: SharedPreferences
    private var savedLayoutParams: MutableMap<View, ConstraintLayout.LayoutParams> = mutableMapOf()

    // Gradient animations:
    private lateinit var autopilotAnimation: ValueAnimator
    private lateinit var blindspotAnimation: ValueAnimator
    private lateinit var overlayGradient: GradientDrawable
    private var gradientColorFrom: Int = 0

    /**
     * This converts a float to the correct unit of measurement specified in sharedPreferences,
     * then rounds it to the provided decimal places and returns a string
     * which is formatted to show that many decimal places.
     *
     * @param nativeUnit Specify the original unit of measurement of the value
     * @param dp If null, decimal places are automatically determined by size of the Float
     */
    private fun Float.convertAndRoundToString(nativeUnit: Units, dp: Int? = null): String {
        return unitConverter.convertToPreferredUnit(nativeUnit, this).roundToString(dp)
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

    private fun View.showWhen(signalName: String, isValue: Array<Float?>) {
        this.visible = (viewModel.carState[signalName] in isValue)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentDashBinding.inflate(inflater, container, false)
        prefs = requireContext().getSharedPreferences("dash", Context.MODE_PRIVATE)
        return binding.root
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
            binding.telltaleDrl,
            binding.telltaleLb,
            binding.telltaleHb,
            binding.telltaleFogFront,
            binding.telltaleFogRear,
            binding.battHeat,
            binding.battCharge,
            binding.batterypercent,
            binding.telltaleLimRegen,
            binding.blackout,
            binding.PINWarning,
            binding.APWarning,
            binding.BSWarningLeft,
            binding.BSWarningRight,
            binding.FCWarning,
            binding.warningGradientOverlay,
        ) + minMaxPowerViews() + doorViewsCenter() + doorViews()

    /**
     * These views are bumped up when in split screen closer to the status bar
     */
    private fun topUIViews(): Set<View> =
        setOf(
            binding.PRND,
            binding.batterypercent,
            binding.battery,
            binding.batteryOverlay,
            binding.leftTurnSignalLight,
            binding.leftTurnSignalDark,
            binding.rightTurnSignalLight,
            binding.rightTurnSignalDark,
            binding.autopilot,
            binding.speed,
            binding.unit
        )



    /**
     * These are telltales which should be hidden when in split screen
     */
    private fun splitScreenHiddenTelltales(): Set<View> =
        setOf(
            binding.telltaleDrl,
            binding.telltaleLb,
            binding.telltaleHb,
            binding.telltaleFogFront,
            binding.telltaleFogRear,
            binding.odometer,
            binding.battCharge,
            binding.battHeat,
            binding.telltaleTPMSFaultHard,
            binding.telltaleTPMSFaultSoft,
            binding.telltaleLimRegen,
            binding.TACC
        )

    private fun leftSideUIViews(): Set<View> =
        setOf(
            binding.fronttemp,
            binding.fronttemplabel,
            binding.fronttempunits,
            binding.fronttempgauge,
            binding.reartemp,
            binding.reartemplabel,
            binding.reartempunits,
            binding.reartempgauge,
            binding.frontbraketemp,
            binding.frontbraketemplabel,
            binding.frontbraketempunits,
            binding.frontbraketempgauge,
            binding.rearbraketemp,
            binding.rearbraketemplabel,
            binding.rearbraketempunits,
            binding.rearbraketempgauge,
        )

    private fun rightSideUIViews(): Set<View> =
        setOf(
            binding.fronttorque,
            binding.fronttorquelabel,
            binding.fronttorqueunits,
            binding.fronttorquegauge,
            binding.reartorque,
            binding.reartorquelabel,
            binding.reartorqueunits,
            binding.reartorquegauge,
            binding.coolantflow,
            binding.coolantflowlabel,
            binding.coolantflowunits,
            binding.coolantflowgauge,
            binding.batttemp,
            binding.batttemplabel,
            binding.batttempunits,
            binding.batttempgauge,
        )

    private fun sideUIViews(): Set<View> = leftSideUIViews() + rightSideUIViews()

    private fun awdOnlyViews(): Set<View> =
        setOf(
            binding.fronttemp,
            binding.fronttemplabel,
            binding.fronttempunits,
            binding.fronttempgauge,
            binding.fronttorque,
            binding.fronttorquelabel,
            binding.fronttorqueunits,
            binding.fronttorquegauge,
        )

    private fun chargingViews(): Set<View> =
        setOf(
            binding.bigsoc,
            binding.bigsocpercent,
            binding.chargerate,
            binding.chargemeter
        )

    private fun drivingViews(): Set<View> =
        setOf(
            binding.powerBar,
            binding.power,
            binding.speed,
            binding.unit
        )

    private fun centerDoorHiddenViews(): Set<View> =
        setOf(
            binding.speed,
            binding.unit
        ) + chargingViews()

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

    private fun doorViewsCenter(): Set<View> =
        setOf(
            binding.modelyCenter,
            binding.frontleftdoorCenter,
            binding.frontrightdoorCenter,
            binding.rearleftdoorCenter,
            binding.rearrightdoorCenter,
            binding.hoodCenter,
            binding.hatchCenter
        )

    private fun minMaxPowerViews(): Set<View> =
        setOf(
            binding.maxpower,
            binding.minpower,
        )

    private fun getScreenWidth(): Int {
        val displayMetrics = DisplayMetrics()
        activity?.windowManager
            ?.defaultDisplay?.getMetrics(displayMetrics)
        return displayMetrics.widthPixels
    }

    private fun getRealScreenWidth(): Int {
        val displayMetrics = DisplayMetrics()
        activity?.windowManager
            ?.defaultDisplay?.getRealMetrics(displayMetrics)
        return displayMetrics.widthPixels
    }

    private fun isSplitScreen(): Boolean {
        return getRealScreenWidth() > getScreenWidth() * 2
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        prefs = requireContext().getSharedPreferences("dash", Context.MODE_PRIVATE)
        unitConverter = UnitConverter(prefs)

        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(requireActivity()).get(DashViewModel::class.java)

        if (!prefs.prefContains(Constants.gaugeMode)) {
            prefs.setPref(Constants.gaugeMode, Constants.showFullGauges)
        }

        if (prefs.getBooleanPref(Constants.tempInF)) {
            binding.frontbraketempunits.text = "°F"
            binding.rearbraketempunits.text = "°F"
            binding.fronttempunits.text = "°F"
            binding.reartempunits.text = "°F"
            binding.batttempunits.text = "°F"
        } else {
            binding.frontbraketempunits.text = "°C"
            binding.rearbraketempunits.text = "°C"
            binding.fronttempunits.text = "°C"
            binding.reartempunits.text = "°C"
            binding.batttempunits.text = "°C"
        }

        if (prefs.getBooleanPref(Constants.torqueInLbfFt)) {
            binding.fronttorqueunits.text = "lb-ft"
            binding.reartorqueunits.text = "lb-ft"
        } else {
            binding.fronttorqueunits.text = "Nm"
            binding.reartorqueunits.text = "Nm"
        }

        setupGradientOverlays()

        binding.blackout.visible = false

        if (!isSplitScreen()) {
            for (topUIView in topUIViews()) {
                savedLayoutParams[topUIView] =
                    ConstraintLayout.LayoutParams(topUIView.layoutParams as ConstraintLayout.LayoutParams)
            }
        }

        // This is executed now to kick-start some logic even before we get car state data
        setColors()
        setGaugeVisibility()
        setLayoutOrder()

        binding.minpower.setOnLongClickListener {
            prefs.setPref("minPower", 0f)
            binding.infoToast.text = "Reset min power value"
            binding.infoToast.visible = true
            binding.infoToast.startAnimation(fadeOut(5000))
            return@setOnLongClickListener true
        }
        binding.maxpower.setOnLongClickListener {
            prefs.setPref("maxPower", 0f)
            binding.infoToast.text = "Reset max power value"
            binding.infoToast.visible = true
            binding.infoToast.startAnimation(fadeOut(5000))
            return@setOnLongClickListener true
        }
        binding.root.setOnLongClickListener {
            viewModel.switchToInfoFragment()
            return@setOnLongClickListener true
        }
        binding.batterypercent.setOnClickListener {
            prefs.setBooleanPref(Constants.showBattRange, !prefs.getBooleanPref(Constants.showBattRange))
            processBattery()
            binding.infoToast.text = if (prefs.getBooleanPref(Constants.showBattRange)) "Showing battery range" else "Showing battery SOC"
            binding.infoToast.visible = true
            binding.infoToast.startAnimation(fadeOut(5000))
        }
        binding.unit.setOnClickListener {
            if (prefs.getPref(Constants.gaugeMode) < Constants.showFullGauges) {
                prefs.setPref(Constants.gaugeMode, prefs.getPref(Constants.gaugeMode) + 1f)
            } else {
                prefs.setPref(Constants.gaugeMode, Constants.showSimpleGauges)
            }
            setGaugeVisibility()
            binding.infoToast.text = when (prefs.getPref(Constants.gaugeMode)) {
                Constants.showSimpleGauges -> "Showing simple gauges"
                Constants.showRegularGauges -> "Showing regular gauges"
                Constants.showFullGauges -> "Showing full gauges"
                else -> "Unknown gauge selection"
            }
            binding.infoToast.visible = true
            binding.infoToast.startAnimation(fadeOut(5000))
        }

        binding.power.setOnClickListener {
            if (prefs.getPref(Constants.powerUnits) < Constants.powerUnitPs) {
                prefs.setPref(Constants.powerUnits, prefs.getPref(Constants.powerUnits) + 1f)
            } else {
                prefs.setPref(Constants.powerUnits, Constants.powerUnitKw)
            }
            binding.infoToast.text = "Power unit:" + unitConverter.prefPowerUnit().tag
            binding.infoToast.visible = true
            binding.infoToast.startAnimation(fadeOut(5000))
        }

        binding.PRND.setOnLongClickListener {
            prefs.setBooleanPref(Constants.forceRHD, !prefs.getBooleanPref(Constants.forceRHD))
            setLayoutOrder()
            binding.infoToast.text = if (prefs.getBooleanPref(Constants.forceRHD)) "Force right-hand drive" else "Auto right-hand drive"
            binding.infoToast.visible = true
            binding.infoToast.startAnimation(fadeOut(5000))
            return@setOnLongClickListener true
        }

        binding.speed.setOnLongClickListener {
            prefs.setBooleanPref(Constants.forceNightMode, !prefs.getBooleanPref(Constants.forceNightMode))
            setColors()
            binding.infoToast.text = if (prefs.getBooleanPref(Constants.forceNightMode)) "Force dark mode" else "Auto dark mode"
            binding.infoToast.visible = true
            binding.infoToast.startAnimation(fadeOut(5000))
            return@setOnLongClickListener true
        }

        binding.blackout.setOnClickListener {
            // wake screen on tap, then eventually sleep again
            binding.blackout.visible = false
            view.postDelayed({updateBlackout()}, Constants.blackoutOverrideSeconds * 1000L)
        }

        val efficiencyCalculator = EfficiencyCalculator(viewModel, prefs)

        binding.efficiency.setOnClickListener {
            binding.infoToast.text = efficiencyCalculator.changeLookBack()
            binding.infoToast.visible = true
            binding.infoToast.startAnimation(fadeOut(5000))
        }

        binding.efficiency.setOnLongClickListener {
            efficiencyCalculator.clearHistory()
            binding.infoToast.text = "Cleared efficiency history"
            binding.infoToast.visible = true
            binding.infoToast.startAnimation(fadeOut(5000))
            return@setOnLongClickListener true
        }

        viewModel.getSplitScreen().observe(viewLifecycleOwner) { isSplit ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && view.windowToken != null) {
                // only needed for Android 11+
                if (isSplit) {
                    for (topUIView in topUIViews()) {
                        val params = topUIView.layoutParams as ConstraintLayout.LayoutParams
                        val savedParams = savedLayoutParams[topUIView]
                        if (topUIView.equals(binding.speed)){
                            params.setMargins(
                                savedParams!!.leftMargin,
                                savedParams.topMargin - 30.px,
                                savedParams.rightMargin,
                                savedParams.bottomMargin + 30.px
                            )

                        }else {
                            params.setMargins(
                                savedParams!!.leftMargin,
                                savedParams.topMargin - 30.px,
                                savedParams.rightMargin,
                                savedParams.bottomMargin
                            )
                        }
                      if (topUIView.equals(binding.unit)) {
                            params.circleRadius = savedParams.circleRadius - 30
                        }
                        topUIView.layoutParams = params
                    }
                } else {
                    //no split screen
                    for (topUIView in topUIViews()) {
                        val params = topUIView.layoutParams as ConstraintLayout.LayoutParams
                        val savedParams = savedLayoutParams[topUIView]
                        params.setMargins(
                            savedParams!!.leftMargin,
                            savedParams.topMargin,
                            savedParams.rightMargin,
                            savedParams.bottomMargin
                        )
                        params.circleRadius = savedParams.circleRadius
                        topUIView.layoutParams = params
                    }
                }
            }
            // Update views which are affected by split screen changes
            setGaugeVisibility()
            updateDoorStateUI()
            updateSplitScreenTellTales()
            updateSpeedLimitSign()
        }

        /**
         * Add signal observers and logic below.
         * Use one of viewModel.onSignal or onSomeSignals
         * Remember that it will only run when the value of the signal(s) change
         */

        viewModel.onSignal(viewLifecycleOwner, SName.keepClimateReq) {
            if (it == SVal.keepClimateParty) {
                viewModel.switchToPartyFragment()
            }
        }

        viewModel.onSignal(viewLifecycleOwner, SName.driverOrientation) {
            prefs.setBooleanPref(
                Constants.detectedRHD,
                it in setOf(2f, 4f)
            )
            setLayoutOrder()
        }

        viewModel.onSignal(viewLifecycleOwner, SName.driveConfig) {
            if (it == SVal.rwd) {
                awdOnlyViews().forEach { view -> view.visible = false }
            }
        }

        // set display night/day mode based on reported car status
        viewModel.onSignal(viewLifecycleOwner, SName.isSunUp) {
            setColors()
        }

        viewModel.onSomeSignals(viewLifecycleOwner, listOf(SName.displayOn, SName.gearSelected)) {
            updateBlackout()
        }

        viewModel.onSomeSignals(viewLifecycleOwner, listOf(SName.uiSpeed, SName.brakeHold)) {
            val speed = it[SName.uiSpeed]
            if (speed != null) {
                if (it[SName.brakeHold] == 1f) {
                    binding.speed.text = ""
                } else {
                    binding.speed.text = speed.roundToString(0)
                }
            } else {
                binding.speed.text = ""
            }
        }

        viewModel.onSomeSignals(viewLifecycleOwner, listOf(SName.uiSpeedUnits, SName.brakeHold)) {
            val uiSpeedUnits = it[SName.uiSpeedUnits]
            if (uiSpeedUnits != null) {
                prefs.setBooleanPref(Constants.uiSpeedUnitsMPH, (uiSpeedUnits == 0f))
                if (it[SName.brakeHold] == 1f) {
                    binding.unit.text = "HOLD"

                } else {
                    binding.unit.text = unitConverter.prefSpeedUnit().tag.trim().uppercase()
                }
            } else {
                binding.unit.text = ""
            }
        }

        viewModel.onSignal(viewLifecycleOwner, SName.power) {
            if (it != null) {
                if (it > prefs.getPref("maxPower")) prefs.setPref("maxPower", it)
                if (!carIsCharging()) {
                    // do not store min power if car is charging
                    if (it < prefs.getPref("minPower")) prefs.setPref("minPower", it)
                }
                binding.minpower.text = formatPower(prefs.getPref("minPower"))
                binding.maxpower.text = formatPower(prefs.getPref("maxPower"))
                binding.power.text = formatPower(it)

                if (it >= 0) {
                    binding.powerBar.setGauge(((it / prefs.getPref("maxPower")).pow(0.75f)))
                } else {
                    binding.powerBar.setGauge(-((it / prefs.getPref("minPower")).pow(0.75f)))
                }
                // Negate for charge rate view (not using absolute so that power loss can be observed)
                binding.chargerate.text = (it * -1f).wToKw.roundToString() + " kW"
            } else {
                binding.powerBar.setGauge(0f)
                binding.power.text = ""
                binding.minpower.text = ""
                binding.maxpower.text = ""
                binding.chargerate.text = ""
            }
        }

        viewModel.onSomeSignals(viewLifecycleOwner, listOf(SName.gearSelected, SName.autopilotState)) {
            updateGearView()
        }

        viewModel.onSomeSignals(viewLifecycleOwner, listOf(SName.brakeTempFL, SName.brakeTempFR)) {
            val fl = it[SName.brakeTempFL]
            val fr = it[SName.brakeTempFR]
            if (fl != null && fr != null) {
                val frontBrakeTemp = max(fl, fr)
                binding.frontbraketemp.text = frontBrakeTemp.convertAndRoundToString(Units.TEMPERATURE_C, 0)
                binding.frontbraketempgauge.setGauge(frontBrakeTemp / 984f)
            } else {
                binding.frontbraketemp.text = ""
                binding.frontbraketempgauge.setGauge(0f)
            }
        }

        viewModel.onSomeSignals(viewLifecycleOwner, listOf(SName.brakeTempRL, SName.brakeTempRR)) {
            val rl = it[SName.brakeTempRL]
            val rr = it[SName.brakeTempRR]
            if (rl != null && rr != null) {
                val rearBrakeTemp = max(rl, rr)
                binding.rearbraketemp.text = rearBrakeTemp.convertAndRoundToString(Units.TEMPERATURE_C, 0)
                binding.rearbraketempgauge.setGauge(rearBrakeTemp / 984f)
            } else {
                binding.rearbraketemp.text = ""
                binding.rearbraketempgauge.setGauge(0f)
            }
        }

        viewModel.onSignal(viewLifecycleOwner, SName.frontTemp) {
            if (it != null) {
                binding.fronttemp.text = it.convertAndRoundToString(Units.TEMPERATURE_C, 0)
                binding.fronttempgauge.setGauge(it / 214f)
            } else {
                binding.fronttemp.text = ""
                binding.fronttempgauge.setGauge(0f)
            }
        }

        viewModel.onSignal(viewLifecycleOwner, SName.rearTemp) {
            if (it != null) {
                binding.reartemp.text = it.convertAndRoundToString(Units.TEMPERATURE_C, 0)
                binding.reartempgauge.setGauge(it / 214f)
            } else {
                binding.reartemp.text = ""
                binding.reartempgauge.setGauge(0f)
            }
        }

        viewModel.onSignal(viewLifecycleOwner, SName.coolantFlow) {
            if (it != null) {
                binding.coolantflow.text = it.roundToString(1)
                binding.coolantflowgauge.setGauge(it / 40f)
            } else {
                binding.coolantflow.text = ""
                binding.coolantflowgauge.setGauge(0f)
            }
        }

        viewModel.onSignal(viewLifecycleOwner, SName.frontTorque) {
            if (it != null) {
                var frontTorqueVal = it
                if (viewModel.carState[SName.gearSelected] == SVal.gearReverse) {
                    frontTorqueVal = -(frontTorqueVal)
                }
                binding.fronttorque.text = frontTorqueVal.convertAndRoundToString(Units.TORQUE_NM, 0)
                if (prefs.getPref("frontTorqueMax") < abs(frontTorqueVal)) {
                    prefs.setPref("frontTorqueMax", abs(frontTorqueVal))
                }
                binding.fronttorquegauge.setGauge(frontTorqueVal / prefs.getPref("frontTorqueMax"))
            } else {
                binding.fronttorque.text = ""
                binding.fronttorquegauge.setGauge(0f)
            }
        }

        viewModel.onSignal(viewLifecycleOwner, SName.rearTorque) {
            if (it != null) {
                var rearTorqueVal = it
                if (viewModel.carState[SName.gearSelected] == SVal.gearReverse) {
                    rearTorqueVal = -(rearTorqueVal)
                }
                binding.reartorque.text = rearTorqueVal.convertAndRoundToString(Units.TORQUE_NM, 0)
                if (abs(prefs.getPref("rearTorqueMax")) < rearTorqueVal) {
                    prefs.setPref("rearTorqueMax", abs(rearTorqueVal))
                }
                binding.reartorquegauge.setGauge(rearTorqueVal / prefs.getPref("rearTorqueMax"))
            } else {
                binding.reartorque.text = ""
                binding.reartorquegauge.setGauge(0f)
            }
        }

        viewModel.onSignal(viewLifecycleOwner, SName.battBrickMin) {
            if (it != null) {
                binding.batttemp.text = it.convertAndRoundToString(Units.TEMPERATURE_C, 0)
                binding.batttempgauge.setGauge((it + 40f) / 128)
            } else {
                binding.batttemp.text = ""
                binding.batttempgauge.setGauge(0f)
            }
        }

        viewModel.onSignal(viewLifecycleOwner, SName.autopilotHands) {
            updateAPWarning(it ?: 0f)
        }

        viewModel.onSomeSignals(viewLifecycleOwner, listOf(SName.uiRange, SName.stateOfCharge, SName.chargeStatus)) { processBattery() }

        viewModel.onSomeSignals(viewLifecycleOwner, SGroup.closures) { updateDoorStateUI() }

        viewModel.onSomeSignals(viewLifecycleOwner, listOf(SName.autopilotState, SName.gearSelected, SName.brakeApplied)) {
            updateAutopilotUI()
        }

        viewModel.onSignal(viewLifecycleOwner, SName.steeringAngle) {
            updateAutopilotRotation()
        }

        viewModel.onSomeSignals(viewLifecycleOwner, listOf(SName.accState, SName.accActive, SName.gearSelected, SName.brakeApplied)) {
            updateTaccUI()
        }

        viewModel.onSomeSignals(viewLifecycleOwner, listOf(SName.gearSelected, SName.fusedSpeedLimit, SName.mapRegion)) {
            updateSpeedLimitSign()
        }

        // Basic telltales all have the same logic:
        // If second == third: show first; else: hide first
        setOf(
            Triple(binding.speedoBrakeHold, SName.brakeHold, 1f),
            Triple(binding.leftTurnSignalDark, SName.turnSignalLeft, 1f),
            Triple(binding.leftTurnSignalLight, SName.turnSignalLeft, 2f),
            Triple(binding.rightTurnSignalDark, SName.turnSignalRight, 1f),
            Triple(binding.rightTurnSignalLight, SName.turnSignalRight, 2f),
            Triple(binding.battHeat, SName.heatBattery, 1f),
            Triple(binding.battCharge, SName.chargeStatus, SVal.chargeStatusActive),
            Triple(binding.telltaleLimRegen, SName.limRegen, 1f),
            Triple(binding.telltaleBrakePark, SName.brakePark, SVal.brakeParkRed),
            Triple(binding.telltaleBrakeParkFault, SName.brakePark, SVal.brakeParkAmber),
        ).forEach { triple ->
            viewModel.onSignal(viewLifecycleOwner, triple.second) {
                triple.first.showWhen(triple.second, triple.third)
            }
        }

        // If any telltales need more advanced logic add them below
        // If any should be hidden when in split screen, add to splitScreenHiddenTelltales

        viewModel.onSomeSignals(viewLifecycleOwner, SGroup.lights) {
            processLightTellTales()
        }

        viewModel.onSomeSignals(viewLifecycleOwner, listOf(SName.gearSelected, SName.driverUnbuckled, SName.passengerUnbuckled)) {
            binding.telltaleSeatbelt.visible = gearState() != SVal.gearInvalid &&
                    ((it[SName.driverUnbuckled] == 1f) or
                            (it[SName.passengerUnbuckled] == 1f))
        }

        /*viewModel.onSomeSignals(viewLifecycleOwner, listOf(SName.odometer, SName.uiSpeedUnits)) {
            val odometerVal = it[SName.odometer]
            if (!prefs.getBooleanPref(Constants.hideOdometer) && odometerVal != null) {
                binding.odometer.visible = true
                binding.odometer.text = odometerVal.convertAndRoundToString(
                    Units.DISTANCE_KM,
                    1
                ) + unitConverter.prefDistanceUnit().tag
            } else {
                binding.odometer.visible = false
            }
        }*/

        viewModel.onSomeSignals(viewLifecycleOwner, listOf(SName.odometer, SName.gearSelected)) {
            efficiencyCalculator.updateKwhHistory()
        }

        // Power is always changing, it's enough to only observe this for rapid updates to the efficiency view
        viewModel.onSignal(viewLifecycleOwner, SName.power) {
            val efficiencyText = efficiencyCalculator.getEfficiencyText()
            if (efficiencyText == null || gearState() in setOf(SVal.gearInvalid, SVal.gearPark) || prefs.getBooleanPref(Constants.hideEfficiency)) {
                binding.efficiency.visible = false
            } else {
                binding.efficiency.text = efficiencyText
                binding.efficiency.visible = true
            }
        }

        viewModel.onSignal(viewLifecycleOwner, SName.blindSpotLeft) {
            // Don't show BS warning if in AP
            if ((viewModel.carState[SName.autopilotState] ?: 0f) !in 3f..7f || it == 0f) {
                updateBSWarning(it, binding.BSWarningLeft, Orientation.LEFT_RIGHT)
            }
        }

        viewModel.onSignal(viewLifecycleOwner, SName.blindSpotRight) {
            // Don't show BS warning if in AP
            if ((viewModel.carState[SName.autopilotState] ?: 0f) !in 3f..7f || it == 0f) {
                updateBSWarning(it, binding.BSWarningRight, Orientation.RIGHT_LEFT)
            }
        }

        viewModel.onSignal(viewLifecycleOwner, SName.forwardCollisionWarning) {
            updateFCWWarning(it)
        }

        viewModel.onSignal(viewLifecycleOwner, SName.leftVehicle) {
            val distance = it ?: 99999f
            val l1Distance = viewModel.carState[SName.l1Distance] ?: Constants.l1DistanceLowSpeed
            val l2Distance = viewModel.carState[SName.l2Distance] ?: Constants.l2DistanceLowSpeed
            if (gearState() in setOf(SVal.gearPark, SVal.gearInvalid) || prefs.getBooleanPref(Constants.hideBs)) {
                binding.blindSpotLeft1.visible = false
                binding.blindSpotLeft2.visible = false
            } else {
                binding.blindSpotLeft1.visible = (distance in l2Distance..l1Distance)
                binding.blindSpotLeft2.visible = (distance < l2Distance)
            }
        }

        viewModel.onSignal(viewLifecycleOwner, SName.rightVehicle) {
            val distance = it ?: 99999f
            val l1Distance = viewModel.carState[SName.l1Distance] ?: Constants.l1DistanceLowSpeed
            val l2Distance = viewModel.carState[SName.l2Distance] ?: Constants.l2DistanceLowSpeed
            if (gearState() in setOf(SVal.gearPark, SVal.gearInvalid) || prefs.getBooleanPref(Constants.hideBs)) {
                binding.blindSpotRight1.visible = false
                binding.blindSpotRight2.visible = false
            } else {
                binding.blindSpotRight1.visible = (distance in l2Distance..l1Distance)
                binding.blindSpotRight2.visible = (distance < l2Distance)
            }
        }

        viewModel.onSomeSignals(viewLifecycleOwner, listOf(SName.PINpassed, SName.brakeApplied)) {
            if (it[SName.PINenabled] == 1f) {
                if (it[SName.PINpassed] == 0f &&
                    !binding.PINWarning.visible &&
                    it[SName.brakeApplied] == 2f) {
                    binding.PINWarning.clearAnimation()
                    binding.PINWarning.startAnimation(fadeIn())
                    binding.PINWarning.visible = true
                } else if(it[SName.PINpassed] == 1f) {
                    binding.PINWarning.clearAnimation()
                    if (binding.PINWarning.visible) {
                        binding.PINWarning.startAnimation(fadeOut())
                        binding.PINWarning.visible = false
                    }
                }
            }
        }

        viewModel.onSignal(viewLifecycleOwner, SName.chargeStatus) {
            minMaxPowerViews().forEach {
                it.visible =
                    (prefs.getPref(Constants.gaugeMode) > Constants.showSimpleGauges && !carIsCharging())
            }
            // All of the view visibility is handled here
            setGaugeVisibility()
        }

        // Holy gauges Batman, we're done!
    }

    private fun processBattery() {
        val socVal = viewModel.carState[SName.stateOfCharge]
        if (prefs.getBooleanPref(Constants.showBattRange)) {
            val range = viewModel.carState[SName.uiRange]
            binding.batterypercent.text = if (range != null) range.convertAndRoundToString(
                Units.DISTANCE_MI,
                0
            ) + unitConverter.prefDistanceUnit().tag else ""
        } else {
            binding.batterypercent.text = if (socVal != null) socVal.roundToString(0) + " %" else ""
        }
        binding.batteryOverlay.setGauge(socVal ?: 0f)
        binding.batteryOverlay.setChargeMode(carIsCharging())
        binding.battery.visible = (socVal != null)

        // Set charge meter stuff too, although they may be hidden
        if (socVal != null) {
            binding.chargemeter.setGauge(socVal / 100f, 4f, carIsCharging())
            binding.bigsoc.text = socVal.roundToString(0)
        }
    }

    /**
     * This contains all the show/hide logic for various conflicting views.
     * Call this when changing gauge mode, charging, doors, or split screen.
     */
    private fun setGaugeVisibility() {
        // subtract AWD only views from side views before showing them
        val sideUIViews = sideUIViews().toMutableSet()
        if (viewModel.carState[SName.driveConfig] == SVal.rwd) {
            sideUIViews -= awdOnlyViews()
            awdOnlyViews().forEach { it.visible = false }
        }
        // hide performance gauges if user has elected to hide them or if split screen mode
        sideUIViews.forEach {
            it.visible =
                (prefs.getPref(Constants.gaugeMode) == Constants.showFullGauges && !isSplitScreen())
        }

        drivingViews().forEach { it.visible = !carIsCharging() }
        chargingViews().forEach { it.visible = carIsCharging() }

        // Hide some gauges when doors are open
        if (anyDoorOpen()) {
            when {
                isSplitScreen() -> centerDoorHiddenViews()
                driverOrientRHD() -> rightSideUIViews()
                else -> leftSideUIViews()
            }.forEach { it.visible = false }
        }
        // Always hide unused doors in case of split screen change
        (if (isSplitScreen()) doorViews() else doorViewsCenter()).forEach {
            it.visible = false
        }

        // Battery percent is always hidden in split screen
        binding.batterypercent.visible = !isSplitScreen()
    }

    private fun driverOrientRHD(): Boolean {
        return (prefs.getBooleanPref(Constants.forceRHD) || prefs.getBooleanPref(Constants.detectedRHD))
    }

    private fun setLayoutOrder() {
        // Mirror some layout elements if RHD market vehicle or user forced RHD
        val reverse = driverOrientRHD()
        val topBar = listOf(
            binding.PRND,
            binding.autopilot,
            binding.TACC,
            null,
            binding.battHeat,
            binding.battCharge,
            binding.batterypercent,
            binding.battery
        )
        val doors = listOf(binding.modely, null)
        setHorizontalConstraints(topBar, reverse)
        setHorizontalConstraints(doors, reverse)

        // In case it's switched while doors are open
        updateDoorStateUI()
    }

    /**
     * This sets the constraints of a list of views to order them from left to right.
     *
     * A null may be used once to create a break between left-aligned and right-aligned items.
     * Starting the list with a null will make all the items right-aligned. No null will make
     * all items left aligned. More than 1 null is not supported.
     *
     * @param views The list of views (with optional null) to order horizontally
     */
    private fun setHorizontalConstraints(views: List<View?>, reverse: Boolean = false) {
        var afterBreak = false
        val viewsList = if (reverse) {views.reversed()} else {views}
        for (i in viewsList.indices) {
            val view = viewsList[i]
            if (view == null) {
                afterBreak = true
                continue
            }
            // First clear existing start and end constraints
            view.layoutParams = (view.layoutParams as ConstraintLayout.LayoutParams).apply {
                startToEnd = ConstraintLayout.LayoutParams.UNSET
                startToStart = ConstraintLayout.LayoutParams.UNSET
                endToStart = ConstraintLayout.LayoutParams.UNSET
                endToEnd = ConstraintLayout.LayoutParams.UNSET
            }
            if (i == 0) {
                // Set the start constraint of the first view to the parent layout
                view.layoutParams = (view.layoutParams as ConstraintLayout.LayoutParams).apply {
                    startToStart = ConstraintLayout.LayoutParams.PARENT_ID
                }
            } else if (i == viewsList.size - 1 && afterBreak) {
                // Set the end constraint of the last view to the parent layout
                view.layoutParams = (view.layoutParams as ConstraintLayout.LayoutParams).apply {
                    endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
                }
            } else if (afterBreak) {
                // Set the end constraint of the current view to the start constraint of the next view
                view.layoutParams = (view.layoutParams as ConstraintLayout.LayoutParams).apply {
                    endToStart = viewsList[i + 1]!!.id
                }
            } else {
                // Set the start constraint of the current view to the end constraint of the previous view
                view.layoutParams = (view.layoutParams as ConstraintLayout.LayoutParams).apply {
                    startToEnd = viewsList[i - 1]!!.id
                }
            }
        }
    }

    /**
     * Call this changing split screen, it shows/hides each of `splitScreenHiddenTelltales` by
     * changing the View's alpha, so it doesn't conflict with visibility changes from signal logic.
     */
    private fun updateSplitScreenTellTales() {
        // To keep from conflicting with the different visibilities of TellTales, especially as some
        // use INVISIBLE and some use GONE, for split screen we make them transparent instead of
        // changing the visibility.
        if (isSplitScreen()) {
            splitScreenHiddenTelltales().forEach { it.alpha = 0f }
        } else {
            splitScreenHiddenTelltales().forEach { it.alpha = 1f }
        }
    }

    private fun processLightTellTales() {
        binding.telltaleFogFront.showWhen(SName.frontFogStatus, 1f)
        binding.telltaleFogRear.showWhen(SName.rearFogStatus, 1f)

        // If courtesy lights are on and light switch is not on, hide the DRL telltale to match the center display
        if (viewModel.carState[SName.courtesyLightRequest] == 1f && viewModel.carState[SName.lightSwitch] in setOf(
                SVal.lightSwitchAuto,
                SVal.lightSwitchOff
            )
        ) {
            binding.telltaleDrl.visible = false
        } else {
            if (viewModel.carState[SName.mapRegion] == SVal.mapUS) {
                binding.telltaleDrl.showWhen(SName.lightingState, SVal.lightsPos)
            } else if (viewModel.carState[SName.mapRegion] in arrayOf(SVal.mapEU, SVal.mapTW)) {
                binding.telltaleDrl.showWhen(
                    SName.lightingState,
                    arrayOf(SVal.lightsPos, SVal.lightsOn)
                )
            }
            // add other regions if you dare :)
        }

        updateLowBeam()
        updateHighBeam()
    }

    private fun updateLowBeam() {
        // Low beam only shows when high beam is off
        binding.telltaleLb.visible = (viewModel.carState[SName.lightingState] == SVal.lightsOn && viewModel.carState[SName.highBeamStatus] == 0f)
    }

    private fun updateHighBeam() {
        if (viewModel.carState[SName.highBeamStatus] == 1f) {
            if (viewModel.carState[SName.highBeamRequest] == SVal.highBeamAuto) {
                binding.telltaleHb.setImageResource(R.drawable.ic_telltale_ahb_active)
            } else {
                binding.telltaleHb.setImageResource(R.drawable.ic_telltale_hb)
            }
            binding.telltaleHb.visible = true
        } else {
            if (viewModel.carState[SName.autoHighBeamEnabled] == 1f && viewModel.carState[SName.highBeamRequest] == SVal.highBeamAuto) {
                binding.telltaleHb.setImageResource(R.drawable.ic_telltale_ahb_stdby)
                binding.telltaleHb.visible = true
            } else {
                binding.telltaleHb.visible = false
            }
        }
    }

    /**
     * Provides the current gearSelected value with a default of gearInvalid
     */
    private fun gearState(): Float {
        return viewModel.carState[SName.gearSelected] ?: SVal.gearInvalid
    }
    
    private fun updateGearView() {
        val apState = viewModel.carState[SName.autopilotState] ?: 0f
        val gearColorSelected = when {
            apState in 3f..7f -> requireContext().getColor(R.color.autopilot_blue)
            shouldUseDarkMode() -> Color.LTGRAY
            else -> Color.DKGRAY
        }
        val gearLetterIndex = when (gearState()) {
            SVal.gearPark -> 0
            SVal.gearReverse -> 3
            SVal.gearNeutral -> 6
            SVal.gearDrive -> 9
            else -> null
        }

        val ss = SpannableString(binding.PRND.text.toString())
        if (gearLetterIndex != null) {
            ss.setSpan(
                ForegroundColorSpan(gearColorSelected),
                gearLetterIndex,
                gearLetterIndex + 1,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
        binding.PRND.text = (ss)

        binding.PRND.visible = (gearState() != SVal.gearInvalid)
    }

    private fun updateBlackout() {
        if (viewModel.carState[SName.displayOn] == null) {
            // Don't change the visibility if we lost the signal, maintain the last state
            return
        }
        if (prefs.getBooleanPref(Constants.blankDisplaySync)
            && gearState() in setOf(SVal.gearPark, SVal.gearInvalid)
            && viewModel.carState[SName.displayOn] == 0f
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
    
    private fun shouldUseDarkMode(): Boolean {
        // Save/use the last known value to prevent a light/dark flash upon launching
        val sunUp = viewModel.carState[SName.isSunUp]
        if (sunUp != null) {
            prefs.setPref(Constants.lastSunUp, sunUp)
        }
        return (prefs.getPref(Constants.lastSunUp) == 0f || prefs.getBooleanPref(Constants.forceNightMode))
    }

    private fun setColors() {
        val window: Window? = activity?.window
        val circleGauges = setOf(
            binding.fronttorquegauge,
            binding.reartorquegauge,
            binding.batttempgauge,
            binding.fronttempgauge,
            binding.reartempgauge,
            binding.frontbraketempgauge,
            binding.rearbraketempgauge,
            binding.coolantflowgauge,
            binding.chargemeter
        )
        val textViewsPrimary = setOf(
            binding.speed,
            binding.bigsoc,
            binding.bigsocpercent,
            binding.power,
            binding.efficiency,

            binding.minpower,
            binding.maxpower,
            binding.fronttorque,
            binding.fronttorquelabel,
            binding.fronttorqueunits,

            binding.reartorque,
            binding.reartorquelabel,
            binding.reartorqueunits,
            binding.batttemp,
            binding.batttemplabel,
            binding.batttempunits,
            binding.fronttemp,
            binding.fronttemplabel,
            binding.fronttempunits,
            binding.frontbraketemp,
            binding.frontbraketemplabel,
            binding.frontbraketempunits,
            binding.rearbraketemp,
            binding.rearbraketemplabel,
            binding.rearbraketempunits,
            binding.reartemp,
            binding.reartemplabel,
            binding.reartempunits,
            binding.coolantflow,
            binding.coolantflowlabel,
            binding.coolantflowunits,
            binding.chargerate,
            binding.batterypercent,
            )
        val textViewsSecondary = setOf(
            binding.unit
        )
        val textViewsDisabled = setOf(
            binding.odometer,
            binding.PRND
        )
        val imageViewsSecondary = setOf(
            binding.modely,
            binding.modelyCenter
        )

        // Not using dark-mode for compatibility with older version of Android (pre-29)
        if (shouldUseDarkMode()) {
            window?.statusBarColor = Color.BLACK
            binding.root.setBackgroundColor(Color.BLACK)
            textViewsPrimary.forEach { it.setTextColor(Color.WHITE) }
            textViewsSecondary.forEach { it.setTextColor(Color.LTGRAY) }
            textViewsDisabled.forEach { it.setTextColor(Color.DKGRAY) }
            imageViewsSecondary.forEach { it.setColorFilter(Color.LTGRAY) }
            circleGauges.forEach { it.setDayValue(0) }
            binding.powerBar.setDayValue(0)
            binding.battery.setColorFilter(Color.DKGRAY)
            binding.batteryOverlay.setDayValue(0)
        } else {
            window?.statusBarColor = Color.parseColor("#FFEEEEEE")
            binding.root.setBackgroundColor(requireContext().getColor(R.color.day_background))
            textViewsPrimary.forEach { it.setTextColor(Color.BLACK) }
            textViewsSecondary.forEach { it.setTextColor(Color.GRAY) }
            textViewsDisabled.forEach { it.setTextColor(Color.LTGRAY) }
            imageViewsSecondary.forEach { it.setColorFilter(Color.DKGRAY) }
            circleGauges.forEach { it.setDayValue(1) }
            binding.powerBar.setDayValue(1)
            binding.battery.setColorFilter(Color.parseColor("#FFAAAAAA"))
            binding.batteryOverlay.setDayValue(1)
        }
        updateGearView()
    }

    private fun formatPower(power: Float): String {
        return power.convertAndRoundToString(Units.POWER_W) + unitConverter.prefPowerUnit().tag
    }

    private fun carIsCharging(): Boolean {
        val chargeStatus = viewModel.carState[SName.chargeStatus] ?: SVal.chargeStatusInactive
        return chargeStatus != SVal.chargeStatusInactive
    }

    private fun updateDoorStateUI() {
        val carBody = if (isSplitScreen()) binding.modelyCenter else binding.modely
        carBody.visible = anyDoorOpen()
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
        val sigToBinding = if (!isSplitScreen()) mapOf(
            SName.liftgateState to binding.hatch,
            SName.frunkState to binding.hood,
            SName.frontLeftDoorState to binding.frontleftdoor,
            SName.frontRightDoorState to binding.frontrightdoor,
            SName.rearLeftDoorState to binding.rearleftdoor,
            SName.rearRightDoorState to binding.rearrightdoor
        ) else mapOf(
            SName.liftgateState to binding.hatchCenter,
            SName.frunkState to binding.hoodCenter,
            SName.frontLeftDoorState to binding.frontleftdoorCenter,
            SName.frontRightDoorState to binding.frontrightdoorCenter,
            SName.rearLeftDoorState to binding.rearleftdoorCenter,
            SName.rearRightDoorState to binding.rearrightdoorCenter
        )
        sigToBinding.forEach {
            it.value.visible = closureIsOpen(it.key)
        }
    }

    private fun updateAutopilotUI() {
        val brakeApplied = (viewModel.carState[SName.brakeApplied] == 2f)
        val inDrive = (viewModel.carState[SName.gearSelected] == SVal.gearDrive)
        val autopilotState = if (brakeApplied || !inDrive) 0f else viewModel.carState[SName.autopilotState] ?: 0f

        when (autopilotState) {
            in 3f..7f -> binding.autopilot.setImageResource(R.drawable.ic_autopilot)
            else -> binding.autopilot.setImageResource(R.drawable.ic_autopilot_inactive)
        }

        when {
            autopilotState in 2f..7f && !binding.autopilot.visible -> {
                binding.autopilot.startAnimation(fadeIn(200))
                binding.autopilot.visible = true
            }
            autopilotState !in 2f..7f && binding.autopilot.visible -> {
                binding.autopilot.startAnimation(fadeOut(200))
                binding.autopilot.visible = false
            }
        }
    }

    private fun updateAutopilotRotation() {
        // set pivot to center of image
        binding.autopilot.pivotX = (binding.autopilot.width / 2f)
        binding.autopilot.pivotY = (binding.autopilot.height / 2f)
        val steeringAngle = if ((viewModel.carState[SName.autopilotState] ?: 0f) in 3f..7f)
            (viewModel.carState[SName.steeringAngle] ?: 0f) else 0f
        binding.autopilot.rotation = steeringAngle
    }

    private fun updateTaccUI(){
        if (viewModel.carState[SName.accActive] == 1f) {
            binding.TACC.setImageResource(R.drawable.ic_tacc)
        } else {
            binding.TACC.setImageResource(R.drawable.ic_tacc_inactive)
        }

        val brakeApplied = (viewModel.carState[SName.brakeApplied] == 2f)
        val inDrive = (viewModel.carState[SName.gearSelected] == SVal.gearDrive)

        val taccAvailable = (viewModel.carState[SName.accState] == 4f && !brakeApplied && inDrive)
        if (binding.TACC.visible != taccAvailable) {
            when (taccAvailable) {
                true -> {
                    binding.TACC.startAnimation(fadeIn(200))
                    binding.TACC.visible = true
                }
                false -> {
                    binding.TACC.startAnimation(fadeOut(200))
                    binding.TACC.visible = false
                }
            }
        }
    }

    private fun updateSpeedLimitSign() {
        val speedLimitVal = viewModel.carState[SName.fusedSpeedLimit] ?: SVal.fusedSpeedNone
        val map = viewModel.carState[SName.mapRegion]
        if (speedLimitVal == SVal.fusedSpeedNone || map == null || gearState() != SVal.gearDrive
            || prefs.getBooleanPref(Constants.hideSpeedLimit) || isSplitScreen()
        ) {
            binding.speedLimitUs.visible = false
            binding.speedLimitRound.visible = false
            binding.speedLimitNolimitRound.visible = false
        } else {
            val usMap = (map == SVal.mapUS)
            binding.speedLimitValueUs.text = speedLimitVal.roundToString(0)
            binding.speedLimitValueRound.text = speedLimitVal.roundToString(0)

            binding.speedLimitUs.visible = usMap
            binding.speedLimitRound.visible = (!usMap && speedLimitVal != 155f)
            binding.speedLimitNolimitRound.visible = (!usMap && speedLimitVal == 155f)
        }
    }

    private fun setupGradientOverlays() {
        // init gradient
        gradientColorFrom = requireContext().getColor(R.color.transparent_blank)
        overlayGradient = GradientDrawable().apply {
            colors = intArrayOf(
                gradientColorFrom,
                gradientColorFrom,
            )
            orientation = Orientation.TOP_BOTTOM
            gradientType = GradientDrawable.LINEAR_GRADIENT
        }
        binding.warningGradientOverlay.setImageDrawable(overlayGradient)
        binding.warningGradientOverlay.visible = false

        // init ap animation
        autopilotAnimation = ValueAnimator.ofObject(
            ArgbEvaluator(),
            gradientColorFrom,
            requireContext().getColor(R.color.autopilot_blue)
        )
        // autopilotAnimation is repeated in .doOnEnd
        // set repeatCount to 1 so that it reverses before ending
        autopilotAnimation.repeatCount = 1
        autopilotAnimation.repeatMode = ValueAnimator.REVERSE

        // init bs animation
        blindspotAnimation = ValueAnimator.ofObject(
            ArgbEvaluator(),
            gradientColorFrom,
            requireContext().getColor(R.color.very_red)
        )
        blindspotAnimation.addUpdateListener { animator ->
            overlayGradient.colors =
                intArrayOf(animator.animatedValue as Int, gradientColorFrom)
        }
        blindspotAnimation.doOnEnd {
            binding.warningGradientOverlay.visible = false
            overlayGradient.colors = intArrayOf(gradientColorFrom, gradientColorFrom)
        }
    }

    private fun updateAPWarning(autopilotHandsVal: Float) {
        if ((autopilotHandsVal > 2f) and (autopilotHandsVal < 15f)) {
            // 3 and 4 have a ~2 second delay before starting to flash
            autopilotAnimation.startDelay = if (autopilotHandsVal in 3f..4f) 1900L else 0L

            if (!binding.APWarning.visible) {
                // Warning toast:
                binding.APWarning.visible = true

                // Gradient overlay:
                overlayGradient.orientation = Orientation.TOP_BOTTOM
                autopilotAnimation.addUpdateListener { animator ->
                    overlayGradient.colors =
                        intArrayOf(animator.animatedValue as Int, gradientColorFrom)
                }
                autopilotAnimation.doOnEnd { anim ->
                    // Duration is from low to high, a full cycle is duration * 2
                    anim.duration = max(250L, (anim.duration * 0.9).toLong())
                    anim.startDelay = 0L
                    anim.start()
                }
                autopilotAnimation.duration = 750L
                autopilotAnimation.start()
                binding.warningGradientOverlay.visible = true
            }
        } else {
            if (binding.APWarning.visible) {
                // Warning toast:
                binding.APWarning.visible = false

                // Gradient overlay:
                binding.warningGradientOverlay.visible = false
                autopilotAnimation.removeAllListeners()
                autopilotAnimation.cancel()
                overlayGradient.colors = intArrayOf(gradientColorFrom, gradientColorFrom)
            }
        }
    }

    private fun updateBSWarning(bsValue: Float?, bsBinding: View, orientation: Orientation) {
        when (bsValue) {
            1f -> blindspotAnimation.duration = 300
            2f -> blindspotAnimation.duration = 150
        }
        if (bsValue in setOf(1f, 2f)) {
            // Warning toast:
            bsBinding.clearAnimation()
            bsBinding.visible = true

            // Gradient overlay:
            overlayGradient.orientation = orientation
            blindspotAnimation.repeatCount = ValueAnimator.INFINITE
            blindspotAnimation.repeatMode = ValueAnimator.REVERSE
            blindspotAnimation.start()
            binding.warningGradientOverlay.visible = true
        } else {
            // Warning toast:
            if (bsBinding.visible) {
                bsBinding.startAnimation(fadeOut())
                bsBinding.visible = false

                // Gradient overlay:
                // let it fade out naturally by setting repeat to 1 (so it reverses) then change visibility on end
                blindspotAnimation.repeatCount = 1
            }
        }
    }

    private fun updateFCWWarning(fcwVal: Float?) {
        if (fcwVal == 1f) {
            overlayGradient.orientation = Orientation.TOP_BOTTOM
            blindspotAnimation.duration = 200

            // Warning toast:
            binding.FCWarning.clearAnimation()
            binding.FCWarning.visible = true

            // Gradient overlay:
            // Reuse blindspot animation as it's basically the same
            blindspotAnimation.repeatCount = 4
            blindspotAnimation.repeatMode = ValueAnimator.RESTART
            blindspotAnimation.reverse()
            binding.warningGradientOverlay.visible = true
        } else {
            // Warning toast:
            if (binding.FCWarning.visible) {
                binding.FCWarning.startAnimation(fadeOut())
                binding.FCWarning.visible = false
            }
            // Gradient overlay stops by itself after a fixed repeat count
        }
    }

    private fun fadeIn(duration: Long = 500L): Animation {
        val fadeIn = AnimationUtils.loadAnimation(activity, R.anim.fade_in)
        fadeIn.duration = duration
        return fadeIn
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