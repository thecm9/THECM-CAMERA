package com.example.camera

import android.annotation.SuppressLint
import android.content.Context
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.CaptureResult
import android.hardware.camera2.TotalCaptureResult
import android.hardware.camera2.params.RggbChannelVector
import android.net.Uri
import android.util.Log
import androidx.camera.camera2.interop.Camera2CameraControl
import androidx.camera.camera2.interop.Camera2CameraInfo
import androidx.camera.camera2.interop.Camera2Interop
import androidx.camera.camera2.interop.CaptureRequestOptions
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.camera.core.Camera
import androidx.camera.core.CameraControl
import androidx.camera.core.CameraInfo
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.MeteringPointFactory
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.example.model.DetectedExposure
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors
import kotlin.math.roundToInt

@androidx.annotation.OptIn(ExperimentalCamera2Interop::class)
class ProCameraEngine(private val context: Context) {

  private val TAG = "ProCameraEngine"
  private val cameraExecutor = Executors.newSingleThreadExecutor()

  private var cameraProvider: ProcessCameraProvider? = null
  private var camera: Camera? = null
  private var imageCapture: ImageCapture? = null
  private var imageAnalysis: ImageAnalysis? = null
  private var preview: Preview? = null

  private val _exposureState = MutableStateFlow(DetectedExposure())
  val exposureState: StateFlow<DetectedExposure> = _exposureState.asStateFlow()

  private var currentLensFacing = CameraSelector.LENS_FACING_BACK
  private var currentFps = 24
  private var isOpenGateMode = false

  // Active manual override targets
  private var manualIso: Int? = null
  private var manualShutterNanos: Long? = null
  private var manualKelvin: Int? = null
  private var manualFocusDistance: Float? = null
  private var manualEvIndex: Int = 0

  private val analysisBins = FloatArray(32)

  fun startCamera(
    lifecycleOwner: LifecycleOwner,
    previewView: PreviewView,
    lensFacing: Int = CameraSelector.LENS_FACING_BACK,
    onInitialized: () -> Unit = {}
  ) {
    currentLensFacing = lensFacing
    val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

    cameraProviderFuture.addListener({
      try {
        cameraProvider = cameraProviderFuture.get()
        bindCameraUseCases(lifecycleOwner, previewView)
        onInitialized()
      } catch (e: Exception) {
        Log.e(TAG, "Use case binding failed", e)
      }
    }, ContextCompat.getMainExecutor(context))
  }

  @SuppressLint("UnsafeOptInUsageError")
  private fun bindCameraUseCases(
    lifecycleOwner: LifecycleOwner,
    previewView: PreviewView
  ) {
    if (lifecycleOwner.lifecycle.currentState == androidx.lifecycle.Lifecycle.State.DESTROYED) {
      return
    }
    val provider = cameraProvider ?: return
    try {
      provider.unbindAll()
    } catch (e: Exception) {
      Log.w(TAG, "Error during unbindAll", e)
    }

    val cameraSelector = CameraSelector.Builder()
      .requireLensFacing(currentLensFacing)
      .build()

    // Preview Builder with Camera2 interop for real-time sensor capture telemetry
    val previewBuilder = Preview.Builder()
    val camera2Extender = Camera2Interop.Extender(previewBuilder)

    val captureCallback = object : CameraCaptureSession.CaptureCallback() {
      override fun onCaptureCompleted(
        session: CameraCaptureSession,
        request: CaptureRequest,
        result: TotalCaptureResult
      ) {
        super.onCaptureCompleted(session, request, result)
        processCaptureResult(result)
      }
    }
    camera2Extender.setSessionCaptureCallback(captureCallback)

    preview = previewBuilder.build().apply {
      setSurfaceProvider(previewView.surfaceProvider)
    }

    // Maximum quality ImageCapture for pro manual and Open Gate sensor shots
    imageCapture = ImageCapture.Builder()
      .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
      .setJpegQuality(100)
      .build()

    // Real-time luminance histogram analysis
    imageAnalysis = ImageAnalysis.Builder()
      .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
      .build()
      .apply {
        setAnalyzer(cameraExecutor) { imageProxy ->
          analyzeLuminance(imageProxy)
          imageProxy.close()
        }
      }

    try {
      camera = provider.bindToLifecycle(
        lifecycleOwner,
        cameraSelector,
        preview,
        imageCapture,
        imageAnalysis
      )
      applyManualOverrides()
    } catch (exc: Exception) {
      Log.e(TAG, "Failed to bind camera use cases", exc)
    }
  }

  /**
   * Reads real-time hardware telemetry on every camera frame.
   */
  private fun processCaptureResult(result: TotalCaptureResult) {
    val iso = result.get(CaptureResult.SENSOR_SENSITIVITY) ?: manualIso ?: 100
    val exposureTimeNanos = result.get(CaptureResult.SENSOR_EXPOSURE_TIME) ?: manualShutterNanos ?: 8_000_000L
    val aperture = result.get(CaptureResult.LENS_APERTURE) ?: 1.8f
    val focusDiopters = result.get(CaptureResult.LENS_FOCUS_DISTANCE) ?: 0.5f

    // Calculate approximate focus distance in meters (1 / diopters)
    val focusMeters = if (focusDiopters > 0.01f) {
      (1.0f / focusDiopters).coerceIn(0.05f, 50.0f)
    } else {
      100.0f // Infinity
    }

    // Calculate Shutter Speed formatted string
    val shutterSec = exposureTimeNanos.toDouble() / 1_000_000_000.0
    val shutterString = formatShutterSpeed(shutterSec)

    // Calculate Cinema Shutter Angle: Angle = ExposureTime * FrameRate * 360°
    val shutterAngle = ((shutterSec * currentFps.toDouble() * 360.0) % 360.0).toFloat().coerceIn(1.0f, 360.0f)

    // Calculate Kelvin from R/G/B color correction gains if available
    val colorGains = result.get(CaptureResult.COLOR_CORRECTION_GAINS)
    val calculatedKelvin = if (colorGains != null) {
      estimateKelvinFromGains(colorGains)
    } else {
      manualKelvin ?: 5600
    }

    val evComp = result.get(CaptureResult.CONTROL_AE_EXPOSURE_COMPENSATION) ?: manualEvIndex
    val evFloat = evComp * 0.333f

    _exposureState.value = _exposureState.value.copy(
      iso = iso,
      shutterNanos = exposureTimeNanos,
      shutterString = shutterString,
      shutterAngle = ((shutterAngle * 10f).roundToInt() / 10f),
      kelvin = calculatedKelvin,
      aperture = aperture,
      ev = (evFloat * 10f).roundToInt() / 10f,
      focusDistanceMeters = (focusMeters * 10f).roundToInt() / 10f,
    )
  }

  /**
   * Estimates Correlated Color Temperature (Kelvin) from RGGB color gains.
   */
  private fun estimateKelvinFromGains(gains: RggbChannelVector): Int {
    val r = gains.red.coerceAtLeast(0.1f)
    val b = gains.blue.coerceAtLeast(0.1f)
    // Higher blue relative to red indicates cooler light (higher Kelvin)
    val ratio = b / r
    val kelvin = (3000.0 * ratio + 2200.0).toInt().coerceIn(2000, 10000)
    return (kelvin / 50) * 50 // Round to nearest 50K
  }

  private fun formatShutterSpeed(seconds: Double): String {
    return when {
      seconds <= 0.0 -> "1/8000"
      seconds < 0.3 -> {
        val denom = (1.0 / seconds).roundToInt()
        "1/$denom"
      }
      seconds < 1.0 -> {
        val rounded = (seconds * 10.0).roundToInt() / 10.0
        "${rounded}s"
      }
      else -> {
        val rounded = (seconds * 10.0).roundToInt() / 10.0
        "${rounded}\""
      }
    }
  }

  /**
   * Samples Y-plane pixels to build a lightweight 32-bin luminance histogram.
   */
  private fun analyzeLuminance(imageProxy: ImageProxy) {
    val buffer = imageProxy.planes[0].buffer
    val size = buffer.remaining()
    if (size == 0) return

    val rawCounts = IntArray(32)
    val step = (size / 1024).coerceAtLeast(1)
    var sampled = 0

    var i = 0
    while (i < size) {
      val y = buffer.get(i).toInt() and 0xFF
      val bin = (y * 32 / 256).coerceIn(0, 31)
      rawCounts[bin]++
      sampled++
      i += step
    }

    if (sampled > 0) {
      var maxCount = 1
      for (c in rawCounts) {
        if (c > maxCount) maxCount = c
      }
      for (j in 0 until 32) {
        analysisBins[j] = rawCounts[j].toFloat() / maxCount.toFloat()
      }
      _exposureState.value = _exposureState.value.copy(histogramBins = analysisBins.clone())
    }
  }

  /**
   * Applies manual overrides for ISO, Shutter, Kelvin, EV, and Focus to Camera2.
   */
  fun setManualIso(iso: Int?) {
    manualIso = iso
    applyManualOverrides()
    if (iso != null) {
      _exposureState.value = _exposureState.value.copy(iso = iso)
    }
  }

  fun setManualShutterNanos(shutterNanos: Long?) {
    manualShutterNanos = shutterNanos
    applyManualOverrides()
    if (shutterNanos != null) {
      val sec = shutterNanos.toDouble() / 1_000_000_000.0
      val str = formatShutterSpeed(sec)
      val angle = ((sec * currentFps.toDouble() * 360.0) % 360.0).toFloat().coerceIn(1.0f, 360.0f)
      _exposureState.value = _exposureState.value.copy(
        shutterNanos = shutterNanos,
        shutterString = str,
        shutterAngle = ((angle * 10f).roundToInt() / 10f)
      )
    }
  }

  fun setManualKelvin(kelvin: Int?) {
    manualKelvin = kelvin
    applyManualOverrides()
    if (kelvin != null) {
      _exposureState.value = _exposureState.value.copy(kelvin = kelvin)
    }
  }

  fun setManualFocusDistance(diopters: Float?) {
    manualFocusDistance = diopters
    applyManualOverrides()
    if (diopters != null) {
      val meters = if (diopters > 0.01f) (1.0f / diopters).coerceIn(0.05f, 50.0f) else 100f
      _exposureState.value = _exposureState.value.copy(
        focusDistanceMeters = (meters * 10f).roundToInt() / 10f
      )
    }
  }

  fun setEvIndex(evIndex: Int) {
    manualEvIndex = evIndex
    camera?.cameraControl?.setExposureCompensationIndex(evIndex)
    _exposureState.value = _exposureState.value.copy(ev = (evIndex * 0.333f * 10f).roundToInt() / 10f)
  }

  fun setZoomLinear(zoom: Float) {
    camera?.cameraControl?.setLinearZoom(zoom.coerceIn(0f, 1f))
  }

  fun setTorch(enabled: Boolean) {
    camera?.cameraControl?.enableTorch(enabled)
  }

  fun setOpenGateMode(enabled: Boolean) {
    isOpenGateMode = enabled
    applyManualOverrides()
  }

  fun setTargetFps(fps: Int) {
    currentFps = fps
    // Recompute shutter angle
    val sec = _exposureState.value.shutterNanos.toDouble() / 1_000_000_000.0
    val angle = ((sec * currentFps.toDouble() * 360.0) % 360.0).toFloat().coerceIn(1.0f, 360.0f)
    _exposureState.value = _exposureState.value.copy(shutterAngle = ((angle * 10f).roundToInt() / 10f))
  }

  fun triggerFocusAndMetering(factory: MeteringPointFactory, x: Float, y: Float) {
    val point = factory.createPoint(x, y)
    val action = FocusMeteringAction.Builder(point, FocusMeteringAction.FLAG_AF or FocusMeteringAction.FLAG_AE)
      .setAutoCancelDuration(4, java.util.concurrent.TimeUnit.SECONDS)
      .build()
    camera?.cameraControl?.startFocusAndMetering(action)
  }

  @SuppressLint("UnsafeOptInUsageError")
  private fun applyManualOverrides() {
    val control = camera?.cameraControl ?: return
    val camera2Control = Camera2CameraControl.from(control)
    val requestOptions = CaptureRequestOptions.Builder()

    // 1. Exposure (ISO + Shutter)
    if (manualIso != null || manualShutterNanos != null) {
      requestOptions.setCaptureRequestOption(
        CaptureRequest.CONTROL_AE_MODE,
        CaptureRequest.CONTROL_AE_MODE_OFF
      )
      manualIso?.let {
        requestOptions.setCaptureRequestOption(CaptureRequest.SENSOR_SENSITIVITY, it)
      }
      manualShutterNanos?.let {
        requestOptions.setCaptureRequestOption(CaptureRequest.SENSOR_EXPOSURE_TIME, it)
      }
    } else {
      requestOptions.setCaptureRequestOption(
        CaptureRequest.CONTROL_AE_MODE,
        CaptureRequest.CONTROL_AE_MODE_ON
      )
    }

    // 2. White Balance
    if (manualKelvin != null) {
      requestOptions.setCaptureRequestOption(
        CaptureRequest.CONTROL_AWB_MODE,
        CaptureRequest.CONTROL_AWB_MODE_OFF
      )
      requestOptions.setCaptureRequestOption(
        CaptureRequest.COLOR_CORRECTION_MODE,
        CaptureRequest.COLOR_CORRECTION_MODE_TRANSFORM_MATRIX
      )
      val gains = kelvinToRggb(manualKelvin!!)
      requestOptions.setCaptureRequestOption(CaptureRequest.COLOR_CORRECTION_GAINS, gains)
    } else {
      requestOptions.setCaptureRequestOption(
        CaptureRequest.CONTROL_AWB_MODE,
        CaptureRequest.CONTROL_AWB_MODE_AUTO
      )
    }

    // 3. Focus Distance
    if (manualFocusDistance != null) {
      requestOptions.setCaptureRequestOption(
        CaptureRequest.CONTROL_AF_MODE,
        CaptureRequest.CONTROL_AF_MODE_OFF
      )
      requestOptions.setCaptureRequestOption(
        CaptureRequest.LENS_FOCUS_DISTANCE,
        manualFocusDistance!!
      )
    } else {
      requestOptions.setCaptureRequestOption(
        CaptureRequest.CONTROL_AF_MODE,
        CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE
      )
    }

    // 4. Open Gate ISP Highest Quality Pipeline
    if (isOpenGateMode) {
      requestOptions.setCaptureRequestOption(
        CaptureRequest.NOISE_REDUCTION_MODE,
        CaptureRequest.NOISE_REDUCTION_MODE_HIGH_QUALITY
      )
      requestOptions.setCaptureRequestOption(
        CaptureRequest.EDGE_MODE,
        CaptureRequest.EDGE_MODE_HIGH_QUALITY
      )
      requestOptions.setCaptureRequestOption(
        CaptureRequest.COLOR_CORRECTION_ABERRATION_MODE,
        CaptureRequest.COLOR_CORRECTION_ABERRATION_MODE_HIGH_QUALITY
      )
      requestOptions.setCaptureRequestOption(
        CaptureRequest.HOT_PIXEL_MODE,
        CaptureRequest.HOT_PIXEL_MODE_HIGH_QUALITY
      )
    }

    camera2Control.setCaptureRequestOptions(requestOptions.build())
  }

  private fun kelvinToRggb(kelvin: Int): RggbChannelVector {
    val temp = kelvin.coerceIn(2000, 10000) / 100.0
    val redGain = if (temp <= 66.0) 2.2f else (1.5f + (100.0 - temp).toFloat() * 0.01f).coerceIn(1.0f, 3.0f)
    val blueGain = if (temp >= 66.0) 2.2f else ((temp - 20.0).toFloat() * 0.035f + 0.8f).coerceIn(1.0f, 3.5f)
    return RggbChannelVector(redGain, 1.0f, 1.0f, blueGain)
  }

  fun switchCamera(lifecycleOwner: LifecycleOwner, previewView: PreviewView) {
    currentLensFacing = if (currentLensFacing == CameraSelector.LENS_FACING_BACK) {
      CameraSelector.LENS_FACING_FRONT
    } else {
      CameraSelector.LENS_FACING_BACK
    }
    bindCameraUseCases(lifecycleOwner, previewView)
  }

  fun takePhoto(
    isOpenGate: Boolean = false,
    onPhotoSaved: (Uri, DetectedExposure) -> Unit,
    onError: (String) -> Unit
  ) {
    val capture = imageCapture ?: run {
      onError("Camera capture not ready")
      return
    }

    val isOg = isOpenGate || isOpenGateMode
    val prefix = if (isOg) "OPENGATE_HQ_" else "CINE_"
    val photoFile = File(
      context.cacheDir,
      "${prefix}${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())}.jpg"
    )

    val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()
    val currentMetadata = _exposureState.value.copy()

    capture.takePicture(
      outputOptions,
      ContextCompat.getMainExecutor(context),
      object : ImageCapture.OnImageSavedCallback {
        override fun onImageSaved(output: ImageCapture.OutputFileResults) {
          val savedUri = output.savedUri ?: Uri.fromFile(photoFile)
          onPhotoSaved(savedUri, currentMetadata)
        }

        override fun onError(exc: ImageCaptureException) {
          Log.e(TAG, "Photo capture failed: ${exc.message}", exc)
          onError(exc.message ?: "Capture failed")
        }
      }
    )
  }

  fun stopCamera() {
    try {
      cameraProvider?.unbindAll()
      camera = null
      preview = null
      imageCapture = null
      imageAnalysis = null
    } catch (exc: Exception) {
      Log.e(TAG, "Error in stopCamera", exc)
    }
  }
}
