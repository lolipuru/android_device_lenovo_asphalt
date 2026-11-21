/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lenovo.settings.peripheral

import android.content.SharedPreferences

object PalmRejectionProfiles {
    const val DEFAULT = "default"
    const val PEN = "pen"
    const val GAME = "game"
    const val APP_KEY_PREFIX = "palm_rejection_profile:"

    private const val MIGRATED_KEY = "palm_rejection_profiles_migrated"

    fun keyFor(packageName: String) = APP_KEY_PREFIX + packageName

    fun gameModeFor(prefs: SharedPreferences, packageName: String?) =
        when (packageName?.let { prefs.getString(keyFor(it), DEFAULT) }) {
            PEN -> PenNodeManager.GAME_MODE_PEN
            GAME -> PenNodeManager.GAME_MODE_GAME
            else -> PenNodeManager.GAME_MODE_NORMAL
        }

    fun migrate(prefs: SharedPreferences) {
        if (prefs.getBoolean(MIGRATED_KEY, false)) return

        val editor = prefs.edit()
        if (prefs.getString("palm_rejection_mode_key", "app_based") == "app_based") {
            val penApps = prefs.getStringSet("palm_rejection_pen_apps_key", null).orEmpty()
            val gameApps = prefs.getStringSet("palm_rejection_game_apps_key", null).orEmpty()
            (gameApps - penApps).forEach { editor.putString(keyFor(it), GAME) }
            penApps.forEach { editor.putString(keyFor(it), PEN) }
        }
        editor.putBoolean(MIGRATED_KEY, true).apply()
    }
}
