/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lenovo.settings.peripheral

import android.app.KeyguardManager
import android.app.StatusBarManager
import android.content.ActivityNotFoundException
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.MediaStore
import android.util.Log
import android.view.KeyEvent

class PenButtonReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_GLOBAL_BUTTON) return
        val event = intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT, KeyEvent::class.java) ?: return
        if (event.action != KeyEvent.ACTION_DOWN || event.repeatCount != 0) return

        val device = event.device ?: return
        if (!PenUtilsService.isLenovoBluetoothPen(device)) return

        when (event.keyCode) {
            PEN_SINGLE_CLICK_KEYCODE ->
                context.getSystemService(StatusBarManager::class.java)?.expandSettingsPanel()
            PEN_DOUBLE_CLICK_KEYCODE -> openCamera(context)
        }
    }

    private fun openCamera(context: Context) {
        val action = if (context.getSystemService(KeyguardManager::class.java)?.isKeyguardLocked == true) {
            MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA_SECURE
        } else {
            MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA
        }

        try {
            context.startActivity(Intent(action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (e: ActivityNotFoundException) {
            Log.w(TAG, "No camera activity for the pen shortcut", e)
        }
    }

    companion object {
        private const val TAG = "LenovoPartsPenButton"
        private const val PEN_SINGLE_CLICK_KEYCODE = KeyEvent.KEYCODE_MACRO_1
        private const val PEN_DOUBLE_CLICK_KEYCODE = KeyEvent.KEYCODE_MACRO_2
    }
}
