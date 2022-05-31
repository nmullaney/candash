package app.candash.cluster

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.content.Context
import android.content.SharedPreferences
import android.content.res.Resources
import android.graphics.Color
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
    private var showSOC: Boolean = true
    private var uiSpeedUnitsMPH: Boolean = true
    private var power: Float = 0f
    private var battAmps: Float = 0f
    private var battVolts: Float = 0f
    private var doorOpen = false
    private var HRSPRS: Boolean = false
    private var l2Distance: Int = 200
    private var l1Distance: Int = 300
    private var gearState: Int = Constants.gearPark
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
    val Float.kmh: Float
        get() = (this / .621371).toFloat()
    val Float.mph: Float
        get() = (this * .621371).toFloat()

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
            binding.autopilotInactive
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

    private fun setPref(name: String, value: Float) {
        with(prefs.edit()) {
            putFloat(name, value)
            apply()
        }
    }

    private fun setBooleanPref(name: String, value: Boolean) {
        with(prefs.edit()) {
            putBoolean(name, value)
            apply()
        }
    }

    private fun prefContains(name: String): Boolean {
        return prefs.contains(name)
    }

    private fun getPref(name: String): Float {
        return prefs.getFloat(name, 0f)
    }

    private fun getBooleanPref(name: String): Boolean {
        return prefs.getBoolean(name, false)
    }

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
        var colorFrom: Int
        if (!prefContains(Constants.gaugeMode)) {
            setPref(Constants.gaugeMode, 2f)
        }
        if (getBooleanPref("forceNightMode")) {
            colorFrom = getBackgroundColor(0)
        } else {
            colorFrom = getBackgroundColor(isSunUp(viewModel))
        }
        uiSpeedUnitsMPH = getBooleanPref("uiSpeedUnitsMPH")

        HRSPRS = getBooleanPref("HRSPRS")
        val colorTo = requireContext().getColor(R.color.autopilot_blue)
        val bsColorTo = Color.parseColor("#FFEE0000")
        val autopilotAnimation = ValueAnimator.ofObject(ArgbEvaluator(), colorFrom, colorTo)
        val blindspotAnimation = ValueAnimator.ofObject(ArgbEvaluator(), colorFrom, bsColorTo)

        // milliseconds
        /*
        if (!isSplitScreen()) {
            for (topUIView in topUIViews()) {
                savedLayoutParams[topUIView] =
                    ConstraintLayout.LayoutParams(topUIView.layoutParams as ConstraintLayout.LayoutParams)
            }
        }

         */


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

        }
        viewModel.getValue(Constants.isSunUp)?.let { sunUpVal ->
            if (!prefContains("sunUp")) {
                setColors(sunUpVal.toInt())
                setPref("sunUp", sunUpVal)
            }
            if (getPref("sunUp") != sunUpVal) {
                setColors(sunUpVal.toInt())
                setPref("sunUp", sunUpVal)
            }
        }
        binding.minpower.setOnClickListener {
            setPref("minPower", 0f)
        }
        binding.maxpower.setOnClickListener {
            setPref("maxPower", 0f)
        }
        binding.root.setOnLongClickListener {
            viewModel.switchToInfoFragment()
            return@setOnLongClickListener true
        }
        binding.batterypercent.setOnClickListener {
            showSOC = !showSOC
            return@setOnClickListener
        }
        binding.unit.setOnClickListener {
            if (getPref(Constants.gaugeMode) < 2f) {
                setPref(Constants.gaugeMode, getPref(Constants.gaugeMode) + 1f)
            } else {
                setPref(Constants.gaugeMode, 0f)
            }
        }

        binding.power.setOnClickListener {

            setBooleanPref("HRSPRS", !getBooleanPref("HRSPRS"))
        }


        binding.speed.setOnLongClickListener {
            setBooleanPref("forceNightMode", !getBooleanPref("forceNightMode"))
            return@setOnLongClickListener true
        }

        viewModel.getSplitScreen().observe(viewLifecycleOwner) {
            val window: Window? = activity?.window


            if (view.windowToken != null) {
                if (it) {
                    // splitscreen is on
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        window?.insetsController?.hide(WindowInsets.Type.statusBars())
                        window?.insetsController?.systemBarsBehavior =
                            WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                        val wm = activity?.windowManager

                        wm?.removeViewImmediate(window?.getDecorView());
                        wm?.addView(window?.getDecorView(), window?.getAttributes());
                    }
                    /*
                    for (topUIView in topUIViews()) {
                        val params = topUIView.layoutParams as ConstraintLayout.LayoutParams
                        val savedParams = savedLayoutParams[topUIView]
                        params.setMargins(
                            savedParams!!.leftMargin,
                            savedParams.topMargin + 4.px,
                            savedParams.rightMargin,
                            savedParams.bottomMargin
                        )
                        topUIView.layoutParams = params
                    }

                     */
                    for (sideUIView in sideUIViews()) {
                        sideUIView.visibility = View.GONE
                    }
                } else {
                    //no splitscreen
                    /*
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
                     */
                    for (sideUIView in sideUIViews()) {
                        sideUIView.visibility = View.VISIBLE
                    }
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    window?.insetsController?.hide(WindowInsets.Type.statusBars())
                    window?.insetsController?.systemBarsBehavior =
                        WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                    val wm = activity?.windowManager

                    wm?.removeViewImmediate(window?.getDecorView());
                    wm?.addView(window?.getDecorView(), window?.getAttributes());
                }
            }
        }
        viewModel.carState().observe(viewLifecycleOwner) { it ->
            // set display night/day mode based on reported car status
            it.getValue(Constants.isSunUp)?.let { sunUpVal ->
                setColors(sunUpVal.toInt())
            }



            it.getValue(Constants.battAmps)?.let { battAmpsVal ->
                //batt amps and batt volts are on the same signal so if amps are there so are volts
                battAmps = battAmpsVal.toFloat()
            }
            it.getValue(Constants.battVolts)?.let { battVoltsVal ->
                //batt amps and batt volts are on the same signal so if amps are there so are volts
                battVolts = battVoltsVal.toFloat()
            }

            power = (battAmps * battVolts)
            if (power > getPref("maxPower")) setPref("maxPower", power)
            if (viewModel.getValue(Constants.chargeStatus)
                    ?.toInt() == Constants.chargeStatusInactive.toInt()
            ) {
                // do not store minpower if car is charging
                if (power < getPref("minPower")) setPref("minPower", power)
            }
            //binding.power.text = "%.2f".format(power)
            if (getPref(Constants.gaugeMode) > 0f) {
                binding.minpower.visibility = View.VISIBLE
                binding.maxpower.visibility = View.VISIBLE
                if (!getBooleanPref("HRSPRS")) {
                    binding.minpower.text = formatWatts(getPref("minPower"))
                    binding.maxpower.text = formatWatts(getPref("maxPower"))

                } else {
                    binding.minpower.text =
                        (getPref("minPower") * 0.00134102).toInt().toString() + " hp"
                    binding.maxpower.text =
                        (getPref("maxPower") * 0.00134102).toInt().toString() + " hp"

                }
            } else {
                binding.minpower.visibility = View.INVISIBLE
                binding.maxpower.visibility = View.INVISIBLE
            }
            if (!getBooleanPref("HRSPRS")) {
                binding.power.text = formatWatts(power)
            } else {
                binding.power.text = (power * 0.00134102).toInt().toString() + " hp"
            }
            if (power >= 0) {
                binding.powerBar.setGauge(((power / getPref("maxPower")).pow(0.7f)))
            } else {
                binding.powerBar.setGauge(-((abs(power) / abs(getPref("minPower"))).pow(0.7f)))
            }
            binding.powerBar.invalidate()


            viewModel.getValue(Constants.autopilotState)?.let { autopilotStateVal ->
                if (autopilotStateVal.toInt() > 2) {
                    gearColorSelected = requireContext().getColor(R.color.autopilot_blue)
                } else if (isSunUp(viewModel) == 1 && !getBooleanPref("forceNightMode")) {
                    gearColorSelected = Color.DKGRAY
                } else {
                    gearColorSelected = Color.LTGRAY
                }
            }
            viewModel.getValue(Constants.gearSelected)?.let { gearStateVal ->
                val gear: String = binding.PRND.text.toString()
                var ss = SpannableString(gear)
                var gearStartIndex = 0
                var gearEndIndex = 1
                gearState = gearStateVal.toInt()
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
            }
            /*
            it.getValue(Constants.maxSpeedAP)?.let { maxSpeedAPVal ->
                binding.displaymaxspeed.text = maxSpeedAPVal.toInt().toString() + " MPH"
            }

             */
            // hide performance gauges if user has elected to hide them or if splitscreen mode
            if ((getPref(Constants.gaugeMode) < 2f) || isSplitScreen()) {
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
                binding.frontbraketemp.text = frontBrakeTemp.toString()
                binding.frontbraketempgauge.setGauge(frontBrakeTemp.toFloat() / 984f)
                binding.fronttempgauge.invalidate()
            }
            if (it.containsKey(Constants.brakeTempRL) && it.containsKey(Constants.brakeTempRR)) {
                val rearBrakeTemp = max(
                    it.getValue(Constants.brakeTempRL)!!.toInt(),
                    it.getValue(Constants.brakeTempRR)!!.toInt()
                )
                binding.rearbraketemp.text = rearBrakeTemp.toString()
                binding.rearbraketempgauge.setGauge(rearBrakeTemp.toFloat() / 984f)
                binding.reartempgauge.invalidate()
            }
            it.getValue(Constants.frontTemp)?.let { frontTempVal ->
                binding.fronttemp.text = frontTempVal.toInt().toString()
                binding.fronttempgauge.setGauge(frontTempVal.toFloat() / 214f)
                binding.fronttempgauge.invalidate()
            }
            it.getValue(Constants.rearTemp)?.let { rearTempVal ->
                binding.reartemp.text = rearTempVal.toInt().toString()
                binding.reartempgauge.setGauge(rearTempVal.toFloat() / 214f)
                binding.reartempgauge.invalidate()
            }
            it.getValue(Constants.coolantFlow)?.let {
                binding.coolantflow.text = "%.1f".format(it.toFloat())
                binding.coolantflowgauge.setGauge(it.toFloat() / 40f)
                binding.coolantflowgauge.invalidate()
            }
            it.getValue(Constants.frontTorque)?.let {
                var frontTorqueVal = it.toFloat()
                if ((viewModel.getValue(Constants.gearSelected)
                        ?.toInt() == Constants.gearReverse)
                ) {
                    frontTorqueVal = -(frontTorqueVal)
                }

                binding.fronttorque.text = frontTorqueVal.toInt().toString()
                if (abs(getPref("frontTorqueMax")) < frontTorqueVal.toFloat()) {
                    setPref("frontTorqueMax", abs(frontTorqueVal.toFloat()))
                }
                binding.fronttorquegauge.setGauge(frontTorqueVal.toFloat() / getPref("frontTorqueMax"))
                binding.fronttorquegauge.invalidate()
            }
            it.getValue(Constants.rearTorque)?.let {
                var rearTorqueVal = it.toFloat()
                if ((viewModel.getValue(Constants.gearSelected)
                        ?.toInt() == Constants.gearReverse)
                ) {
                    rearTorqueVal = -(rearTorqueVal)
                }
                binding.reartorque.text = rearTorqueVal.toInt().toString()
                if (abs(getPref("rearTorqueMax")) < rearTorqueVal.toFloat()) {
                    setPref("rearTorqueMax", abs(rearTorqueVal.toFloat()))
                }
                binding.reartorquegauge.setGauge(rearTorqueVal.toFloat() / getPref("rearTorqueMax"))
                binding.reartorquegauge.invalidate()
            }
            it.getValue(Constants.battBrickMin)?.let {
                binding.batttemp.text = "%.1f".format(it.toFloat())
                binding.batttempgauge.setGauge((it.toFloat() + 40f) / 128)
            }
            it.getValue(Constants.autopilotHands)?.let { autopilotHandsVal ->
                if (getBooleanPref("forceNightMode")) {
                    colorFrom = getBackgroundColor(0)
                } else {
                    colorFrom = getBackgroundColor(isSunUp(viewModel))
                }
                //TODO: change colors to autopilot_blue constant
                if ((autopilotHandsVal.toInt() > 2) and (autopilotHandsVal.toInt() < 15)) {
                    binding.APWarning.clearAnimation()
                    if (getBooleanPref("forceNightMode")) {
                        colorFrom = getBackgroundColor(0)
                    } else {
                        colorFrom = getBackgroundColor(isSunUp(viewModel))
                    }
                    autopilotAnimation.setObjectValues(colorFrom, colorTo)
                    autopilotAnimation.duration = 1000

                    autopilotAnimation.addUpdateListener { animator ->
                        binding.root.setBackgroundColor(
                            animator.animatedValue as Int
                        )
                    }
                    autopilotAnimation.repeatCount = ValueAnimator.INFINITE
                    autopilotAnimation.repeatMode = ValueAnimator.REVERSE
                    if (autopilotHandsToggle == false) {
                        val fadeInWarning = AnimationUtils.loadAnimation(activity, R.anim.fade_in)
                        binding.APWarning.startAnimation(fadeInWarning)
                        binding.APWarning.visibility = View.VISIBLE

                        autopilotAnimation.start()
                        autopilotHandsToggle = true
                    } else {
                        binding.APWarning.clearAnimation()
                        binding.APWarning.visibility = View.VISIBLE
                        binding.root.setBackgroundColor(requireContext().getColor(R.color.autopilot_blue))

                    }
                } else {

                    autopilotAnimation.cancel()

                    binding.APWarning.clearAnimation()
                    binding.root.clearAnimation()
                    binding.root.setBackgroundColor(colorFrom)
                    autopilotHandsToggle = false
                    val fadeOutWarning = AnimationUtils.loadAnimation(activity, R.anim.fade_out)
                    if (binding.APWarning.visibility != View.GONE) {
                        binding.APWarning.startAnimation(fadeOutWarning)
                        binding.APWarning.visibility = View.GONE
                    }

                }


            }

            it.getValue(Constants.uiSpeed)?.let { vehicleSpeedVal ->
                var sensingSpeedLimit = 35
                binding.speed.scaleY = .9f

                binding.speed.text = vehicleSpeedVal.toInt().toString()

                if (viewModel.getValue(Constants.uiSpeedUnits) != 0f) {
                    sensingSpeedLimit = 35f.kmh.toInt()
                }
                if (vehicleSpeedVal.toInt() > sensingSpeedLimit) {
                    l1Distance = 400
                    l2Distance = 250
                } else {
                    l1Distance = 300
                    l2Distance = 200
                }
            }


            it.getValue(Constants.uiSpeedUnits)?.let { uiSpeedUnitsVal ->
                if (uiSpeedUnitsVal.toInt() == 0) {

                    uiSpeedUnitsMPH = true
                    setBooleanPref("uiSpeedUnitsMPH", true)
                    binding.unit.text = "MPH"
                } else {
                    uiSpeedUnitsMPH = false
                    setBooleanPref("uiSpeedUnitsMPH", false)
                    binding.unit.text = "KM/H"

                }
            }
            if (showSOC == true) {
                viewModel.getValue(Constants.stateOfCharge)?.let { stateOfChargeVal ->
                    if (binding.batterypercent.text != stateOfChargeVal.toInt().toString()) {
                        binding.batterypercent.text =
                            stateOfChargeVal.toInt().toString() + " %"
                    }

                }
            } else {
                if (uiSpeedUnitsMPH == true) {


                    viewModel.getValue(Constants.uiRange)?.let { stateOfChargeVal ->
                        binding.batterypercent.text =
                            stateOfChargeVal.toInt().toString() + " mi"

                    }
                } else {

                    viewModel.getValue(Constants.uiRange)?.let { stateOfChargeVal ->
                        binding.batterypercent.text =
                            ((stateOfChargeVal.toInt()) / .62).toInt().toString() + " km"
                    }
                }
            }
            processDoors()

            it.getValue(Constants.stateOfCharge)?.let { stateOfChargeVal ->
                binding.fullbattery.setGauge(stateOfChargeVal.toFloat())
                binding.fullbattery.invalidate()
            }



            it.getValue(Constants.autopilotState)?.let { autopilotStateVal ->
                updateAutopilotUI(
                    autopilotStateVal.toInt(),
                    it.getValue(Constants.steeringAngle)?.toInt()
                )
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
            }


            // check if AP is not engaged, otherwise blind spot supersedes the AP

            if (viewModel.getValue(Constants.autopilotState) != 3f) {
                val bsFadeIn = AnimationUtils.loadAnimation(activity, R.anim.fade_in)
                val bsFadeOut = AnimationUtils.loadAnimation(activity, R.anim.fade_out)
                var bsBinding = binding.BSWarningLeft
                var colorFrom = getBackgroundColor(1)
                if (getBooleanPref("forceNightMode")) {
                    colorFrom = getBackgroundColor(0)
                } else if (viewModel.getValue(Constants.isSunUp) != null) {
                    colorFrom = getBackgroundColor(viewModel.getValue(Constants.isSunUp)!!.toInt())
                }
                if (it.getValue(Constants.blindSpotLeft) in setOf(1f, 2f)) {
                    bsBinding = binding.BSWarningLeft
                } else if (it.getValue(Constants.blindSpotRight) in setOf(1f, 2f)) {
                    bsBinding = binding.BSWarningRight
                }
                if (binding.BSWarningLeft.visibility == View.VISIBLE) {
                    bsBinding = binding.BSWarningLeft
                } else if (binding.BSWarningRight.visibility == View.VISIBLE) {
                    bsBinding = binding.BSWarningRight
                }

                if ((it.getValue(Constants.blindSpotLeft) in setOf(1f, 2f)) or (it.getValue(
                        Constants.blindSpotRight
                    ) in setOf(1f, 2f))
                ) {

                    blindspotAnimation.setObjectValues(colorFrom, bsColorTo)
                    blindspotAnimation.duration = 250
                    // milliseconds

                    blindspotAnimation.addUpdateListener { animator ->
                        binding.root.setBackgroundColor(
                            animator.animatedValue as Int
                        )
                    }
                    blindspotAnimation.repeatCount = ValueAnimator.INFINITE
                    blindspotAnimation.repeatMode = ValueAnimator.REVERSE
                    blindspotAnimation.start()


                    bsBinding.startAnimation(bsFadeIn)
                    bsBinding.visibility = View.VISIBLE


                } else {
                    if (bsBinding.visibility != View.GONE) {
                        bsBinding.startAnimation(bsFadeOut)
                        bsBinding.visibility = View.GONE

                    }
                    blindspotAnimation.cancel()
                    binding.root.setBackgroundColor(colorFrom)
                }

            }
            if (gearState != Constants.gearPark) {
                it.getValue(Constants.leftVehicle)?.let { sensorVal ->
                    if ((sensorVal.toInt() < l1Distance) and (sensorVal.toInt() >= l2Distance)) {
                        binding.blindSpotLeft1a.visibility = View.VISIBLE
                    } else if (sensorVal.toInt() < l2Distance) {
                        binding.blindSpotLeft2a.visibility = View.VISIBLE
                    } else {
                        binding.blindSpotLeft1a.visibility = View.INVISIBLE
                        binding.blindSpotLeft2a.visibility = View.INVISIBLE
                    }
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
                }
            } else {
                // in park


                binding.blindSpotLeft1a.visibility = View.INVISIBLE
                binding.blindSpotLeft2a.visibility = View.INVISIBLE
                binding.blindSpotRight1a.visibility = View.INVISIBLE
                binding.blindSpotRight2a.visibility = View.INVISIBLE
            }


            it.getValue(Constants.chargeStatus)?.let { chargeStatusVal ->
                if (!isSplitScreen()) {
                    if (chargeStatusVal != Constants.chargeStatusInactive) {
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
                                binding.chargerate.text = formatWatts(abs(battAmps * battVolts))
                                binding.chargerate.visibility = View.VISIBLE

                            }


                    } else {
                        binding.chargemeter.visibility = View.INVISIBLE
                        binding.bigsoc.visibility = View.INVISIBLE
                        binding.bigsocpercent.visibility = View.INVISIBLE

                        binding.chargerate.visibility = View.INVISIBLE

                        for (chargingHiddenView in chargingHiddenViews()) {
                            chargingHiddenView.visibility = View.VISIBLE
                        }
                        if (getPref(Constants.gaugeMode) > 0f) {
                            for (minMaxchargingHiddenView in minMaxchargingHiddenViews()) {
                                minMaxchargingHiddenView.visibility = View.VISIBLE
                            }
                        }
                    }
                } else {
                    if (chargeStatusVal != Constants.chargeStatusInactive) {
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
                        binding.chargemeter.visibility = View.INVISIBLE
                        binding.bigsoc.visibility = View.INVISIBLE
                        binding.bigsocpercent.visibility = View.INVISIBLE

                        binding.chargerate.visibility = View.INVISIBLE
                        if (!doorOpen){
                            for (chargingHiddenView in chargingHiddenViews()) {
                                chargingHiddenView.visibility = View.VISIBLE
                            }
                        }
                        if (getPref(Constants.gaugeMode) > 0f) {
                            for (minMaxchargingHiddenView in minMaxchargingHiddenViews()) {
                                minMaxchargingHiddenView.visibility = View.VISIBLE
                            }
                        }
                    }
                }
            }
        }


    }


    fun processDoors() {

        if ((viewModel.getValue(Constants.liftgateState) in setOf(1f, 4f)) or
            (viewModel.getValue(Constants.frunkState) in setOf(1f, 4f)) or
            (viewModel.getValue(Constants.frontLeftDoorState) in setOf(1f, 4f)) or
            (viewModel.getValue(Constants.frontRightDoorState) in setOf(1f, 4f)) or
            (viewModel.getValue(Constants.rearLeftDoorState) in setOf(1f, 4f)) or
            (viewModel.getValue(Constants.rearRightDoorState) in setOf(1f, 4f))
        ) {
            doorOpen = true
            updateCarStateUI(doorOpen)

            displayOpenDoors()
        } else if ((viewModel.getValue(Constants.liftgateState) == 2f) and
            (viewModel.getValue(Constants.frunkState) == 2f) and
            (viewModel.getValue(Constants.frontLeftDoorState) == 2f) and
            (viewModel.getValue(Constants.frontRightDoorState) == 2f) and
            (viewModel.getValue(Constants.rearLeftDoorState) == 2f) and
            (viewModel.getValue(Constants.rearRightDoorState) == 2f)
        ) {
            displayOpenDoors()
            doorOpen = false
            updateCarStateUI(doorOpen)
        }
    }

    fun displayOpenDoors() {
        if (!isSplitScreen()) {
            viewModel.getValue(Constants.liftgateState)?.let { liftgateVal ->
                if (liftgateVal.toInt() in setOf(1, 4)) {
                    binding.hatch.visibility = View.VISIBLE
                } else {
                    binding.hatch.visibility = View.GONE
                }

            }

            viewModel.getValue(Constants.frunkState)?.let { frunkVal ->

                if (frunkVal.toInt() in setOf(1, 4)) {
                    binding.hood.visibility = View.VISIBLE
                } else {
                    binding.hood.visibility = View.GONE
                }

            }

            viewModel.getValue(Constants.frontLeftDoorState)?.let { frontLeftDoorVal ->

                if (frontLeftDoorVal.toInt() in setOf(1, 4)) {
                    binding.frontleftdoor.visibility = View.VISIBLE
                } else {
                    binding.frontleftdoor.visibility = View.GONE
                }

            }
            viewModel.getValue(Constants.frontRightDoorState)?.let { frontRightDoorVal ->

                if (frontRightDoorVal.toInt() in setOf(1, 4)) {
                    binding.frontrightdoor.visibility = View.VISIBLE
                } else {
                    binding.frontrightdoor.visibility = View.GONE
                }

            }
            viewModel.getValue(Constants.rearLeftDoorState)?.let { rearLeftDoorVal ->

                if (rearLeftDoorVal.toInt() in setOf(1, 4)) {
                    binding.rearleftdoor.visibility = View.VISIBLE
                } else {
                    binding.rearleftdoor.visibility = View.GONE
                }

            }
            viewModel.getValue(Constants.rearRightDoorState)?.let { rearRightDoorVal ->

                if (rearRightDoorVal.toInt() in setOf(1, 4)) {
                    binding.rearrightdoor.visibility = View.VISIBLE
                } else {
                    binding.rearrightdoor.visibility = View.GONE
                }

            }
        } else {
            viewModel.getValue(Constants.liftgateState)?.let { liftgateVal ->
                if (liftgateVal.toInt() in setOf(1, 3)) {
                    binding.hatchCenter.visibility = View.VISIBLE
                } else {
                    binding.hatchCenter.visibility = View.GONE
                }

            }

            viewModel.getValue(Constants.frunkState)?.let { frunkVal ->

                if (frunkVal.toInt() in setOf(1, 4)) {
                    binding.hoodCenter.visibility = View.VISIBLE
                } else {
                    binding.hoodCenter.visibility = View.GONE
                }

            }

            viewModel.getValue(Constants.frontLeftDoorState)?.let { frontLeftDoorVal ->

                if (frontLeftDoorVal.toInt() in setOf(1, 4)) {
                    binding.frontleftdoorCenter.visibility = View.VISIBLE
                } else {
                    binding.frontleftdoorCenter.visibility = View.GONE
                }

            }
            viewModel.getValue(Constants.frontRightDoorState)?.let { frontRightDoorVal ->

                if (frontRightDoorVal.toInt() in setOf(1, 4)) {
                    binding.frontrightdoorCenter.visibility = View.VISIBLE
                } else {
                    binding.frontrightdoorCenter.visibility = View.GONE
                }

            }
            viewModel.getValue(Constants.rearLeftDoorState)?.let { rearLeftDoorVal ->

                if (rearLeftDoorVal.toInt() in setOf(1, 4)) {
                    binding.rearleftdoorCenter.visibility = View.VISIBLE
                } else {
                    binding.rearleftdoorCenter.visibility = View.GONE
                }

            }
            viewModel.getValue(Constants.rearRightDoorState)?.let { rearRightDoorVal ->

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
        if (sunUpVal == 0 || getBooleanPref("forceNightMode")) {
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
            //window?.statusBarColor = Color.BLACK
            binding.root.setBackgroundColor(Color.BLACK)
            //binding.speed.setTypeface(resources.getFont(R.font.orbitronlight), Typeface.NORMAL )
            binding.speed.setTextColor(Color.WHITE)
            binding.bigsoc.setTextColor(Color.WHITE)
            binding.bigsocpercent.setTextColor(Color.WHITE)

            binding.chargerate.setTextColor(Color.LTGRAY)
            binding.chargemeter.setDayValue(0)
            binding.unit.setTextColor(Color.LTGRAY)
            binding.batterypercent.setTextColor(Color.LTGRAY)
            binding.deadbattery.setColorFilter(Color.DKGRAY)
            gearColorSelected = Color.LTGRAY
            gearColor = Color.DKGRAY
            binding.PRND.setTextColor(Color.DKGRAY)
            binding.modely.setColorFilter(Color.LTGRAY)
            binding.modelyCenter.setColorFilter(Color.LTGRAY)

            binding.power.setTextColor(Color.WHITE)
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
            //window?.statusBarColor = Color.parseColor("#FFEEEEEE")
            binding.speed.setTextColor(Color.BLACK)
            binding.speed.setTextColor(Color.BLACK)
            binding.bigsoc.setTextColor(Color.BLACK)
            binding.bigsocpercent.setTextColor(Color.BLACK)
            binding.chargerate.setTextColor(Color.DKGRAY)
            binding.chargemeter.setDayValue(1)

            binding.unit.setTextColor(Color.GRAY)
            binding.batterypercent.setTextColor(Color.DKGRAY)
            binding.deadbattery.clearColorFilter()
            gearColorSelected = Color.DKGRAY
            gearColor = Color.parseColor("#FFDDDDDD")
            binding.PRND.setTextColor(Color.parseColor("#FFDDDDDD"))
            binding.modely.setColorFilter(Color.GRAY)
            binding.modelyCenter.setColorFilter(Color.GRAY)

            binding.power.setTextColor(Color.DKGRAY)
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

    fun formatWatts(power: Float): String {
        if ((abs(power) < 10000)) {
            return "%.1f".format(power / 1000f) + " kW"
        } else {
            return (power / 1000f).toInt().toString() + " kW"
        }
    }

    fun updateCarStateUI(doorOpen: Boolean) {
        val fadeIn = AnimationUtils.loadAnimation(activity, R.anim.fade_in)
        val fadeOut = AnimationUtils.loadAnimation(activity, R.anim.fade_out)
        val fadeInCenter = AnimationUtils.loadAnimation(activity, R.anim.fade_in)

        fadeIn.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation?) {
                if (getPref(Constants.gaugeMode) == 2f) {
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
                        if (getPref(Constants.gaugeMode) == 2f) {
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
                autopilotStateVal == 1 -> binding.autopilotInactive.startAnimation(fadeOut)
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


}