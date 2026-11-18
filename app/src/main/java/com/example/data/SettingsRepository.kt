package com.example.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.CameraSettings
import com.example.model.VideoResolution
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore(name = "faceswap_cam_prefs")

class SettingsRepository(private val context: Context) {

    private object Keys {
        val IS_FRONT_CAMERA = booleanPreferencesKey("is_front_camera")
        val SMOOTHING_STRENGTH = floatPreferencesKey("smoothing_strength")
        val BLEND_OPACITY = floatPreferencesKey("blend_opacity")
        val RESOLUTION = stringPreferencesKey("resolution")
        val IS_MIRRORED = booleanPreferencesKey("is_mirrored")
    }

    val settingsFlow: Flow<CameraSettings> = context.settingsDataStore.data.map { prefs ->
        val resName = prefs[Keys.RESOLUTION] ?: VideoResolution.HD_720P.name
        val resolution = runCatching { VideoResolution.valueOf(resName) }
            .getOrDefault(VideoResolution.HD_720P)

        CameraSettings(
            isFrontCamera = prefs[Keys.IS_FRONT_CAMERA] ?: true,
            smoothingStrength = (prefs[Keys.SMOOTHING_STRENGTH] ?: 0.65f).coerceIn(0f, 1f),
            blendOpacity = (prefs[Keys.BLEND_OPACITY] ?: 0.92f).coerceIn(0.2f, 1f),
            resolution = resolution,
            isMirrored = prefs[Keys.IS_MIRRORED] ?: true,
        )
    }

    suspend fun setFrontCamera(isFront: Boolean) {
        context.settingsDataStore.edit { prefs ->
            prefs[Keys.IS_FRONT_CAMERA] = isFront
            prefs[Keys.IS_MIRRORED] = isFront
        }
    }

    suspend fun setSmoothingStrength(value: Float) {
        context.settingsDataStore.edit { prefs ->
            prefs[Keys.SMOOTHING_STRENGTH] = value.coerceIn(0f, 1f)
        }
    }

    suspend fun setBlendOpacity(value: Float) {
        context.settingsDataStore.edit { prefs ->
            prefs[Keys.BLEND_OPACITY] = value.coerceIn(0.2f, 1f)
        }
    }

    suspend fun setResolution(resolution: VideoResolution) {
        context.settingsDataStore.edit { prefs ->
            prefs[Keys.RESOLUTION] = resolution.name
        }
    }

    suspend fun setMirrored(mirrored: Boolean) {
        context.settingsDataStore.edit { prefs ->
            prefs[Keys.IS_MIRRORED] = mirrored
        }
    }
}
