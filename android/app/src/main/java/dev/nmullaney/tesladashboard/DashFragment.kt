package dev.nmullaney.tesladashboard

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import dev.nmullaney.tesladashboard.databinding.FragmentDashBinding

class DashFragment : Fragment() {

    private lateinit var binding : FragmentDashBinding

    private lateinit var viewModel: DashViewModel

    private var rearLeftVehDetected:Int = 500
    private var rearRightVehDetected:Int = 500
    private var leftVehDetected:Int = 500
    private var rightVehDetected:Int = 500
    private var liftgateStatus:Int = 5


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentDashBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(requireActivity()).get(DashViewModel::class.java)

        binding.root.setOnLongClickListener {
            viewModel.switchToInfoFragment()
            return@setOnLongClickListener true
        }

        viewModel.carState().observe(viewLifecycleOwner) {
            it.getValue(Constants.isSunUp)?.let { isSunUpVal ->
                // Not using dark-mode for compatibility with older version of Android (pre-29)
                if (isSunUpVal.toInt() == 0) {
                    view.setBackgroundColor(Color.BLACK)
                    binding.speed.setTextColor(Color.WHITE)
                    binding.unit.setTextColor(Color.LTGRAY)
                } else {
                    view.setBackgroundColor(Color.WHITE)
                    binding.speed.setTextColor(Color.BLACK)
                    binding.unit.setTextColor(Color.DKGRAY)
                }
            }

            binding.speed.text = (it.getValue(Constants.uiSpeed)?.toInt() ?: "").toString()
            it.getValue(Constants.autopilotState)?.let { autopilotStateVal ->
                binding.autopilotInactive.visibility =
                    if (autopilotStateVal.toInt() == 2) View.VISIBLE else View.GONE
                binding.autopilot.visibility =
                    if (autopilotStateVal.toInt() == 3) View.VISIBLE else View.GONE
            }

            it.getValue(Constants.liftgateState)?.let { liftgateVal ->
                binding.hatch.visibility =
                    if (liftgateVal.toInt() == 1)  View.VISIBLE else View.GONE
            }
            it.getValue(Constants.frunkState)?.let { frunkVal ->
                binding.hood.visibility =
                    if (frunkVal.toInt() == 1)  View.VISIBLE else View.GONE
            }
            it.getValue(Constants.turnSignalLeft)?.let { leftTurnSignalVal ->
                binding.leftTurnSignalDark.visibility =
                    if (leftTurnSignalVal.toInt() == 1) View.VISIBLE else View.GONE
                binding.leftTurnSignalLight.visibility =
                    if (leftTurnSignalVal.toInt() == 2) View.VISIBLE else View.GONE
            }
            it.getValue(Constants.turnSignalRight)?.let { rightTurnSignalVal ->
                binding.rightTurnSignalDark.visibility =
                    if (rightTurnSignalVal.toInt() == 1) View.VISIBLE else View.GONE
                binding.rightTurnSignalLight.visibility =
                    if (rightTurnSignalVal.toInt() == 2) View.VISIBLE else View.GONE
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

    override fun onStart() {
        super.onStart()
        viewModel.startUp()
    }

    override fun onStop() {
        super.onStop()
        viewModel.shutdown()
    }
}