package com.example.vision

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.SystemClock
import android.util.Log
import androidx.camera.core.ImageProxy
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.core.Delegate
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarker
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarkerResult

data class LiveFaceTrackingResult(
    val smoothedXY: FloatArray,
    val landmarkCount: Int,
    val jawOpen: Float,
    val hasFace: Boolean,
)

class FaceLandmarkerHelper(
    private val context: Context,
    private val onLiveResult: (LiveFaceTrackingResult) -> Unit,
) {
    private var liveLandmarker: FaceLandmarker? = null
    private var imageLandmarker: FaceLandmarker? = null
    private val smoother = LandmarkOneEuroSmoother()

    @Volatile
    var smoothingStrength: Float = 0.65f

    @Volatile
    private var lastFrameTimestampMs: Long = 0L

    fun initialize(): Boolean {
        val liveOk = createLiveLandmarker(Delegate.GPU) || createLiveLandmarker(Delegate.CPU)
        val imageOk = createImageLandmarker(Delegate.GPU) || createImageLandmarker(Delegate.CPU)
        return liveOk && imageOk
    }

    private fun createLiveLandmarker(delegate: Delegate): Boolean {
        return try {
            val baseOptions = BaseOptions.builder()
                .setModelAssetPath("face_landmarker.task")
                .setDelegate(delegate)
                .build()

            val options = FaceLandmarker.FaceLandmarkerOptions.builder()
                .setBaseOptions(baseOptions)
                .setRunningMode(RunningMode.LIVE_STREAM)
                .setNumFaces(1)
                .setMinFaceDetectionConfidence(0.5f)
                .setMinFacePresenceConfidence(0.5f)
                .setMinTrackingConfidence(0.5f)
                .setOutputFaceBlendshapes(true)
                .setResultListener { result, _ -> handleLiveStreamResult(result) }
                .setErrorListener { err ->
                    Log.e(TAG, "Live FaceLandmarker error (${delegate.name})", err)
                }
                .build()

            liveLandmarker = FaceLandmarker.createFromOptions(context, options)
            true
        } catch (t: Throwable) {
            Log.w(TAG, "Failed to init LIVE_STREAM with ${delegate.name}", t)
            false
        }
    }

    private fun createImageLandmarker(delegate: Delegate): Boolean {
        return try {
            val baseOptions = BaseOptions.builder()
                .setModelAssetPath("face_landmarker.task")
                .setDelegate(delegate)
                .build()

            val options = FaceLandmarker.FaceLandmarkerOptions.builder()
                .setBaseOptions(baseOptions)
                .setRunningMode(RunningMode.IMAGE)
                .setNumFaces(1)
                .setMinFaceDetectionConfidence(0.45f)
                .build()

            imageLandmarker = FaceLandmarker.createFromOptions(context, options)
            true
        } catch (t: Throwable) {
            Log.w(TAG, "Failed to init IMAGE with ${delegate.name}", t)
            false
        }
    }

    fun detectStaticImage(bitmap: Bitmap): FloatArray? {
        val detector = imageLandmarker ?: return null
        return try {
            val mpImage = BitmapImageBuilder(bitmap).build()
            val result = detector.detect(mpImage)
            val landmarks = result.faceLandmarks().firstOrNull() ?: return null
            val out = FloatArray(landmarks.size * 2)
            for (i in landmarks.indices) {
                out[i * 2] = landmarks[i].x()
                out[i * 2 + 1] = landmarks[i].y()
            }
            out
        } catch (t: Throwable) {
            Log.e(TAG, "Static face detection failed", t)
            null
        }
    }

    fun detectLiveFrame(imageProxy: ImageProxy) {
        val detector = liveLandmarker
        if (detector == null) {
            imageProxy.close()
            return
        }

        try {
            val rawBitmap = imageProxy.toBitmap()
            val rotation = imageProxy.imageInfo.rotationDegrees
            val uprightBitmap = if (rotation != 0) {
                val matrix = Matrix().apply { postRotate(rotation.toFloat()) }
                val rotated = Bitmap.createBitmap(
                    rawBitmap,
                    0,
                    0,
                    rawBitmap.width,
                    rawBitmap.height,
                    matrix,
                    true,
                )
                if (rotated !== rawBitmap) rawBitmap.recycle()
                rotated
            } else {
                rawBitmap
            }

            val ts = maxOf(SystemClock.uptimeMillis(), lastFrameTimestampMs + 1L)
            lastFrameTimestampMs = ts
            val mpImage = BitmapImageBuilder(uprightBitmap).build()
            detector.detectAsync(mpImage, ts)
        } catch (t: Throwable) {
            Log.e(TAG, "Live frame analysis error", t)
        } finally {
            imageProxy.close()
        }
    }

    private fun handleLiveStreamResult(result: FaceLandmarkerResult) {
        val landmarks = result.faceLandmarks().firstOrNull()
        if (landmarks == null || landmarks.isEmpty()) {
            smoother.reset()
            onLiveResult(
                LiveFaceTrackingResult(
                    smoothedXY = FloatArray(0),
                    landmarkCount = 0,
                    jawOpen = 0f,
                    hasFace = false,
                ),
            )
            return
        }

        val count = landmarks.size
        val raw = FloatArray(count * 2)
        for (i in 0 until count) {
            raw[i * 2] = landmarks[i].x()
            raw[i * 2 + 1] = landmarks[i].y()
        }

        val nowMs = SystemClock.uptimeMillis()
        val smoothed = smoother.smooth(raw, count, nowMs, smoothingStrength)

        val rawJaw = result.faceBlendshapes()
            .orElse(emptyList())
            .firstOrNull()
            ?.firstOrNull { it.categoryName() == "jawOpen" }
            ?.score()
            ?: 0f
        val smoothedJaw = smoother.smoothJawOpen(rawJaw, nowMs)

        onLiveResult(
            LiveFaceTrackingResult(
                smoothedXY = smoothed,
                landmarkCount = count,
                jawOpen = smoothedJaw,
                hasFace = true,
            ),
        )
    }

    fun close() {
        runCatching { liveLandmarker?.close() }
        runCatching { imageLandmarker?.close() }
        liveLandmarker = null
        imageLandmarker = null
    }

    companion object {
        private const val TAG = "FaceLandmarkerHelper"
    }
}
