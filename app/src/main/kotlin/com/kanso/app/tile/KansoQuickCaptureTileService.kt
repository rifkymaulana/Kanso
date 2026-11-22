package com.kanso.app.tile

import android.app.PendingIntent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.kanso.app.R
import com.kanso.app.navigation.KansoUris
import com.kanso.app.navigation.createLaunchIntent

class KansoQuickCaptureTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        qsTile?.apply {
            state = Tile.STATE_INACTIVE
            label = getString(R.string.tile_quick_capture_label)
            updateTile()
        }
    }

    override fun onClick() {
        super.onClick()
        val intent = createLaunchIntent(this, KansoUris.QUICK_CAPTURE_EXPENSE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val pendingIntent = PendingIntent.getActivity(
                this,
                3001,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            startActivityAndCollapse(pendingIntent)
        } else {
            @Suppress("DEPRECATION")
            startActivityAndCollapse(intent)
        }
    }
}
