package com.hjun.aifoodcamera

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.hjun.aifoodcamera.ui.navigation.AppNavigation
import com.hjun.aifoodcamera.ui.theme.AIFoodCameraTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            AIFoodCameraTheme {
                AppNavigation()
            }
        }
    }
}
