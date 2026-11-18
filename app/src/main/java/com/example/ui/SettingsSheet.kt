package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.FlipCameraAndroid
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.CameraSettings
import com.example.model.VideoResolution
import com.example.ui.theme.MintCyan
import com.example.ui.theme.MintCyanDim
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.SurfaceSheet
import com.example.ui.theme.SurfaceStroke
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSheet(
    settings: CameraSettings,
    onFlipCamera: () -> Unit,
    onMirrorChanged: (Boolean) -> Unit,
    onSmoothingChanged: (Float) -> Unit,
    onOpacityChanged: (Float) -> Unit,
    onResolutionSelected: (VideoResolution) -> Unit,
    onDismiss: () -> Unit,
) {
    BackHandler { onDismiss() }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SurfaceSheet,
        contentColor = TextPrimary,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Column {
                Text(
                    text = "Studio Settings",
                    style = MaterialTheme.typography.headlineSmall,
                    color = TextPrimary,
                )
                Text(
                    text = "স্টুডিও সেটিংস • ক্যামেরা ও ফেস মেশ নিয়ন্ত্রণ",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted,
                )
            }

            // Camera Flip Row
            Surface(
                onClick = onFlipCamera,
                color = SurfaceCard,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, SurfaceStroke),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("settings_flip_camera"),
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Icon(Icons.Filled.FlipCameraAndroid, contentDescription = null, tint = MintCyan)
                        Column {
                            Text("Camera Lens", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                            Text("ক্যামেরা লেন্স পরিবর্তন", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                        }
                    }
                    Text(
                        text = if (settings.isFrontCamera) "Front • সামনে" else "Rear • পিছনে",
                        style = MaterialTheme.typography.labelMedium,
                        color = MintCyan,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            // Mirror Toggle Row
            Surface(
                color = SurfaceCard,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, SurfaceStroke),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Icon(Icons.Filled.Flip, contentDescription = null, tint = MintCyan)
                        Column {
                            Text("Mirror Mode", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                            Text("মিরর মোড (অনুভূমিক উল্টানো)", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                        }
                    }
                    Switch(
                        checked = settings.isMirrored,
                        onCheckedChange = onMirrorChanged,
                        modifier = Modifier.testTag("settings_mirror_switch"),
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = ObsidianBg,
                            checkedTrackColor = MintCyan,
                        ),
                    )
                }
            }

            // One Euro Smoothing Slider
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Tune, contentDescription = null, tint = MintCyan)
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text("Smoothing (One Euro)", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                            Text("স্মুথিং মাত্রা (কম কাঁপুনি)", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                        }
                    }
                    Text(
                        text = "${(settings.smoothingStrength * 100).roundToInt()}%",
                        style = MaterialTheme.typography.labelMedium,
                        color = MintCyan,
                    )
                }
                Slider(
                    value = settings.smoothingStrength,
                    onValueChange = onSmoothingChanged,
                    valueRange = 0f..1f,
                    modifier = Modifier.testTag("settings_smoothing_slider"),
                    colors = SliderDefaults.colors(thumbColor = MintCyan, activeTrackColor = MintCyan),
                )
            }

            // Blend / Opacity Slider
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Opacity, contentDescription = null, tint = MintCyan)
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text("Blend / Opacity", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                            Text("ব্লেন্ড ও স্বচ্ছতা", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                        }
                    }
                    Text(
                        text = "${(settings.blendOpacity * 100).roundToInt()}%",
                        style = MaterialTheme.typography.labelMedium,
                        color = MintCyan,
                    )
                }
                Slider(
                    value = settings.blendOpacity,
                    onValueChange = onOpacityChanged,
                    valueRange = 0.2f..1f,
                    modifier = Modifier.testTag("settings_opacity_slider"),
                    colors = SliderDefaults.colors(thumbColor = MintCyan, activeTrackColor = MintCyan),
                )
            }

            // Resolution Selector
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.HighQuality, contentDescription = null, tint = MintCyan)
                    Spacer(Modifier.width(8.dp))
                    Text("Recording Resolution • ভিডিও রেজোলিউশন", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    VideoResolution.entries.forEach { res ->
                        val selected = settings.resolution == res
                        FilterChip(
                            selected = selected,
                            onClick = { onResolutionSelected(res) },
                            label = {
                                Text(
                                    text = "${res.labelEn} • ${res.labelBn}",
                                    style = MaterialTheme.typography.labelSmall,
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("resolution_${res.name}"),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MintCyanDim,
                                selectedLabelColor = MintCyan,
                            ),
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}
