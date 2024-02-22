package app.candash.cluster

import android.content.Context
import android.content.SharedPreferences
import android.net.nsd.NsdServiceInfo
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import app.candash.cluster.databinding.FragmentInfoBinding
import app.candash.cluster.databinding.FragmentSettingsBinding

class SettingsFragment() : Fragment() {
    private val TAG = SettingsFragment::class.java.simpleName
    private lateinit var binding: FragmentSettingsBinding
    private lateinit var pandaInfo: NsdServiceInfo
    private lateinit var viewModel: DashViewModel
    private lateinit var prefs: SharedPreferences

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSettingsBinding.inflate(inflater, container, false)
        prefs = requireContext().getSharedPreferences("dash", Context.MODE_PRIVATE)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        viewModel = ViewModelProvider(requireActivity()).get(DashViewModel::class.java)

        binding.saveSettings.setOnClickListener {
            saveSettings()
        }
        binding.cancelSettings.setOnClickListener {
            launchInfoFragment()
        }
        if (prefs.getBooleanPref(Constants.forceDarkMode)) {
            binding.displaydark.isChecked = true
        } else {
            binding.displayauto.isChecked = true
        }
        when (prefs.getPref(Constants.gaugeMode)) {
            Constants.showFullGauges -> binding.gaugemodefull.isChecked = true
            Constants.showRegularGauges -> binding.gaugemoderegular.isChecked = true
            Constants.showSimpleGauges -> binding.gaugemodesimple.isChecked = true
        }
        binding.cyberMode.isChecked = prefs.getBooleanPref(Constants.cyberMode)
        // These are inverted so that the default value (false) shows the object (show = !hide)
        binding.showodo.isChecked = !prefs.getBooleanPref(Constants.hideOdometer)
        binding.showBs.isChecked = !prefs.getBooleanPref(Constants.hideBs)
        binding.showSpeedLimit.isChecked = !prefs.getBooleanPref(Constants.hideSpeedLimit)
        binding.displaySync.isChecked = !prefs.getBooleanPref(Constants.disableDisplaySync)
        binding.autoBrightness.isChecked = !prefs.getBooleanPref(Constants.disableAutoBrightness)
        binding.showEfficiency.isChecked = !prefs.getBooleanPref(Constants.hideEfficiency)
        // Uncheck and disable efficiency chart if efficiency is hidden
        if (!binding.showEfficiency.isChecked) {
            binding.showEfficiencyChart.isChecked = false
            binding.showEfficiencyChart.isEnabled = false
            binding.showEfficiencyChart.alpha = 0.3f
        } else {
            binding.showEfficiencyChart.isChecked = !prefs.getBooleanPref(Constants.hideEfficiencyChart)
            binding.showEfficiencyChart.alpha = 1f
        }
        if (prefs.getBooleanPref(Constants.tempInF)) {
            binding.tempUnitF.isChecked = true
        } else {
            binding.tempUnitC.isChecked = true
        }
        when (prefs.getPref(Constants.powerUnits)) {
            Constants.powerUnitKw -> binding.powerUnitKw.isChecked = true
            Constants.powerUnitHp -> binding.powerUnitHp.isChecked = true
            Constants.powerUnitPs -> binding.powerUnitPs.isChecked = true
        }
        if (prefs.getBooleanPref(Constants.torqueInLbfFt)) {
            binding.torqueUnitLbf.isChecked = true
        } else {
            binding.torqueUnitNm.isChecked = true
        }

        // Update efficiency chart checkbox on efficiency checkbox change
        binding.showEfficiency.setOnCheckedChangeListener { _, isChecked ->
            binding.showEfficiencyChart.isEnabled = isChecked
            binding.showEfficiencyChart.alpha = if (isChecked) 1f else 0.3f
            if (!isChecked) {
                binding.showEfficiencyChart.isChecked = false
            }
        }
    }


    private fun saveSettings() {
        when (binding.displaysettings.checkedRadioButtonId) {
            R.id.displaydark -> prefs.setBooleanPref(Constants.forceDarkMode, true)
            R.id.displayauto -> prefs.setBooleanPref(Constants.forceDarkMode, false)
        }
        when (binding.gaugemode.checkedRadioButtonId) {
            R.id.gaugemodefull -> prefs.setPref(Constants.gaugeMode, Constants.showFullGauges)
            R.id.gaugemoderegular -> prefs.setPref(Constants.gaugeMode, Constants.showRegularGauges)
            R.id.gaugemodesimple -> prefs.setPref(Constants.gaugeMode, Constants.showSimpleGauges)
        }
        if (binding.cyberMode.isChecked) {
            prefs.setBooleanPref(Constants.cyberMode, true)
        } else {
            prefs.setBooleanPref(Constants.cyberMode, false)
        }
        // These are inverted so that the default value (false) shows the object (show = !hide)
        if (binding.showodo.isChecked) {
            prefs.setBooleanPref(Constants.hideOdometer, false)
        } else {
            prefs.setBooleanPref(Constants.hideOdometer, true)
        }
        if (binding.showBs.isChecked) {
            prefs.setBooleanPref(Constants.hideBs, false)
        } else {
            prefs.setBooleanPref(Constants.hideBs, true)
        }
        if (binding.showSpeedLimit.isChecked) {
            prefs.setBooleanPref(Constants.hideSpeedLimit, false)
        } else {
            prefs.setBooleanPref(Constants.hideSpeedLimit, true)
        }
        if (binding.displaySync.isChecked) {
            prefs.setBooleanPref(Constants.disableDisplaySync, false)
        } else {
            prefs.setBooleanPref(Constants.disableDisplaySync, true)
        }
        if (binding.autoBrightness.isChecked) {
            prefs.setBooleanPref(Constants.disableAutoBrightness, false)
        } else {
            prefs.setBooleanPref(Constants.disableAutoBrightness, true)
        }

        if (binding.showEfficiency.isChecked) {
            prefs.setBooleanPref(Constants.hideEfficiency, false)
        } else {
            prefs.setBooleanPref(Constants.hideEfficiency, true)
        }
        if (binding.showEfficiencyChart.isChecked) {
            prefs.setBooleanPref(Constants.hideEfficiencyChart, false)
        } else {
            prefs.setBooleanPref(Constants.hideEfficiencyChart, true)
        }
        when (binding.tempUnits.checkedRadioButtonId) {
            R.id.tempUnitC -> prefs.setBooleanPref(Constants.tempInF, false)
            R.id.tempUnitF -> prefs.setBooleanPref(Constants.tempInF, true)
        }
        when (binding.powerUnits.checkedRadioButtonId) {
            R.id.powerUnitKw -> prefs.setPref(Constants.powerUnits, Constants.powerUnitKw)
            R.id.powerUnitHp -> prefs.setPref(Constants.powerUnits, Constants.powerUnitHp)
            R.id.powerUnitPs -> prefs.setPref(Constants.powerUnits, Constants.powerUnitPs)
        }
        when (binding.torqueUnits.checkedRadioButtonId) {
            R.id.torqueUnitNm -> prefs.setBooleanPref(Constants.torqueInLbfFt, false)
            R.id.torqueUnitLbf -> prefs.setBooleanPref(Constants.torqueInLbfFt, true)
        }
        launchInfoFragment()
    }

    private fun launchInfoFragment() {
        viewModel.switchToInfoFragment()
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
}