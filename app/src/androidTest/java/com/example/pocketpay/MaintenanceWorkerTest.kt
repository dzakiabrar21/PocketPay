package com.example.pocketpay

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.ListenableWorker
import androidx.work.WorkManager
import androidx.work.testing.TestListenableWorkerBuilder
import com.example.pocketpay.work.MaintenanceWorker
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Runs the real worker against the real (encrypted) database through the Hilt entry point. */
@RunWith(AndroidJUnit4::class)
class MaintenanceWorkerTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun worker_runsSuccessfully_andIsRepeatable() {
        val worker = TestListenableWorkerBuilder<MaintenanceWorker>(context).build()
        assertEquals(ListenableWorker.Result.success(), runBlocking { worker.doWork() })
        // A second run the same day must also succeed (idempotent accrual, milestones announced once).
        val again = TestListenableWorkerBuilder<MaintenanceWorker>(context).build()
        assertEquals(ListenableWorker.Result.success(), runBlocking { again.doWork() })
    }

    @Test
    fun appStart_schedulesTheDailyJob() {
        MaintenanceWorker.schedule(context)
        val infos = WorkManager.getInstance(context).getWorkInfosForUniqueWork(MaintenanceWorker.UNIQUE_NAME).get()
        assertTrue("daily job not scheduled", infos.isNotEmpty())
    }
}
