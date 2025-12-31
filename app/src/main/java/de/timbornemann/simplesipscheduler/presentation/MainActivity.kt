package de.timbornemann.simplesipscheduler.presentation

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
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
import de.timbornemann.simplesipscheduler.receiver.ReminderReceiver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

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
                        }
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
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