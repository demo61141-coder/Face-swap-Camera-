package com.example

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
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
                val lifecycleOwner = LocalLifecycleOwner.current
                val viewModel: FaceSwapViewModel = viewModel()
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()

                fun checkPermissions(): Pair<Boolean, Boolean> {
                    val cam = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                        PackageManager.PERMISSION_GRANTED
                    val mic = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                        PackageManager.PERMISSION_GRANTED
                    return cam to mic
                }

                var hasCamera by remember { mutableStateOf(checkPermissions().first) }
                var hasAudio by remember { mutableStateOf(checkPermissions().second) }
                var permissionPromptCount by remember { mutableStateOf(0) }

                DisposableEffect(lifecycleOwner) {
                    val observer = LifecycleEventObserver { _, event ->
                        if (event == Lifecycle.Event.ON_RESUME) {
                            val (cam, mic) = checkPermissions()
                            hasCamera = cam
                            hasAudio = mic
                        }
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)
                    onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
                }

                val permissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestMultiplePermissions(),
                    onResult = { grants ->
                        val (cam, mic) = checkPermissions()
                        hasCamera = grants[Manifest.permission.CAMERA] == true || cam
                        hasAudio = grants[Manifest.permission.RECORD_AUDIO] == true || mic
                        permissionPromptCount++
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
                    showAppSettingsShortcut = permissionPromptCount > 0 && (!hasCamera || !hasAudio),
                    onRequestPermissions = {
                        permissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.CAMERA,
                                Manifest.permission.RECORD_AUDIO,
                            ),
                        )
                    },
                    onOpenAppSettings = {
                        val intent = Intent(
                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.fromParts("package", context.packageName, null),
                        )
                        context.startActivity(intent)
                    },
                    viewModel = viewModel,
                )
            }
        }
    }
}
