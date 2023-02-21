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
        if (prefs.getBooleanPref(Constants.forceNightMode)) {
            binding.displaydark.isChecked = true
        } else {
            binding.displayauto.isChecked = true
        }
        when (prefs.getPref(Constants.gaugeMode)) {
            Constants.showFullGauges -> binding.gaugemodefull.isChecked = true
            Constants.showRegularGauges -> binding.gaugemoderegular.isChecked = true
            Constants.showSimpleGauges -> binding.gaugemodesimple.isChecked = true
        }
        // These are inverted so that the default value (false) shows the object (show = !hide)
        binding.showodo.isChecked = !prefs.getBooleanPref(Constants.hideOdometer)
        binding.showBs.isChecked = !prefs.getBooleanPref(Constants.hideBs)
        binding.showSpeedLimit.isChecked = !prefs.getBooleanPref(Constants.hideSpeedLimit)
        binding.blankDisplaySync.isChecked = !prefs.getBooleanPref(Constants.blankDisplaySync)
        binding.showEfficiency.isChecked = !prefs.getBooleanPref(Constants.hideEfficiency)
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

    }


    private fun saveSettings() {
        when (binding.displaysettings.checkedRadioButtonId) {
            R.id.displaydark -> prefs.setBooleanPref(Constants.forceNightMode, true)
            R.id.displayauto -> prefs.setBooleanPref(Constants.forceNightMode, false)
        }
        when (binding.gaugemode.checkedRadioButtonId) {
            R.id.gaugemodefull -> prefs.setPref(Constants.gaugeMode, Constants.showFullGauges)
            R.id.gaugemoderegular -> prefs.setPref(Constants.gaugeMode, Constants.showRegularGauges)
            R.id.gaugemodesimple -> prefs.setPref(Constants.gaugeMode, Constants.showSimpleGauges)
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
        // This is not inverted, because defaulting to blank display makes the app appear broken on first launch
        if (binding.blankDisplaySync.isChecked) {
            prefs.setBooleanPref(Constants.blankDisplaySync, true)
        } else {
            prefs.setBooleanPref(Constants.blankDisplaySync, false)
        }

        if (binding.showEfficiency.isChecked) {
            prefs.setBooleanPref(Constants.hideEfficiency, false)
        } else {
            prefs.setBooleanPref(Constants.hideEfficiency, true)
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