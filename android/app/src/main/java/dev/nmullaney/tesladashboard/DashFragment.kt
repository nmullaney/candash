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

        viewModel.carState().observe(viewLifecycleOwner, {
            it.getValue(Constants.isSunUp)?.let { isSunUpVal ->
                if (isSunUpVal.toInt() == 0) view.setBackgroundColor(Color.BLACK) else view.setBackgroundColor(Color.WHITE)
                if (isSunUpVal.toInt() == 0) binding.speed.setTextColor(Color.WHITE) else binding.speed.setTextColor(Color.BLACK)
                if (isSunUpVal.toInt() == 0) binding.unit.setTextColor(Color.LTGRAY) else binding.unit.setTextColor(Color.DKGRAY)


            }
            binding.speed.text = (it.getValue(Constants.uiSpeed)?.toInt() ?: "").toString()
            it.getValue(Constants.turnSignalLeft)?.let { leftTurnSignalVal ->
                binding.leftTurnSignalDark.visibility = if (leftTurnSignalVal.toInt() == 1) View.VISIBLE else View.GONE
                binding.leftTurnSignalLight.visibility = if (leftTurnSignalVal.toInt() == 2) View.VISIBLE else View.GONE
            }
            it.getValue(Constants.turnSignalRight)?.let { rightTurnSignalVal ->
                binding.rightTurnSignalDark.visibility = if (rightTurnSignalVal.toInt() == 1) View.VISIBLE else View.GONE
                binding.rightTurnSignalLight.visibility = if (rightTurnSignalVal.toInt() == 2) View.VISIBLE else View.GONE
            }
            it.getValue(Constants.blindSpotLeft)?.let { blindSpotLeftVal ->
                binding.blindSpotLeft1.visibility = if ((blindSpotLeftVal.toInt() > 0) and (blindSpotLeftVal.toInt() < 3)) View.VISIBLE else View.GONE
            }
            it.getValue(Constants.blindSpotRight)?.let { blindSpotRightVal ->
                binding.blindSpotRight1.visibility = if ((blindSpotRightVal.toInt() > 0) and (blindSpotRightVal.toInt() < 3))View.VISIBLE else View.GONE
            }


        })
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