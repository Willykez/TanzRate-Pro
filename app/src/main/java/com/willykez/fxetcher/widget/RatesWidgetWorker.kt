package com.willykez.fxetcher.widget

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.Constraints
import com.willykez.fxetcher.data.RatesRepository
import com.willykez.fxetcher.data.UserPreferencesRepository
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

/**
 * Keeps the home-screen widget's rates fresh in the background. WorkManager's
 * minimum periodic interval is 15 minutes, which is a sensible cadence for a
 * widget anyway.
 */
class RatesWidgetWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val repo = RatesRepository()
            val prefs = UserPreferencesRepository(applicationContext)
            when (val result = repo.fetchRates()) {
                is RatesRepository.FetchResult.Success -> {
                    val previous = prefs.ratesFlow.first()
                    prefs.saveRates(result.rates, previous)
                }
                is RatesRepository.FetchResult.Failure -> { /* keep last cached rates */ }
            }
            RatesWidget().updateAll(applicationContext)
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    companion object {
        private const val PERIODIC_NAME = "rates_widget_periodic_refresh"
        private const val ONE_OFF_NAME = "rates_widget_one_off_refresh"

        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            val request = PeriodicWorkRequestBuilder<RatesWidgetWorker>(15, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .build()
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(PERIODIC_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
        }

        fun runOnce(context: Context) {
            val request = OneTimeWorkRequestBuilder<RatesWidgetWorker>().build()
            WorkManager.getInstance(context)
                .enqueueUniqueWork(ONE_OFF_NAME, ExistingWorkPolicy.REPLACE, request)
        }
    }
}
