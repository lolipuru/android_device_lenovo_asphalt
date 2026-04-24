/*
 * Copyright (C) 2026 The LineageOS Project
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lenovo.settings.peripheral

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.hardware.input.InputManager
import android.os.IBinder
import android.util.Log
import androidx.preference.PreferenceManager
import java.io.File
import java.io.IOException

class PenUtilsService : Service() {

    private var isPenModeEnabled = false
    private var isPenModeForced = false

    private lateinit var inputManager: InputManager
    private lateinit var sharedPrefs: SharedPreferences

    private val inputDeviceListener = object : InputManager.InputDeviceListener {
        override fun onInputDeviceAdded(id: Int) = refreshPenMode()
        override fun onInputDeviceRemoved(id: Int) = refreshPenMode()
        override fun onInputDeviceChanged(id: Int) = refreshPenMode()
    }

    private val sharedPrefsListener = SharedPreferences.OnSharedPreferenceChangeListener { prefs, key ->
        if (key == STYLUS_KEY) {
            isPenModeForced = prefs.getBoolean(STYLUS_KEY, false)
            refreshPenMode()
        }
    }

    override fun onCreate() {
        super.onCreate()
        if (DEBUG) Log.d(TAG, "Creating service")
        
        inputManager = getSystemService(Context.INPUT_SERVICE) as InputManager
        inputManager.registerInputDeviceListener(inputDeviceListener, null)
        
        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this)
        sharedPrefs.registerOnSharedPreferenceChangeListener(sharedPrefsListener)
        
        isPenModeForced = sharedPrefs.getBoolean(STYLUS_KEY, false)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (DEBUG) Log.d(TAG, "onStartCommand")
        refreshPenMode()
        return START_STICKY
    }

    override fun onDestroy() {
        if (DEBUG) Log.d(TAG, "onDestroy")
        inputManager.unregisterInputDeviceListener(inputDeviceListener)
        sharedPrefs.unregisterOnSharedPreferenceChangeListener(sharedPrefsListener)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun setValue(path: String?, value: String) {
        if (path == null) return

        val file = File(path)
        if (file.exists() && file.canWrite()) {
            try {
                // Kotlin simplifies File I/O significantly 
                file.writeText(value)
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    private fun updatePenMode() {
        Log.d(TAG, "refreshPenMode: $isPenModeEnabled")
        setValue(PEN_MODE_NODE, if (isPenModeEnabled) "1" else "0")
    }

    private fun refreshPenMode() {
        if (isPenModeForced) {
            if (DEBUG) Log.d(TAG, "refreshPenMode: Pen Mode forced")
            if (!isPenModeEnabled) {
                isPenModeEnabled = true
                updatePenMode()
            }
            return
        }
        
        for (id in inputManager.inputDeviceIds) {
            if (isDeviceLenovoPen(id)) {
                if (DEBUG) Log.d(TAG, "refreshPenMode: Found Lenovo Pen")
                if (!isPenModeEnabled) {
                    isPenModeEnabled = true
                    updatePenMode()
                }
                return
            }
        }
        
        if (DEBUG) Log.d(TAG, "refreshPenMode: No Lenovo Pen found")
        if (isPenModeEnabled) {
            isPenModeEnabled = false
            updatePenMode()
        }
    }

    private fun isDeviceLenovoPen(id: Int): Boolean {
        val inputDevice = inputManager.getInputDevice(id) ?: return false
        return inputDevice.vendorId == 6127 && inputDevice.productId == 24959
    }

    companion object {
        private const val TAG = "LenovoPartsPenUtilsService"
        private const val DEBUG = false

        private const val STYLUS_KEY = "stylus_switch_key"
        private const val PEN_MODE_NODE = "/proc/support_pen"
    }
}