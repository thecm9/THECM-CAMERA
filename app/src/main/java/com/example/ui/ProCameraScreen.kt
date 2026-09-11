package com.example.ui

import android.Manifest
import android.content.pm.PackageManager
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.MeteringPointFactory
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.CropFree
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.ui.components.CapturedPhotoDialog
import com.example.ui.components.ExposureTelemetryTape
import com.example.ui.components.LiveHistogramView
import com.example.ui.components.PresetsBottomSheet
import com.example.ui.components.TacticalControlWheel
import com.example.ui.components.TopCinemaBar
import com.example.ui.components.ViewfinderOverlay
import com.example.ui.theme.ProAmber
import com.example.ui.theme.ProCyan
import com.example.ui.theme.ProDarkBackground
import com.example.ui.theme.ProGreen
import com.example.ui.theme.ProSurface
import com.example.ui.theme.ProSurfaceBorder
import com.example.ui.theme.ProTallyRed

@Composable
fun ProCameraScreen(
  viewModel: ProCameraViewModel,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  val lifecycleOwner = LocalLifecycleOwner.current
  val uiState by viewModel.uiState.collectAsState()

  var hasCameraPermission by remember {
    mutableStateOf(
      ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    )
  }

  val permissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestPermission()
  ) { isGranted ->
    hasCameraPermission = isGranted
  }

  LaunchedEffect(Unit) {
    if (!hasCameraPermission) {
      permissionLauncher.launch(Manifest.permission.CAMERA)
    }
  }

  val snackbarHostState = remember { SnackbarHostState() }
  LaunchedEffect(uiState.toastMessage) {
    uiState.toastMessage?.let {
      snackbarHostState.showSnackbar(it)
      viewModel.clearToast()
    }
  }

  var previewViewRef by remember { mutableStateOf<PreviewView?>(null) }
  var shutterFlashTrigger by remember { mutableStateOf(false) }

  // Clean lifecycle tracking to avoid abandoned buffer queues
  DisposableEffect(lifecycleOwner) {
    val observer = LifecycleEventObserver { _, event ->
      when (event) {
        Lifecycle.Event.ON_STOP -> {
          viewModel.cameraEngine.stopCamera()
        }
        Lifecycle.Event.ON_START -> {
          if (hasCameraPermission) {
            previewViewRef?.let { pv ->
              viewModel.cameraEngine.startCamera(lifecycleOwner, pv)
            }
          }
        }
        else -> {}
      }
    }
    lifecycleOwner.lifecycle.addObserver(observer)
    onDispose {
      lifecycleOwner.lifecycle.removeObserver(observer)
      viewModel.cameraEngine.stopCamera()
    }
  }

  LaunchedEffect(shutterFlashTrigger) {
    if (shutterFlashTrigger) {
      kotlinx.coroutines.delay(120)
      shutterFlashTrigger = false
    }
  }

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(ProDarkBackground)
  ) {
    if (!hasCameraPermission) {
      // Permission request banner
      PermissionRequestView(
        onRequestPermission = { permissionLauncher.launch(Manifest.permission.CAMERA) }
      )
    } else {
      // Camera Viewfinder Surface
      AndroidView(
        factory = { ctx ->
          PreviewView(ctx).apply {
            layoutParams = FrameLayout.LayoutParams(
              ViewGroup.LayoutParams.MATCH_PARENT,
              ViewGroup.LayoutParams.MATCH_PARENT
            )
            // COMPATIBLE mode (TextureView) avoids SurfaceView BLAST Consumer
            // buffer queue abandon crashes during Compose recomposition & overlays
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            scaleType = PreviewView.ScaleType.FILL_CENTER
            previewViewRef = this
            viewModel.cameraEngine.startCamera(lifecycleOwner, this)
          }
        },
        onRelease = {
          previewViewRef = null
          viewModel.cameraEngine.stopCamera()
        },
        modifier = Modifier
          .fillMaxSize()
          .testTag("camera_preview_view")
      )

      // Viewfinder Reticle & Cinema Framing Guides
      ViewfinderOverlay(
        aspectRatio = uiState.aspectRatio,
        showGrid = uiState.showGrid,
        showCenterMark = uiState.showCenterMark,
        showHorizonLevel = uiState.showHorizonLevel,
        showZebraAlert = uiState.showZebraAlert,
        horizonOrientation = uiState.horizonOrientation,
        focusDistanceMeters = uiState.detectedExposure.focusDistanceMeters,
        onTapToFocus = { x, y ->
          previewViewRef?.let { pv ->
            val factory: MeteringPointFactory = pv.meteringPointFactory
            viewModel.cameraEngine.triggerFocusAndMetering(factory, x, y)
          }
        },
        modifier = Modifier.fillMaxSize()
      )

      // Shutter Flash Animation (white blink on photo capture)
      if (shutterFlashTrigger) {
        Box(
          modifier = Modifier
            .fillMaxSize()
            .background(Color.White.copy(alpha = 0.85f))
        )
      }

      // Live Histogram View (upper right quadrant overlay)
      if (uiState.showHistogram) {
        Box(
          modifier = Modifier
            .align(Alignment.TopEnd)
            .statusBarsPadding()
            .padding(top = 70.dp, end = 12.dp)
        ) {
          LiveHistogramView(bins = uiState.detectedExposure.histogramBins)
        }
      }

      // Top Cinema Bar (Tally, FPS, Aspect, Shutter Mode, Tools)
      Box(
        modifier = Modifier
          .align(Alignment.TopCenter)
          .statusBarsPadding()
      ) {
        TopCinemaBar(
          isCinemaMode = uiState.isCinemaMode,
          isRecording = uiState.isRecording,
          recordingDurationSeconds = uiState.recordingDurationSeconds,
          frameRate = uiState.frameRate,
          aspectRatio = uiState.aspectRatio,
          useShutterAngle = uiState.useShutterAngleDisplay,
          isTorchOn = uiState.isTorchOn,
          isOpenGateMode = uiState.isOpenGateMode,
          showGrid = uiState.showGrid,
          showHistogram = uiState.showHistogram,
          showHorizonLevel = uiState.showHorizonLevel,
          showZebraAlert = uiState.showZebraAlert,
          onCycleFrameRate = { viewModel.cycleFrameRate() },
          onCycleAspectRatio = { viewModel.cycleAspectRatio() },
          onToggleShutterAngle = { viewModel.toggleShutterAngleDisplay() },
          onToggleTorch = { viewModel.toggleTorch() },
          onToggleOpenGate = { viewModel.toggleOpenGateMode() },
          onToggleGrid = { viewModel.toggleGrid() },
          onToggleHistogram = { viewModel.toggleHistogram() },
          onToggleHorizon = { viewModel.toggleHorizonLevel() },
          onToggleZebra = { viewModel.toggleZebra() },
          onOpenPresets = { viewModel.openPresetsSheet() },
          onSwitchCamera = {
            previewViewRef?.let { pv ->
              viewModel.cameraEngine.switchCamera(lifecycleOwner, pv)
            }
          }
        )
      }

      // Left Viewfinder Tactical Floating HUD Buttons (Open Gate & Rule of Thirds)
      Column(
        modifier = Modifier
          .align(Alignment.CenterStart)
          .padding(start = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        OpenGateFloatingButton(
          isActive = uiState.isOpenGateMode || uiState.aspectRatio.isOpenGate,
          onClick = { viewModel.toggleOpenGateMode() }
        )

        RuleOfThirdsFloatingButton(
          isActive = uiState.showGrid,
          onClick = { viewModel.toggleGrid() }
        )
      }

      // Bottom Control Stack (Exposure Telemetry Tape + Tactical Wheel + Trigger Controls)
      Column(
        modifier = Modifier
          .align(Alignment.BottomCenter)
          .navigationBarsPadding()
          .fillMaxWidth()
      ) {
        // Real-Time Detected Exposure Telemetry Tape (ISO, Shutter, Kelvin, EV, Focus)
        ExposureTelemetryTape(
          exposure = uiState.detectedExposure,
          activeControl = uiState.activeControl,
          manualIso = uiState.manualIso,
          manualShutterNanos = uiState.manualShutterNanos,
          manualKelvin = uiState.manualKelvin,
          manualEvIndex = uiState.manualEvIndex,
          manualFocusDistance = uiState.manualFocusDistance,
          useShutterAngle = uiState.useShutterAngleDisplay,
          onSelectControl = { viewModel.selectControl(it) },
          onResetAutoAll = { viewModel.resetAllToAuto() }
        )

        // Expandable Tactical Manual Control Wheel
        AnimatedVisibility(
          visible = uiState.activeControl != null,
          enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
          exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
        ) {
          TacticalControlWheel(
            activeControl = uiState.activeControl,
            exposure = uiState.detectedExposure,
            manualIso = uiState.manualIso,
            manualShutterNanos = uiState.manualShutterNanos,
            manualKelvin = uiState.manualKelvin,
            manualEvIndex = uiState.manualEvIndex,
            manualFocusDistance = uiState.manualFocusDistance,
            frameRate = uiState.frameRate,
            onSetIso = { viewModel.setManualIso(it) },
            onSetShutter = { viewModel.setManualShutter(it) },
            onSetKelvin = { viewModel.setManualKelvin(it) },
            onSetEvIndex = { viewModel.setManualEvIndex(it) },
            onSetFocus = { viewModel.setManualFocus(it) },
            onClose = { viewModel.selectControl(null) }
          )
        }

        // Bottom Deck: Zoom lenses, Shutter Button, Mode switch
        BottomActionDeck(
          isCinemaMode = uiState.isCinemaMode,
          isRecording = uiState.isRecording,
          zoomRatio = uiState.zoomRatio,
          onZoomSelect = { viewModel.setZoom(it) },
          onToggleCinemaMode = { viewModel.toggleCinemaMode() },
          onOpenPresets = { viewModel.openPresetsSheet() },
          onTriggerShutter = {
            if (uiState.isCinemaMode) {
              viewModel.toggleRecording()
            } else {
              shutterFlashTrigger = true
              viewModel.cameraEngine.takePhoto(
                onPhotoSaved = { uri, meta ->
                  viewModel.onPhotoCaptured(uri, meta)
                },
                onError = { err ->
                  // Fallback for emulator without file write access
                  viewModel.onPhotoCaptured(
                    android.net.Uri.EMPTY,
                    uiState.detectedExposure
                  )
                }
              )
            }
          }
        )
      }
    }

    // Snackbar Host for status notifications
    SnackbarHost(
      hostState = snackbarHostState,
      modifier = Modifier
        .align(Alignment.BottomCenter)
        .padding(bottom = 120.dp)
    )

    // Presets Bottom Sheet
    if (uiState.showPresetsSheet) {
      PresetsBottomSheet(
        presets = viewModel.cinemaPresets,
        onSelectPreset = { preset -> viewModel.applyPreset(preset) },
        onDismiss = { viewModel.closePresetsSheet() }
      )
    }

    // Captured Photo & EXIF Metadata Modal
    if (uiState.showPhotoDialog) {
      CapturedPhotoDialog(
        photoUri = uiState.capturedPhotoUri,
        metadata = uiState.capturedPhotoMetadata,
        isOpenGate = uiState.isOpenGateMode || uiState.aspectRatio.isOpenGate,
        onDismiss = { viewModel.dismissPhotoDialog() }
      )
    }
  }
}

@Composable
private fun BottomActionDeck(
  isCinemaMode: Boolean,
  isRecording: Boolean,
  zoomRatio: Float,
  onZoomSelect: (Float) -> Unit,
  onToggleCinemaMode: () -> Unit,
  onOpenPresets: () -> Unit,
  onTriggerShutter: () -> Unit,
) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .background(Color(0xF0070A0E))
      .padding(horizontal = 20.dp, vertical = 10.dp),
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    // Zoom lens focal length pills: 0.5x, 1x, 2x
    Row(
      horizontalArrangement = Arrangement.spacedBy(12.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      ZoomLensPill(label = "0.5x", isSelected = zoomRatio == 0.0f, onClick = { onZoomSelect(0.0f) })
      ZoomLensPill(label = "1x", isSelected = zoomRatio == 0.25f, onClick = { onZoomSelect(0.25f) })
      ZoomLensPill(label = "2x", isSelected = zoomRatio == 0.5f, onClick = { onZoomSelect(0.5f) })
      ZoomLensPill(label = "5x", isSelected = zoomRatio == 1.0f, onClick = { onZoomSelect(1.0f) })
    }

    Spacer(modifier = Modifier.height(12.dp))

    // Main Master Controls Row
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceAround,
      verticalAlignment = Alignment.CenterVertically
    ) {
      // Left: Mode Toggle (PHOTO vs CINEMA)
      Box(
        modifier = Modifier
          .clip(RoundedCornerShape(20.dp))
          .background(Color(0x33FFFFFF))
          .border(1.dp, ProSurfaceBorder, RoundedCornerShape(20.dp))
          .clickable { onToggleCinemaMode() }
          .testTag("mode_toggle_button")
          .padding(horizontal = 14.dp, vertical = 8.dp)
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = if (isCinemaMode) Icons.Default.Videocam else Icons.Default.Camera,
            contentDescription = "Mode Switch",
            tint = if (isCinemaMode) ProCyan else ProAmber,
            modifier = Modifier.size(16.dp)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = if (isCinemaMode) "CINEMA" else "STILL",
            color = if (isCinemaMode) ProCyan else ProAmber,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold
          )
        }
      }

      // Center: Master Shutter / Record Trigger Button
      Box(
        modifier = Modifier
          .size(76.dp)
          .clip(CircleShape)
          .background(Color(0x33FFFFFF))
          .border(3.dp, if (isCinemaMode) ProTallyRed else ProAmber, CircleShape)
          .clickable { onTriggerShutter() }
          .testTag("master_shutter_trigger_button")
          .padding(6.dp),
        contentAlignment = Alignment.Center
      ) {
        if (isCinemaMode) {
          // Cinema Record Button
          Box(
            modifier = Modifier
              .size(if (isRecording) 32.dp else 52.dp)
              .clip(if (isRecording) RoundedCornerShape(6.dp) else CircleShape)
              .background(ProTallyRed)
          )
        } else {
          // Photography Shutter Button with Aluminum Look
          Box(
            modifier = Modifier
              .size(52.dp)
              .clip(CircleShape)
              .background(Color(0xFFF1F5F9))
              .border(2.dp, Color(0xFF94A3B8), CircleShape)
          )
        }
      }

      // Right: Presets / Looks quick launcher
      Box(
        modifier = Modifier
          .clip(RoundedCornerShape(20.dp))
          .background(Color(0x33FFFFFF))
          .border(1.dp, ProSurfaceBorder, RoundedCornerShape(20.dp))
          .clickable { onOpenPresets() }
          .testTag("open_looks_presets_button")
          .padding(horizontal = 14.dp, vertical = 8.dp)
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = Icons.Default.PhotoLibrary,
            contentDescription = "Cinema Looks",
            tint = Color(0xFFF1F5F9),
            modifier = Modifier.size(16.dp)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = "LOOKS",
            color = Color(0xFFF1F5F9),
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold
          )
        }
      }
    }
  }
}

@Composable
private fun ZoomLensPill(
  label: String,
  isSelected: Boolean,
  onClick: () -> Unit,
) {
  Box(
    modifier = Modifier
      .clip(CircleShape)
      .background(if (isSelected) ProAmber else Color(0x33000000))
      .border(1.dp, if (isSelected) ProAmber else Color(0x44FFFFFF), CircleShape)
      .clickable { onClick() }
      .testTag("zoom_lens_$label")
      .padding(horizontal = 10.dp, vertical = 4.dp),
    contentAlignment = Alignment.Center
  ) {
    Text(
      text = label,
      color = if (isSelected) ProDarkBackground else Color.White,
      fontSize = 10.sp,
      fontFamily = FontFamily.Monospace,
      fontWeight = FontWeight.Bold
    )
  }
}

@Composable
private fun PermissionRequestView(onRequestPermission: () -> Unit) {
  Column(
    modifier = Modifier
      .fillMaxSize()
      .padding(32.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center
  ) {
    Icon(
      imageVector = Icons.Default.Camera,
      contentDescription = null,
      tint = ProAmber,
      modifier = Modifier.size(64.dp)
    )

    Spacer(modifier = Modifier.height(16.dp))

    Text(
      text = "CAMERA HARDWARE ACCESS",
      color = ProAmber,
      fontSize = 18.sp,
      fontFamily = FontFamily.Monospace,
      fontWeight = FontWeight.ExtraBold,
      letterSpacing = 1.sp
    )

    Spacer(modifier = Modifier.height(8.dp))

    Text(
      text = "Pro Camera requires camera permission to detect ISO, shutter speed, and white balance in real-time through the viewfinder with full manual controls.",
      color = Color(0xFF94A3B8),
      fontSize = 13.sp,
      textAlign = androidx.compose.ui.text.style.TextAlign.Center,
      lineHeight = 18.sp
    )

    Spacer(modifier = Modifier.height(24.dp))

    Box(
      modifier = Modifier
        .clip(RoundedCornerShape(8.dp))
        .background(ProAmber)
        .clickable { onRequestPermission() }
        .testTag("grant_camera_permission_button")
        .padding(horizontal = 24.dp, vertical = 12.dp)
    ) {
      Text(
        text = "INITIALIZE CAMERA",
        color = ProDarkBackground,
        fontSize = 12.sp,
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold
      )
    }
  }
}

@Composable
private fun OpenGateFloatingButton(
  isActive: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Box(
    modifier = modifier
      .clip(RoundedCornerShape(10.dp))
      .background(if (isActive) Color(0xDD00242E) else Color(0xAA0D1117))
      .border(
        width = 1.2.dp,
        color = if (isActive) ProCyan else ProSurfaceBorder,
        shape = RoundedCornerShape(10.dp)
      )
      .clickable { onClick() }
      .testTag("opengate_floating_button")
      .padding(horizontal = 8.dp, vertical = 8.dp),
    contentAlignment = Alignment.Center
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center
    ) {
      Icon(
        imageVector = Icons.Default.CropFree,
        contentDescription = "Toggle Open Gate Full Sensor 3:2 Mode",
        tint = if (isActive) ProCyan else Color(0xBBFFFFFF),
        modifier = Modifier.size(20.dp)
      )
      Spacer(modifier = Modifier.height(3.dp))
      Text(
        text = "3:2",
        color = if (isActive) ProCyan else Color(0x99FFFFFF),
        fontSize = 9.sp,
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold
      )
      Text(
        text = if (isActive) "GATE ON" else "GATE",
        color = if (isActive) ProCyan else Color(0x66FFFFFF),
        fontSize = 7.sp,
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold
      )
    }
  }
}

@Composable
private fun RuleOfThirdsFloatingButton(
  isActive: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Box(
    modifier = modifier
      .clip(RoundedCornerShape(10.dp))
      .background(if (isActive) Color(0xDD241C0A) else Color(0xAA0D1117))
      .border(
        width = 1.2.dp,
        color = if (isActive) ProAmber else ProSurfaceBorder,
        shape = RoundedCornerShape(10.dp)
      )
      .clickable { onClick() }
      .testTag("rule_of_thirds_toggle_button")
      .padding(horizontal = 8.dp, vertical = 8.dp),
    contentAlignment = Alignment.Center
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center
    ) {
      Icon(
        imageVector = Icons.Default.GridOn,
        contentDescription = "Toggle Rule of Thirds Grid Overlay",
        tint = if (isActive) ProAmber else Color(0xBBFFFFFF),
        modifier = Modifier.size(20.dp)
      )
      Spacer(modifier = Modifier.height(3.dp))
      Text(
        text = "3×3",
        color = if (isActive) ProAmber else Color(0x99FFFFFF),
        fontSize = 9.sp,
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold
      )
      Text(
        text = if (isActive) "GRID ON" else "GRID",
        color = if (isActive) ProGreen else Color(0x66FFFFFF),
        fontSize = 7.sp,
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold
      )
    }
  }
}
