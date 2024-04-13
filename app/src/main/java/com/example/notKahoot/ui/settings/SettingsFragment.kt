package com.example.notKahoot.ui.settings

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import com.example.notKahoot.R


class SettingsFragment : PreferenceFragmentCompat() {

//    private var _binding: FragmentSettingsBinding? = null
//    private val binding get() = _binding!!

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey)
    }

}