package dev.nmullaney.tesladashboard

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.text.bold
import androidx.core.text.buildSpannedString
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import dev.nmullaney.tesladashboard.databinding.FragmentInfoBinding

class InfoFragment() : Fragment() {
    private val TAG = InfoFragment::class.java.simpleName

    private lateinit var binding : FragmentInfoBinding

    private lateinit var viewModel: DashViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentInfoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(requireActivity()).get(DashViewModel::class.java)

        binding.root.setOnLongClickListener {
            switchToDash()
        }
        binding.infoText.setOnLongClickListener{
            switchToDash()
        }
        binding.scrollView.setOnLongClickListener{
            switchToDash()
        }

        viewModel.carState().observe(viewLifecycleOwner, { carState ->
            logCarState(carState)
            binding.infoText.text = buildSpannedString {
                carState.carData.forEach { entry ->
                    bold {
                        append(entry.key)
                        append(": ")
                    }
                    append(entry.value.toString())
                    append("\n")
                }
            }
        })
    }

    fun switchToDash() : Boolean {
        viewModel.switchToDashFragment()
        return true
    }

    fun logCarState(carState: CarState) {
        Log.d(TAG, "Car state size: " + carState.carData.size)
        carState.carData.forEach {
            Log.d(TAG, "Name: " + it.key + ", Value: " + it.value)
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