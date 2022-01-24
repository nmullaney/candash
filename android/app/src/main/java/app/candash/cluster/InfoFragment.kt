package app.candash.cluster

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.text.bold
import androidx.core.text.buildSpannedString
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import app.candash.cluster.databinding.FragmentInfoBinding

class InfoFragment() : Fragment() {
    private val TAG = InfoFragment::class.java.simpleName
    private lateinit var binding : FragmentInfoBinding
    private lateinit var pandaInfo: NsdServiceInfo
    private lateinit var viewModel: DashViewModel

    private var zeroconfHost : String = ""



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


        binding.toggleServerGroup.check(if (viewModel.useMockServer()) R.id.mock_server_button else R.id.real_server_button)

        binding.editIpAddress.text = SpannableStringBuilder(viewModel.serverIpAddress())

        binding.saveButton.setOnClickListener {
            viewModel.saveSettings(binding.toggleServerGroup.checkedButtonId == R.id.mock_server_button, binding.editIpAddress.text.toString())
        }
        binding.scanButton.setOnClickListener {
            if (viewModel.getZeroConfIpAddress() != "0.0.0.0"){
                viewModel.saveSettings(false, viewModel.getZeroConfIpAddress())
            }
            binding.editIpAddress.text = SpannableStringBuilder(viewModel.serverIpAddress())
        }
        binding.startButton.setOnClickListener {
            if (viewModel.isRunning() == false){
                viewModel.startUp()
            }

        }

        binding.stopButton.setOnClickListener {
            if (viewModel.isRunning() == true){
                viewModel.shutdown()
            }
        }

        binding.root.setOnLongClickListener {
            switchToDash()
        }
        binding.startDashButton.setOnClickListener(){
            switchToDash()
        }
        binding.scrollView.setOnLongClickListener{
            switchToDash()
        }

        viewModel.carState().observe(viewLifecycleOwner) { carState ->
            //logCarState(carState)


            binding.infoText.text = buildSpannedString {
                val sortedMap = carState.carData.toSortedMap()
                sortedMap.forEach() { entry ->
                    bold {
                        append(entry.key)
                        append(": ")
                    }
                    append(entry.value.toString())
                    append("\n")
                }
            }
        }
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

    override fun onResume() {
        super.onResume()
        viewModel.startDiscoveryService()
    }

    override fun onPause() {
        super.onPause()
        viewModel.stopDiscoveryService()
    }
}
