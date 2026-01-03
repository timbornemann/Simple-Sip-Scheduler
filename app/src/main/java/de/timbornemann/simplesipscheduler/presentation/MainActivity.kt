package de.timbornemann.simplesipscheduler.presentation

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.viewmodel.compose.viewModel
import de.timbornemann.simplesipscheduler.SimpleSipApplication
import de.timbornemann.simplesipscheduler.presentation.overview.OverviewScreen
import de.timbornemann.simplesipscheduler.presentation.quickdrink.QuickDrinkScreen
import de.timbornemann.simplesipscheduler.presentation.settings.SettingsScreen
import de.timbornemann.simplesipscheduler.presentation.stats.StatsScreen
import de.timbornemann.simplesipscheduler.presentation.theme.SimpleSipTheme
import de.timbornemann.simplesipscheduler.receiver.ReminderManager
import de.timbornemann.simplesipscheduler.receiver.ReminderReceiver
import de.timbornemann.simplesipscheduler.receiver.ReminderTimeCalculator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import java.time.LocalDateTime
import java.time.ZoneId

class MainActivity : ComponentActivity() {
    private var viewModel: MainViewModel? = null
    private val quickActionReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == ReminderReceiver.ACTION_ADD_DRINK) {
                val amount = intent.getIntExtra(ReminderReceiver.EXTRA_AMOUNT, 0)
                if (amount > 0) {
                    // Use application context to add drink directly
                    val app = context?.applicationContext as? SimpleSipApplication
                    app?.let {
                        CoroutineScope(Dispatchers.IO).launch {
                            it.drinkRepository.addDrink(amount)
                            val settings = it.settingsRepository
                            val reminderEnabled = settings.reminderEnabled.first()
                            if (reminderEnabled) {
                                val intervalMinutes = settings.reminderInterval.first()
                                val startHour = settings.quietHoursStart.first()
                                val endHour = settings.quietHoursEnd.first()
                                val nextReminder = ReminderTimeCalculator.calculateNextReminderTime(
                                    now = LocalDateTime.now(),
                                    intervalMinutes = intervalMinutes,
                                    quietStartHour = startHour,
                                    quietEndHour = endHour
                                )
                                val nextReminderMillis = nextReminder.atZone(ZoneId.systemDefault())
                                    .toInstant()
                                    .toEpochMilli()
                                val delayMs = (nextReminderMillis - System.currentTimeMillis()).coerceAtLeast(0)
                                ReminderManager.scheduleReminder(it, delayMs)
                                settings.setNextReminderAt(nextReminderMillis)
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        requestNotificationPermissionIfNeeded()
        setContent {
            val app = application as SimpleSipApplication
            viewModel = viewModel(
                factory = MainViewModelFactory(app, app.drinkRepository, app.settingsRepository)
            )
            
            WearApp(viewModel!!)
        }
    }

    override fun onResume() {
        super.onResume()
        val filter = IntentFilter(ReminderReceiver.ACTION_ADD_DRINK).apply {
            addCategory(Intent.CATEGORY_DEFAULT)
        }
        registerReceiver(quickActionReceiver, filter, Context.RECEIVER_EXPORTED)
    }

    override fun onPause() {
        super.onPause()
        try {
            unregisterReceiver(quickActionReceiver)
        } catch (e: Exception) {
            // Receiver might not be registered
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 100)
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WearApp(viewModel: MainViewModel) {
    SimpleSipTheme {
        val pagerState = rememberPagerState(pageCount = { 4 })

        HorizontalPager(state = pagerState) { page ->
            when (page) {
                0 -> OverviewScreen(viewModel)
                1 -> QuickDrinkScreen(viewModel)
                2 -> {
                    // Lazy load heavy screens
                    if (pagerState.currentPage == page) {
                        StatsScreen(viewModel)
                    } else {
                        Box(modifier = Modifier.fillMaxSize())
                    }
                }
                3 -> {
                    if (pagerState.currentPage == page) {
                        SettingsScreen(viewModel)
                    } else {
                        Box(modifier = Modifier.fillMaxSize())
                    }
                }
            }
        }
    }
}
