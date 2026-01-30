package ir.sysfail.chatguard.features.main

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import ir.sysfail.chatguard.features.guide.GuideScreen
import ir.sysfail.chatguard.features.home.HomeScreen
import ir.sysfail.chatguard.features.intro.IntroScreen
import ir.sysfail.chatguard.features.web_frame.WebFrameScreen
import ir.sysfail.chatguard.ui.theme.ChatGuardTheme
import ir.sysfail.chatguard.utils.Screens
import org.koin.androidx.compose.koinViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        error("")

        enableEdgeToEdge()
        setContent {
            val viewModel = koinViewModel<MainViewModel>()
            val navController = rememberNavController()

            val hasKeyPair by viewModel.hasKeyPair.collectAsStateWithLifecycle()

            ChatGuardTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { _ ->
                    if (hasKeyPair == null) return@Scaffold

                    NavHost(
                        startDestination = if (hasKeyPair!!) {
                            Screens.Home
                        } else Screens.Intro,
                        navController = navController
                    ) {
                        composable<Screens.Intro> {
                            IntroScreen(
                                onGoToHome = {
                                    navController.navigate(Screens.Home) {
                                        popUpTo(Screens.Intro) {
                                            inclusive = true
                                        }
                                    }
                                }
                            )
                        }
                        composable<Screens.Home> {
                            HomeScreen(
                                onGoToWebFrame = { platform ->
                                    navController.navigate(Screens.Web(platform))
                                },
                                onGoToIntro = {
                                    navController.navigate(Screens.Intro) {
                                        popUpTo(Screens.Home) {
                                            inclusive = true
                                        }
                                    }
                                },
                                onGoToGuides = {
                                    navController.navigate(Screens.Guides)
                                }
                            )
                        }
                        composable<Screens.Web> {
                            val info = it.toRoute<Screens.Web>()

                            WebFrameScreen(
                                platform = info.platform,
                                onBack = navController::navigateUp
                            )
                        }
                        composable<Screens.Guides> {
                            GuideScreen(
                                onBack = navController::navigateUp
                            )
                        }
                    }
                }
            }
        }
    }
}