/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lenovo.settings.peripheral

import android.content.Intent
import android.os.Bundle
import androidx.preference.ListPreference
import androidx.preference.Preference
import com.android.settingslib.widget.SettingsBasePreferenceFragment
import com.lenovo.settings.R
import java.text.Collator

class PalmRejectionAppsFragment : SettingsBasePreferenceFragment() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        val context = requireContext()
        val prefs = requireNotNull(preferenceManager.sharedPreferences)
        PalmRejectionProfiles.migrate(prefs)

        val packageManager = context.packageManager
        val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val collator = Collator.getInstance()
        val apps = packageManager.queryIntentActivities(launcherIntent, 0)
            .distinctBy { it.activityInfo.packageName }
            .map { it to it.loadLabel(packageManager).toString() }
            .sortedWith { first, second -> collator.compare(first.second, second.second) }

        val profileLabels = arrayOf(
            getText(R.string.palm_rejection_profile_default),
            getText(R.string.palm_rejection_profile_pen),
            getText(R.string.palm_rejection_profile_game)
        )
        val profileValues = arrayOf(
            PalmRejectionProfiles.DEFAULT,
            PalmRejectionProfiles.PEN,
            PalmRejectionProfiles.GAME
        )

        preferenceScreen = preferenceManager.createPreferenceScreen(context).apply {
            apps.forEach { (app, label) ->
                val profileKey = PalmRejectionProfiles.keyFor(app.activityInfo.packageName)
                addPreference(ListPreference(context).apply {
                    key = profileKey
                    title = label
                    dialogTitle = label
                    icon = app.loadIcon(packageManager)
                    entries = profileLabels
                    entryValues = profileValues
                    isPersistent = false
                    value = prefs.getString(profileKey, PalmRejectionProfiles.DEFAULT)
                    summaryProvider = ListPreference.SimpleSummaryProvider.getInstance()
                    setOnPreferenceChangeListener { _, profile ->
                        val editor = prefs.edit()
                        if (profile == PalmRejectionProfiles.DEFAULT) {
                            editor.remove(profileKey)
                        } else {
                            editor.putString(profileKey, profile as String)
                        }
                        editor.apply()
                        true
                    }
                })
            }
            if (apps.isEmpty()) {
                addPreference(Preference(context).apply {
                    setTitle(R.string.palm_rejection_no_apps)
                    isSelectable = false
                })
            }
        }
    }

    override fun onResume() {
        super.onResume()
        requireActivity().setTitle(R.string.palm_rejection_app_profiles_title)
    }
}
