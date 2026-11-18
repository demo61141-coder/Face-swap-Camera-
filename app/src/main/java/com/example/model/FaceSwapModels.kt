package com.example.model

import android.graphics.Bitmap
import android.net.Uri
import androidx.annotation.DrawableRes
import androidx.camera.video.Quality

enum class VideoResolution(
    val labelEn: String,
    val labelBn: String,
) {
    SD_480P("480p", "৪৮০পি"),
    HD_720P("720p", "৭২০পি"),
    FHD_1080P("1080p", "১০৮০পি");

    val cameraQuality: Quality
        get() = when (this) {
            SD_480P -> Quality.SD
            HD_720P -> Quality.HD
            FHD_1080P -> Quality.FHD
        }
}

data class CameraSettings(
    val isFrontCamera: Boolean = true,
    val smoothingStrength: Float = 0.65f,
    val blendOpacity: Float = 0.92f,
    val resolution: VideoResolution = VideoResolution.HD_720P,
    val isMirrored: Boolean = true,
)

sealed interface FaceSource {
    data class BundledDrawable(@DrawableRes val resId: Int) : FaceSource
    data class UploadedUri(val uri: Uri) : FaceSource
}

data class CharacterFace(
    val id: String,
    val nameEn: String,
    val nameBn: String,
    val styleEn: String,
    val styleBn: String,
    val source: FaceSource,
    val bitmap: Bitmap? = null,
    val uvLandmarks: FloatArray? = null,
) {
    val isReady: Boolean get() = bitmap != null && uvLandmarks != null
}

data class BilingualMessage(
    val en: String,
    val bn: String,
)

data class FaceSwapUiState(
    val settings: CameraSettings = CameraSettings(),
    val faces: List<CharacterFace> = emptyList(),
    val selectedFaceId: String? = "cyber_aria",
    val isFaceTracking: Boolean = false,
    val isModelReady: Boolean = false,
    val isRecording: Boolean = false,
    val recordingDurationMs: Long = 0L,
    val isValidatingUpload: Boolean = false,
    val errorMessage: BilingualMessage? = null,
    val lastSavedVideoUri: Uri? = null,
    val showSettings: Boolean = false,
    val showGuide: Boolean = false,
)
