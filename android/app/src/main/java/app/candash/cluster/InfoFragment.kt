package app.candash.cluster

import android.content.ClipData
import android.content.Intent
import android.net.Uri
import android.net.nsd.NsdServiceInfo
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.core.content.FileProvider.getUriForFile
import androidx.core.text.bold
import androidx.core.text.buildSpannedString
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import app.candash.cluster.databinding.FragmentInfoBinding
import java.io.File


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

        val options = viewModel.getCANServiceOptions(requireContext())
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, options)
        binding.chooseService.adapter = adapter
        binding.chooseService.setSelection(viewModel.getCurrentCANServiceIndex())

        binding.editIpAddress.text = SpannableStringBuilder(viewModel.serverIpAddress())

        binding.saveButton.setOnClickListener {
            viewModel.saveSettings(getSelectedCANServiceIndex(), binding.editIpAddress.text.toString())
        }
        binding.startButton.setOnClickListener {
            if (!viewModel.isRunning()){
                viewModel.startUp(signalNames())
            }

        }

        binding.stopButton.setOnClickListener {
            if (viewModel.isRunning()){
                viewModel.shutdown()
            }
        }
        binding.settings.setOnClickListener(){
            switchToSettings()
        }
        binding.root.setOnLongClickListener {
            switchToDash()
        }
        binding.startDashButton.setOnClickListener(){
            switchToDash()
        }

        binding.emailLogs.setOnClickListener() {
            sendEmailLogs()
        }

        binding.scrollView.setOnLongClickListener{
            switchToDash()
        }

        binding.trash.setOnClickListener {
            viewModel.clearCarState()
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

    fun getSelectedCANServiceIndex() : Int {
        return binding.chooseService.selectedItemPosition
    }
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
        viewModel.startDiscoveryService()
    }

    fun signalNames() : List<String> {
        return arrayListOf()
    }

    override fun onPause() {
        super.onPause()
        viewModel.stopDiscoveryService()

    }

    fun switchToDash() : Boolean {
        viewModel.switchToDashFragment()
        return true
    }

    fun switchToSettings() : Boolean {
        viewModel.switchToSettingsFragment()
        return true
    }
    fun logCarState(carState: CarState) {
        Log.d(TAG, "Car state size: " + carState.carData.size)
        carState.carData.forEach {
            Log.d(TAG, "Name: " + it.key + ", Value: " + it.value)
        }
    }

    private fun sendEmailLogs() {
        context?.let { ctx ->
            val logPath = File(ctx.externalCacheDir, "logs/")
            logPath.mkdirs()
            val outputFile = File(logPath, "output.txt")
            val contentUri: Uri =
                getUriForFile(ctx, BuildConfig.APPLICATION_ID + ".fileprovider", outputFile)
            try {
                Runtime.getRuntime().exec(
                    "logcat -f " + outputFile.absolutePath
                )
            } catch (e: Exception) {
                Log.e(TAG, "Cannot generate logs", e)
            }
            outputFile.setReadable(true)

            val emailIntent = Intent(Intent.ACTION_SEND)
            emailIntent.clipData = ClipData.newRawUri("CANDash Logs", contentUri)
            emailIntent.type = "vnd.android.cursor.dir/email"
            val to = arrayOf("info@candash.app")
            emailIntent.putExtra(Intent.EXTRA_EMAIL, to)
            emailIntent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            emailIntent.putExtra(Intent.EXTRA_STREAM, contentUri)
            emailIntent.putExtra(Intent.EXTRA_SUBJECT, "CANDash Logs")

            val chooser = Intent.createChooser(emailIntent, "Send email...")
            chooser.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            startActivity(chooser)
        }
    }
}
