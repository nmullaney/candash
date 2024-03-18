package app.candash.cluster

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.FileProvider.getUriForFile
import androidx.core.text.bold
import androidx.core.text.buildSpannedString
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import app.candash.cluster.databinding.FragmentInfoBinding
import java.io.File


class InfoFragment : Fragment() {
    private val TAG = InfoFragment::class.java.simpleName
    private lateinit var binding: FragmentInfoBinding
    private lateinit var viewModel: DashViewModel
    private lateinit var prefs: SharedPreferences

    private val infoTextViews = mutableListOf<View>()
    private var zoomSignal: String? = null
    private val signalMaxes = mutableMapOf<String, Float?>()
    private val signalMinis = mutableMapOf<String, Float?>()


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentInfoBinding.inflate(inflater, container, false)
        prefs = requireContext().getSharedPreferences("dash", Context.MODE_PRIVATE)
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

        binding.infoTextConstraint.visibility = View.VISIBLE
        binding.zoomGauge.visibility = View.INVISIBLE
        binding.zoomValue.visibility = View.INVISIBLE
        binding.zoomName.visibility = View.INVISIBLE

        binding.saveButton.setOnClickListener {
            viewModel.saveSettings(
                getSelectedCANServiceIndex(),
                binding.editIpAddress.text.toString()
            )
            reload()
        }
        binding.startButton.setOnClickListener {
            if (!viewModel.isRunning()) {
                viewModel.startUp()
            }
        }

        binding.stopButton.setOnClickListener {
            if (viewModel.isRunning()) {
                viewModel.shutdown()
            }
        }
        binding.settings.setOnClickListener {
            switchToSettings()
        }
        binding.root.setOnLongClickListener {
            switchToDash()
        }
        binding.startDashButton.setOnClickListener {
            switchToDash()
        }

        binding.emailLogs.setOnClickListener {
            sendEmailLogs()
        }

        binding.scrollView.setOnLongClickListener {
            switchToDash()
        }

        binding.trash.setOnClickListener {
            viewModel.clearCarState()
        }

        binding.zoomValue.setOnClickListener {
            zoomSignal = null
            binding.infoTextConstraint.visibility = View.VISIBLE
            binding.zoomGauge.visibility = View.INVISIBLE
            binding.zoomValue.visibility = View.INVISIBLE
            binding.zoomName.visibility = View.INVISIBLE
        }


        viewModel.onSomeSignals(
            viewLifecycleOwner, listOf(
                SName.displayBrightnessLev,
                SName.solarBrightnessFactor
            )
        ) {
            val solarBrightnessFactor = it[SName.solarBrightnessFactor]
            if (solarBrightnessFactor != null) {
                prefs.setPref(Constants.lastSolarBrightnessFactor, solarBrightnessFactor)
            }
            viewModel.updateBrightness()
        }

        viewModel.onSignal(viewLifecycleOwner, SName.isDarkMode) {
            if (it != null) {
                if (it != prefs.getPref(Constants.lastDarkMode)) {
                    prefs.setPref(Constants.lastDarkMode, it)
                    viewModel.updateTheme()
                }
            }
        }

        viewModel.onSignal(viewLifecycleOwner, SName.isSunUp) {
            if (it != null) {
                if (it != prefs.getPref(Constants.lastSunUp)) {
                    prefs.setPref(Constants.lastSunUp, it)
                    viewModel.updateTheme()
                }
            }
        }

        viewModel.allSignalNames().forEach { signal ->
            signalMaxes[signal] = null
            signalMinis[signal] = null
            val lastViewID = infoTextViews.lastOrNull()?.id ?: binding.headerText.id
            val textView = TextView(this.context).apply {
                id = View.generateViewId()
                layoutParams = ConstraintLayout.LayoutParams(
                    ConstraintLayout.LayoutParams.MATCH_PARENT,
                    ConstraintLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    topToBottom = lastViewID
                    startToStart = ConstraintLayout.LayoutParams.PARENT_ID
                }
                setPadding(0, 2, 20, 0)
                textSize = 18f
            }
            textView.setOnClickListener {
                zoomSignal = signal
                binding.infoTextConstraint.visibility = View.GONE
                binding.zoomGauge.visibility = View.VISIBLE
                binding.zoomValue.visibility = View.VISIBLE
                binding.zoomName.visibility = View.VISIBLE
                updateZoom(signal)
            }
            viewModel.onSignal(viewLifecycleOwner, signal) { value ->
                if (value != null) {
                    val sigMax = signalMaxes[signal]
                    val sigMin = signalMinis[signal]
                    if (sigMax == null || value > sigMax) {
                        signalMaxes[signal] = value
                    }
                    if (sigMin == null || value < sigMin) {
                        signalMinis[signal] = value
                    }
                    textView.text = buildSpannedString {
                        bold { append("$signal: ") }
                        append(value.roundToString())
                    }
                    textView.visibility = View.VISIBLE
                } else {
                    textView.visibility = View.GONE
                }
                if (zoomSignal == signal) {
                    updateZoom(signal)
                }
            }
            binding.infoTextConstraint.addView(textView)
            infoTextViews.add(textView)
        }
    }

    private fun updateZoom(signal: String) {
        val value = viewModel.carState[signal]
        binding.zoomName.text = signal
        if (value == null) {
            binding.zoomGauge.setGauge(0f, 6f)
            binding.zoomValue.text = "null"
        } else {
            val percent = if (value < 0) {
                value / signalMinis[signal]!! * -1
            } else {
                value / signalMaxes[signal]!!
            }
            binding.zoomGauge.setGauge(percent, 6f)
            binding.zoomValue.text = value.roundToString()
        }
    }

    private fun getSelectedCANServiceIndex(): Int {
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

    override fun onPause() {
        super.onPause()
        viewModel.stopDiscoveryService()

    }

    private fun reload(): Boolean {
        viewModel.switchToInfoFragment()
        return true
    }

    private fun switchToDash(): Boolean {
        viewModel.switchToDashFragment()
        return true
    }

    private fun switchToSettings(): Boolean {
        viewModel.switchToSettingsFragment()
        return true
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
