/*
 * Copyright (C) 2026 The LineageOS Project
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lenovo.settings.peripheral

import android.content.SharedPreferences
import android.os.Bundle
import androidx.preference.PreferenceFragment
import androidx.preference.PreferenceManager
import androidx.preference.SwitchPreference
import com.lenovo.settings.R

class StylusSettingsFragment : PreferenceFragment(),
    SharedPreferences.OnSharedPreferenceChangeListener {

    private lateinit var stylusPreference: SharedPreferences

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.stylus_settings)
        stylusPreference = PreferenceManager.getDefaultSharedPreferences(context!!)
        
        val switchPreference = findPreference<SwitchPreference>(STYLUS_KEY)
        switchPreference?.let {
            it.isChecked = stylusPreference.getBoolean(STYLUS_KEY, false)
            it.isEnabled = true
        }
    }

    override fun onResume() {
        super.onResume()
        stylusPreference.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onPause() {
        super.onPause()
        stylusPreference.unregisterOnSharedPreferenceChangeListener(this)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
    }

    companion object {
        private const val TAG = "LenovoPeripheralManagerPenUtils"
        private const val STYLUS_KEY = "stylus_switch_key"
    }
}