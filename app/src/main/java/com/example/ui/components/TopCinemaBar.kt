package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.FlashlightOff
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CinemaAspectRatio
import com.example.model.CinemaFrameRate
import com.example.ui.theme.ProAmber
import com.example.ui.theme.ProCyan
import com.example.ui.theme.ProDarkBackground
import com.example.ui.theme.ProGreen
import com.example.ui.theme.ProTallyRed

@Composable
fun TopCinemaBar(
  isCinemaMode: Boolean,
  isRecording: Boolean,
  recordingDurationSeconds: Int,
  frameRate: CinemaFrameRate,
  aspectRatio: CinemaAspectRatio,
  useShutterAngle: Boolean,
  isTorchOn: Boolean,
  isOpenGateMode: Boolean,
  showGrid: Boolean,
  showHistogram: Boolean,
  showHorizonLevel: Boolean,
  showZebraAlert: Boolean,
  onCycleFrameRate: () -> Unit,
  onCycleAspectRatio: () -> Unit,
  onToggleShutterAngle: () -> Unit,
  onToggleTorch: () -> Unit,
  onToggleOpenGate: () -> Unit,
  onToggleGrid: () -> Unit,
  onToggleHistogram: () -> Unit,
  onToggleHorizon: () -> Unit,
  onToggleZebra: () -> Unit,
  onOpenPresets: () -> Unit,
  onSwitchCamera: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val tallyColor by animateColorAsState(
    targetValue = if (isRecording) ProTallyRed else Color.Transparent,
    animationSpec = infiniteRepeatable(
      animation = keyframes {
        durationMillis = 1000
        ProTallyRed at 0
        ProTallyRed at 500
        Color.Transparent at 501
        Color.Transparent at 1000
      },
      repeatMode = RepeatMode.Restart
    ),
    label = "tally_blink"
  )

  Column(
    modifier = modifier
      .fillMaxWidth()
      .background(
        Brush.verticalGradient(
          colors = listOf(
            Color(0xEE070A0E),
            Color(0x99070A0E),
            Color(0x00070A0E)
          )
        )
      )
      .padding(horizontal = 12.dp, vertical = 6.dp)
  ) {
    // Row 1: Studio Telemetry Badges (Tally, FPS, Format, Aspect, Shutter Mode)
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      // Left Group: Mode / Tally & Timecode
      Row(verticalAlignment = Alignment.CenterVertically) {
        if (isRecording) {
          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(4.dp))
              .background(ProTallyRed)
              .padding(horizontal = 6.dp, vertical = 2.dp),
            contentAlignment = Alignment.Center
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Box(
                modifier = Modifier
                  .size(6.dp)
                  .clip(CircleShape)
                  .background(Color.White)
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                text = "REC",
                color = Color.White,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
              )
            }
          }
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = formatDuration(recordingDurationSeconds),
            color = ProTallyRed,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold
          )
        } else {
          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(4.dp))
              .background(if (isCinemaMode) Color(0x3300E5FF) else Color(0x22FFFFFF))
              .border(
                1.dp,
                if (isCinemaMode) ProCyan.copy(alpha = 0.6f) else Color(0x33FFFFFF),
                RoundedCornerShape(4.dp)
              )
              .padding(horizontal = 6.dp, vertical = 2.dp)
          ) {
            Text(
              text = if (isCinemaMode) "CINEMA 4K" else "PRO STILL",
              color = if (isCinemaMode) ProCyan else Color.White,
              fontSize = 11.sp,
              fontFamily = FontFamily.Monospace,
              fontWeight = FontWeight.Bold
            )
          }
        }

        Spacer(modifier = Modifier.width(6.dp))

        // Fake stereo audio meter visualizer in cinema mode
        if (isCinemaMode) {
          Row(
            modifier = Modifier
              .height(14.dp)
              .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            AudioMeterBar(heightRatio = if (isRecording) 0.85f else 0.4f)
            Spacer(modifier = Modifier.width(2.dp))
            AudioMeterBar(heightRatio = if (isRecording) 0.72f else 0.35f)
          }
        }
      }

      // Center Group: FPS & Aspect Ratio Badges (Tappable)
      Row(verticalAlignment = Alignment.CenterVertically) {
        // Frame Rate Badge
        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(Color(0x33000000))
            .border(1.dp, Color(0x44FFFFFF), RoundedCornerShape(4.dp))
            .clickable { onCycleFrameRate() }
            .testTag("cycle_fps_button")
            .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
          Text(
            text = "${frameRate.fps} FPS",
            color = ProAmber,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold
          )
        }

        Spacer(modifier = Modifier.width(6.dp))

        // Aspect Ratio Badge
        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(if (aspectRatio.isOpenGate) Color(0x3300E5FF) else Color(0x33000000))
            .border(
              1.dp,
              if (aspectRatio.isOpenGate) ProCyan else Color(0x44FFFFFF),
              RoundedCornerShape(4.dp)
            )
            .clickable { onCycleAspectRatio() }
            .testTag("cycle_aspect_ratio_button")
            .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
          Text(
            text = aspectRatio.badge,
            color = if (aspectRatio.isOpenGate) ProCyan else Color(0xFFF1F5F9),
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold
          )
        }

        Spacer(modifier = Modifier.width(6.dp))

        // Shutter Readout Format Toggle: 180° vs 1/s
        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(if (useShutterAngle) Color(0x33FFB300) else Color(0x22FFFFFF))
            .border(
              1.dp,
              if (useShutterAngle) ProAmber.copy(alpha = 0.7f) else Color(0x33FFFFFF),
              RoundedCornerShape(4.dp)
            )
            .clickable { onToggleShutterAngle() }
            .testTag("toggle_shutter_angle_button")
            .padding(horizontal = 5.dp, vertical = 2.dp)
        ) {
          Text(
            text = if (useShutterAngle) "ANGLE" else "SPEED",
            color = if (useShutterAngle) ProAmber else Color.White,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.SemiBold
          )
        }
      }

      // Right Group: Presets & Lens Switch
      Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(
          onClick = onOpenPresets,
          modifier = Modifier
            .size(36.dp)
            .testTag("open_presets_button")
        ) {
          Icon(
            imageVector = Icons.Default.Palette,
            contentDescription = "Presets",
            tint = ProAmber,
            modifier = Modifier.size(18.dp)
          )
        }

        IconButton(
          onClick = onSwitchCamera,
          modifier = Modifier
            .size(36.dp)
            .testTag("switch_camera_button")
        ) {
          Icon(
            imageVector = Icons.Default.Cameraswitch,
            contentDescription = "Switch Camera",
            tint = Color.White,
            modifier = Modifier.size(18.dp)
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(4.dp))

    // Row 2: Tactical Viewfinder Tool Toggles (Open Gate, Grid, Horizon, Histogram, Zebra, Torch)
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceEvenly,
      verticalAlignment = Alignment.CenterVertically
    ) {
      QuickTogglePill(
        label = "OPEN GATE",
        isActive = isOpenGateMode || aspectRatio.isOpenGate,
        onClick = onToggleOpenGate,
        activeColor = ProCyan,
        tag = "toggle_opengate_pill"
      )

      QuickTogglePill(
        label = "3x3 GRID",
        isActive = showGrid,
        onClick = onToggleGrid,
        activeColor = ProAmber,
        tag = "toggle_grid_pill"
      )

      QuickTogglePill(
        label = "HORIZON",
        isActive = showHorizonLevel,
        onClick = onToggleHorizon,
        tag = "toggle_horizon_pill"
      )

      QuickTogglePill(
        label = "HISTOGRAM",
        isActive = showHistogram,
        onClick = onToggleHistogram,
        tag = "toggle_histogram_pill"
      )

      QuickTogglePill(
        label = "ZEBRA",
        isActive = showZebraAlert,
        onClick = onToggleZebra,
        tag = "toggle_zebra_pill"
      )

      QuickTogglePill(
        label = if (isTorchOn) "TORCH ON" else "TORCH",
        isActive = isTorchOn,
        onClick = onToggleTorch,
        activeColor = ProAmber,
        tag = "toggle_torch_pill"
      )
    }
  }
}

@Composable
private fun AudioMeterBar(heightRatio: Float) {
  Box(
    modifier = Modifier
      .width(4.dp)
      .height(14.dp)
      .clip(RoundedCornerShape(1.dp))
      .background(Color(0x44FFFFFF)),
    contentAlignment = Alignment.BottomCenter
  ) {
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .height((14 * heightRatio).dp)
        .background(if (heightRatio > 0.8f) ProTallyRed else if (heightRatio > 0.6f) ProAmber else ProGreen)
    )
  }
}

@Composable
private fun QuickTogglePill(
  label: String,
  isActive: Boolean,
  onClick: () -> Unit,
  tag: String,
  activeColor: Color = ProCyan,
) {
  Box(
    modifier = Modifier
      .clip(RoundedCornerShape(12.dp))
      .background(if (isActive) activeColor.copy(alpha = 0.2f) else Color(0x22000000))
      .border(
        1.dp,
        if (isActive) activeColor.copy(alpha = 0.8f) else Color(0x33FFFFFF),
        RoundedCornerShape(12.dp)
      )
      .clickable { onClick() }
      .testTag(tag)
      .padding(horizontal = 8.dp, vertical = 3.dp),
    contentAlignment = Alignment.Center
  ) {
    Text(
      text = label,
      color = if (isActive) activeColor else Color(0xAAFFFFFF),
      fontSize = 9.sp,
      fontFamily = FontFamily.Monospace,
      fontWeight = FontWeight.Bold,
      letterSpacing = 0.5.sp
    )
  }
}

private fun formatDuration(seconds: Int): String {
  val mins = seconds / 60
  val secs = seconds % 60
  return String.format(java.util.Locale.US, "%02d:%02d", mins, secs)
}
