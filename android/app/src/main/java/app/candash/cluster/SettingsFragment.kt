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

        binding.saveSettings.setOnClickListener{
            saveSettings()
        }
        binding.cancelSettings.setOnClickListener{
            launchInfoFragment()
        }
        if (prefs.getBooleanPref("forceNightMode")) {
            binding.displaydark.isChecked = true
        } else {
            binding.displayauto.isChecked = true
        }
        when (prefs.getPref(Constants.gaugeMode)){
            2f -> binding.gaugemodefull.isChecked = true
            1f -> binding.gaugemoderegular.isChecked = true
            0f -> binding.gaugemodesimple.isChecked = true
        }
        binding.showodo.isChecked = prefs.getBooleanPref(Constants.odometer)

    }



    private fun saveSettings(){
        when (binding.displaysettings.checkedRadioButtonId) {
            R.id.displaydark -> prefs.setBooleanPref("forceNightMode", true)
            R.id.displayauto -> prefs.setBooleanPref("forceNightMode", false)
        }
        // val radioButton: RadioButton? = binding.gaugemode.findViewById<RadioButton>(binding.gaugemode.checkedRadioButtonId)
        when(binding.gaugemode.checkedRadioButtonId){
            R.id.gaugemodefull -> prefs.setPref(Constants.gaugeMode, 2f)
            R.id.gaugemoderegular -> prefs.setPref(Constants.gaugeMode, 1f)
            R.id.gaugemodesimple -> prefs.setPref(Constants.gaugeMode, 0f)
        }
        if (binding.showodo.isChecked){
            prefs.setBooleanPref(Constants.odometer, true)
        } else {
            prefs.setBooleanPref(Constants.odometer, false)
        }
        launchInfoFragment()
    }
    private fun launchInfoFragment(){
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