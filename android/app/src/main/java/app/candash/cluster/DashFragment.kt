package app.candash.cluster

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.content.Context
import android.content.SharedPreferences
import android.content.res.Resources
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.util.DisplayMetrics
import android.view.*
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.animation.doOnEnd
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import app.candash.cluster.databinding.FragmentDashBinding
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.pow

class DashFragment : Fragment() {
    private lateinit var binding: FragmentDashBinding

    private lateinit var viewModel: DashViewModel


    private var gearColor: Int = Color.parseColor("#FFEEEEEE")
    private var gearColorSelected: Int = Color.DKGRAY
    private var lastAutopilotState: Int = 0
    private var autopilotHandsToggle: Boolean = false
    private var bsWarningToggle: Boolean = false
    private var fcwToggle: Boolean = false
    private var blackoutToastToggle: Boolean = false
    private var showSOC: Boolean = true
    private var uiSpeedUnitsMPH: Boolean = true
    private var power: Float = 0f
    private var battAmps: Float = 0f
    private var battAmpsHistory: MutableList<Float> = mutableListOf<Float>()
    private var battVolts: Float = 0f
    private var doorOpen = false
    private var l2Distance: Int = 200
    private var l1Distance: Int = 300
    private var gearState: Int = Constants.gearInvalid
    private var mapRegion: Float = 0f
    private lateinit var prefs: SharedPreferences


    private var savedLayoutParams: MutableMap<View, ConstraintLayout.LayoutParams> = mutableMapOf()
    val Int.dp: Int
        get() = (this / Resources.getSystem().displayMetrics.density).toInt()
    val Int.px: Int
        get() = (this * Resources.getSystem().displayMetrics.density).toInt()

    val Float.dp: Float
        get() = (this / Resources.getSystem().displayMetrics.density)
    val Float.px: Float
        get() = (this * Resources.getSystem().displayMetrics.density)
    // Distance conversions
    val Float.miToKm: Float
        get() = (this / .621371).toFloat()
    val Float.kmToMi: Float
        get() = (this * .621371).toFloat()
    // Temperature conversions
    val Float.cToF: Float
        get() = ((this * 9/5) + 32)
    val Int.cToF: Int
        get() = ((this * 9/5) + 32)
    // Power conversions
    val Float.wToHp: Float
        get() = (this  / 745.7f)
    val Float.wToPs: Float
        get() = (this / 735.5f)
    // Torque conversions
    val Int.nmToLbfFt: Int
        get() = (this * 0.73756).toInt()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentDashBinding.inflate(inflater, container, false)
        prefs = requireContext().getSharedPreferences("dash", Context.MODE_PRIVATE)
        return binding.root
    }

    private fun topUIViews(): List<View> =
        listOf(
            binding.PRND,
            binding.batterypercent,
            binding.deadbattery,
            binding.fullbattery,
            binding.leftTurnSignal,
            binding.rightTurnSignal,
            binding.leftTurnSignalLight,
            binding.leftTurnSignalDark,
            binding.rightTurnSignalLight,
            binding.rightTurnSignalDark,
            binding.autopilot,
            binding.autopilotInactive,
        )
    private fun telltaleUIViews(): List<View> =
        listOf(
            binding.telltaleDrl,
            binding.telltaleLb,
            binding.telltaleHb,
            binding.telltaleAhbStdby,
            binding.telltaleAhbActive,
            binding.telltaleFogFront,
            binding.telltaleFogRear,
            binding.odometer,
            binding.battCharge
        )
    private fun sideUIViews(): List<View> =
        listOf(
            binding.fronttorque,
            binding.fronttorquelabel,
            binding.fronttorqueunits,
            binding.fronttorquegauge,
            binding.reartorque,
            binding.reartorquelabel,
            binding.reartorqueunits,
            binding.reartorquegauge,
            binding.batttemp,
            binding.batttemplabel,
            binding.batttempunits,
            binding.batttempgauge,
            binding.fronttemp,
            binding.fronttemplabel,
            binding.fronttempunits,
            binding.fronttempgauge,
            binding.reartemp,
            binding.reartemplabel,
            binding.reartempunits,
            binding.reartempgauge,
            binding.coolantflow,
            binding.coolantflowlabel,
            binding.coolantflowunits,
            binding.coolantflowgauge,
            binding.frontbraketemp,
            binding.frontbraketemplabel,
            binding.frontbraketempunits,
            binding.frontbraketempgauge,
            binding.rearbraketemp,
            binding.rearbraketemplabel,
            binding.rearbraketempunits,
            binding.rearbraketempgauge,
        )

    private fun leftSideUIViews(): List<View> =
        listOf(
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

    private fun rightSideUIViews(): List<View> =
        listOf(
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


    private fun chargingHiddenViews(): List<View> =
        listOf(
            binding.powerBar,
            binding.power,
            binding.speed,
            binding.unit,
        )

    private fun chargingViews(): List<View> =
        listOf(
            binding.bigsoc,
            binding.bigsocpercent,
            binding.chargerate
        )

    private fun doorViews(): List<View> =
        listOf(
            binding.modely,
            binding.frontleftdoor,
            binding.frontrightdoor,
            binding.rearleftdoor,
            binding.rearrightdoor,
            binding.hood,
            binding.hatch
        )

    private fun doorViewsCenter(): List<View> =
        listOf(
            binding.modelyCenter,
            binding.frontleftdoorCenter,
            binding.frontrightdoorCenter,
            binding.rearleftdoorCenter,
            binding.rearrightdoorCenter,
            binding.hood,
            binding.hatch
        )

    private fun minMaxchargingHiddenViews(): List<View> =
        listOf(
            binding.maxpower,
            binding.minpower,
        )

    fun getBackgroundColor(sunUpVal: Int): Int {
        return when (sunUpVal) {
            1 -> requireContext().getColor(R.color.day_background)
            else -> requireContext().getColor(R.color.night_background)
        }
    }
/*
    private fun prefs.setPref(name: String, value: Float) {
        with(prefs.edit()) {
            putFloat(name, value)
            apply()
        }
    }

    private fun prefs.setBooleanPref(name: String, value: Boolean) {
        with(prefs.edit()) {
            putBoolean(name, value)
            apply()
        }
    }

    private fun prefs.prefContains(name: String): Boolean {
        return prefs.contains(name)
    }

    private fun prefs.getPref(name: String): Float {
        return prefs.getFloat(name, 0f)
    }

    private fun prefs.getBooleanPref(name: String): Boolean {
        return prefs.getBoolean(name, false)
    }
*/
    private fun getScreenWidth(): Int {
        val displayMetrics = DisplayMetrics()
        activity?.windowManager
            ?.defaultDisplay?.getMetrics(displayMetrics)
        return displayMetrics.widthPixels
    }

    private fun getRealScreenWidth(): Int {
        var displayMetrics = DisplayMetrics()
        activity?.windowManager
            ?.defaultDisplay?.getRealMetrics(displayMetrics)
        return displayMetrics.widthPixels
    }

    private fun isSplitScreen(): Boolean {

        return getRealScreenWidth() > getScreenWidth() * 2
    }

    private fun isSunUp(viewModel: DashViewModel): Int {
        if (viewModel.getValue(Constants.isSunUp) != null) {
            return viewModel.getValue(Constants.isSunUp)!!.toInt()
        } else {
            return 0
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        prefs = requireContext().getSharedPreferences("dash", Context.MODE_PRIVATE)

        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(requireActivity()).get(DashViewModel::class.java)
        if (!prefs.prefContains(Constants.gaugeMode)) {
            prefs.setPref(Constants.gaugeMode, Constants.showFullGauges)
        }
        if (prefs.getBooleanPref(Constants.forceNightMode)) {
            // Go ahead and load dark mode instead of waiting on a signal
            setColors(0)
        }
        uiSpeedUnitsMPH = prefs.getBooleanPref("uiSpeedUnitsMPH")

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

        // Power unit labels handled in formatWatts()

        val colorFrom = requireContext().getColor(R.color.transparent_blank)
        val colorTo = requireContext().getColor(R.color.autopilot_blue)
        val bsColorTo = requireContext().getColor(R.color.very_red)
        val autopilotAnimation = ValueAnimator.ofObject(ArgbEvaluator(), colorFrom, colorTo)
        val blindspotAnimation = ValueAnimator.ofObject(ArgbEvaluator(), colorFrom, bsColorTo)
        val overlayGradient = GradientDrawable().apply {
            colors = intArrayOf(
                colorFrom,
                colorFrom,
            )
            orientation = GradientDrawable.Orientation.TOP_BOTTOM
            gradientType = GradientDrawable.LINEAR_GRADIENT
        }
        binding.warningGradientOverlay.setImageDrawable(overlayGradient)
        binding.warningGradientOverlay.visibility = View.GONE

        binding.blackout.visibility = View.GONE

        // milliseconds
        if (!isSplitScreen()) {
            for (topUIView in topUIViews()) {
                savedLayoutParams[topUIView] =
                    ConstraintLayout.LayoutParams(topUIView.layoutParams as ConstraintLayout.LayoutParams)
            }
        } else {
            for (telltaleUIView in telltaleUIViews()){
                telltaleUIView.visibility = View.INVISIBLE
            }
        }


        // set initial speedometer value
        viewModel.getValue(Constants.uiSpeed)?.let { vehicleSpeedVal ->
            // lower height of default Inter font to match tesla
            binding.speed.scaleY = .9f
            binding.speed.text = vehicleSpeedVal.toInt().toString()
        }

        if (viewModel.getValue(Constants.driveConfig) == Constants.rwd) {
            binding.fronttorquegauge.visibility = View.INVISIBLE
            binding.fronttorquelabel.visibility = View.INVISIBLE
            binding.fronttorque.visibility = View.INVISIBLE
            binding.fronttorqueunits.visibility = View.INVISIBLE
            binding.fronttempgauge.visibility = View.INVISIBLE
            binding.fronttemplabel.visibility = View.INVISIBLE
            binding.fronttemp.visibility = View.INVISIBLE
            binding.fronttempunits.visibility = View.INVISIBLE
        }


        viewModel.getValue(Constants.stateOfCharge)?.let {
            binding.batterypercent.text = it.toInt().toString() + " %"
            binding.fullbattery.setGauge(it)
            binding.fullbattery.invalidate()
        } ?: run {
            binding.batterypercent.text = ""
            binding.fullbattery.setGauge(0f)
            binding.fullbattery.invalidate()
        }
        viewModel.getValue(Constants.isSunUp)?.let { sunUpVal ->
            if (!prefs.prefContains("sunUp")) {
                setColors(sunUpVal.toInt())
                prefs.setPref("sunUp", sunUpVal)
            }
            if (prefs.getPref("sunUp") != sunUpVal) {
                setColors(sunUpVal.toInt())
                prefs.setPref("sunUp", sunUpVal)
            }
        }
        binding.minpower.setOnClickListener {
            prefs.setPref("minPower", 0f)
        }
        binding.maxpower.setOnClickListener {
            prefs.setPref("maxPower", 0f)
        }
        binding.root.setOnLongClickListener {
            viewModel.switchToInfoFragment()
            viewModel.switchToInfoFragment()
            return@setOnLongClickListener true
        }
        binding.batterypercent.setOnClickListener {
            showSOC = !showSOC
            return@setOnClickListener
        }
        binding.unit.setOnClickListener {
            if (prefs.getPref(Constants.gaugeMode) < Constants.showFullGauges) {
                prefs.setPref(Constants.gaugeMode, prefs.getPref(Constants.gaugeMode) + 1f)
            } else {
                prefs.setPref(Constants.gaugeMode, Constants.showSimpleGauges)
            }
        }

        binding.power.setOnClickListener {
            if (prefs.getPref(Constants.powerUnits) < Constants.powerUnitPs) {
                prefs.setPref(Constants.powerUnits, prefs.getPref(Constants.powerUnits) + 1f)
            } else {
                prefs.setPref(Constants.powerUnits, Constants.powerUnitKw)
            }
        }


        binding.speed.setOnLongClickListener {
            prefs.setBooleanPref(Constants.forceNightMode, !prefs.getBooleanPref(Constants.forceNightMode))
            return@setOnLongClickListener true
        }

        viewModel.getSplitScreen().observe(viewLifecycleOwner) {
            val window: Window? = activity?.window

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // only needed for Android 11+
                if (view.windowToken != null) {
                    if (it) {

                        for (topUIView in topUIViews()) {
                            val params = topUIView.layoutParams as ConstraintLayout.LayoutParams
                            val savedParams = savedLayoutParams[topUIView]
                            params.setMargins(
                                savedParams!!.leftMargin,
                                savedParams.topMargin - 30.px,
                                savedParams.rightMargin,
                                savedParams.bottomMargin
                            )
                            topUIView.layoutParams = params
                        }

                        for (sideUIView in sideUIViews()) {
                            sideUIView.visibility = View.GONE
                        }
                    } else {
                        //no splitscreen
                        for (topUIView in topUIViews()) {
                            var params = topUIView.layoutParams as ConstraintLayout.LayoutParams
                            var savedParams = savedLayoutParams[topUIView]
                            params.setMargins(
                                savedParams!!.leftMargin,
                                savedParams.topMargin,
                                savedParams.rightMargin,
                                savedParams.bottomMargin
                            )
                            topUIView.layoutParams = params
                        }
                        for (sideUIView in sideUIViews()) {
                            sideUIView.visibility = View.VISIBLE
                        }
                    }
                }
            }
        }
        viewModel.carState().observe(viewLifecycleOwner) { it ->
            // set display night/day mode based on reported car status
            it.getValue(Constants.isSunUp)?.let { sunUpVal ->
                setColors(sunUpVal.toInt())
            }

            it.getValue(Constants.displayOn)?.let { displayOnVal ->
                if (displayOnVal.toFloat() == 0f && prefs.getBooleanPref(Constants.blankDisplaySync)
                    // Never turn off screen if in gear
                    && gearState != Constants.gearDrive && gearState != Constants.gearReverse) {
                    binding.blackout.visibility = View.VISIBLE

                    if (!blackoutToastToggle) {
                        binding.blackoutToast.visibility = View.VISIBLE
                        val fadeOut = AnimationUtils.loadAnimation(activity, R.anim.fade_out)
                        fadeOut.duration = 5000
                        binding.blackoutToast.startAnimation(fadeOut)
                    }
                    blackoutToastToggle = true
                } else {
                    binding.blackout.visibility = View.GONE
                    binding.blackoutToast.visibility = View.GONE
                    blackoutToastToggle = false
                }
            }

            it.getValue(Constants.battAmps)?.let { battAmpsVal ->

                // Simple smoothing of amps using average of last 10 values (100 ms)
                battAmpsHistory.add(battAmpsVal.toFloat())
                while (battAmpsHistory.count() > 10) {
                    battAmpsHistory.removeAt(0)
                }
                battAmps = battAmpsHistory.average().toFloat()
            }
            it.getValue(Constants.battVolts)?.let { battVoltsVal ->
                battVolts = battVoltsVal.toFloat()
            }
            if (it.getValue(Constants.battVolts) != null) {
                //batt amps and batt volts are on the same signal so if amps are there so are volts
                power = (battAmps * battVolts)
                if (power > prefs.getPref("maxPower")) prefs.setPref("maxPower", power)
                if (viewModel.getValue(Constants.chargeStatus)
                        ?.toInt() == Constants.chargeStatusInactive.toInt()
                ) {
                    // do not store minpower if car is charging
                    if (power < prefs.getPref("minPower")) prefs.setPref("minPower", power)
                }
                if (prefs.getPref(Constants.gaugeMode) > Constants.showSimpleGauges) {
                    binding.minpower.visibility = View.VISIBLE
                    binding.maxpower.visibility = View.VISIBLE
                    binding.minpower.text = formatPower(prefs.getPref("minPower"))
                    binding.maxpower.text = formatPower(prefs.getPref("maxPower"))

                } else {
                    binding.minpower.visibility = View.INVISIBLE
                    binding.maxpower.visibility = View.INVISIBLE
                }
                binding.power.text = formatPower(power)


                if (power >= 0) {
                    binding.powerBar.setGauge(((power / prefs.getPref("maxPower")).pow(0.75f)))
                } else {
                    binding.powerBar.setGauge(
                        -((abs(power) / abs(prefs.getPref("minPower"))).pow(
                            0.75f
                        ))
                    )
                }
                binding.powerBar.invalidate()
            } else {
                binding.powerBar.setGauge(0f)
                binding.powerBar.invalidate()
                binding.powerBar.visibility = View.INVISIBLE
                binding.power.text = ""
                binding.minpower.text = ""
                binding.maxpower.text = ""
            }

            viewModel.getValue(Constants.autopilotState)?.let { autopilotStateVal ->
                gearColorSelected = if (autopilotStateVal.toInt() in 3..7) {
                    requireContext().getColor(R.color.autopilot_blue)
                } else if (isSunUp(viewModel) == 1 && !prefs.getBooleanPref(Constants.forceNightMode)) {
                    Color.DKGRAY
                } else {
                    Color.LTGRAY
                }
            }
            it.getValue(Constants.gearSelected)?.let { gearStateVal ->
                val gear: String = binding.PRND.text.toString()
                var ss = SpannableString(gear)
                var gearStartIndex = 0
                var gearEndIndex = 0
                gearState = gearStateVal.toInt()
                if (gearStateVal.toInt() == Constants.gearInvalid) {
                    binding.autopilotInactive.visibility = View.INVISIBLE
                    binding.PRND.visibility = View.INVISIBLE
                    gearStartIndex = 0
                    gearEndIndex = 0

                } else {
                    binding.PRND.visibility = View.VISIBLE
                }
                if (gearStateVal.toInt() == Constants.gearPark) {
                    binding.autopilotInactive.visibility = View.INVISIBLE
                    gearStartIndex = 0
                    gearEndIndex = 1
                }
                if (gearStateVal.toInt() == Constants.gearReverse) {
                    gearStartIndex = 3
                    gearEndIndex = 4
                }
                if (gearStateVal.toInt() == Constants.gearNeutral) {
                    gearStartIndex = 6
                    gearEndIndex = 7
                }
                if (gearStateVal.toInt() == Constants.gearDrive) {
                    gearStartIndex = 9
                    gearEndIndex = 10
                }
                ss.setSpan(
                    ForegroundColorSpan(gearColorSelected),
                    gearStartIndex,
                    gearEndIndex,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                binding.PRND.text = (ss)
            } ?: run {
                binding.PRND.visibility = View.INVISIBLE
                gearState = Constants.gearInvalid
            }
            /*
            it.getValue(Constants.maxSpeedAP)?.let { maxSpeedAPVal ->
                binding.displaymaxspeed.text = maxSpeedAPVal.toInt().toString() + " MPH"
            }

             */
            // hide performance gauges if user has elected to hide them or if splitscreen mode
            if ((prefs.getPref(Constants.gaugeMode) < Constants.showFullGauges) || isSplitScreen()) {
                for (leftSideUIView in leftSideUIViews()) {
                    leftSideUIView.visibility = View.INVISIBLE
                }
                for (rightSideUIView in rightSideUIViews()) {
                    rightSideUIView.visibility = View.INVISIBLE
                }
            } else {
                //do not pop up left side gauges even if enabled if any door is open or if is a
                if (!doorOpen) {
                    for (leftSideUIView in leftSideUIViews()) {
                        leftSideUIView.visibility = View.VISIBLE
                    }
                }

                for (rightSideUIView in rightSideUIViews()) {
                    rightSideUIView.visibility = View.VISIBLE
                }
            }

            if (it.containsKey(Constants.brakeTempFL) && it.containsKey(Constants.brakeTempFR)) {
                val frontBrakeTemp = max(
                    it.getValue(Constants.brakeTempFL)!!.toInt(),
                    it.getValue(Constants.brakeTempFR)!!.toInt()
                )
                if (prefs.getBooleanPref(Constants.tempInF)) {
                    binding.frontbraketemp.text = frontBrakeTemp.cToF.toString()
                } else {
                    binding.frontbraketemp.text = frontBrakeTemp.toString()
                }
                binding.frontbraketempgauge.setGauge(frontBrakeTemp.toFloat() / 984f)
                binding.fronttempgauge.invalidate()
            } else {
                binding.frontbraketemp.text = ""
                binding.frontbraketempgauge.setGauge(0f)
                binding.frontbraketempgauge.invalidate()
            }
            if (it.containsKey(Constants.brakeTempRL) && it.containsKey(Constants.brakeTempRR)) {
                val rearBrakeTemp = max(
                    it.getValue(Constants.brakeTempRL)!!.toInt(),
                    it.getValue(Constants.brakeTempRR)!!.toInt()
                )
                if (prefs.getBooleanPref(Constants.tempInF)) {
                    binding.rearbraketemp.text = rearBrakeTemp.cToF.toString()
                } else {
                    binding.rearbraketemp.text = rearBrakeTemp.toString()
                }
                binding.rearbraketempgauge.setGauge(rearBrakeTemp.toFloat() / 984f)
                binding.reartempgauge.invalidate()
            } else {
                binding.rearbraketemp.text = ""
                binding.rearbraketempgauge.setGauge(0f)
                binding.rearbraketempgauge.invalidate()
            }
            it.getValue(Constants.frontTemp)?.let { frontTempVal ->
                if (prefs.getBooleanPref(Constants.tempInF)) {
                    binding.fronttemp.text = frontTempVal.toFloat().cToF.toInt().toString()
                } else {
                    binding.fronttemp.text = frontTempVal.toInt().toString()
                }
                binding.fronttempgauge.setGauge(frontTempVal.toFloat() / 214f)
                binding.fronttempgauge.invalidate()
            } ?: run {
                binding.fronttemp.text = ""
                binding.fronttempgauge.setGauge(0f)
                binding.fronttempgauge.invalidate()
            }
            it.getValue(Constants.rearTemp)?.let { rearTempVal ->
                if (prefs.getBooleanPref(Constants.tempInF)) {
                    binding.reartemp.text = rearTempVal.toFloat().cToF.toInt().toString()
                } else {
                    binding.reartemp.text = rearTempVal.toInt().toString()
                }
                binding.reartempgauge.setGauge(rearTempVal.toFloat() / 214f)
                binding.reartempgauge.invalidate()
            } ?: run {
                binding.reartemp.text = ""
                binding.reartempgauge.setGauge(0f)
                binding.reartempgauge.invalidate()
            }
            it.getValue(Constants.coolantFlow)?.let {
                binding.coolantflow.text = "%.1f".format(it.toFloat())
                binding.coolantflowgauge.setGauge(it.toFloat() / 40f)
                binding.coolantflowgauge.invalidate()
            } ?: run {
                binding.coolantflow.text = ""
                binding.coolantflowgauge.setGauge(0f)
                binding.coolantflowgauge.invalidate()
            }
            it.getValue(Constants.frontTorque)?.let {
                var frontTorqueVal = it.toFloat()
                if ((viewModel.getValue(Constants.gearSelected)
                        ?.toInt() == Constants.gearReverse)
                ) {
                    frontTorqueVal = -(frontTorqueVal)
                }
                if (prefs.getBooleanPref(Constants.torqueInLbfFt)) {
                    binding.fronttorque.text = frontTorqueVal.toInt().nmToLbfFt.toString()
                } else {
                    binding.fronttorque.text = frontTorqueVal.toInt().toString()
                }
                if (abs(prefs.getPref("frontTorqueMax")) < frontTorqueVal.toFloat()) {
                    prefs.setPref("frontTorqueMax", abs(frontTorqueVal.toFloat()))
                }
                binding.fronttorquegauge.setGauge(frontTorqueVal.toFloat() / prefs.getPref("frontTorqueMax"))
                binding.fronttorquegauge.invalidate()
            } ?: run {
                binding.fronttorque.text = ""
                binding.fronttorquegauge.setGauge(0f)
                binding.fronttorquegauge.invalidate()
            }
            it.getValue(Constants.rearTorque)?.let {
                var rearTorqueVal = it.toFloat()
                if ((viewModel.getValue(Constants.gearSelected)
                        ?.toInt() == Constants.gearReverse)
                ) {
                    rearTorqueVal = -(rearTorqueVal)
                }
                if (prefs.getBooleanPref(Constants.torqueInLbfFt)) {
                    binding.reartorque.text = rearTorqueVal.toInt().nmToLbfFt.toString()
                } else {
                    binding.reartorque.text = rearTorqueVal.toInt().toString()
                }
                if (abs(prefs.getPref("rearTorqueMax")) < rearTorqueVal.toFloat()) {
                    prefs.setPref("rearTorqueMax", abs(rearTorqueVal.toFloat()))
                }
                binding.reartorquegauge.setGauge(rearTorqueVal.toFloat() / prefs.getPref("rearTorqueMax"))
                binding.reartorquegauge.invalidate()
            } ?: run {
                binding.reartorque.text = ""
                binding.reartorquegauge.setGauge(0f)
                binding.reartorquegauge.invalidate()
            }
            it.getValue(Constants.battBrickMin)?.let {
                if (prefs.getBooleanPref(Constants.tempInF)) {
                    binding.batttemp.text = it.toFloat().cToF.toInt().toString()
                } else {
                    binding.batttemp.text = "%.1f".format(it.toFloat())
                }
                binding.batttempgauge.setGauge((it.toFloat() + 40f) / 128)
            } ?: run {
                binding.batttemp.text = ""
                binding.batttempgauge.setGauge(0f)
                binding.batttempgauge.invalidate()
            }
            it.getValue(Constants.autopilotHands)?.let { autopilotHandsVal ->
                if ((autopilotHandsVal.toInt() > 2) and (autopilotHandsVal.toInt() < 15)) {
                    if (autopilotHandsVal.toInt() in 3..4) {
                        // 3 and 4 start with a delay between toast and flash
                        // Delay is really 2 seconds, but subtract a bit of lag
                        autopilotAnimation.startDelay = 1900L
                    } else {
                        // other warnings flash immediately
                        autopilotAnimation.startDelay = 0L
                    }
                    if (autopilotHandsToggle == false) {
                        // Warning toast:
                        binding.APWarning.visibility = View.VISIBLE

                        // Gradient overlay:
                        // autopilotAnimation is repeated in .doOnEnd
                        // set repeatCount to 1 so that it reverses before ending
                        autopilotAnimation.repeatCount = 1
                        autopilotAnimation.repeatMode = ValueAnimator.REVERSE
                        overlayGradient.orientation = GradientDrawable.Orientation.TOP_BOTTOM
                        autopilotAnimation.addUpdateListener { animator ->
                            overlayGradient.colors =
                                intArrayOf(animator.animatedValue as Int, colorFrom)
                        }
                        autopilotAnimation.doOnEnd {
                            // Duration is from low to high, a full cycle is duration * 2
                            it.duration = max(250L, (it.duration * 0.9).toLong())
                            it.startDelay = 0L
                            it.start()
                        }
                        autopilotAnimation.duration = 750L
                        autopilotAnimation.start()
                        binding.warningGradientOverlay.visibility = View.VISIBLE
                        autopilotHandsToggle = true
                    }
                } else {
                    if (autopilotHandsToggle) {
                        // Warning toast:
                        binding.APWarning.visibility = View.GONE

                        // Gradient overlay:
                        binding.warningGradientOverlay.visibility = View.GONE
                        autopilotAnimation.removeAllListeners()
                        autopilotAnimation.cancel()
                        overlayGradient.colors = intArrayOf(colorFrom, colorFrom)
                        autopilotHandsToggle = false
                    }
                }
            }

            it.getValue(Constants.PINenabled)?.let { PINenabled ->
                if (PINenabled == 1f) {
                    if (viewModel.getValue(Constants.PINpassed) == 0f &&
                        binding.PINWarning.visibility != View.VISIBLE &&
                        viewModel.getValue(Constants.brakeApplied) == 2f) {
                        binding.PINWarning.clearAnimation()
                        val fadeInWarning = AnimationUtils.loadAnimation(activity, R.anim.fade_in)
                        binding.PINWarning.startAnimation(fadeInWarning)
                        binding.PINWarning.visibility = View.VISIBLE
                    } else if(viewModel.getValue(Constants.PINpassed) == 1f) {
                        binding.PINWarning.clearAnimation()
                        val fadeOutWarning = AnimationUtils.loadAnimation(activity, R.anim.fade_out)
                        if (binding.PINWarning.visibility != View.GONE) {
                            binding.PINWarning.startAnimation(fadeOutWarning)
                            binding.PINWarning.visibility = View.GONE
                        }
                    }
                } else {
                    binding.PINWarning.clearAnimation()
                    binding.PINWarning.visibility = View.GONE
                }
            }

            it.getValue(Constants.uiSpeed)?.let { vehicleSpeedVal ->
                var sensingSpeedLimit = 35
                binding.speed.scaleY = .9f

                binding.speed.text = vehicleSpeedVal.toInt().toString()
                if (vehicleSpeedVal.toInt() > 0 && power > 0 && !prefs.getBooleanPref(Constants.hideInstEfficiency)){
                    binding.instefficiency.visibility = View.VISIBLE
                    if (prefs.getBooleanPref("uiSpeedUnitsMPH")){
                        binding.instefficiency.text = formatWatts(power/vehicleSpeedVal.toFloat()) + "h/Mi"
                    }
                    else {
                        binding.instefficiency.text = formatWatts(power/vehicleSpeedVal.toFloat()) + "h/km"
                    }
                }
                else {
                    binding.instefficiency.visibility = View.INVISIBLE
                }

                if (viewModel.getValue(Constants.uiSpeedUnits) != 0f) {
                    sensingSpeedLimit = 35f.miToKm.toInt()
                }
                if (vehicleSpeedVal.toInt() > sensingSpeedLimit) {
                    l1Distance = 400
                    l2Distance = 250
                } else {
                    l1Distance = 300
                    l2Distance = 200
                }
            } ?: run {
                binding.speed.text = ""
            }
            it.getValue(Constants.uiSpeedUnits)?.let { uiSpeedUnitsVal ->
                if (uiSpeedUnitsVal.toInt() == 0) {
                    uiSpeedUnitsMPH = true
                    prefs.setBooleanPref("uiSpeedUnitsMPH", true)
                    binding.unit.text = "MPH"
                } else {
                    uiSpeedUnitsMPH = false
                    prefs.setBooleanPref("uiSpeedUnitsMPH", false)
                    binding.unit.text = "KM/H"
                }
            } ?: run {
                binding.unit.text = ""
            }
            var battText = ""
            if (showSOC == true) {
                it.getValue(Constants.stateOfCharge)?.let { stateOfChargeVal ->
                    battText = stateOfChargeVal.toInt().toString() + " %"
                }
            } else {
                if (uiSpeedUnitsMPH == true) {
                    it.getValue(Constants.uiRange)?.let { stateOfChargeVal ->
                        battText = stateOfChargeVal.toInt().toString() + " mi"
                    }
                } else {
                    it.getValue(Constants.uiRange)?.let { stateOfChargeVal ->
                        battText = ((stateOfChargeVal.toInt()) / .62).toInt().toString() + " km"
                    }
                }
            }
            if (binding.batterypercent.text != battText) {
                binding.batterypercent.text = battText
            }

            processDoors(it)

            it.getValue(Constants.stateOfCharge)?.let { stateOfChargeVal ->
                binding.fullbattery.setGauge(stateOfChargeVal.toFloat())
                binding.fullbattery.invalidate()
            } ?: run {
                binding.fullbattery.setGauge(0f)
                binding.fullbattery.invalidate()
            }

            it.getValue(Constants.autopilotState)?.let { autopilotStateVal ->
                updateAutopilotUI(
                    autopilotStateVal.toInt(),
                    it.getValue(Constants.steeringAngle)?.toInt()
                )
            } ?: run {
                updateAutopilotUI(0, 0)
            }

            it.getValue(Constants.mapRegion)?.let { mapRegionVal ->
                mapRegion = mapRegionVal.toFloat()
            }

            if (!isSplitScreen() && !prefs.getBooleanPref(Constants.hideSpeedLimit) && gearState == Constants.gearDrive) {
                it.getValue(Constants.fusedSpeedLimit)?.let { speedLimitVal ->
                    if (speedLimitVal.toFloat() != Constants.fusedSpeedNone) {
                        if (mapRegion == Constants.mapUS) {
                            // There's no CA map region from the dbc, assuming that CA uses US map region and sign
                            binding.speedLimitValueUs.text = speedLimitVal.toInt().toString()
                            binding.speedLimitUs.visibility = View.VISIBLE
                            binding.speedLimitRound.visibility = View.INVISIBLE
                        } else {
                            // Apologies if I wrongly assumed the rest of the world uses the round sign
                            if (speedLimitVal.toInt() != 155) {
                                binding.speedLimitNolimitRound.visibility = View.INVISIBLE
                                binding.speedLimitValueRound.text = speedLimitVal.toInt().toString()
                                binding.speedLimitRound.visibility = View.VISIBLE
                                binding.speedLimitUs.visibility = View.INVISIBLE
                            } else {
                                binding.speedLimitNolimitRound.visibility = View.VISIBLE
                                binding.speedLimitUs.visibility = View.INVISIBLE
                                binding.speedLimitRound.visibility = View.INVISIBLE
                            }
                        }
                    } else {
                        binding.speedLimitUs.visibility = View.INVISIBLE
                        binding.speedLimitRound.visibility = View.INVISIBLE
                        binding.speedLimitNolimitRound.visibility = View.INVISIBLE
                    }
                } ?: run {
                    binding.speedLimitUs.visibility = View.INVISIBLE
                    binding.speedLimitRound.visibility = View.INVISIBLE
                    binding.speedLimitNolimitRound.visibility = View.INVISIBLE
                }
            } else {
                binding.speedLimitUs.visibility = View.INVISIBLE
                binding.speedLimitRound.visibility = View.INVISIBLE
                binding.speedLimitNolimitRound.visibility = View.INVISIBLE
            }


            it.getValue(Constants.turnSignalLeft)?.let { leftTurnSignalVal ->
                when (leftTurnSignalVal.toInt()) {
                    0 -> {
                        binding.leftTurnSignalDark.visibility = View.INVISIBLE
                        binding.leftTurnSignalLight.visibility = View.INVISIBLE
                    }
                    1 -> {
                        binding.leftTurnSignalLight.visibility = View.INVISIBLE
                        binding.leftTurnSignalDark.visibility = View.VISIBLE
                    }
                    2 -> {
                        binding.leftTurnSignalLight.visibility = View.VISIBLE
                    }
                }
            } ?: run {
                binding.leftTurnSignalDark.visibility = View.INVISIBLE
                binding.leftTurnSignalLight.visibility = View.INVISIBLE
            }

            it.getValue(Constants.turnSignalRight)?.let { rightTurnSignalVal ->
                when (rightTurnSignalVal.toInt()) {
                    0 -> {
                        binding.rightTurnSignalDark.visibility = View.INVISIBLE
                        binding.rightTurnSignalLight.visibility = View.INVISIBLE
                    }
                    1 -> {
                        binding.rightTurnSignalLight.visibility = View.INVISIBLE
                        binding.rightTurnSignalDark.visibility = View.VISIBLE
                    }
                    2 -> {
                        binding.rightTurnSignalLight.visibility = View.VISIBLE
                    }

                }
            } ?: run {
                binding.rightTurnSignalDark.visibility = View.INVISIBLE
                binding.rightTurnSignalLight.visibility = View.INVISIBLE
            }

            it.getValue(Constants.drlMode)?.let { drlModeVal ->
                if ((drlModeVal != Constants.drlModePosition) or
                    ((gearState == Constants.gearPark) and
                     (viewModel.getValue(Constants.lowBeamLeft) == Constants.lowBeamLeftOff))) {
                    binding.telltaleDrl.visibility = View.INVISIBLE
                } else {
                    binding.telltaleDrl.visibility = View.VISIBLE
                }
            } ?: run {
                binding.telltaleDrl.visibility = View.INVISIBLE
            }

            it.getValue(Constants.lowBeamLeft)?.let { lowBeamVal ->
                if (lowBeamVal == Constants.lowBeamLeftOn) {
                    binding.telltaleLb.visibility = View.VISIBLE
                } else {
                    binding.telltaleLb.visibility = View.INVISIBLE
                }
            } ?: run {
                binding.telltaleLb.visibility = View.INVISIBLE
            }

            it.getValue(Constants.autoHighBeamEnabled)?.let { ahbEnabledVal ->
                if (ahbEnabledVal == 1f) {
                    binding.telltaleHb.visibility = View.INVISIBLE
                    if (viewModel.getValue(Constants.highBeamRequest) == 1f) {
                        if (viewModel.getValue(Constants.highLowBeamDecision) == 2f) {
                            // Auto High Beam is on, AHB decision is ON
                            binding.telltaleAhbStdby.visibility = View.INVISIBLE
                            binding.telltaleAhbActive.visibility = View.VISIBLE
                            binding.telltaleLb.visibility = View.INVISIBLE
                        } else {
                            // Auto High Beam is on, AHB decision is OFF
                            binding.telltaleAhbStdby.visibility = View.VISIBLE
                            binding.telltaleAhbActive.visibility = View.INVISIBLE
                            if (viewModel.getValue(Constants.highBeamStalkStatus) == 1f) {
                                // Pulled on left stalk, flash HB
                                binding.telltaleLb.visibility = View.INVISIBLE
                                binding.telltaleHb.visibility = View.VISIBLE
                            } else {
                                // Stalk idle, business as usual
                                if (viewModel.getValue(Constants.lowBeamLeft) == Constants.lowBeamLeftOn) {
                                    binding.telltaleLb.visibility = View.VISIBLE
                                }
                                binding.telltaleHb.visibility = View.INVISIBLE
                            }
                        }
                    } else {
                        binding.telltaleAhbStdby.visibility = View.INVISIBLE
                        binding.telltaleAhbActive.visibility = View.INVISIBLE
                        if (viewModel.getValue(Constants.highBeamStalkStatus) == 1f) {
                            // Pulled on left stalk, flash HB
                            binding.telltaleLb.visibility = View.INVISIBLE
                            binding.telltaleHb.visibility = View.VISIBLE
                        }
                    }
                } else {
                    binding.telltaleAhbStdby.visibility = View.INVISIBLE
                    binding.telltaleAhbActive.visibility = View.INVISIBLE
                    if ((viewModel.getValue(Constants.highBeamRequest) == 1f) or
                        (viewModel.getValue(Constants.highBeamStalkStatus) == 1f)) {
                        binding.telltaleLb.visibility = View.INVISIBLE
                        binding.telltaleHb.visibility = View.VISIBLE
                    } else {
                        if (viewModel.getValue(Constants.lowBeamLeft) == Constants.lowBeamLeftOn) {
                            binding.telltaleLb.visibility = View.VISIBLE
                        }
                        binding.telltaleHb.visibility = View.INVISIBLE
                    }
                }
            } ?: run {
                binding.telltaleAhbStdby.visibility = View.INVISIBLE
                binding.telltaleAhbActive.visibility = View.INVISIBLE
            }

            it.getValue(Constants.rearFogSwitch)?.let { fogRearVal ->
                if (fogRearVal == Constants.rearFogSwitchOn) {
                    binding.telltaleFogRear.visibility = View.VISIBLE
                } else {
                    binding.telltaleFogRear.visibility = View.INVISIBLE
                }
            } ?: run {
                binding.telltaleFogRear.visibility = View.INVISIBLE
            }

            it.getValue(Constants.frontFogSwitch)?.let { fogFrontVal ->
                if (fogFrontVal == Constants.frontFogSwitchOn) {
                    binding.telltaleFogFront.visibility = View.VISIBLE
                } else {
                    binding.telltaleFogFront.visibility = View.INVISIBLE
                }
            } ?: run {
                binding.telltaleFogFront.visibility = View.INVISIBLE
            }

            if (gearState != Constants.gearInvalid &&
                ((it.getValue(Constants.driverUnbuckled) == 1f) or
                (it.getValue(Constants.passengerUnbuckled) == 1f))) {
                binding.telltaleSeatbelt.visibility = View.VISIBLE
            } else {
                binding.telltaleSeatbelt.visibility = View.INVISIBLE
            }

            it.getValue(Constants.heatBattery)?.let { heatBatteryVal ->
                if (heatBatteryVal == 1f) {
                    binding.battHeat.visibility = View.VISIBLE
                } else {
                    binding.battHeat.visibility = View.GONE
                }
            } ?: run {
                binding.battHeat.visibility = View.GONE
            }

            it.getValue(Constants.chargeStatus)?.let { chargeStatusVal ->
                if (chargeStatusVal == Constants.chargeStatusActive) {
                    binding.battCharge.visibility = View.VISIBLE
                } else {
                    binding.battCharge.visibility = View.GONE
                }
            } ?: run {
                binding.battCharge.visibility = View.GONE
            }

            it.getValue(Constants.brakePark)?.let { brakeParkVal ->
                if (brakeParkVal == Constants.brakeParkRed) {
                    binding.telltaleBrakePark.visibility = View.VISIBLE
                    binding.telltaleBrakeParkFault.visibility = View.INVISIBLE
                } else if(brakeParkVal == Constants.brakeParkAmber) {
                    binding.telltaleBrakePark.visibility = View.INVISIBLE
                    binding.telltaleBrakeParkFault.visibility = View.VISIBLE
                } else {
                    binding.telltaleBrakePark.visibility = View.INVISIBLE
                    binding.telltaleBrakeParkFault.visibility = View.INVISIBLE
                }
            } ?: run {
                binding.telltaleBrakePark.visibility = View.INVISIBLE
                binding.telltaleBrakeParkFault.visibility = View.INVISIBLE
            }

            it.getValue(Constants.brakeHold)?.let { brakeHoldVal ->
                if (brakeHoldVal == 1f) {
                    binding.telltaleBrakeHold.visibility = View.VISIBLE
                } else {
                    binding.telltaleBrakeHold.visibility = View.INVISIBLE
                }
            } ?: run {
                binding.telltaleBrakeHold.visibility = View.INVISIBLE
            }

            it.getValue(Constants.tpmsHard)?.let { tpmsHardVal ->
                if (tpmsHardVal == 1f) {
                    binding.telltaleTPMSFaultHard.visibility = View.VISIBLE
                } else {
                    binding.telltaleTPMSFaultHard.visibility = View.INVISIBLE
                    if(viewModel.getValue(Constants.tpmsSoft) == 1f) {
                        binding.telltaleTPMSFaultSoft.visibility = View.VISIBLE
                    } else {
                        binding.telltaleTPMSFaultSoft.visibility = View.INVISIBLE
                    }
                }
            } ?: run {
                binding.telltaleTPMSFaultSoft.visibility = View.INVISIBLE
                binding.telltaleTPMSFaultHard.visibility = View.INVISIBLE
            }

            it.getValue(Constants.odometer)?.let { odometerVal ->
                if (!prefs.getBooleanPref(Constants.hideOdometer)) {
                    binding.odometer.visibility = View.VISIBLE
                    binding.odometer.text = if (uiSpeedUnitsMPH) {
                        odometerVal.toFloat().kmToMi.toInt().toString() + " mi"
                    } else {
                        odometerVal.toInt().toString() + " km"
                    }
                }
                else {
                    binding.odometer.visibility = View.INVISIBLE
                }
            } ?: run {
                binding.odometer.visibility = View.INVISIBLE
            }

            // Check if AP is not engaged, otherwise blindspot alerts are disabled
            if (viewModel.getValue(Constants.autopilotState)?.toInt() !in 3..7) {
                var bsBinding = binding.BSWarningLeft
                var bsLevel = 0f
                if (it.getValue(Constants.blindSpotLeft) in setOf(1f, 2f)) {
                    bsBinding = binding.BSWarningLeft
                    bsLevel = it.getValue(Constants.blindSpotLeft) as Float
                    overlayGradient.orientation = GradientDrawable.Orientation.LEFT_RIGHT
                } else if (it.getValue(Constants.blindSpotRight) in setOf(1f, 2f)) {
                    bsBinding = binding.BSWarningRight
                    bsLevel = it.getValue(Constants.blindSpotLeft) as Float
                    overlayGradient.orientation = GradientDrawable.Orientation.RIGHT_LEFT
                } else {
                    overlayGradient.orientation = GradientDrawable.Orientation.TOP_BOTTOM
                }
                // Set speed based on severity
                if (bsLevel == 2f) {
                    // Duration is milliseconds from low to high, a full cycle is duration * 2
                    blindspotAnimation.duration = 150
                } else {
                    blindspotAnimation.duration = 300
                }

                if (bsLevel in setOf(1f, 2f)) {
                    if (bsWarningToggle == false) {
                        // Warning toast:
                        bsBinding.clearAnimation()
                        bsBinding.visibility = View.VISIBLE

                        // Gradient overlay:
                        blindspotAnimation.addUpdateListener { animator ->
                            overlayGradient.colors =
                                intArrayOf(animator.animatedValue as Int, colorFrom)
                        }
                        blindspotAnimation.repeatCount = ValueAnimator.INFINITE
                        blindspotAnimation.repeatMode = ValueAnimator.REVERSE
                        blindspotAnimation.start()
                        binding.warningGradientOverlay.visibility = View.VISIBLE

                        bsWarningToggle = true
                    }

                } else {
                    if (binding.BSWarningLeft.visibility == View.VISIBLE) {
                        bsBinding = binding.BSWarningLeft
                    } else if (binding.BSWarningRight.visibility == View.VISIBLE) {
                        bsBinding = binding.BSWarningRight
                    }
                    if (bsWarningToggle == true) {
                        // Warning toast:
                        val bsFadeOut = AnimationUtils.loadAnimation(activity, R.anim.fade_out)
                        bsBinding.startAnimation(bsFadeOut)
                        bsBinding.visibility = View.GONE

                        // Gradient overlay:
                        // let it fade out naturally by setting repeat to 1 (so it reverses) then change visibility on end
                        blindspotAnimation.doOnEnd {
                            binding.warningGradientOverlay.visibility = View.GONE
                            overlayGradient.colors = intArrayOf(colorFrom, colorFrom)
                        }
                        blindspotAnimation.repeatCount = 1

                        bsWarningToggle = false
                    }
                }
            }

            it.getValue(Constants.forwardCollisionWarning)?.let {fcwVal ->
                if (fcwVal.toFloat() == 1f) {
                    overlayGradient.orientation = GradientDrawable.Orientation.TOP_BOTTOM
                    // Duration is milliseconds from low to high, a full cycle is duration * 2
                    blindspotAnimation.duration = 125
                    if (fcwToggle == false) {
                        // Warning toast:
                        binding.FCWarning.clearAnimation()
                        binding.FCWarning.visibility = View.VISIBLE

                        // Gradient overlay:
                        // Reuse blindspot animation as it's basically the same
                        blindspotAnimation.addUpdateListener { animator ->
                            overlayGradient.colors =
                                intArrayOf(animator.animatedValue as Int, colorFrom)
                        }
                        blindspotAnimation.doOnEnd {
                            binding.warningGradientOverlay.visibility = View.GONE
                            overlayGradient.colors = intArrayOf(colorFrom, colorFrom)
                        }
                        blindspotAnimation.repeatCount = 4
                        blindspotAnimation.repeatMode = ValueAnimator.RESTART
                        blindspotAnimation.reverse()
                        binding.warningGradientOverlay.visibility = View.VISIBLE
                        fcwToggle = true
                    }
                } else {
                    if (fcwToggle == true) {
                        // Warning toast:
                        val fcwFadeOut = AnimationUtils.loadAnimation(activity, R.anim.fade_out)
                        binding.FCWarning.startAnimation(fcwFadeOut)
                        binding.FCWarning.visibility = View.GONE
                        // Gradient overlay stops by itself after a fixed repeat count
                        fcwToggle = false
                    }
                }
            }

            if (gearState in setOf(Constants.gearDrive, Constants.gearNeutral, Constants.gearReverse) && !prefs.getBooleanPref(Constants.hideBs)) {
                it.getValue(Constants.leftVehicle)?.let { sensorVal ->
                    if ((sensorVal.toInt() < l1Distance) and (sensorVal.toInt() >= l2Distance)) {
                        binding.blindSpotLeft1a.visibility = View.VISIBLE
                    } else if (sensorVal.toInt() < l2Distance) {
                        binding.blindSpotLeft2a.visibility = View.VISIBLE
                    } else {
                        binding.blindSpotLeft1a.visibility = View.INVISIBLE
                        binding.blindSpotLeft2a.visibility = View.INVISIBLE
                    }
                } ?: run {
                    binding.blindSpotLeft1a.visibility = View.INVISIBLE
                    binding.blindSpotLeft2a.visibility = View.INVISIBLE
                }


                it.getValue(Constants.rightVehicle)?.let { sensorVal ->
                    if ((sensorVal.toInt() < l1Distance) and (sensorVal.toInt() >= l2Distance)) {
                        binding.blindSpotRight1a.visibility = View.VISIBLE
                    } else if (sensorVal.toInt() < l2Distance) {
                        binding.blindSpotRight2a.visibility = View.VISIBLE
                    } else {
                        binding.blindSpotRight1a.visibility = View.INVISIBLE
                        binding.blindSpotRight2a.visibility = View.INVISIBLE
                    }
                } ?: run {
                    binding.blindSpotRight1a.visibility = View.INVISIBLE
                    binding.blindSpotRight2a.visibility = View.INVISIBLE
                }
            } else {
                // in park or off
                binding.blindSpotLeft1a.visibility = View.INVISIBLE
                binding.blindSpotLeft2a.visibility = View.INVISIBLE
                binding.blindSpotRight1a.visibility = View.INVISIBLE
                binding.blindSpotRight2a.visibility = View.INVISIBLE
            }


            it.getValue(Constants.chargeStatus)?.let { chargeStatusVal ->
                if (!isSplitScreen()) {
                    if (chargeStatusVal != Constants.chargeStatusInactive) {
                        binding.fullbattery.setChargeMode(1)
                        for (chargingHiddenView in chargingHiddenViews()) {
                            chargingHiddenView.visibility = View.GONE
                        }
                        for (minMaxchargingHiddenView in minMaxchargingHiddenViews()) {
                            minMaxchargingHiddenView.visibility = View.GONE
                        }

                        binding.chargemeter.visibility = View.VISIBLE
                        viewModel.getValue(Constants.stateOfCharge)?.toFloat()
                            ?.let { socVal ->
                                binding.chargemeter.setGauge(socVal / 100f, 4f, true)
                                binding.bigsoc.text = socVal.toInt().toString()
                                binding.chargemeter.invalidate()
                                binding.bigsoc.visibility = View.VISIBLE
                                binding.bigsocpercent.visibility = View.VISIBLE
                                binding.chargerate.text = formatPower(abs(battAmps * battVolts), convert = false)
                                binding.chargerate.visibility = View.VISIBLE

                            }
                    } else {
                        binding.fullbattery.setChargeMode(0)
                        binding.chargemeter.visibility = View.INVISIBLE
                        binding.bigsoc.visibility = View.INVISIBLE
                        binding.bigsocpercent.visibility = View.INVISIBLE

                        binding.chargerate.visibility = View.INVISIBLE

                        for (chargingHiddenView in chargingHiddenViews()) {
                            chargingHiddenView.visibility = View.VISIBLE
                        }
                        if (prefs.getPref(Constants.gaugeMode) > Constants.showSimpleGauges) {
                            for (minMaxchargingHiddenView in minMaxchargingHiddenViews()) {
                                minMaxchargingHiddenView.visibility = View.VISIBLE
                            }
                        } else {}
                    }
                } else {
                    if (chargeStatusVal != Constants.chargeStatusInactive) {
                        binding.fullbattery.setChargeMode(1)
                        for (chargingHiddenView in chargingHiddenViews()) {
                            chargingHiddenView.visibility = View.GONE
                        }
                        for (minMaxchargingHiddenView in minMaxchargingHiddenViews()) {
                            minMaxchargingHiddenView.visibility = View.GONE
                        }
                        if (doorOpen) {
                            binding.chargemeter.visibility = View.INVISIBLE
                            binding.bigsoc.visibility = View.INVISIBLE
                            binding.bigsocpercent.visibility = View.INVISIBLE

                            binding.chargerate.visibility = View.INVISIBLE
                        } else {
                            binding.chargemeter.visibility = View.VISIBLE
                            binding.bigsoc.visibility = View.VISIBLE
                            binding.bigsocpercent.visibility = View.VISIBLE

                            binding.chargerate.visibility = View.VISIBLE
                        }
                    } else {
                        binding.fullbattery.setChargeMode(0)
                        binding.chargemeter.visibility = View.INVISIBLE
                        binding.bigsoc.visibility = View.INVISIBLE
                        binding.bigsocpercent.visibility = View.INVISIBLE

                        binding.chargerate.visibility = View.INVISIBLE
                        if (!doorOpen){
                            for (chargingHiddenView in chargingHiddenViews()) {
                                chargingHiddenView.visibility = View.VISIBLE
                            }
                        }
                        if (prefs.getPref(Constants.gaugeMode) > Constants.showSimpleGauges) {
                            for (minMaxchargingHiddenView in minMaxchargingHiddenViews()) {
                                minMaxchargingHiddenView.visibility = View.VISIBLE
                            }
                        } else {}
                    }
                }
            } ?: run {
                binding.fullbattery.setChargeMode(0)
                binding.chargemeter.visibility = View.INVISIBLE
                binding.bigsoc.visibility = View.INVISIBLE
                binding.bigsocpercent.visibility = View.INVISIBLE
                binding.chargerate.visibility = View.INVISIBLE
            }
        }
    }


    fun processDoors(cState: CarState) {
        // Passing through carState so we're not using historical (old) state info
        if (cState.getValue(Constants.frontLeftDoorState) == null) {
            // if there's no state for FL door, assume there's no state for any door
            for (door in doorViews()) {
                door.visibility = View.GONE
            }
            for (door in doorViewsCenter()) {
                door.visibility = View.GONE
            }
            doorOpen = false
            updateCarStateUI(doorOpen)
        } else if ((cState.getValue(Constants.liftgateState) in setOf(1f, 4f)) or
            (cState.getValue(Constants.frunkState) in setOf(1f, 4f)) or
            (cState.getValue(Constants.frontLeftDoorState) in setOf(1f, 4f)) or
            (cState.getValue(Constants.frontRightDoorState) in setOf(1f, 4f)) or
            (cState.getValue(Constants.rearLeftDoorState) in setOf(1f, 4f)) or
            (cState.getValue(Constants.rearRightDoorState) in setOf(1f, 4f))
        ) {
            doorOpen = true
            updateCarStateUI(doorOpen)

            displayOpenDoors(cState)
        } else if ((cState.getValue(Constants.liftgateState) == 2f) and
            (cState.getValue(Constants.frunkState) == 2f) and
            (cState.getValue(Constants.frontLeftDoorState) == 2f) and
            (cState.getValue(Constants.frontRightDoorState) == 2f) and
            (cState.getValue(Constants.rearLeftDoorState) == 2f) and
            (cState.getValue(Constants.rearRightDoorState) == 2f)
        ) {
            displayOpenDoors(cState)
            doorOpen = false
            updateCarStateUI(doorOpen)
        }
    }

    fun displayOpenDoors(cState: CarState) {
        if (!isSplitScreen()) {
            cState.getValue(Constants.liftgateState)?.let { liftgateVal ->
                if (liftgateVal.toInt() in setOf(1, 4)) {
                    binding.hatch.visibility = View.VISIBLE
                } else {
                    binding.hatch.visibility = View.GONE
                }
            }

            cState.getValue(Constants.frunkState)?.let { frunkVal ->

                if (frunkVal.toInt() in setOf(1, 4)) {
                    binding.hood.visibility = View.VISIBLE
                } else {
                    binding.hood.visibility = View.GONE
                }

            }

            cState.getValue(Constants.frontLeftDoorState)?.let { frontLeftDoorVal ->

                if (frontLeftDoorVal.toInt() in setOf(1, 4)) {
                    binding.frontleftdoor.visibility = View.VISIBLE
                } else {
                    binding.frontleftdoor.visibility = View.GONE
                }

            }
            cState.getValue(Constants.frontRightDoorState)?.let { frontRightDoorVal ->

                if (frontRightDoorVal.toInt() in setOf(1, 4)) {
                    binding.frontrightdoor.visibility = View.VISIBLE
                } else {
                    binding.frontrightdoor.visibility = View.GONE
                }

            }
            cState.getValue(Constants.rearLeftDoorState)?.let { rearLeftDoorVal ->

                if (rearLeftDoorVal.toInt() in setOf(1, 4)) {
                    binding.rearleftdoor.visibility = View.VISIBLE
                } else {
                    binding.rearleftdoor.visibility = View.GONE
                }

            }
            cState.getValue(Constants.rearRightDoorState)?.let { rearRightDoorVal ->

                if (rearRightDoorVal.toInt() in setOf(1, 4)) {
                    binding.rearrightdoor.visibility = View.VISIBLE
                } else {
                    binding.rearrightdoor.visibility = View.GONE
                }

            }
        } else {
            cState.getValue(Constants.liftgateState)?.let { liftgateVal ->
                if (liftgateVal.toInt() in setOf(1, 3)) {
                    binding.hatchCenter.visibility = View.VISIBLE
                } else {
                    binding.hatchCenter.visibility = View.GONE
                }

            }

            cState.getValue(Constants.frunkState)?.let { frunkVal ->

                if (frunkVal.toInt() in setOf(1, 4)) {
                    binding.hoodCenter.visibility = View.VISIBLE
                } else {
                    binding.hoodCenter.visibility = View.GONE
                }

            }

            cState.getValue(Constants.frontLeftDoorState)?.let { frontLeftDoorVal ->

                if (frontLeftDoorVal.toInt() in setOf(1, 4)) {
                    binding.frontleftdoorCenter.visibility = View.VISIBLE
                } else {
                    binding.frontleftdoorCenter.visibility = View.GONE
                }

            }
            cState.getValue(Constants.frontRightDoorState)?.let { frontRightDoorVal ->

                if (frontRightDoorVal.toInt() in setOf(1, 4)) {
                    binding.frontrightdoorCenter.visibility = View.VISIBLE
                } else {
                    binding.frontrightdoorCenter.visibility = View.GONE
                }

            }
            cState.getValue(Constants.rearLeftDoorState)?.let { rearLeftDoorVal ->

                if (rearLeftDoorVal.toInt() in setOf(1, 4)) {
                    binding.rearleftdoorCenter.visibility = View.VISIBLE
                } else {
                    binding.rearleftdoorCenter.visibility = View.GONE
                }

            }
            cState.getValue(Constants.rearRightDoorState)?.let { rearRightDoorVal ->

                if (rearRightDoorVal.toInt() in setOf(1, 4)) {
                    binding.rearrightdoorCenter.visibility = View.VISIBLE
                } else {
                    binding.rearrightdoorCenter.visibility = View.GONE
                }

            }
        }
    }

    fun setColors(sunUpVal: Int) {
        val window: Window? = activity?.window

        // Not using dark-mode for compatibility with older version of Android (pre-29)
        if (sunUpVal == 0 || prefs.getBooleanPref(Constants.forceNightMode)) {
            binding.powerBar.setDayValue(0)
            binding.fullbattery.setDayValue(0)
            binding.fronttorquegauge.setDayValue(0)
            binding.reartorquegauge.setDayValue(0)
            binding.batttempgauge.setDayValue(0)
            binding.fronttempgauge.setDayValue(0)
            binding.reartempgauge.setDayValue(0)
            binding.frontbraketempgauge.setDayValue(0)
            binding.rearbraketempgauge.setDayValue(0)
            binding.coolantflowgauge.setDayValue(0)
            binding.powerBar.invalidate()
            binding.fullbattery.invalidate()
            binding.fronttorquegauge.invalidate()
            binding.reartorquegauge.invalidate()
            binding.batttempgauge.invalidate()
            binding.fronttempgauge.invalidate()
            binding.reartempgauge.invalidate()
            binding.frontbraketempgauge.invalidate()
            binding.rearbraketempgauge.invalidate()
            binding.coolantflowgauge.invalidate()
            window?.statusBarColor = Color.BLACK
            binding.root.setBackgroundColor(Color.BLACK)
            //binding.speed.setTypeface(resources.getFont(R.font.orbitronlight), Typeface.NORMAL )
            binding.speed.setTextColor(Color.WHITE)
            binding.bigsoc.setTextColor(Color.WHITE)
            binding.bigsocpercent.setTextColor(Color.WHITE)

            binding.chargerate.setTextColor(Color.LTGRAY)
            binding.chargemeter.setDayValue(0)
            binding.unit.setTextColor(Color.LTGRAY)
            binding.batterypercent.setTextColor(Color.LTGRAY)
            binding.odometer.setTextColor(Color.DKGRAY)
            binding.deadbattery.setColorFilter(Color.DKGRAY)
            gearColorSelected = Color.LTGRAY
            gearColor = Color.DKGRAY
            binding.PRND.setTextColor(Color.DKGRAY)
            binding.modely.setColorFilter(Color.LTGRAY)
            binding.modelyCenter.setColorFilter(Color.LTGRAY)

            binding.power.setTextColor(Color.WHITE)
            binding.instefficiency.setTextColor(Color.WHITE)

            binding.minpower.setTextColor(Color.WHITE)
            binding.maxpower.setTextColor(Color.WHITE)
            binding.fronttorque.setTextColor(Color.WHITE)
            binding.fronttorquelabel.setTextColor(Color.WHITE)
            binding.fronttorqueunits.setTextColor(Color.WHITE)

            binding.reartorque.setTextColor(Color.WHITE)
            binding.reartorquelabel.setTextColor(Color.WHITE)
            binding.reartorqueunits.setTextColor(Color.WHITE)
            binding.batttemp.setTextColor(Color.WHITE)
            binding.batttemplabel.setTextColor(Color.WHITE)
            binding.batttempunits.setTextColor(Color.WHITE)
            binding.fronttemp.setTextColor(Color.WHITE)
            binding.fronttemplabel.setTextColor(Color.WHITE)
            binding.fronttempunits.setTextColor(Color.WHITE)
            binding.frontbraketemp.setTextColor(Color.WHITE)
            binding.frontbraketemplabel.setTextColor(Color.WHITE)
            binding.frontbraketempunits.setTextColor(Color.WHITE)
            binding.rearbraketemp.setTextColor(Color.WHITE)
            binding.rearbraketemplabel.setTextColor(Color.WHITE)
            binding.rearbraketempunits.setTextColor(Color.WHITE)

            binding.reartemp.setTextColor(Color.WHITE)
            binding.reartemplabel.setTextColor(Color.WHITE)
            binding.reartempunits.setTextColor(Color.WHITE)
            binding.coolantflow.setTextColor(Color.WHITE)
            binding.coolantflowlabel.setTextColor(Color.WHITE)
            binding.coolantflowunits.setTextColor(Color.WHITE)

            binding.displaymaxspeed.setTextColor(Color.WHITE)


        } else {
            binding.powerBar.setDayValue(1)
            binding.fullbattery.setDayValue(1)
            binding.fronttorquegauge.setDayValue(1)
            binding.reartorquegauge.setDayValue(1)
            binding.batttempgauge.setDayValue(1)
            binding.fronttempgauge.setDayValue(1)
            binding.reartempgauge.setDayValue(1)
            binding.coolantflowgauge.setDayValue(1)
            binding.frontbraketempgauge.setDayValue(1)
            binding.rearbraketempgauge.setDayValue(1)
            binding.coolantflowgauge.setDayValue(1)
            binding.powerBar.invalidate()
            binding.fullbattery.invalidate()
            binding.fronttorquegauge.invalidate()
            binding.reartorquegauge.invalidate()
            binding.batttempgauge.invalidate()
            binding.fronttempgauge.invalidate()
            binding.reartempgauge.invalidate()
            binding.frontbraketempgauge.invalidate()
            binding.rearbraketempgauge.invalidate()
            binding.coolantflowgauge.invalidate()
            //view.setBackgroundColor(Color.parseColor("#"+Integer.toString(R.color.day_background, 16)))
            binding.root.setBackgroundColor(requireContext().getColor(R.color.day_background))
            window?.statusBarColor = Color.parseColor("#FFEEEEEE")
            binding.speed.setTextColor(Color.BLACK)
            binding.speed.setTextColor(Color.BLACK)
            binding.bigsoc.setTextColor(Color.BLACK)
            binding.bigsocpercent.setTextColor(Color.BLACK)
            binding.chargerate.setTextColor(Color.DKGRAY)
            binding.chargemeter.setDayValue(1)

            binding.unit.setTextColor(Color.GRAY)
            binding.batterypercent.setTextColor(Color.DKGRAY)
            binding.odometer.setTextColor(Color.LTGRAY)
            binding.deadbattery.clearColorFilter()
            gearColorSelected = Color.DKGRAY
            gearColor = Color.parseColor("#FFDDDDDD")
            binding.PRND.setTextColor(Color.parseColor("#FFDDDDDD"))
            binding.modely.setColorFilter(Color.GRAY)
            binding.modelyCenter.setColorFilter(Color.GRAY)

            binding.power.setTextColor(Color.DKGRAY)
            binding.instefficiency.setTextColor(Color.DKGRAY)
            binding.minpower.setTextColor(Color.DKGRAY)
            binding.maxpower.setTextColor(Color.DKGRAY)
            binding.fronttorque.setTextColor(Color.DKGRAY)
            binding.fronttorquelabel.setTextColor(Color.DKGRAY)
            binding.fronttorqueunits.setTextColor(Color.DKGRAY)

            binding.reartorque.setTextColor(Color.DKGRAY)
            binding.reartorquelabel.setTextColor(Color.DKGRAY)
            binding.reartorqueunits.setTextColor(Color.DKGRAY)
            binding.batttemp.setTextColor(Color.DKGRAY)
            binding.batttemplabel.setTextColor(Color.DKGRAY)
            binding.batttempunits.setTextColor(Color.DKGRAY)
            binding.fronttemp.setTextColor(Color.DKGRAY)
            binding.fronttemplabel.setTextColor(Color.DKGRAY)
            binding.fronttempunits.setTextColor(Color.DKGRAY)

            binding.reartemp.setTextColor(Color.DKGRAY)
            binding.reartemplabel.setTextColor(Color.DKGRAY)
            binding.reartempunits.setTextColor(Color.DKGRAY)

            binding.frontbraketemp.setTextColor(Color.DKGRAY)
            binding.frontbraketemplabel.setTextColor(Color.DKGRAY)
            binding.frontbraketempunits.setTextColor(Color.DKGRAY)
            binding.rearbraketemp.setTextColor(Color.DKGRAY)
            binding.rearbraketemplabel.setTextColor(Color.DKGRAY)
            binding.rearbraketempunits.setTextColor(Color.DKGRAY)
            binding.coolantflow.setTextColor(Color.DKGRAY)
            binding.coolantflowlabel.setTextColor(Color.DKGRAY)
            binding.coolantflowunits.setTextColor(Color.DKGRAY)

            binding.displaymaxspeed.setTextColor(Color.BLACK)

        }
        val wm = activity?.windowManager

    }

    fun formatPower(power: Float, convert: Boolean = true): String {
        if (prefs.getPref(Constants.powerUnits) == Constants.powerUnitHp && convert) {
            val hp = power.wToHp
            return if ((abs(hp) < 10)) {
                "%.1f".format(hp) + " hp"
            } else {
                hp.toInt().toString() + " hp"
            }
        }

        if (prefs.getPref(Constants.powerUnits) == Constants.powerUnitPs && convert) {
            val ps = power.wToPs
            return if ((abs(ps) < 10)) {
                "%.1f".format(ps) + " PS"
            } else {
                ps.toInt().toString() + " PS"
            }
        }
        return formatWatts(power)
    }

    fun formatWatts(power: Float): String {
        val kw = power / 1000f
        return if ((abs(kw) < 10)) {
            "%.1f".format(kw) + " kW"
        } else {
            kw.toInt().toString() + " kW"
        }
    }
    fun updateCarStateUI(doorOpen: Boolean) {
        val fadeIn = AnimationUtils.loadAnimation(activity, R.anim.fade_in)
        val fadeOut = AnimationUtils.loadAnimation(activity, R.anim.fade_out)
        val fadeInCenter = AnimationUtils.loadAnimation(activity, R.anim.fade_in)

        fadeIn.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation?) {
                if (prefs.getPref(Constants.gaugeMode) == Constants.showFullGauges) {
                    for (leftSideUIView in leftSideUIViews()) {
                        leftSideUIView.visibility = View.INVISIBLE
                    }

                }
                for (doorViewCenter in doorViewsCenter()) {
                    doorViewCenter.visibility = View.INVISIBLE
                }
                binding.modelyCenter.visibility = View.GONE
            }

            override fun onAnimationEnd(animation: Animation?) {
                binding.modely.visibility = View.VISIBLE


            }

            override fun onAnimationRepeat(animation: Animation?) {
            }
        })

        fadeInCenter.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation?) {
                binding.speed.visibility = View.INVISIBLE
                binding.unit.visibility = View.INVISIBLE
            }

            override fun onAnimationEnd(animation: Animation?) {

                binding.modelyCenter.visibility = View.VISIBLE


            }

            override fun onAnimationRepeat(animation: Animation?) {
            }
        })


        fadeOut.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation?) {
            }

            override fun onAnimationEnd(animation: Animation?) {
                for (leftSideUIView in leftSideUIViews()) {
                    leftSideUIView.visibility = View.VISIBLE
                }
            }

            override fun onAnimationRepeat(animation: Animation?) {
            }
        })
        if (!isSplitScreen()) {

            binding.modely.clearAnimation()
            if (doorOpen && !fadeIn.hasStarted() && binding.modely.visibility != View.VISIBLE) {

                binding.modely.startAnimation(fadeIn)

            } else {
                if (doorOpen) {
                    for (doorViewCenter in doorViewsCenter()) {
                        doorViewCenter.visibility = View.INVISIBLE
                    }
                    binding.modely.visibility = View.VISIBLE
                    binding.modelyCenter.visibility = View.INVISIBLE
                }
                if (!doorOpen && !fadeOut.hasStarted() && (binding.modely.visibility == View.VISIBLE)) {
                    binding.modely.startAnimation(fadeOut)
                    binding.modely.visibility = View.INVISIBLE
                    binding.speed.visibility = View.VISIBLE
                    binding.unit.visibility = View.VISIBLE
                } else {
                    if (!doorOpen) {
                        binding.modely.visibility = View.INVISIBLE
                        // in case split screen is turned off while door open
                        binding.speed.visibility = View.VISIBLE
                        binding.unit.visibility = View.VISIBLE
                        if (prefs.getPref(Constants.gaugeMode) == Constants.showFullGauges) {
                            for (leftSideUIView in leftSideUIViews()) {
                                leftSideUIView.visibility = View.VISIBLE
                            }
                        }
                    }
                }
            }
        } else {
            for (doorView in doorViews()) {
                doorView.visibility = View.INVISIBLE
            }
            binding.modelyCenter.clearAnimation()
            if (doorOpen && !fadeInCenter.hasStarted() && binding.modelyCenter.visibility != View.VISIBLE) {


                binding.modelyCenter.startAnimation(fadeInCenter)

            } else {
                if (doorOpen) {
                    for (doorView in doorViews()) {
                        doorView.visibility = View.INVISIBLE
                    }
                    binding.modelyCenter.visibility = View.VISIBLE
                }
                if (!doorOpen && !fadeOut.hasStarted() && (binding.modelyCenter.visibility == View.VISIBLE)) {
                    binding.modelyCenter.startAnimation(fadeOut)
                    binding.unit.visibility = View.VISIBLE
                    binding.modelyCenter.visibility = View.INVISIBLE
                    binding.speed.visibility = View.VISIBLE
                    binding.unit.visibility = View.VISIBLE
                } else {
                    if (!doorOpen) {
                        binding.modelyCenter.visibility = View.INVISIBLE
                        binding.speed.visibility = View.VISIBLE
                        binding.unit.visibility = View.VISIBLE

                    }
                }
            }

        }
    }

    fun updateAutopilotUI(autopilotStateVal: Int, steeringAngleVal: Int?) {

        var steeringAngle: Int

        if (steeringAngleVal == null) {
            steeringAngle = 0
        } else {
            steeringAngle = steeringAngleVal
        }
        if (lastAutopilotState != autopilotStateVal) {

            val fadeIn = AnimationUtils.loadAnimation(activity, R.anim.fade_in)
            val fadeOut = AnimationUtils.loadAnimation(activity, R.anim.fade_out)
            when {
                lastAutopilotState == 1 -> fadeOut.cancel()
                lastAutopilotState == 2 -> fadeIn.cancel()
                lastAutopilotState > 2 -> binding.autopilot.visibility = View.INVISIBLE
            }
            binding.autopilotInactive.clearAnimation()
            binding.autopilotInactive.visibility =
                if (autopilotStateVal > 2 ) View.INVISIBLE else View.VISIBLE
            when  {
                autopilotStateVal < 2 -> binding.autopilotInactive.startAnimation(fadeOut)
                autopilotStateVal > 7 -> binding.autopilotInactive.startAnimation(fadeOut)
                autopilotStateVal == 2 -> binding.autopilotInactive.startAnimation(fadeIn)
                autopilotStateVal > 2 -> binding.autopilot.visibility = View.VISIBLE
            }
        }
        if (autopilotStateVal > 2) {
            // set pivot to center of image
            binding.autopilot.pivotX = (binding.autopilot.width / 2).toFloat()
            binding.autopilot.pivotY = (binding.autopilot.height / 2).toFloat()
            binding.autopilot.rotation = steeringAngle.toFloat()
        }
        lastAutopilotState = autopilotStateVal
    }

    fun convertDPtoPixels(dp: Int): Int {
        val d = requireContext().resources.displayMetrics.density
        val margin = (dp * d).toInt() // margin in pixels
        return margin
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