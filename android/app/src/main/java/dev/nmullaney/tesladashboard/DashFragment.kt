package dev.nmullaney.tesladashboard

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.TransitionDrawable
import android.os.BatteryManager
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.animation.AnimationUtils
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import dev.nmullaney.tesladashboard.databinding.FragmentDashBinding


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
    private var lastAutopilotState: Int = 1
    private var autopilotHandsToggle: Boolean = false
    private var lastSunUp: Int = 1
    private var showSOC: Boolean = true
    private var uiSpeedUnitsMPH: Boolean = true
    private var power: Float = 0f
    private var battAmps:Float = 0f
    private var battVolts:Float = 0f
    private var minPower: Float = 0f
    private var maxPower: Float = 0f

//    private var argbEvaluator:ArgbEvaluator = ArgbEvaluator()

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
            maxPower = 0f
            minPower = 0f
        }
        viewModel.carState().observe(viewLifecycleOwner) {
            it.getValue(Constants.isSunUp)?.let { sunUpVal ->
                setColors(sunUpVal.toInt())
            }
            // get battery status to decide whether or not to disable screen dimming
            var batteryStatus: Intent? = IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { ifilter ->
                context?.registerReceiver(null, ifilter)
            }
            // How are we charging?
            val chargePlug: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1) ?: -1
            val isPlugged: Boolean = chargePlug == BatteryManager.BATTERY_PLUGGED_USB
                    || chargePlug == BatteryManager.BATTERY_PLUGGED_AC
                    || chargePlug == BatteryManager.BATTERY_PLUGGED_WIRELESS
            Log.d(TAG, "keep_screen_on" + isPlugged.toString())

            if (isPlugged){
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

            binding.power.text = "Min: "+ minPower.toInt().toString() +" W Cur: "+ power.toInt().toString()+ " W Max: " + maxPower.toInt().toString() + " W"

            val percent = power/300000f
            binding.powerBar.measuredWidth
            it.getValue(Constants.autopilotState)?.let { autopilotStateVal ->
                if (autopilotStateVal.toInt() > 2) {
                    gearColorSelected = requireContext().getColor(R.color.autopilot_blue)
                } else if (lastSunUp == 1) {
                    gearColorSelected = Color.DKGRAY
                } else {
                    gearColorSelected = Color.LTGRAY
                }
            }
            it.getValue(Constants.gearSelected)?.let { gearStateVal ->
                val gear: String = binding.PRND.text.toString()
                var ss: SpannableString = SpannableString(gear)
                var gearStartIndex = 0
                var gearEndIndex = 1
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

                //TODO: change colors to autopilot_blue constant
                if ((autopilotHandsVal.toInt() > 2) and (autopilotHandsVal.toInt() < 15)) {
                    if (autopilotHandsToggle == false) {
                        val colorFrom = getBackgroundColor(lastSunUp)
                        val colorTo = Color.parseColor("#FF7791F7")
                        val colorAnimation = ValueAnimator.ofObject(ArgbEvaluator(), colorFrom, colorTo)
                        colorAnimation.duration = 2000
                        // milliseconds

                        colorAnimation.addUpdateListener { animator ->
                            binding.root.setBackgroundColor(
                                animator.animatedValue as Int
                            )
                        }
                        colorAnimation.start()
                        autopilotHandsToggle = true
                    }
                    else {
                        binding.root.setBackgroundColor(Color.parseColor("#FF7791F7"))

                    }
                } else {
                    binding.root.setBackgroundColor(getBackgroundColor(lastSunUp))
                    autopilotHandsToggle = false
                }


            }

            binding.speed.text = (it.getValue(Constants.uiSpeed)?.toInt() ?: "").toString()
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
                }
                else {
                    binding.unit.text =
                        "KPH"
                    it.getValue(Constants.uiRange)?.let { stateOfChargeVal ->
                        binding.batterypercent.text =
                            ((stateOfChargeVal.toInt()) / .62).toString() + " km"
                    }
                }
            }

            it.getValue(Constants.stateOfCharge)?.let { stateOfChargeVal ->
                binding.fullbattery.scrollX =
                    (83 - ((stateOfChargeVal.toLong() * 83) / 100).toInt())
            }
/*
            it.getValue(Constants.cruiseControlSpeed)?.let { cruiseControlSpeedVal ->
                if (cruiseControlSpeedVal.toInt() == 0) {
                    binding.displaymaxspeed.visibility = View.INVISIBLE
                } else {
                    binding.displaymaxspeed.visibility = View.VISIBLE
                    binding.autopilotMaxSpeedInactive.visibility = View.VISIBLE
                    if (cruiseControlSpeedVal.toInt() < 200){
                        binding.displaymaxspeed.text = cruiseControlSpeedVal.toInt().toString()
                    }
                }
            }
*/
            it.getValue(Constants.autopilotState)?.let { autopilotStateVal ->
                updateAutopilotUI(
                    autopilotStateVal.toInt(),
                    it.getValue(Constants.steeringAngle)?.toInt()
                )
            }

            it.getValue(Constants.liftgateState)?.let { liftgateVal ->
                binding.hatch.visibility =
                    if (liftgateVal.toInt() == 1) View.VISIBLE else View.GONE
            }
            it.getValue(Constants.frunkState)?.let { frunkVal ->
                binding.hood.visibility =
                    if (frunkVal.toInt() == 1) View.VISIBLE else View.GONE
            }
            it.getValue(Constants.frontLeftDoorState)?.let { doorVal ->
                binding.frontleftdoor.visibility =
                    if (doorVal.toInt() == 1) View.VISIBLE else View.GONE
            }
            it.getValue(Constants.frontRightDoorState)?.let { doorVal ->
                binding.frontrightdoor.visibility =
                    if (doorVal.toInt() == 1) View.VISIBLE else View.GONE
            }
            it.getValue(Constants.rearLeftDoorState)?.let { doorVal ->
                binding.rearleftdoor.visibility =
                    if (doorVal.toInt() == 1) View.VISIBLE else View.GONE
            }
            it.getValue(Constants.rearRightDoorState)?.let { doorVal ->
                binding.rearrightdoor.visibility =
                    if (doorVal.toInt() == 1) View.VISIBLE else View.GONE
            }
            it.getValue(Constants.turnSignalLeft)?.let { leftTurnSignalVal ->
                binding.leftTurnSignalDark.visibility =
                    if (leftTurnSignalVal.toInt() == 1) View.VISIBLE else View.INVISIBLE
                binding.leftTurnSignalLight.visibility =
                    if (leftTurnSignalVal.toInt() == 2) View.VISIBLE else View.INVISIBLE
            }
            it.getValue(Constants.turnSignalRight)?.let { rightTurnSignalVal ->
                binding.rightTurnSignalDark.visibility =
                    if (rightTurnSignalVal.toInt() == 1) View.VISIBLE else View.INVISIBLE
                binding.rightTurnSignalLight.visibility =
                    if (rightTurnSignalVal.toInt() == 2) View.VISIBLE else View.INVISIBLE
            }
            it.getValue(Constants.blindSpotLeft)?.let { blindSpotLeftVal ->
                binding.blindSpotLeft1.visibility =
                    if ((blindSpotLeftVal.toInt() > 0) and (blindSpotLeftVal.toInt() < 3)) View.VISIBLE else View.GONE
            }
            it.getValue(Constants.blindSpotRight)?.let { blindSpotRightVal ->
                binding.blindSpotRight1.visibility =
                    if ((blindSpotRightVal.toInt() > 0) and (blindSpotRightVal.toInt() < 3)) View.VISIBLE else View.GONE
            }
            if (it.getValue(Constants.rearLeftVehicle) != null) rearLeftVehDetected =
                it.getValue(Constants.rearLeftVehicle)!!.toInt() else rearLeftVehDetected = 500
            if (it.getValue(Constants.rearRightVehicle) != null) rearRightVehDetected =
                it.getValue(Constants.rearRightVehicle)!!.toInt() else rearRightVehDetected = 500
            if (it.getValue(Constants.leftVehicle) != null) leftVehDetected =
                it.getValue(Constants.leftVehicle)!!.toInt() else leftVehDetected = 500
            if (it.getValue(Constants.rightVehicle) != null) rightVehDetected =
                it.getValue(Constants.rightVehicle)!!.toInt() else rightVehDetected = 500

            if ((rearLeftVehDetected < 200) or (leftVehDetected < 200)) {
                binding.blindSpotLeft1.visibility = View.VISIBLE
            } else {
                binding.blindSpotLeft1.visibility = View.GONE
            }
            if ((rearRightVehDetected < 200) or (rightVehDetected < 200)) {
                binding.blindSpotRight1.visibility = View.VISIBLE
            } else {
                binding.blindSpotRight1.visibility = View.GONE
            }
            if ((rearLeftVehDetected <= 100) or (leftVehDetected <= 100)) {
                binding.blindSpotLeft1.visibility = View.GONE
                binding.blindSpotLeft2.visibility = View.VISIBLE
            } else {
                binding.blindSpotLeft2.visibility = View.GONE
            }
            if ((rearRightVehDetected < 100) or (rightVehDetected < 100)) {
                binding.blindSpotRight1.visibility = View.GONE
                binding.blindSpotRight2.visibility = View.VISIBLE
            } else {
                binding.blindSpotRight2.visibility = View.GONE
            }

        }
    }

    fun setColors(sunUpVal: Int) {

        if (lastSunUp.toInt() != sunUpVal) {
            // Not using dark-mode for compatibility with older version of Android (pre-29)
            if (sunUpVal == 0) {
                binding.root.setBackgroundColor(Color.BLACK)

                binding.speed.setTextColor(Color.WHITE)
                binding.unit.setTextColor(Color.LTGRAY)
                binding.batterypercent.setTextColor(Color.LTGRAY)
                binding.deadbattery.setColorFilter(Color.DKGRAY)
                binding.deadbatterymask.setColorFilter(Color.DKGRAY)
                binding.fullbattery.setColorFilter(Color.LTGRAY)
                gearColorSelected = Color.LTGRAY
                gearColor = Color.DKGRAY
                binding.PRND.setTextColor(Color.DKGRAY)
                binding.modely.setColorFilter(Color.parseColor("#FF333333"))
                binding.power.setTextColor(Color.WHITE)

                //binding.displaymaxspeed.setTextColor(Color.WHITE)


            } else {
                //view.setBackgroundColor(Color.parseColor("#"+Integer.toString(R.color.day_background, 16)))
                binding.root.setBackgroundColor(Color.parseColor("#FFEEEEEE"))

                binding.speed.setTextColor(Color.BLACK)
                binding.unit.setTextColor(Color.DKGRAY)
                binding.batterypercent.setTextColor(Color.DKGRAY)
                binding.deadbattery.clearColorFilter()
                binding.deadbatterymask.clearColorFilter()
                binding.fullbattery.clearColorFilter()
                gearColorSelected = Color.DKGRAY
                gearColor = Color.parseColor("#FFEEEEEE")
                binding.PRND.setTextColor(Color.parseColor("#FFEEEEEE"))
                binding.modely.setColorFilter(Color.LTGRAY)
                binding.power.setTextColor(Color.DKGRAY)

                //binding.displaymaxspeed.setTextColor(Color.BLACK)

            }
        }
        lastSunUp = sunUpVal
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
}