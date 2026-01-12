package de.timbornemann.simplesipscheduler.presentation

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.compose.setContent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.health.connect.client.permission.PermissionController
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import de.timbornemann.simplesipscheduler.SimpleSipApplication
import de.timbornemann.simplesipscheduler.data.health.HealthConnectManager
import de.timbornemann.simplesipscheduler.presentation.overview.OverviewScreen
import de.timbornemann.simplesipscheduler.presentation.quickdrink.QuickDrinkScreen
import de.timbornemann.simplesipscheduler.presentation.settings.SettingsScreen
import de.timbornemann.simplesipscheduler.presentation.stats.StatsScreen
import de.timbornemann.simplesipscheduler.presentation.theme.SimpleSipTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private var viewModel: MainViewModel? = null
    private lateinit var healthConnectPermissionLauncher: ActivityResultLauncher<Set<String>>
    private lateinit var healthConnectManager: HealthConnectManager

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        requestNotificationPermissionIfNeeded()
        val app = application as SimpleSipApplication
        healthConnectManager = app.healthConnectManager
        healthConnectPermissionLauncher = registerForActivityResult(
            PermissionController.createRequestPermissionResultContract()
        ) { _ ->
            // No-op, UI will update based on granted permissions.
        }
        requestHealthConnectPermissionsIfNeeded()
        setContent {
            viewModel = viewModel(
                factory = MainViewModelFactory(
                    app,
                    app.drinkRepository,
                    app.settingsRepository,
                    app.healthConnectManager
                )
            )
            
            WearApp(viewModel!!)
        }
    }

    override fun onResume() {
        super.onResume()
        // Check schedule on resume to ensure alarm is set
        viewModel?.let { vm ->
            CoroutineScope(Dispatchers.IO).launch {
                vm.checkSchedule()
            }
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 100)
            }
        }
    }

    private fun requestHealthConnectPermissionsIfNeeded() {
        if (!healthConnectManager.isAvailable()) {
            return
        }
        lifecycleScope.launch {
            val granted = healthConnectManager.hasHydrationPermissions()
            if (!granted) {
                healthConnectPermissionLauncher.launch(healthConnectManager.hydrationPermissions)
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
