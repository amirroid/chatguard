package ir.sysfail.chatguard.features.main

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import ir.sysfail.chatguard.features.home.HomeScreen
import ir.sysfail.chatguard.features.web_frame.WebFrameScreen
import ir.sysfail.chatguard.ui.theme.ChatGuardTheme
import ir.sysfail.chatguard.utils.Screens

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        enableEdgeToEdge()
        setContent {
            val navController = rememberNavController()

            ChatGuardTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { _ ->
                    NavHost(
                        startDestination = Screens.Home,
                        navController = navController
                    ) {
                        composable<Screens.Home> {
                            HomeScreen(
                                onGoToWebFrame = { platform ->
                                    navController.navigate(Screens.Web(platform))
                                }
                            )
                        }
                        composable<Screens.Web> {
                            val info = it.toRoute<Screens.Web>()

                            WebFrameScreen(
                                platform = info.platform
                            )
                        }
                    }
                }
            }
        }
    }
}