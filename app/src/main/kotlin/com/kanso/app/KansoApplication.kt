package com.kanso.app

import android.app.Application
import com.kanso.app.share.ReceiptShareProcessor
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class KansoApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        ReceiptShareProcessor.warmUpOcrModule(this)
    }
}
