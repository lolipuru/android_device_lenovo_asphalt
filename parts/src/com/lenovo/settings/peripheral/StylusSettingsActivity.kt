/*
 * Copyright (C) 2026 The LineageOS Project
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lenovo.settings.peripheral

import android.os.Bundle
import com.android.settingslib.collapsingtoolbar.CollapsingToolbarBaseActivity

class StylusSettingsActivity : CollapsingToolbarBaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        fragmentManager.beginTransaction().replace(
            com.android.settingslib.collapsingtoolbar.R.id.content_frame,
            StylusSettingsFragment(), 
            TAG_STYLUS
        ).commit()
    }

    companion object {
        private const val TAG_STYLUS = "stylus"
    }
}