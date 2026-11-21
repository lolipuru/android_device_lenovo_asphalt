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
import android.view.InputEvent
import android.view.InputEventReceiver
import android.view.InputMonitor
import android.view.MotionEvent
import android.view.Surface
import androidx.preference.PreferenceManager
import kotlin.math.abs

class PenUtilsService : Service() {

    private val mainHandler = Handler(Looper.getMainLooper())

    private var isPenModeEnabled = false
    private var isPenModeForced = false
    private var isAutoPanelDirection = true
    private var isPenRefreshRateRequested = false
    private val activeStylusDevices = mutableSetOf<Int>()
    private var topPackageName: String? = null

    private lateinit var inputManager: InputManager
    private lateinit var displayManager: DisplayManager
    private lateinit var activityManager: ActivityManager
    private lateinit var sharedPrefs: SharedPreferences

    private var inputMonitor: InputMonitor? = null
    private var inputEventReceiver: InputEventReceiver? = null

    private val releasePenRefreshRate = Runnable {
        setPenRefreshRateRequested(false)
    }

    private val inputDeviceListener = object : InputDeviceListener {
        override fun onInputDeviceAdded(id: Int) = refreshPenMode()
        override fun onInputDeviceRemoved(id: Int) {
            endStylusInteraction(id)
            refreshPenMode()
        }
        override fun onInputDeviceChanged(id: Int) = refreshPenMode()
    }

    private val displayListener = object : DisplayListener {
        override fun onDisplayAdded(displayId: Int) {
            if (displayId == Display.DEFAULT_DISPLAY) refreshPanelDirection()
        }

        override fun onDisplayRemoved(displayId: Int) {
            if (displayId == Display.DEFAULT_DISPLAY) resetStylusInteraction()
        }

        override fun onDisplayChanged(displayId: Int) {
            if (displayId != Display.DEFAULT_DISPLAY) return
            if (displayManager.getDisplay(displayId)?.state != Display.STATE_ON) {
                resetStylusInteraction()
            }
            refreshPanelDirection()
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
            else -> {
                if (key?.startsWith(PalmRejectionProfiles.APP_KEY_PREFIX) == true) {
                    refreshGameMode()
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        if (DEBUG) Log.d(TAG, "Creating service")

        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this)
        PalmRejectionProfiles.migrate(sharedPrefs)
        sharedPrefs.registerOnSharedPreferenceChangeListener(sharedPrefsListener)

        isPenModeForced = sharedPrefs.getBoolean(STYLUS_KEY, false)
        isAutoPanelDirection = sharedPrefs.getBoolean(AUTO_PANEL_DIRECTION_KEY, true)

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

        startStylusMonitor()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_PEN_DETECTED) {
            onPenDetected()
            return START_STICKY
        }
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
        stopStylusMonitor()
        mainHandler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    @Suppress("DEPRECATION")
    private fun startStylusMonitor() {
        try {
            val monitor = inputManager.monitorGestureInput(
                "LenovoPartsStylusMonitor",
                Display.DEFAULT_DISPLAY
            )
            inputMonitor = monitor
            inputEventReceiver =
                object : InputEventReceiver(monitor.inputChannel, mainHandler.looper) {
                    override fun onInputEvent(event: InputEvent) {
                        try {
                            if (event is MotionEvent) handleStylusMotion(event)
                        } finally {
                            finishInputEvent(event, false)
                        }
                    }
                }
        } catch (e: RuntimeException) {
            Log.e(TAG, "Failed to start stylus InputMonitor", e)
            stopStylusMonitor()
        }
    }

    private fun stopStylusMonitor() {
        resetStylusInteraction()
        inputEventReceiver?.dispose()
        inputEventReceiver = null
        inputMonitor?.dispose()
        inputMonitor = null
    }

    private fun handleStylusMotion(event: MotionEvent) {
        val action = event.actionMasked
        val deviceId = event.deviceId
        when (action) {
            MotionEvent.ACTION_UP, MotionEvent.ACTION_HOVER_EXIT, MotionEvent.ACTION_CANCEL -> {
                endStylusInteraction(deviceId)
                return
            }
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE, MotionEvent.ACTION_POINTER_DOWN,
            MotionEvent.ACTION_POINTER_UP, MotionEvent.ACTION_HOVER_ENTER,
            MotionEvent.ACTION_HOVER_MOVE -> Unit
            else -> return
        }

        val liftedPointer = if (action == MotionEvent.ACTION_POINTER_UP) event.actionIndex else -1
        val hasStylus = (0 until event.pointerCount).any { index ->
            if (index == liftedPointer) return@any false
            val toolType = event.getToolType(index)
            toolType == MotionEvent.TOOL_TYPE_STYLUS || toolType == MotionEvent.TOOL_TYPE_ERASER
        }
        if (!hasStylus) {
            endStylusInteraction(deviceId)
            return
        }
        if (isPenRefreshRateRequested && deviceId in activeStylusDevices) return

        val display = displayManager.getDisplay(Display.DEFAULT_DISPLAY) ?: return
        if (display.state != Display.STATE_ON) return

        if (activeStylusDevices.add(deviceId)) {
            mainHandler.removeCallbacks(releasePenRefreshRate)
        }
        requestPenRefreshRate(display)
    }

    private fun onPenDetected() {
        val display = displayManager.getDisplay(Display.DEFAULT_DISPLAY) ?: return
        if (display.state != Display.STATE_ON) return

        requestPenRefreshRate(display)
        if (isPenRefreshRateRequested && activeStylusDevices.isEmpty()) {
            schedulePenRefreshRelease()
        }
    }

    private fun requestPenRefreshRate(display: Display) {
        if (isPenRefreshRateRequested) return

        val modes = display.supportedModes
        if (modes.any { abs(it.refreshRate - STYLUS_REFRESH_RATE_HZ) < 0.01f } &&
            modes.any { it.refreshRate > STYLUS_REFRESH_RATE_HZ + 0.01f }
        ) {
            setPenRefreshRateRequested(true)
        }
    }

    private fun endStylusInteraction(deviceId: Int) {
        if (activeStylusDevices.remove(deviceId) && activeStylusDevices.isEmpty()) {
            schedulePenRefreshRelease()
        }
    }

    private fun schedulePenRefreshRelease() {
        mainHandler.removeCallbacks(releasePenRefreshRate)
        mainHandler.postDelayed(releasePenRefreshRate, STYLUS_REFRESH_HOLD_MS)
    }

    private fun resetStylusInteraction() {
        activeStylusDevices.clear()
        mainHandler.removeCallbacks(releasePenRefreshRate)
        setPenRefreshRateRequested(false)
    }

    private fun setPenRefreshRateRequested(requested: Boolean) {
        if (isPenRefreshRateRequested == requested) return
        try {
            val maxRefreshRate = if (requested) STYLUS_REFRESH_RATE_HZ else 0f
            displayManager.requestMaxRefreshRate(Display.DEFAULT_DISPLAY, maxRefreshRate)
            isPenRefreshRateRequested = requested
        } catch (e: RuntimeException) {
            Log.w(TAG, "Failed to update the stylus refresh rate request", e)
        }
    }

    private fun refreshPenMode() {
        val shouldEnable = isPenModeForced ||
            inputManager.inputDeviceIds.any { isDeviceLenovoPen(it) }

        if (shouldEnable != isPenModeEnabled) {
            isPenModeEnabled = shouldEnable
            Log.d(TAG, "refreshPenMode: $isPenModeEnabled")
            PenNodeManager.setPenMode(isPenModeEnabled)
        }
    }

    private fun refreshPanelDirection() {
        if (!isAutoPanelDirection) return
        val display = displayManager.getDisplay(Display.DEFAULT_DISPLAY) ?: return
        // ZUI panel_direction: 0=portrait(cam top), 1=landscape(usb on user),
        // 2=portrait(cam bottom), 3=landscape(usb opposite). Android ROTATION_90 -> 3.
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
        val targetMode = PalmRejectionProfiles.gameModeFor(sharedPrefs, topPackageName)
        if (DEBUG) Log.d(TAG, "refreshGameMode: top=$topPackageName -> $targetMode")
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

        const val ACTION_PEN_DETECTED = "com.lenovo.settings.action.PEN_DETECTED"
        const val STYLUS_KEY = "stylus_switch_key"
        const val AUTO_PANEL_DIRECTION_KEY = "auto_panel_direction_key"

        private const val STYLUS_REFRESH_RATE_HZ = 120f
        private const val STYLUS_REFRESH_HOLD_MS = 3_000L

        private const val LENOVO_PEN_VENDOR_ID = 6127
        private const val LENOVO_PEN_PRODUCT_ID = 24959
    }
}
