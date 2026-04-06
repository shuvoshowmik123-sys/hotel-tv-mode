package com.hotelvision.launcher

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.app.ActivityOptionsCompat
import com.hotelvision.launcher.performance.LocalLauncherPerformanceProfile
import com.hotelvision.launcher.performance.LauncherPerformanceProfile
import com.hotelvision.launcher.performance.resolveLauncherPerformanceProfile
import com.hotelvision.launcher.setup.DefaultLauncherCoordinator
import com.hotelvision.launcher.setup.DefaultLauncherFlow
import com.hotelvision.launcher.setup.DefaultLauncherPromptStore
import com.hotelvision.launcher.setup.DefaultLauncherRequest
import com.hotelvision.launcher.setup.DefaultLauncherUiState
import com.hotelvision.launcher.ui.LauncherAction
import com.hotelvision.launcher.ui.LauncherScreen
import com.hotelvision.launcher.ui.LauncherViewModel
import com.hotelvision.launcher.ui.theme.HotelVisionTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: LauncherViewModel by viewModels()
    private val performanceProfile by lazy { resolveLauncherPerformanceProfile(applicationContext) }
    private val defaultLauncherCoordinator by lazy {
        DefaultLauncherCoordinator(
            context = this,
            launcherPackageName = packageName,
            launcherActivityClassName = MainActivity::class.java.name
        )
    }
    private var defaultLauncherUiState by mutableStateOf<DefaultLauncherUiState?>(null)
    private var showDefaultLauncherPrompt by mutableStateOf(false)
    private val roleRequestLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            refreshDefaultLauncherState(showPromptIfNotDefault = true)
        }

    // Inactivity idle timer
    private val idleHandler = Handler(Looper.getMainLooper())
    private val idleTimeoutMs get() = viewModel.screensaverTimeoutMs
    private val launchScreensaver = Runnable {
        val images = viewModel.screensaverImages
        val intent = Intent(this, ScreensaverActivity::class.java).apply {
            putStringArrayListExtra(ScreensaverActivity.EXTRA_IMAGES, ArrayList(images))
        }
        startActivity(intent)
    }

    private fun resetIdleTimer() {
        idleHandler.removeCallbacks(launchScreensaver)
        idleHandler.postDelayed(launchScreensaver, idleTimeoutMs)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        configureImmersiveWindow()

        viewModel.onIntentAction(intent?.action)
        resetIdleTimer()
        refreshDefaultLauncherState(
            showPromptIfNotDefault = shouldPromptForDefaultLauncher()
        )

        setContent {
            val uiState by viewModel.uiState.collectAsState()
            val activeDefaultLauncherState = defaultLauncherUiState ?: remember {
                defaultLauncherCoordinator.buildUiState()
            }

            CompositionLocalProvider(LocalLauncherPerformanceProfile provides performanceProfile) {
                HotelVisionTheme {
                    LauncherScreen(
                        uiState = uiState,
                        launcherSetupState = activeDefaultLauncherState,
                        defaultLauncherPromptVisible = showDefaultLauncherPrompt,
                        onDestinationSelected = viewModel::onDestinationSelected,
                        onAction = ::handleLauncherAction,
                        onRequestDefaultLauncher = ::requestDefaultLauncher,
                        onOpenVendorSettings = ::openVendorSettings,
                        onDismissDefaultLauncherPrompt = ::dismissDefaultLauncherPrompt,
                        onInstalledAppOrderChanged = viewModel::saveInstalledAppOrder,
                        onResetGuestPersonalization = viewModel::resetGuestPersonalization
                    )
                }
            }
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onBackPressed() {
    }

    override fun onResume() {
        super.onResume()
        resetIdleTimer()
        refreshDefaultLauncherState(
            showPromptIfNotDefault = shouldPromptForDefaultLauncher()
        )
    }

    override fun onPause() {
        super.onPause()
        idleHandler.removeCallbacks(launchScreensaver)
    }

    override fun onDestroy() {
        super.onDestroy()
        idleHandler.removeCallbacks(launchScreensaver)
    }

    override fun onUserInteraction() {
        super.onUserInteraction()
        resetIdleTimer()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        viewModel.onIntentAction(intent.action)
        configureImmersiveWindow()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            configureImmersiveWindow()
        }
    }

    private fun handleLauncherAction(action: LauncherAction) {
        when (action) {
            LauncherAction.None -> Unit
            LauncherAction.OpenNotificationsPanel -> Unit
            LauncherAction.OpenControlPanel -> Unit
            LauncherAction.ExitTransientUi -> Unit
            LauncherAction.ResetGuestPersonalization -> viewModel.resetGuestPersonalization()
            is LauncherAction.OpenDestination -> viewModel.onDestinationSelected(action.destination)
            is LauncherAction.EnterAppMoveMode -> Unit
            is LauncherAction.LaunchIntent -> launchIntent(action.intent)
            is LauncherAction.LaunchPackage -> {
                val appIntent = packageManager.getLeanbackLaunchIntentForPackage(action.packageName)
                    ?: packageManager.getLaunchIntentForPackage(action.packageName)
                if (appIntent != null) {
                    launchIntent(appIntent)
                }
            }
            LauncherAction.OpenAllApps -> Unit
        }
    }

    private fun launchIntent(intent: Intent) {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (performanceProfile == LauncherPerformanceProfile.LOW_RAM) {
            runCatching { startActivity(intent) }
        } else {
            val options = ActivityOptionsCompat.makeSceneTransitionAnimation(this).toBundle()
            runCatching { startActivity(intent, options) }
        }
    }

    private fun requestDefaultLauncher() {
        when (val request = defaultLauncherCoordinator.resolveRequest()) {
            is DefaultLauncherRequest.RoleRequest -> {
                defaultLauncherCoordinator.markFlowAttempted(DefaultLauncherFlow.ROLE_MANAGER)
                roleRequestLauncher.launch(request.intent)
            }

            is DefaultLauncherRequest.HomeChooserRequest -> {
                defaultLauncherCoordinator.markFlowAttempted(DefaultLauncherFlow.HOME_CHOOSER)
                showDefaultLauncherPrompt = true
                runCatching { startActivity(request.intent) }
            }

            is DefaultLauncherRequest.VendorSettingsRequest -> {
                defaultLauncherCoordinator.markFlowAttempted(DefaultLauncherFlow.VENDOR_SETTINGS)
                showDefaultLauncherPrompt = true
                runCatching { startActivity(request.option.intent) }
            }

            DefaultLauncherRequest.ProvisioningInstructions -> {
                showDefaultLauncherPrompt = true
            }
        }
    }

    private fun openVendorSettings() {
        val vendorSettingsIntent = defaultLauncherCoordinator.resolveVendorSettingsIntent() ?: return
        showDefaultLauncherPrompt = true
        runCatching { startActivity(vendorSettingsIntent) }
    }

    private fun dismissDefaultLauncherPrompt() {
        showDefaultLauncherPrompt = false
        DefaultLauncherPromptStore.markPromptPending(this, false)
    }

    private fun refreshDefaultLauncherState(showPromptIfNotDefault: Boolean) {
        val currentState = defaultLauncherCoordinator.buildUiState()
        defaultLauncherUiState = currentState
        if (currentState.isDefaultLauncher) {
            defaultLauncherCoordinator.clearPromptState()
            showDefaultLauncherPrompt = false
        } else if (showPromptIfNotDefault) {
            showDefaultLauncherPrompt = true
        }
    }

    private fun shouldPromptForDefaultLauncher(): Boolean {
        return DefaultLauncherPromptStore.isPromptPending(this) ||
            !defaultLauncherCoordinator.isLauncherDefaultHome()
    }

    private fun configureImmersiveWindow() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }
}
