package com.example.pocketpay

import android.app.Application
import androidx.work.Configuration
import com.example.pocketpay.work.MaintenanceWorker
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class PocketPayApp : Application(), Configuration.Provider {
    // On-demand WorkManager configuration: works even where the startup initializer has not run.
    override val workManagerConfiguration: Configuration = Configuration.Builder().build()

    override fun onCreate() {
        super.onCreate()
        MaintenanceWorker.schedule(this)
    }
}