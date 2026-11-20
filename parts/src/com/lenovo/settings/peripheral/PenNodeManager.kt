/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lenovo.settings.peripheral

import android.util.Log
import java.io.File
import java.io.IOException

object PenNodeManager {
    private const val TAG = "PenNodeManager"

    const val PEN_MODE_NODE = "/proc/support_pen"
    const val GAME_MODE_NODE = "/proc/game_mode"
    const val PANEL_DIRECTION_NODE = "/proc/panel_direction"

    const val GAME_MODE_NORMAL = 0
    const val GAME_MODE_PEN = 1
    const val GAME_MODE_GAME = 2

    fun readInt(path: String): Int? {
        return try {
            val file = File(path)
            if (file.exists() && file.canRead()) {
                file.readText().trim().toIntOrNull()
            } else {
                null
            }
        } catch (e: IOException) {
            Log.w(TAG, "Failed to read $path", e)
            null
        }
    }

    fun writeInt(path: String, value: Int): Boolean {
        return try {
            val file = File(path)
            if (file.exists() && file.canWrite()) {
                file.writeText("$value\n")
                true
            } else {
                Log.w(TAG, "Node not writable: $path")
                false
            }
        } catch (e: IOException) {
            Log.e(TAG, "Failed to write $value to $path", e)
            false
        }
    }

    fun setPenMode(enabled: Boolean): Boolean {
        val target = if (enabled) 1 else 0
        if (readInt(PEN_MODE_NODE) == target) return true
        return writeInt(PEN_MODE_NODE, target)
    }

    fun setGameMode(mode: Int): Boolean {
        if (readInt(GAME_MODE_NODE) == mode) return true
        return writeInt(GAME_MODE_NODE, mode)
    }

    fun setPanelDirection(direction: Int): Boolean {
        if (readInt(PANEL_DIRECTION_NODE) == direction) return true
        return writeInt(PANEL_DIRECTION_NODE, direction)
    }
}

