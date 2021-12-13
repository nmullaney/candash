package dev.nmullaney.tesladashboard

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.graphics.drawable.AnimationDrawable
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.view.*
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import dev.nmullaney.tesladashboard.databinding.FragmentDashBinding
import kotlin.math.abs
import kotlin.math.pow


class DashFragment : Fragment() {

    private val TAG = DashViewModel::class.java.simpleName


    private lateinit var binding: FragmentDashBinding

    private lateinit var viewModel: DashViewModel

    private var rearLeftVehDetected: Int = 500
    private var rearRightVehDetected: Int = 500
    private var leftVehDetected: Int = 500
    private var rightVehDetected: Int = 500
    private var gearColor: Int = Color.parseColor("#FFEEEEEE")
    private var gearColorSelected: Int = Color.DKGRAY
    private var lastAutopilotState: Int = 0
    private var lastDoorOpen: Boolean = false
    private var autopilotHandsToggle: Boolean = false
    private var blindSpotAlertToggle: Boolean = false
    private var lastSunUp: Int = 1
    private var showSOC: Boolean = true
    private var uiSpeedUnitsMPH: Boolean = true
    private var power: Float = 0f
    private var battAmps: Float = 0f
    private var battVolts: Float = 0f
    private var minPower: Float = 0f
    private var maxPower: Float = 0f
    private var HRSPRS: Boolean = false
    private var maxVehiclePower: Int = 350000
    private var forceNightMode: Boolean = false
    private var l2Distance: Int = 200
    private var l1Distance: Int = 300
    private var gearState: Int = Constants.gearPark
    private var vehicleSpeed: Int = 0

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentDashBinding.inflate(inflater, container, false)
        return binding.root
    }

    fun getBackgroundColor(sunUpVal: Int): Int {
        return when (sunUpVal) {
            1 -> requireContext().getColor(R.color.day_background)
            else -> requireContext().getColor(R.color.night_background)
        }
    }
    private fun hideSystemBars(window: Window) {
        val windowInsetsController =
            ViewCompat.getWindowInsetsController(window?.decorView) ?: return
        // Configure the behavior of the hidden system bars
        windowInsetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        // Hide both the status bar and the navigation bar
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(requireActivity()).get(DashViewModel::class.java)


        binding.root.setOnLongClickListener {
            viewModel.switchToInfoFragment()
            return@setOnLongClickListener true
        }
        binding.batterypercent.setOnClickListener {
            showSOC = !showSOC
            return@setOnClickListener
        }

        binding.power.setOnClickListener {
            Log.d(TAG, "HRSPRS: " + HRSPRS.toString())

            HRSPRS = !HRSPRS
        }
        binding.minpower.setOnClickListener {
            minPower = 0f
        }
        binding.maxpower.setOnClickListener {
            maxPower = 0f
        }
        binding.speed.setOnClickListener {
            forceNightMode = !forceNightMode
        }
        binding.powerBar.getSplitScreen().observe(viewLifecycleOwner) {
            val window: Window? = activity?.window


            if (it) {

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    window?.insetsController?.hide(WindowInsets.Type.statusBars())
                    window?.insetsController?.systemBarsBehavior =
                        WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                    val wm = activity?.windowManager

                    wm?.removeViewImmediate(window?.getDecorView());
                    wm?.addView(window?.getDecorView(), window?.getAttributes());
                }
                var params = binding.PRND.layoutParams as ConstraintLayout.LayoutParams
                // shift down by 24dp
                params.setMargins(
                    0,
                    convertDPtoPixels(24),
                    0,
                    0
                ) //substitute parameters for left, top, right, bottom
                binding.PRND.layoutParams = params

                params = binding.batterypercent.layoutParams as ConstraintLayout.LayoutParams
                params.setMargins(
                    0,
                    convertDPtoPixels(24),
                    0,
                    0
                ) //substitute parameters for left, top, right, bottom
                binding.batterypercent.layoutParams = params

                params = binding.deadbattery.layoutParams as ConstraintLayout.LayoutParams
                params.setMargins(
                    0,
                    convertDPtoPixels(24),
                    0,
                    0
                ) //substitute parameters for left, top, right, bottom
                binding.deadbattery.layoutParams = params

                params = binding.deadbatterymask.layoutParams as ConstraintLayout.LayoutParams
                params.setMargins(
                    0,
                    convertDPtoPixels(24),
                    0,
                    0
                ) //substitute parameters for left, top, right, bottom
                binding.deadbatterymask.layoutParams = params

                params = binding.fullbattery.layoutParams as ConstraintLayout.LayoutParams
                params.setMargins(
                    0,
                    convertDPtoPixels(24),
                    0,
                    0
                ) //substitute parameters for left, top, right, bottom
                binding.fullbattery.layoutParams = params

                params = binding.leftTurnSignal.layoutParams as ConstraintLayout.LayoutParams
                params.setMargins(
                    convertDPtoPixels(10),
                    convertDPtoPixels(64),
                    0,
                    0
                ) //substitute parameters for left, top, right, bottom
                binding.leftTurnSignal.layoutParams = params

                params = binding.rightTurnSignal.layoutParams as ConstraintLayout.LayoutParams
                params.setMargins(
                    0,
                    convertDPtoPixels(64),
                    convertDPtoPixels(10),
                    0
                ) //substitute parameters for left, top, right, bottom
                binding.rightTurnSignal.layoutParams = params
            } else {
                var params = binding.PRND.layoutParams as ConstraintLayout.LayoutParams
                // shift down by 24dp
                params.setMargins(0, 0, 0, 0) //substitute parameters for left, top, right, bottom
                binding.PRND.layoutParams = params

                params = binding.batterypercent.layoutParams as ConstraintLayout.LayoutParams
                params.setMargins(
                    0,
                    convertDPtoPixels(0),
                    0,
                    0
                ) //substitute parameters for left, top, right, bottom
                binding.batterypercent.layoutParams = params

                params = binding.deadbattery.layoutParams as ConstraintLayout.LayoutParams
                params.setMargins(
                    0,
                    convertDPtoPixels(0),
                    0,
                    0
                ) //substitute parameters for left, top, right, bottom
                binding.deadbattery.layoutParams = params

                params = binding.deadbatterymask.layoutParams as ConstraintLayout.LayoutParams
                params.setMargins(
                    0,
                    convertDPtoPixels(0),
                    0,
                    0
                ) //substitute parameters for left, top, right, bottom
                binding.deadbatterymask.layoutParams = params

                params = binding.fullbattery.layoutParams as ConstraintLayout.LayoutParams
                params.setMargins(
                    0,
                    convertDPtoPixels(0),
                    0,
                    0
                ) //substitute parameters for left, top, right, bottom
                binding.fullbattery.layoutParams = params

                params = binding.leftTurnSignal.layoutParams as ConstraintLayout.LayoutParams
                params.setMargins(
                    convertDPtoPixels(10),
                    convertDPtoPixels(40),
                    0,
                    0
                ) //substitute parameters for left, top, right, bottom
                binding.leftTurnSignal.layoutParams = params

                params = binding.rightTurnSignal.layoutParams as ConstraintLayout.LayoutParams
                params.setMargins(
                    0,
                    convertDPtoPixels(40),
                    convertDPtoPixels(10),
                    0
                ) //substitute parameters for left, top, right, bottom
                binding.rightTurnSignal.layoutParams = params
            }
            val wm = activity?.windowManager
            wm?.removeViewImmediate(window?.getDecorView());
            wm?.addView(window?.getDecorView(), window?.getAttributes());
        }
        viewModel.carState().observe(viewLifecycleOwner) {

            it.getValue(Constants.isSunUp)?.let { sunUpVal ->
                setColors(sunUpVal.toInt())
            }

            // get battery status to decide whether or not to disable screen dimming
            var batteryStatus: Intent? =
                IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { ifilter ->
                    context?.registerReceiver(null, ifilter)
                }
            // How are we charging?
            val chargePlug: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1) ?: -1
            val isPlugged: Boolean = chargePlug == BatteryManager.BATTERY_PLUGGED_USB
                    || chargePlug == BatteryManager.BATTERY_PLUGGED_AC
                    || chargePlug == BatteryManager.BATTERY_PLUGGED_WIRELESS
            Log.d(TAG, "keep_screen_on" + isPlugged.toString())

            if (isPlugged) {
                activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                Log.d(TAG, "keep_screen_on")
            } else {
                activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                Log.d(TAG, "do not keep screen on")
            }
            it.getValue(Constants.battAmps)?.let { battAmpsStateVal ->
                battAmps = battAmpsStateVal.toFloat()
            }
            it.getValue(Constants.battVolts)?.let { battVoltsStateVal ->
                battVolts = battVoltsStateVal.toFloat()
            }
            power = (battAmps * battVolts)
            if (power > maxPower) maxPower = power
            if (power < minPower) minPower = power
            //binding.power.text = "%.2f".format(power)

            if (!HRSPRS) {
                binding.power.text = formatWatts(power)
                binding.minpower.text = formatWatts(minPower)
                binding.maxpower.text = formatWatts(maxPower)

            } else {
                binding.power.text = (power * 0.00134102).toInt().toString() + " hp"
                binding.minpower.text = (minPower * 0.00134102).toInt().toString() + " hp"
                binding.maxpower.text = (maxPower * 0.00134102).toInt().toString() + " hp"

            }
            if (power >= 0) {
                binding.powerBar.setGauge(((power / maxVehiclePower).pow(0.7f)))
            } else {
                binding.powerBar.setGauge(-((abs(power) / maxVehiclePower).pow(0.7f)))
            }

            binding.powerBar.invalidate()


            it.getValue(Constants.autopilotState)?.let { autopilotStateVal ->
                if (autopilotStateVal.toInt() > 2) {
                    gearColorSelected = requireContext().getColor(R.color.autopilot_blue)
                } else if (lastSunUp == 1 && !forceNightMode) {
                    gearColorSelected = Color.DKGRAY
                } else {
                    gearColorSelected = Color.LTGRAY
                }
            }
            it.getValue(Constants.gearSelected)?.let { gearStateVal ->
                val gear: String = binding.PRND.text.toString()
                var ss = SpannableString(gear)
                var gearStartIndex = 0
                var gearEndIndex = 1
                gearState = gearStateVal.toInt()
                if (gearStateVal.toInt() == Constants.gearPark) {
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
            it.getValue(Constants.autopilotHands)?.let { autopilotHandsVal ->
                val colorFrom: Int
                val fadeInWarning = AnimationUtils.loadAnimation(activity, R.anim.fade_in)
                val fadeOutWarning = AnimationUtils.loadAnimation(activity, R.anim.fade_out)
                if (forceNightMode) {
                    colorFrom = getBackgroundColor(0)
                } else {
                    colorFrom = getBackgroundColor(lastSunUp)
                }
                //TODO: change colors to autopilot_blue constant
                if ((autopilotHandsVal.toInt() > 2) and (autopilotHandsVal.toInt() < 15)) {
                    binding.APWarning.clearAnimation()
                    if (autopilotHandsToggle == false) {
                        binding.APWarning.startAnimation(fadeInWarning)
                        binding.APWarning.visibility = View.VISIBLE

                        val colorTo = requireContext().getColor(R.color.autopilot_blue)
                        val colorAnimation =
                            ValueAnimator.ofObject(ArgbEvaluator(), colorFrom, colorTo)
                        colorAnimation.duration = 2000
                        // milliseconds

                        colorAnimation.addUpdateListener { animator ->
                            binding.root.setBackgroundColor(
                                animator.animatedValue as Int
                            )
                        }
                        colorAnimation.start()
                        autopilotHandsToggle = true
                    } else {
                        binding.APWarning.clearAnimation()
                        binding.APWarning.visibility = View.VISIBLE
                        binding.root.setBackgroundColor(requireContext().getColor(R.color.autopilot_blue))

                    }
                } else {
                    binding.APWarning.clearAnimation()
                    binding.root.setBackgroundColor(colorFrom)
                    autopilotHandsToggle = false
                    binding.APWarning.startAnimation(fadeOutWarning)
                    binding.APWarning.visibility = View.GONE

                }


            }
            if (gearState != Constants.gearPark) {

                it.getValue(Constants.uiSpeed)?.let { vehicleSpeedVal ->
                    binding.unit.visibility = View.VISIBLE
                    binding.speed.visibility = View.VISIBLE
                    if (vehicleSpeed != vehicleSpeedVal.toInt()) {
                        binding.speed.text = vehicleSpeedVal.toInt().toString()
                        var sensingSpeedLimit: Int = 35
                        if (!uiSpeedUnitsMPH) {
                            sensingSpeedLimit = 56
                        }
                        if (vehicleSpeedVal.toInt() > sensingSpeedLimit) {
                            l1Distance = 400
                            l2Distance = 250
                        } else {
                            l1Distance = 300
                            l2Distance = 200
                        }
                        vehicleSpeed = vehicleSpeedVal.toInt()
                    }
                }
            } else {
                binding.unit.visibility = View.GONE
                binding.speed.visibility = View.GONE
            }

            it.getValue(Constants.uiSpeedUnits)?.let { uiSpeedUnitsVal ->
                uiSpeedUnitsMPH = uiSpeedUnitsVal.toInt() == 0
            }
            if (showSOC == true) {
                it.getValue(Constants.stateOfCharge)?.let { stateOfChargeVal ->
                    binding.batterypercent.text =
                        stateOfChargeVal.toInt().toString() + " %"
                }
            } else {
                if (uiSpeedUnitsMPH == true) {
                    binding.unit.text = "MPH"

                    it.getValue(Constants.uiRange)?.let { stateOfChargeVal ->
                        binding.batterypercent.text =
                            stateOfChargeVal.toInt().toString() + " mi"

                    }
                } else {
                    binding.unit.text =
                        "KPH"

                    it.getValue(Constants.uiRange)?.let { stateOfChargeVal ->
                        binding.batterypercent.text =
                            ((stateOfChargeVal.toInt()) / .62).toString() + " km"
                    }
                }
            }
            processDoors(it)

            it.getValue(Constants.stateOfCharge)?.let { stateOfChargeVal ->
                binding.fullbattery.scrollX =
                    (83 - ((stateOfChargeVal.toLong() * 83) / 100).toInt())
            }

            it.getValue(Constants.autopilotState)?.let { autopilotStateVal ->
                updateAutopilotUI(
                    autopilotStateVal.toInt(),
                    it.getValue(Constants.steeringAngle)?.toInt()
                )
            }

            it.getValue(Constants.turnSignalLeft)?.let { leftTurnSignalVal ->
                binding.leftTurnSignal.setBackgroundResource(R.drawable.left_turn_anim)
                var turnSignalAnimation =
                    binding.leftTurnSignal.background as AnimationDrawable
                if (leftTurnSignalVal.toInt() > 0) {
                    binding.leftTurnSignal.visibility = View.VISIBLE
                    turnSignalAnimation.start()
                } else {
                    turnSignalAnimation.stop()
                    binding.leftTurnSignal.visibility = View.INVISIBLE
                }
            }
            it.getValue(Constants.turnSignalRight)?.let { rightTurnSignalVal ->
                binding.rightTurnSignal.setBackgroundResource(R.drawable.right_turn_anim)
                var turnSignalAnimation =
                    binding.rightTurnSignal.background as AnimationDrawable
                if (rightTurnSignalVal.toInt() > 0) {
                    binding.rightTurnSignal.visibility = View.VISIBLE
                    turnSignalAnimation.start()
                } else {
                    turnSignalAnimation.stop()
                    binding.rightTurnSignal.visibility = View.INVISIBLE
                }
            }
            // check if AP is not engaged, otherwise blind spot supersedes the AP
            if (gearColorSelected != requireContext().getColor(R.color.autopilot_blue)) {

                it.getValue(Constants.blindSpotLeft)?.let { blindSpotLeftVal ->
                    val colorFrom: Int
                    if (forceNightMode) {
                        colorFrom = getBackgroundColor(0)
                    } else {
                        colorFrom = getBackgroundColor(lastSunUp)
                    }

                    if ((blindSpotLeftVal.toInt() >= 1) and (blindSpotLeftVal.toInt() <= 2)) {

                        if (blindSpotAlertToggle == false) {

                            val colorTo = Color.parseColor("#FFEE0000")
                            val colorAnimation =
                                ValueAnimator.ofObject(ArgbEvaluator(), colorFrom, colorTo)
                            colorAnimation.duration = 250
                            // milliseconds

                            colorAnimation.addUpdateListener { animator ->
                                binding.root.setBackgroundColor(
                                    animator.animatedValue as Int
                                )
                            }
                            colorAnimation.start()
                            blindSpotAlertToggle = true
                        } else {
                            binding.root.setBackgroundColor(Color.parseColor("#FFEE0000"))

                        }
                    } else {
                        binding.root.setBackgroundColor(colorFrom)
                        blindSpotAlertToggle = false
                    }
                }
                it.getValue(Constants.blindSpotRight)?.let { blindSpotRightVal ->
                    val colorFrom: Int
                    if (forceNightMode) {
                        colorFrom = getBackgroundColor(0)
                    } else {
                        colorFrom = getBackgroundColor(lastSunUp)
                    }
                    if ((blindSpotRightVal.toInt() >= 1) and (blindSpotRightVal.toInt() <= 2)) {
                        if (blindSpotAlertToggle == false) {

                            val colorTo = Color.parseColor("#FFEE0000")
                            val colorAnimation =
                                ValueAnimator.ofObject(ArgbEvaluator(), colorFrom, colorTo)
                            colorAnimation.duration = 250
                            // milliseconds

                            colorAnimation.addUpdateListener { animator ->
                                binding.root.setBackgroundColor(
                                    animator.animatedValue as Int
                                )
                            }
                            colorAnimation.start()
                            blindSpotAlertToggle = true
                        } else {
                            binding.root.setBackgroundColor(Color.parseColor("#FFEE0000"))

                        }
                    } else {
                        binding.root.setBackgroundColor(colorFrom)
                        blindSpotAlertToggle = false
                    }
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
                binding.blindSpotLeft1a.visibility = View.INVISIBLE
                binding.blindSpotLeft2a.visibility = View.INVISIBLE
                binding.blindSpotRight1a.visibility = View.INVISIBLE
                binding.blindSpotRight2a.visibility = View.INVISIBLE
            }


        }
    }


    fun processDoors(it: CarState) {

        var doorOpen = false
        it.getValue(Constants.liftgateState)?.let { liftgateVal ->

            if (liftgateVal.toInt() == 1) {
                binding.hatch.visibility = View.VISIBLE
                doorOpen = true
            } else {
                binding.hatch.visibility = View.GONE
            }

        }
        it.getValue(Constants.frunkState)?.let { frunkVal ->

            if (frunkVal.toInt() == 1) {
                binding.hood.visibility = View.VISIBLE
                doorOpen = true
            } else {
                binding.hood.visibility = View.GONE
            }

        }

        it.getValue(Constants.frontLeftDoorState)?.let { frontLeftDoorVal ->

            if (frontLeftDoorVal.toInt() == 1) {
                binding.frontleftdoor.visibility = View.VISIBLE
                doorOpen = true
            } else {
                binding.frontleftdoor.visibility = View.GONE
            }

        }
        it.getValue(Constants.frontRightDoorState)?.let { frontRightDoorVal ->

            if (frontRightDoorVal.toInt() == 1) {
                binding.frontrightdoor.visibility = View.VISIBLE
                doorOpen = true
            } else {
                binding.frontrightdoor.visibility = View.GONE
            }

        }
        it.getValue(Constants.rearLeftDoorState)?.let { rearLeftDoorVal ->

            if (rearLeftDoorVal.toInt() == 1) {
                binding.rearleftdoor.visibility = View.VISIBLE
                doorOpen = true
            } else {
                binding.rearleftdoor.visibility = View.GONE
            }

        }
        it.getValue(Constants.rearRightDoorState)?.let { rearRightDoorVal ->

            if (rearRightDoorVal.toInt() == 1) {
                binding.rearrightdoor.visibility = View.VISIBLE
                doorOpen = true
            } else {
                binding.rearrightdoor.visibility = View.GONE
            }

        }
        updateCarStateUI(doorOpen)
    }

    fun setColors(sunUpVal: Int) {
        val window: Window? = activity?.window

        // Not using dark-mode for compatibility with older version of Android (pre-29)
        if (sunUpVal == 0 || forceNightMode) {

            binding.powerBar.setDayValue(0)
            //window?.statusBarColor = Color.BLACK
            binding.root.setBackgroundColor(Color.BLACK)
            //binding.speed.setTypeface(resources.getFont(R.font.orbitronlight), Typeface.NORMAL )
            binding.speed.setTextColor(Color.WHITE)
            binding.unit.setTextColor(Color.LTGRAY)
            binding.batterypercent.setTextColor(Color.LTGRAY)
            binding.deadbattery.setColorFilter(Color.DKGRAY)
            binding.deadbatterymask.setColorFilter(Color.DKGRAY)
            binding.fullbattery.setColorFilter(Color.LTGRAY)
            gearColorSelected = Color.LTGRAY
            gearColor = Color.DKGRAY
            binding.PRND.setTextColor(Color.DKGRAY)
            binding.modely.setColorFilter(Color.LTGRAY)
            binding.power.setTextColor(Color.WHITE)
            binding.minpower.setTextColor(Color.WHITE)
            binding.maxpower.setTextColor(Color.WHITE)

            //binding.displaymaxspeed.setTextColor(Color.WHITE)


        } else {
            binding.powerBar.setDayValue(1)

            //view.setBackgroundColor(Color.parseColor("#"+Integer.toString(R.color.day_background, 16)))
            binding.root.setBackgroundColor(Color.parseColor("#FFEEEEEE"))
            //window?.statusBarColor = Color.parseColor("#FFEEEEEE")

            binding.speed.setTextColor(Color.BLACK)
            binding.unit.setTextColor(Color.DKGRAY)
            binding.batterypercent.setTextColor(Color.DKGRAY)
            binding.deadbattery.clearColorFilter()
            binding.deadbatterymask.clearColorFilter()
            binding.fullbattery.clearColorFilter()
            gearColorSelected = Color.DKGRAY
            gearColor = Color.parseColor("#FFDDDDDD")
            binding.PRND.setTextColor(Color.parseColor("#FFDDDDDD"))
            binding.modely.setColorFilter(Color.LTGRAY)
            binding.power.setTextColor(Color.DKGRAY)
            binding.minpower.setTextColor(Color.DKGRAY)
            binding.maxpower.setTextColor(Color.DKGRAY)
            //binding.displaymaxspeed.setTextColor(Color.BLACK)

        }
        val wm = activity?.windowManager

        lastSunUp = sunUpVal
    }

    fun formatWatts(power: Float): String {
        if (abs(power) < 1000) {
            return "${power.toInt()} W"
        } else if ((abs(power) >= 1000) and (abs(power) < 10000)) {
            "%.2f".format(power / 1000f)
            return "%.2f".format(power / 1000f) + " kW"
        } else {
            return (power / 1000f).toInt().toString() + " kW"
        }
    }

    fun updateCarStateUI(doorOpen: Boolean) {
        if (lastDoorOpen != doorOpen) {
            val fadeIn = AnimationUtils.loadAnimation(activity, R.anim.fade_in)
            val fadeOut = AnimationUtils.loadAnimation(activity, R.anim.fade_out)
            binding.modely.clearAnimation()
            if (doorOpen) {
                binding.modely.startAnimation(fadeIn)
            } else {
                binding.modely.startAnimation(fadeOut)
            }
        }
        lastDoorOpen = doorOpen
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
            when (lastAutopilotState) {
                1 -> fadeOut.cancel()
                2 -> fadeIn.cancel()
                3 -> binding.autopilot.visibility = View.INVISIBLE
            }
            binding.autopilotInactive.clearAnimation()
            binding.autopilotInactive.visibility =
                if (autopilotStateVal == 3) View.INVISIBLE else View.VISIBLE
            when (autopilotStateVal) {
                1 -> binding.autopilotInactive.startAnimation(fadeOut)
                2 -> binding.autopilotInactive.startAnimation(fadeIn)
                3 -> binding.autopilot.visibility = View.VISIBLE
            }
        }
        if (autopilotStateVal == 3) {

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