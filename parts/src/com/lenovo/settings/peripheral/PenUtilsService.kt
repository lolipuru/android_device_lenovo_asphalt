/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lenovo.settings.peripheral

import android.app.ActivityManager
import android.app.ActivityTaskManager
import android.app.Service
import android.app.TaskStackListener
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.hardware.display.DisplayManager
import android.hardware.display.DisplayManager.DisplayListener
import android.hardware.input.InputManager
import android.hardware.input.InputManager.InputDeviceListener
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.RemoteException
import android.util.Log
import android.view.Display
import android.view.Surface
import androidx.preference.PreferenceManager

class PenUtilsService : Service() {

    private val mainHandler = Handler(Looper.getMainLooper())

    private var isPenModeEnabled = false
    private var isPenModeForced = false
    private var isAutoPanelDirection = true
    private var palmRejectionMode = PALM_MODE_APP_BASED
    private var penApps: Set<String> = emptySet()
    private var gameApps: Set<String> = emptySet()
    private var topPackageName: String? = null

    private lateinit var inputManager: InputManager
    private lateinit var displayManager: DisplayManager
    private lateinit var activityManager: ActivityManager
    private lateinit var sharedPrefs: SharedPreferences

    private val inputDeviceListener = object : InputDeviceListener {
        override fun onInputDeviceAdded(id: Int) = refreshPenMode()
        override fun onInputDeviceRemoved(id: Int) = refreshPenMode()
        override fun onInputDeviceChanged(id: Int) = refreshPenMode()
    }

    private val displayListener = object : DisplayListener {
        override fun onDisplayAdded(displayId: Int) {
            if (displayId == Display.DEFAULT_DISPLAY) refreshPanelDirection()
        }

        override fun onDisplayRemoved(displayId: Int) {}

        override fun onDisplayChanged(displayId: Int) {
            if (displayId == Display.DEFAULT_DISPLAY) refreshPanelDirection()
        }
    }

    private val taskStackListener = object : TaskStackListener() {
        override fun onTaskStackChanged() {
            mainHandler.post { refreshTopPackageAndGameMode() }
        }
    }

    private val sharedPrefsListener = OnSharedPreferenceChangeListener { _, key ->
        when (key) {
            STYLUS_KEY -> {
                isPenModeForced = sharedPrefs.getBoolean(STYLUS_KEY, false)
                refreshPenMode()
            }
            AUTO_PANEL_DIRECTION_KEY -> {
                isAutoPanelDirection = sharedPrefs.getBoolean(AUTO_PANEL_DIRECTION_KEY, true)
                refreshPanelDirection()
            }
            PALM_REJECTION_MODE_KEY,
            PALM_REJECTION_PEN_APPS_KEY,
            PALM_REJECTION_GAME_APPS_KEY -> {
                loadPalmPrefs()
                refreshGameMode()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        if (DEBUG) Log.d(TAG, "Creating service")

        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this)
        sharedPrefs.registerOnSharedPreferenceChangeListener(sharedPrefsListener)

        isPenModeForced = sharedPrefs.getBoolean(STYLUS_KEY, false)
        isAutoPanelDirection = sharedPrefs.getBoolean(AUTO_PANEL_DIRECTION_KEY, true)
        loadPalmPrefs()

        inputManager = getSystemService(Context.INPUT_SERVICE) as InputManager
        inputManager.registerInputDeviceListener(inputDeviceListener, mainHandler)

        displayManager = getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
        displayManager.registerDisplayListener(displayListener, mainHandler)

        activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager

        try {
            ActivityTaskManager.getService().registerTaskStackListener(taskStackListener)
        } catch (e: RemoteException) {
            Log.e(TAG, "Failed to register TaskStackListener", e)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (DEBUG) Log.d(TAG, "onStartCommand")
        refreshPenMode()
        refreshPanelDirection()
        refreshTopPackageAndGameMode()
        return START_STICKY
    }

    override fun onDestroy() {
        if (DEBUG) Log.d(TAG, "onDestroy")
        inputManager.unregisterInputDeviceListener(inputDeviceListener)
        displayManager.unregisterDisplayListener(displayListener)
        sharedPrefs.unregisterOnSharedPreferenceChangeListener(sharedPrefsListener)
        try {
            ActivityTaskManager.getService().unregisterTaskStackListener(taskStackListener)
        } catch (e: RemoteException) {
            Log.w(TAG, "Failed to unregister TaskStackListener", e)
        }
        mainHandler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun loadPalmPrefs() {
        palmRejectionMode = sharedPrefs.getString(PALM_REJECTION_MODE_KEY, PALM_MODE_APP_BASED)
            ?: PALM_MODE_APP_BASED
        penApps = sharedPrefs.getStringSet(PALM_REJECTION_PEN_APPS_KEY, emptySet()) ?: emptySet()
        gameApps = sharedPrefs.getStringSet(PALM_REJECTION_GAME_APPS_KEY, emptySet()) ?: emptySet()
    }

    private fun refreshPenMode() {
        val shouldEnable = isPenModeForced ||
            inputManager.inputDeviceIds.any { isDeviceLenovoPen(it) }

        if (shouldEnable != isPenModeEnabled) {
            isPenModeEnabled = shouldEnable
            Log.d(TAG, "refreshPenMode: $isPenModeEnabled")
            PenNodeManager.setPenMode(isPenModeEnabled)
            if (palmRejectionMode == PALM_MODE_FOLLOW_PEN) {
                refreshGameMode()
            }
        }
    }

    private fun refreshPanelDirection() {
        if (!isAutoPanelDirection) return
        val display = displayManager.getDisplay(Display.DEFAULT_DISPLAY) ?: return
        val direction = when (display.rotation) {
            Surface.ROTATION_0 -> 0
            Surface.ROTATION_90 -> 3
            Surface.ROTATION_180 -> 2
            Surface.ROTATION_270 -> 1
            else -> 0
        }
        if (DEBUG) Log.d(TAG, "refreshPanelDirection: rotation=${display.rotation} -> $direction")
        PenNodeManager.setPanelDirection(direction)
    }

    @Suppress("DEPRECATION")
    private fun refreshTopPackageAndGameMode() {
        topPackageName = activityManager.getRunningTasks(1)
            .firstOrNull()
            ?.topActivity
            ?.packageName
        refreshGameMode()
    }

    private fun refreshGameMode() {
        val targetMode = when (palmRejectionMode) {
            PALM_MODE_OFF -> PenNodeManager.GAME_MODE_NORMAL
            PALM_MODE_FOLLOW_PEN -> {
                if (isPenModeEnabled) PenNodeManager.GAME_MODE_PEN
                else PenNodeManager.GAME_MODE_NORMAL
            }
            PALM_MODE_APP_BASED -> {
                val pkg = topPackageName
                when {
                    pkg != null && gameApps.contains(pkg) -> PenNodeManager.GAME_MODE_GAME
                    pkg != null && penApps.contains(pkg) -> PenNodeManager.GAME_MODE_PEN
                    else -> PenNodeManager.GAME_MODE_NORMAL
                }
            }
            else -> PenNodeManager.GAME_MODE_NORMAL
        }
        if (DEBUG) Log.d(TAG, "refreshGameMode: mode=$palmRejectionMode top=$topPackageName -> $targetMode")
        PenNodeManager.setGameMode(targetMode)
    }

    private fun isDeviceLenovoPen(id: Int): Boolean {
        val inputDevice = inputManager.getInputDevice(id) ?: return false
        return inputDevice.vendorId == LENOVO_PEN_VENDOR_ID &&
            inputDevice.productId == LENOVO_PEN_PRODUCT_ID
    }

    companion object {
        private const val TAG = "LenovoPartsPenUtilsService"
        private const val DEBUG = false

        const val STYLUS_KEY = "stylus_switch_key"
        const val AUTO_PANEL_DIRECTION_KEY = "auto_panel_direction_key"
        const val PALM_REJECTION_MODE_KEY = "palm_rejection_mode_key"
        const val PALM_REJECTION_PEN_APPS_KEY = "palm_rejection_pen_apps_key"
        const val PALM_REJECTION_GAME_APPS_KEY = "palm_rejection_game_apps_key"

        const val PALM_MODE_OFF = "off"
        const val PALM_MODE_FOLLOW_PEN = "follow_pen"
        const val PALM_MODE_APP_BASED = "app_based"

        private const val LENOVO_PEN_VENDOR_ID = 6127
        private const val LENOVO_PEN_PRODUCT_ID = 24959
    }
}
