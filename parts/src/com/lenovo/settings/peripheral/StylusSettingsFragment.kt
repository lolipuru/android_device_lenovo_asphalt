/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lenovo.settings.peripheral

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import androidx.preference.ListPreference
import androidx.preference.MultiSelectListPreference
import androidx.preference.PreferenceFragment
import androidx.preference.PreferenceManager
import androidx.preference.SwitchPreference
import com.lenovo.settings.R

@Suppress("DEPRECATION")
class StylusSettingsFragment : PreferenceFragment(),
    SharedPreferences.OnSharedPreferenceChangeListener {

    private lateinit var stylusPreference: SharedPreferences
    private var palmModePref: ListPreference? = null
    private var penAppsPref: MultiSelectListPreference? = null
    private var gameAppsPref: MultiSelectListPreference? = null

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.stylus_settings)

        stylusPreference = PreferenceManager.getDefaultSharedPreferences(context)

        findPreference<SwitchPreference>(PenUtilsService.STYLUS_KEY)?.apply {
            isChecked = stylusPreference.getBoolean(PenUtilsService.STYLUS_KEY, false)
            isEnabled = true
        }

        findPreference<SwitchPreference>(PenUtilsService.AUTO_PANEL_DIRECTION_KEY)?.apply {
            isChecked = stylusPreference.getBoolean(PenUtilsService.AUTO_PANEL_DIRECTION_KEY, true)
        }

        palmModePref = findPreference(PenUtilsService.PALM_REJECTION_MODE_KEY)
        penAppsPref = findPreference(PenUtilsService.PALM_REJECTION_PEN_APPS_KEY)
        gameAppsPref = findPreference(PenUtilsService.PALM_REJECTION_GAME_APPS_KEY)

        populateAppLists()
        updatePalmRejectionUi()
    }

    override fun onResume() {
        super.onResume()
        stylusPreference.registerOnSharedPreferenceChangeListener(this)
        updatePalmRejectionUi()
    }

    override fun onPause() {
        super.onPause()
        stylusPreference.unregisterOnSharedPreferenceChangeListener(this)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        when (key) {
            PenUtilsService.PALM_REJECTION_MODE_KEY,
            PenUtilsService.PALM_REJECTION_PEN_APPS_KEY,
            PenUtilsService.PALM_REJECTION_GAME_APPS_KEY -> updatePalmRejectionUi()
        }
    }

    private fun populateAppLists() {
        val pm = context.packageManager
        val launchIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val apps = pm.queryIntentActivities(launchIntent, 0)
            .map { it.activityInfo.packageName to it.loadLabel(pm).toString() }
            .distinctBy { it.first }
            .sortedBy { it.second.lowercase() }

        val entries = apps.map { it.second }.toTypedArray()
        val entryValues = apps.map { it.first }.toTypedArray()

        penAppsPref?.entries = entries
        penAppsPref?.entryValues = entryValues
        gameAppsPref?.entries = entries
        gameAppsPref?.entryValues = entryValues
    }

    private fun updatePalmRejectionUi() {
        val mode = stylusPreference.getString(
            PenUtilsService.PALM_REJECTION_MODE_KEY,
            PenUtilsService.PALM_MODE_APP_BASED
        )
        palmModePref?.summary = palmModePref?.entry

        val isAppBased = mode == PenUtilsService.PALM_MODE_APP_BASED
        penAppsPref?.isEnabled = isAppBased
        gameAppsPref?.isEnabled = isAppBased

        val penCount = stylusPreference.getStringSet(
            PenUtilsService.PALM_REJECTION_PEN_APPS_KEY,
            emptySet()
        )?.size ?: 0
        val gameCount = stylusPreference.getStringSet(
            PenUtilsService.PALM_REJECTION_GAME_APPS_KEY,
            emptySet()
        )?.size ?: 0

        penAppsPref?.summary = resources.getQuantityString(
            R.plurals.palm_rejection_apps_summary, penCount, penCount
        )
        gameAppsPref?.summary = resources.getQuantityString(
            R.plurals.palm_rejection_apps_summary, gameCount, gameCount
        )
    }
}
