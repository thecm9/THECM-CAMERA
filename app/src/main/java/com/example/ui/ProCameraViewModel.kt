package com.example.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.camera.ProCameraEngine
import com.example.model.CinemaAspectRatio
import com.example.model.CinemaFrameRate
import com.example.model.CinemaPreset
import com.example.model.DetectedExposure
import com.example.model.ManualControlType
import com.example.sensor.DeviceOrientationSensor
import com.example.sensor.HorizonOrientation
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ProCameraUiState(
  val detectedExposure: DetectedExposure = DetectedExposure(),
  val activeControl: ManualControlType? = null,
  val manualIso: Int? = null,
  val manualShutterNanos: Long? = null,
  val manualKelvin: Int? = null,
  val manualEvIndex: Int = 0,
  val manualFocusDistance: Float? = null,
  val isCinemaMode: Boolean = false,
  val useShutterAngleDisplay: Boolean = false,
  val frameRate: CinemaFrameRate = CinemaFrameRate.FPS_24,
  val aspectRatio: CinemaAspectRatio = CinemaAspectRatio.WIDE_16_9,
  val showGrid: Boolean = true,
  val showCenterMark: Boolean = true,
  val showHistogram: Boolean = true,
  val showHorizonLevel: Boolean = true,
  val showZebraAlert: Boolean = false,
  val showFocusPeaking: Boolean = false,
  val zoomRatio: Float = 0.0f,
  val isTorchOn: Boolean = false,
  val isOpenGateMode: Boolean = false,
  val isRecording: Boolean = false,
  val recordingDurationSeconds: Int = 0,
  val horizonOrientation: HorizonOrientation = HorizonOrientation(),
  val capturedPhotoUri: Uri? = null,
  val capturedPhotoMetadata: DetectedExposure? = null,
  val showPhotoDialog: Boolean = false,
  val showPresetsSheet: Boolean = false,
  val toastMessage: String? = null,
)

class ProCameraViewModel(application: Application) : AndroidViewModel(application) {

  val cameraEngine = ProCameraEngine(application)
  private val orientationSensor = DeviceOrientationSensor(application)

  private val _uiState = MutableStateFlow(ProCameraUiState())
  val uiState: StateFlow<ProCameraUiState> = _uiState.asStateFlow()

  private var recordingTimerJob: Job? = null

  // Built-in professional cinematic looks
  val cinemaPresets = listOf(
    CinemaPreset(
      id = "open_gate_master",
      name = "Open Gate 3:2 Sensor Master",
      description = "Uncropped full-sensor readout (3:2) with maximum resolution and 100% JPEG quality for flexible reframing and anamorphic post-production.",
      iso = 100,
      shutterNanos = 20_833_333L, // 1/48s 180° shutter at 24fps
      kelvin = 5600,
      evIndex = 0,
      aspectRatio = CinemaAspectRatio.OPEN_GATE,
      frameRate = CinemaFrameRate.FPS_24
    ),
    CinemaPreset(
      id = "cinema_24p",
      name = "Cinema 24p Standard",
      description = "Film standard 24fps with 180° shutter (1/48s), 5600K daylight balance, 2.39:1 Anamorphic framing.",
      iso = 100,
      shutterNanos = 20_833_333L, // ~1/48s (180 deg shutter angle at 24fps)
      kelvin = 5600,
      evIndex = 0,
      aspectRatio = CinemaAspectRatio.CINEMA_239,
      frameRate = CinemaFrameRate.FPS_24
    ),
    CinemaPreset(
      id = "golden_hour",
      name = "Golden Hour Warmth",
      description = "Warm 4800K tone for sunsets and rich skin highlights, smooth cinematic 1/60s shutter, 16:9 ratio.",
      iso = 100,
      shutterNanos = 16_666_666L,
      kelvin = 4800,
      evIndex = 1,
      aspectRatio = CinemaAspectRatio.WIDE_16_9,
      frameRate = CinemaFrameRate.FPS_30
    ),
    CinemaPreset(
      id = "moody_noir",
      name = "Moody Noir Night",
      description = "High ISO 800 with 3200K incandescent warmth and slight underexposure for atmospheric cinematic night scenes.",
      iso = 800,
      shutterNanos = 20_833_333L,
      kelvin = 3200,
      evIndex = -1,
      aspectRatio = CinemaAspectRatio.CINEMA_239,
      frameRate = CinemaFrameRate.FPS_24
    ),
    CinemaPreset(
      id = "high_speed_action",
      name = "High-Speed Action",
      description = "Crisp 60fps with fast 1/1000s shutter to freeze motion with maximum sharpness and zero motion blur.",
      iso = 400,
      shutterNanos = 1_000_000L,
      kelvin = 5500,
      evIndex = 0,
      aspectRatio = CinemaAspectRatio.WIDE_16_9,
      frameRate = CinemaFrameRate.FPS_60
    ),
    CinemaPreset(
      id = "studio_portrait",
      name = "Studio Clean Daylight",
      description = "Clean 5500K calibrated daylight, ISO 100 base, 1/160s sync speed with 4:3 classic framing.",
      iso = 100,
      shutterNanos = 6_250_000L,
      kelvin = 5500,
      evIndex = 0,
      aspectRatio = CinemaAspectRatio.PHOTO_4_3,
      frameRate = CinemaFrameRate.FPS_24
    )
  )

  init {
    // Observe real-time exposure telemetry from camera engine
    viewModelScope.launch {
      cameraEngine.exposureState.collect { exposure ->
        _uiState.value = _uiState.value.copy(detectedExposure = exposure)
      }
    }

    // Observe real-time device horizon angle
    viewModelScope.launch {
      orientationSensor.getOrientationFlow().collect { orientation ->
        _uiState.value = _uiState.value.copy(horizonOrientation = orientation)
      }
    }
  }

  fun selectControl(control: ManualControlType?) {
    _uiState.value = _uiState.value.copy(
      activeControl = if (_uiState.value.activeControl == control) null else control
    )
  }

  fun setManualIso(iso: Int?) {
    _uiState.value = _uiState.value.copy(manualIso = iso)
    cameraEngine.setManualIso(iso)
  }

  fun setManualShutter(nanos: Long?) {
    _uiState.value = _uiState.value.copy(manualShutterNanos = nanos)
    cameraEngine.setManualShutterNanos(nanos)
  }

  fun setManualKelvin(kelvin: Int?) {
    _uiState.value = _uiState.value.copy(manualKelvin = kelvin)
    cameraEngine.setManualKelvin(kelvin)
  }

  fun setManualEvIndex(index: Int) {
    _uiState.value = _uiState.value.copy(manualEvIndex = index)
    cameraEngine.setEvIndex(index)
  }

  fun setManualFocus(diopters: Float?) {
    _uiState.value = _uiState.value.copy(manualFocusDistance = diopters)
    cameraEngine.setManualFocusDistance(diopters)
  }

  fun resetAllToAuto() {
    _uiState.value = _uiState.value.copy(
      manualIso = null,
      manualShutterNanos = null,
      manualKelvin = null,
      manualEvIndex = 0,
      manualFocusDistance = null,
      activeControl = null,
      toastMessage = "All parameters reset to AUTO"
    )
    cameraEngine.setManualIso(null)
    cameraEngine.setManualShutterNanos(null)
    cameraEngine.setManualKelvin(null)
    cameraEngine.setEvIndex(0)
    cameraEngine.setManualFocusDistance(null)
  }

  fun applyPreset(preset: CinemaPreset) {
    val isOg = preset.aspectRatio.isOpenGate
    _uiState.value = _uiState.value.copy(
      manualIso = preset.iso,
      manualShutterNanos = preset.shutterNanos,
      manualKelvin = preset.kelvin,
      manualEvIndex = preset.evIndex,
      aspectRatio = preset.aspectRatio,
      isOpenGateMode = isOg,
      frameRate = preset.frameRate,
      showPresetsSheet = false,
      toastMessage = "Applied preset: ${preset.name}"
    )
    cameraEngine.setOpenGateMode(isOg)
    cameraEngine.setManualIso(preset.iso)
    cameraEngine.setManualShutterNanos(preset.shutterNanos)
    cameraEngine.setManualKelvin(preset.kelvin)
    cameraEngine.setEvIndex(preset.evIndex)
    cameraEngine.setTargetFps(preset.frameRate.fps)
  }

  fun toggleCinemaMode() {
    val newMode = !_uiState.value.isCinemaMode
    _uiState.value = _uiState.value.copy(
      isCinemaMode = newMode,
      useShutterAngleDisplay = newMode,
      aspectRatio = if (newMode) CinemaAspectRatio.CINEMA_239 else CinemaAspectRatio.WIDE_16_9,
      isOpenGateMode = false,
      toastMessage = if (newMode) "Cinema Mode: 2.39:1 & Shutter Angle Active" else "Photo Mode Active"
    )
    cameraEngine.setOpenGateMode(false)
  }

  fun toggleShutterAngleDisplay() {
    val current = _uiState.value.useShutterAngleDisplay
    _uiState.value = _uiState.value.copy(
      useShutterAngleDisplay = !current,
      toastMessage = if (!current) "Shutter readout: Shutter Angle (°)" else "Shutter readout: Fractions (1/s)"
    )
  }

  fun cycleFrameRate() {
    val current = _uiState.value.frameRate
    val next = when (current) {
      CinemaFrameRate.FPS_24 -> CinemaFrameRate.FPS_30
      CinemaFrameRate.FPS_30 -> CinemaFrameRate.FPS_60
      CinemaFrameRate.FPS_60 -> CinemaFrameRate.FPS_24
    }
    _uiState.value = _uiState.value.copy(frameRate = next)
    cameraEngine.setTargetFps(next.fps)
  }

  fun cycleAspectRatio() {
    val current = _uiState.value.aspectRatio
    val next = when (current) {
      CinemaAspectRatio.OPEN_GATE -> CinemaAspectRatio.CINEMA_239
      CinemaAspectRatio.CINEMA_239 -> CinemaAspectRatio.CINEMA_185
      CinemaAspectRatio.CINEMA_185 -> CinemaAspectRatio.WIDE_16_9
      CinemaAspectRatio.WIDE_16_9 -> CinemaAspectRatio.PHOTO_4_3
      CinemaAspectRatio.PHOTO_4_3 -> CinemaAspectRatio.SQUARE_1_1
      CinemaAspectRatio.SQUARE_1_1 -> CinemaAspectRatio.FULL
      CinemaAspectRatio.FULL -> CinemaAspectRatio.OPEN_GATE
    }
    val isOg = next.isOpenGate
    _uiState.value = _uiState.value.copy(
      aspectRatio = next,
      isOpenGateMode = isOg,
      toastMessage = if (isOg) "OPEN GATE 3:2: Full Sensor Readout (100% Max Quality)" else "Framing: ${next.title}"
    )
    cameraEngine.setOpenGateMode(isOg)
  }

  fun toggleOpenGateMode() {
    val nextMode = !_uiState.value.isOpenGateMode
    val newRatio = if (nextMode) CinemaAspectRatio.OPEN_GATE else CinemaAspectRatio.WIDE_16_9
    _uiState.value = _uiState.value.copy(
      isOpenGateMode = nextMode,
      aspectRatio = newRatio,
      toastMessage = if (nextMode) "OPEN GATE ACTIVE: Full Sensor Readout (3:2) • Highest Quality" else "OPEN GATE OFF: 16:9 Standard"
    )
    cameraEngine.setOpenGateMode(nextMode)
  }

  fun toggleTorch() {
    val next = !_uiState.value.isTorchOn
    _uiState.value = _uiState.value.copy(isTorchOn = next)
    cameraEngine.setTorch(next)
  }

  fun toggleGrid() {
    val next = !_uiState.value.showGrid
    _uiState.value = _uiState.value.copy(
      showGrid = next,
      toastMessage = if (next) "Rule of Thirds Grid: ON (3x3 Composition & Power Points)" else "Rule of Thirds Grid: OFF"
    )
  }

  fun toggleCenterMark() {
    _uiState.value = _uiState.value.copy(showCenterMark = !_uiState.value.showCenterMark)
  }

  fun toggleHistogram() {
    _uiState.value = _uiState.value.copy(showHistogram = !_uiState.value.showHistogram)
  }

  fun toggleHorizonLevel() {
    _uiState.value = _uiState.value.copy(showHorizonLevel = !_uiState.value.showHorizonLevel)
  }

  fun toggleZebra() {
    val next = !_uiState.value.showZebraAlert
    _uiState.value = _uiState.value.copy(
      showZebraAlert = next,
      toastMessage = if (next) "Highlight Zebras: ON (100% IRE)" else "Highlight Zebras: OFF"
    )
  }

  fun toggleFocusPeaking() {
    val next = !_uiState.value.showFocusPeaking
    _uiState.value = _uiState.value.copy(
      showFocusPeaking = next,
      toastMessage = if (next) "Focus Peaking: ON" else "Focus Peaking: OFF"
    )
  }

  fun setZoom(ratio: Float) {
    _uiState.value = _uiState.value.copy(zoomRatio = ratio)
    cameraEngine.setZoomLinear(ratio)
  }

  fun toggleRecording() {
    if (_uiState.value.isRecording) {
      recordingTimerJob?.cancel()
      _uiState.value = _uiState.value.copy(
        isRecording = false,
        toastMessage = "Take recorded (${formatTimecode(_uiState.value.recordingDurationSeconds)})"
      )
    } else {
      _uiState.value = _uiState.value.copy(
        isRecording = true,
        recordingDurationSeconds = 0,
        toastMessage = "RECORDING STARTED"
      )
      recordingTimerJob = viewModelScope.launch {
        while (true) {
          delay(1000)
          _uiState.value = _uiState.value.copy(
            recordingDurationSeconds = _uiState.value.recordingDurationSeconds + 1
          )
        }
      }
    }
  }

  fun openPresetsSheet() {
    _uiState.value = _uiState.value.copy(showPresetsSheet = true)
  }

  fun closePresetsSheet() {
    _uiState.value = _uiState.value.copy(showPresetsSheet = false)
  }

  fun onPhotoCaptured(uri: Uri, meta: DetectedExposure) {
    _uiState.value = _uiState.value.copy(
      capturedPhotoUri = uri,
      capturedPhotoMetadata = meta,
      showPhotoDialog = true,
      toastMessage = "Still Captured: ISO ${meta.iso} • ${meta.shutterString}s • ${meta.kelvin}K"
    )
  }

  fun dismissPhotoDialog() {
    _uiState.value = _uiState.value.copy(showPhotoDialog = false)
  }

  fun clearToast() {
    _uiState.value = _uiState.value.copy(toastMessage = null)
  }

  fun formatTimecode(seconds: Int): String {
    val hrs = seconds / 3600
    val mins = (seconds % 3600) / 60
    val secs = seconds % 60
    val frames = (System.currentTimeMillis() % 24).toInt()
    return String.format(java.util.Locale.US, "%02d:%02d:%02d:%02d", hrs, mins, secs, frames)
  }

  override fun onCleared() {
    super.onCleared()
    cameraEngine.stopCamera()
  }
}
