package dev.nmullaney.tesladashboard

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
import dev.nmullaney.tesladashboard.databinding.FragmentInfoBinding
import java.net.InetAddress

class InfoFragment() : Fragment() {
    private val TAG = InfoFragment::class.java.simpleName
    private lateinit var nsdManager: NsdManager
    private lateinit var binding : FragmentInfoBinding
    private lateinit var pandaInfo: NsdServiceInfo
    private lateinit var viewModel: DashViewModel
    private var zeroconfHost : String = ""
    private val resolveListener = object : NsdManager.ResolveListener {
        override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
            // Called when the resolve fails. Use the error code to debug.
            Log.e(TAG, "Resolve failed: $errorCode")
        }

        override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
            Log.e(TAG, "Resolve Succeeded. $serviceInfo")
            val host: InetAddress = serviceInfo.host
            zeroconfHost = host.hostAddress
            Log.e(TAG, "IP Succeeded. $zeroconfHost")

        }
    }
    private val discoveryListener = object : NsdManager.DiscoveryListener {

        // Called as soon as service discovery begins.
        override fun onDiscoveryStarted(regType: String) {
            Log.d(TAG, "Service discovery started")
        }

        override fun onServiceFound(service: NsdServiceInfo) {
            // A service was found! Do something with it.
            Log.d(TAG, "Service discovery success$service")
            Log.d(TAG, "Service Type: ${service.serviceType} Service Name: ${service.serviceName}")
            nsdManager.resolveService(service, resolveListener)
        }

        override fun onServiceLost(service: NsdServiceInfo) {
            // When the network service is no longer available.
            // Internal bookkeeping code goes here.
            Log.e(TAG, "service lost: $service")
        }

        override fun onDiscoveryStopped(serviceType: String) {
            Log.i(TAG, "Discovery stopped: $serviceType")
        }

        override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
            Log.e(TAG, "Discovery failed: Error code:$errorCode")

            nsdManager?.stopServiceDiscovery(this)
        }

        override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
            Log.e(TAG, "Discovery failed: Error code:$errorCode")
            nsdManager?.stopServiceDiscovery(this)
        }
    }


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

        //nsdManager?.resolveService(resolveListener)

        binding.toggleServerGroup.check(if (viewModel.useMockServer()) R.id.mock_server_button else R.id.real_server_button)

        binding.editIpAddress.text = SpannableStringBuilder(viewModel.serverIpAddress())

        binding.saveButton.setOnClickListener {
            viewModel.saveSettings(binding.toggleServerGroup.checkedButtonId == R.id.mock_server_button, binding.editIpAddress.text.toString())
        }
        binding.scanButton.setOnClickListener {
            nsdManager = (context?.getSystemService(Context.NSD_SERVICE) as NsdManager?)!!
            nsdManager?.discoverServices("_panda._udp", NsdManager.PROTOCOL_DNS_SD, discoveryListener)
            if (zeroconfHost != "") {
                viewModel.saveSettings(binding.toggleServerGroup.checkedButtonId == R.id.mock_server_button, zeroconfHost)
                binding.editIpAddress.text = SpannableStringBuilder(viewModel.serverIpAddress())
            }
        }
        binding.startButton.setOnClickListener {
            viewModel.startUp()
        }

        binding.stopButton.setOnClickListener {
            viewModel.shutdown()
        }

        binding.root.setOnLongClickListener {
            switchToDash()
        }
        binding.infoText.setOnLongClickListener{
            switchToDash()
        }
        binding.scrollView.setOnLongClickListener{
            switchToDash()
        }

        viewModel.carState().observe(viewLifecycleOwner) { carState ->
            //logCarState(carState)
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
}
