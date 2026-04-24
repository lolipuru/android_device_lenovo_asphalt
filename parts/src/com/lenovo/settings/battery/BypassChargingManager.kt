/*
 * Copyright (C) 2026 The LineageOS Project
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lenovo.settings.battery

import java.io.File
import java.io.IOException

object BypassChargingManager {
    private const val BATT_PROTECT_NODE = "/sys/class/qcom-battery/batt_protect_en"

    fun isBypassEnabled(): Boolean {
        return try {
            val file = File(BATT_PROTECT_NODE)
            if (file.exists() && file.canRead()) {
                val value = file.readText().trim()
                value == "1"
            } else {
                false
            }
        } catch (e: IOException) {
            e.printStackTrace()
            false
        }
    }

    fun setBypassEnabled(enable: Boolean): Boolean {
        return try {
            val file = File(BATT_PROTECT_NODE)
            if (file.exists() && file.canWrite()) {
                val value = if (enable) "1" else "0"
                file.writeText(value)
                true
            } else {
                false
            }
        } catch (e: IOException) {
            e.printStackTrace()
            false
        }
    }
}