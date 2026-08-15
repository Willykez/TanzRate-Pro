package com.willykez.fxetcher

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import com.willykez.fxetcher.ui.FxViewModel
import com.willykez.fxetcher.ui.nav.AppScaffold
import com.willykez.fxetcher.ui.onboarding.OnboardingScreen
import com.willykez.fxetcher.ui.strings.LocalStrings
import com.willykez.fxetcher.ui.strings.ProvideStrings
import com.willykez.fxetcher.ui.theme.FXetcherTheme
import com.willykez.fxetcher.ui.theme.useDarkTheme

class MainActivity : ComponentActivity() {

    private val vm: FxViewModel by viewModels()

    private val requestNotificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* no-op: notifications simply won't show if declined */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Hold the splash frame until we know whether onboarding should show,
        // so there's no flash of the wrong screen on first launch.
        splashScreen.setKeepOnScreenCondition { vm.onboardingDone.value == null }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        setContent {
            val settings by vm.settings.collectAsState()
            val language by vm.language.collectAsState()
            val onboardingDone by vm.onboardingDone.collectAsState()
            val darkTheme = useDarkTheme(settings.themeMode)

            val view = LocalView.current
            SideEffect {
                val controller = WindowCompat.getInsetsController(window, view)
                controller.isAppearanceLightStatusBars = !darkTheme
                controller.isAppearanceLightNavigationBars = !darkTheme
            }

            FXetcherTheme(themeMode = settings.themeMode, dynamicColor = settings.dynamicColor) {
                ProvideStrings(language) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        if (onboardingDone == null) {
                            // Splash is still covering the screen at this point.
                        } else {
                            AnimatedContent(
                                targetState = onboardingDone == true,
                                transitionSpec = { fadeIn() togetherWith fadeOut() },
                                label = "onboardingGate"
                            ) { showApp ->
                                if (showApp) {
                                    AppScaffold(vm)
                                } else {
                                    OnboardingScreen(
                                        strings = LocalStrings.current,
                                        onDone = { vm.completeOnboarding() }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
