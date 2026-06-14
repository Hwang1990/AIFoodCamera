package com.hjun.aifoodcamera

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hjun.aifoodcamera.ui.CameraScreen
import com.hjun.aifoodcamera.ui.theme.AIFoodCameraTheme
import com.hjun.aifoodcamera.viewmodel.CameraViewModel

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            AIFoodCameraTheme {
                val viewModel: CameraViewModel = viewModel(
                    factory = CameraViewModel.Factory(applicationContext)
                )
                CameraScreen(viewModel = viewModel)
            }
        }
    }
}
