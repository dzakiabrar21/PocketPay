package com.example.pocketpay

import android.app.Application
import com.example.pocketpay.work.MaintenanceWorker
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class PocketPayApp : Application() {
    override fun onCreate() {
        super.onCreate()
        MaintenanceWorker.schedule(this)
    }
}