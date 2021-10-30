package dev.nmullaney.tesladashboard

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
            binding.speed.text = (it.getValue(Constants.uiSpeed)?.toInt() ?: "").toString()
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