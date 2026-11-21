/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lenovo.settings.peripheral

import android.os.Bundle
import androidx.preference.Preference
import com.android.settingslib.widget.SettingsBasePreferenceFragment
import com.lenovo.settings.R

class StylusSettingsFragment : SettingsBasePreferenceFragment() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.stylus_settings, rootKey)
        findPreference<Preference>("palm_rejection_app_profiles")?.setOnPreferenceClickListener {
            parentFragmentManager.beginTransaction()
                .setReorderingAllowed(true)
                .replace(
                    com.android.settingslib.collapsingtoolbar.R.id.content_frame,
                    PalmRejectionAppsFragment()
                )
                .addToBackStack(null)
                .commit()
            true
        }
    }

    override fun onResume() {
        super.onResume()
        requireActivity().setTitle(R.string.stylus_title)
    }
}
