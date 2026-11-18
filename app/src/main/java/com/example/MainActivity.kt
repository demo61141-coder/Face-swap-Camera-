package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.CameraScreen
import com.example.ui.theme.FaceSwapCamTheme
import com.example.viewmodel.FaceSwapViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        setContent {
            FaceSwapCamTheme {
                val context = LocalContext.current
                val viewModel: FaceSwapViewModel = viewModel()
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()

                var hasCamera by remember {
                    mutableStateOf(
                        ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                            PackageManager.PERMISSION_GRANTED,
                    )
                }
                var hasAudio by remember {
                    mutableStateOf(
                        ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                            PackageManager.PERMISSION_GRANTED,
                    )
                }

                val permissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestMultiplePermissions(),
                    onResult = { grants ->
                        hasCamera = grants[Manifest.permission.CAMERA] == true ||
                            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                            PackageManager.PERMISSION_GRANTED
                        hasAudio = grants[Manifest.permission.RECORD_AUDIO] == true ||
                            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                            PackageManager.PERMISSION_GRANTED
                    },
                )

                LaunchedEffect(Unit) {
                    if (!hasCamera || !hasAudio) {
                        permissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.CAMERA,
                                Manifest.permission.RECORD_AUDIO,
                            ),
                        )
                    }
                }

                CameraScreen(
                    uiState = uiState,
                    hasCameraPermission = hasCamera,
                    hasAudioPermission = hasAudio,
                    onRequestPermissions = {
                        permissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.CAMERA,
                                Manifest.permission.RECORD_AUDIO,
                            ),
                        )
                    },
                    viewModel = viewModel,
                )
            }
        }
    }
}
