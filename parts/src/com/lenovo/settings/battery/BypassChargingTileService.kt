/*
 * Copyright (C) 2026 The LineageOS Project
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lenovo.settings.battery

import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.util.Log

class BypassChargingTileService : TileService() {

    private val TAG = "BypassTileService"

    override fun onStartListening() {
        super.onStartListening()
        updateTileUI()
    }

    override fun onClick() {
        super.onClick()
        
        val currentState = BypassChargingManager.isBypassEnabled()
        val newState = !currentState
        
        val success = BypassChargingManager.setBypassEnabled(newState)
        
        if (success) {
            Log.d(TAG, "Successfully set bypass charging to: $newState")
        } else {
            Log.e(TAG, "Failed to set bypass charging")
        }

        updateTileUI()
    }

    private fun updateTileUI() {
        val tile = qsTile ?: return
        val isEnabled = BypassChargingManager.isBypassEnabled()

        if (isEnabled) {
            tile.state = Tile.STATE_ACTIVE
        } else {
            tile.state = Tile.STATE_INACTIVE
        }
        
        tile.updateTile()
    }
}