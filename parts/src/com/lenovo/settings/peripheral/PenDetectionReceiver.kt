/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lenovo.settings.peripheral

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.view.KeyEvent

class PenDetectionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_GLOBAL_BUTTON) return
        val event = intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT, KeyEvent::class.java) ?: return
        if (event.action != KeyEvent.ACTION_DOWN || event.keyCode != PEN_DETECTION_KEYCODE ||
            event.scanCode != PEN_DETECTION_SCAN_CODE || event.device?.name != TOUCHSCREEN_NAME
        ) return

        context.startService(Intent(context, PenUtilsService::class.java).apply {
            action = PenUtilsService.ACTION_PEN_DETECTED
        })
    }

    companion object {
        // Android's key layout and global key overlay use this platform key code.
        private const val PEN_DETECTION_KEYCODE = KeyEvent.PEN_DETECTION_KEYCODE
        private const val PEN_DETECTION_SCAN_CODE = 249
        private const val TOUCHSCREEN_NAME = "NVTCapacitiveTouchScreen"
    }
}
