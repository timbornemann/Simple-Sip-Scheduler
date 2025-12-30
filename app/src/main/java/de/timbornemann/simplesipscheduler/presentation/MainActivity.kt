package de.timbornemann.simplesipscheduler.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.ExperimentalFoundationApi
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

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        setContent {
            val app = application as SimpleSipApplication
            val viewModel: MainViewModel = viewModel(
                factory = MainViewModelFactory(app, app.drinkRepository, app.settingsRepository)
            )
            
            WearApp(viewModel)
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
                2 -> StatsScreen(viewModel)
                3 -> SettingsScreen(viewModel)
            }
        }
    }
}