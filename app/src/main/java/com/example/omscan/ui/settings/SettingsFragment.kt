package com.example.omscan.ui.settings

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import com.example.omscan.R
import com.example.omscan.databinding.FragmentSettingsBinding

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        
        setupThemeSelection()
        setupQualitySelection()
        
        return binding.root
    }

    private fun setupThemeSelection() {
        val sharedPref = requireActivity().getSharedPreferences("settings", Context.MODE_PRIVATE)
        val currentTheme = sharedPref.getInt("theme", AppCompatDelegate.MODE_NIGHT_NO)

        when (currentTheme) {
            AppCompatDelegate.MODE_NIGHT_NO -> binding.radioLight.isChecked = true
            AppCompatDelegate.MODE_NIGHT_YES -> binding.radioDark.isChecked = true
            AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM -> binding.radioSystem.isChecked = true
        }

        binding.radioGroupTheme.setOnCheckedChangeListener { _, checkedId ->
            val mode = when (checkedId) {
                R.id.radio_light -> AppCompatDelegate.MODE_NIGHT_NO
                R.id.radio_dark -> AppCompatDelegate.MODE_NIGHT_YES
                else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            }
            
            sharedPref.edit().putInt("theme", mode).apply()
            AppCompatDelegate.setDefaultNightMode(mode)
        }
    }

    private fun setupQualitySelection() {
        val sharedPref = requireActivity().getSharedPreferences("settings", Context.MODE_PRIVATE)
        val currentQuality = sharedPref.getInt("pdf_quality", 50) // Default medium (50)

        when (currentQuality) {
            30 -> binding.radioQualityLow.isChecked = true
            50 -> binding.radioQualityMedium.isChecked = true
            80 -> binding.radioQualityHigh.isChecked = true
            else -> binding.radioQualityMedium.isChecked = true
        }

        binding.radioGroupQuality.setOnCheckedChangeListener { _, checkedId ->
            val quality = when (checkedId) {
                R.id.radio_quality_low -> 30
                R.id.radio_quality_medium -> 50
                R.id.radio_quality_high -> 80
                else -> 50
            }
            sharedPref.edit().putInt("pdf_quality", quality).apply()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
