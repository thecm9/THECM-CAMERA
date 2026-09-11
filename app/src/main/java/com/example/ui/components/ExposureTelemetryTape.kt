package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.example.model.DetectedExposure
import com.example.model.ManualControlType
import com.example.ui.theme.ProAmber
import com.example.ui.theme.ProCyan
import com.example.ui.theme.ProGreen
import com.example.ui.theme.ProSurface
import com.example.ui.theme.ProSurfaceBorder

@Composable
fun ExposureTelemetryTape(
  exposure: DetectedExposure,
  activeControl: ManualControlType?,
  manualIso: Int?,
  manualShutterNanos: Long?,
  manualKelvin: Int?,
  manualEvIndex: Int,
  manualFocusDistance: Float?,
  useShutterAngle: Boolean,
  onSelectControl: (ManualControlType?) -> Unit,
  onResetAutoAll: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val scrollState = rememberScrollState()

  Column(
    modifier = modifier
      .fillMaxWidth()
      .background(
        Brush.verticalGradient(
          colors = listOf(
            Color(0x00070A0E),
            Color(0xDD070A0E),
            Color(0xFF070A0E)
          )
        )
      )
      .padding(vertical = 4.dp)
  ) {
    // Header label: LIVE EXPOSURE TELEMETRY & OVERRIDE
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 14.dp, vertical = 2.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
          modifier = Modifier
            .width(6.dp)
            .height(6.dp)
            .clip(RoundedCornerShape(1.dp))
            .background(ProGreen)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
          text = "REAL-TIME SENSOR TELEMETRY",
          color = Color(0xFF94A3B8),
          fontSize = 10.sp,
          fontFamily = FontFamily.Monospace,
          fontWeight = FontWeight.Bold,
          letterSpacing = 1.sp
        )
      }

      val anyManualActive = manualIso != null || manualShutterNanos != null || manualKelvin != null || manualFocusDistance != null || manualEvIndex != 0
      if (anyManualActive) {
        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(Color(0x22FFB300))
            .border(1.dp, ProAmber.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
            .clickable { onResetAutoAll() }
            .testTag("reset_auto_all_button")
            .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
          Text(
            text = "RESET AUTO",
            color = ProAmber,
            fontSize = 9.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(4.dp))

    // Horizontal scrollable telemetry chips
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .horizontalScroll(scrollState)
        .padding(horizontal = 12.dp),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      // 1. ISO CHIP
      TelemetryPill(
        title = "ISO",
        primaryValue = "${exposure.iso}",
        secondaryStatus = if (manualIso != null) "MANUAL" else "AUTO",
        isSelected = activeControl == ManualControlType.ISO,
        isManualOverride = manualIso != null,
        tag = "telemetry_iso_chip",
        onClick = { onSelectControl(ManualControlType.ISO) }
      )

      // 2. SHUTTER SPEED / ANGLE CHIP
      val shutterValue = if (useShutterAngle) {
        "${exposure.shutterAngle}°"
      } else {
        "${exposure.shutterString}s"
      }
      TelemetryPill(
        title = if (useShutterAngle) "ANGLE" else "SHUTTER",
        primaryValue = shutterValue,
        secondaryStatus = if (manualShutterNanos != null) "MANUAL" else "AUTO",
        isSelected = activeControl == ManualControlType.SHUTTER,
        isManualOverride = manualShutterNanos != null,
        tag = "telemetry_shutter_chip",
        onClick = { onSelectControl(ManualControlType.SHUTTER) }
      )

      // 3. WHITE BALANCE (KELVIN) CHIP
      TelemetryPill(
        title = "WHITE BAL",
        primaryValue = "${exposure.kelvin}K",
        secondaryStatus = if (manualKelvin != null) "MANUAL" else getKelvinLabel(exposure.kelvin),
        isSelected = activeControl == ManualControlType.WHITE_BALANCE,
        isManualOverride = manualKelvin != null,
        tag = "telemetry_wb_chip",
        onClick = { onSelectControl(ManualControlType.WHITE_BALANCE) }
      )

      // 4. EV COMPENSATION CHIP
      val evString = if (exposure.ev > 0f) "+${exposure.ev}" else "${exposure.ev}"
      TelemetryPill(
        title = "EV COMP",
        primaryValue = "$evString EV",
        secondaryStatus = if (manualEvIndex != 0) "CUSTOM" else "BALANCED",
        isSelected = activeControl == ManualControlType.EV,
        isManualOverride = manualEvIndex != 0,
        tag = "telemetry_ev_chip",
        onClick = { onSelectControl(ManualControlType.EV) }
      )

      // 5. FOCUS DISTANCE CHIP
      val focusString = if (exposure.focusDistanceMeters >= 50f) "∞ INF" else "${exposure.focusDistanceMeters}m"
      TelemetryPill(
        title = "FOCUS",
        primaryValue = focusString,
        secondaryStatus = if (manualFocusDistance != null) "MANUAL" else "AF-C",
        isSelected = activeControl == ManualControlType.FOCUS,
        isManualOverride = manualFocusDistance != null,
        tag = "telemetry_focus_chip",
        onClick = { onSelectControl(ManualControlType.FOCUS) }
      )

      // 6. APERTURE READOUT (Fixed/detected lens aperture)
      Box(
        modifier = Modifier
          .clip(RoundedCornerShape(8.dp))
          .background(ProSurface.copy(alpha = 0.8f))
          .border(1.dp, ProSurfaceBorder, RoundedCornerShape(8.dp))
          .padding(horizontal = 10.dp, vertical = 6.dp)
      ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Text(
            text = "LENS",
            color = Color(0xFF64748B),
            fontSize = 9.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold
          )
          Text(
            text = "f/${exposure.aperture}",
            color = ProCyan,
            fontSize = 14.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold
          )
          Text(
            text = "FIXED",
            color = Color(0xFF64748B),
            fontSize = 8.sp,
            fontFamily = FontFamily.Monospace
          )
        }
      }
    }
  }
}

@Composable
private fun TelemetryPill(
  title: String,
  primaryValue: String,
  secondaryStatus: String,
  isSelected: Boolean,
  isManualOverride: Boolean,
  tag: String,
  onClick: () -> Unit,
) {
  val borderColor by animateColorAsState(
    targetValue = when {
      isSelected -> ProAmber
      isManualOverride -> ProAmber.copy(alpha = 0.7f)
      else -> ProSurfaceBorder
    },
    label = "pill_border"
  )

  val backgroundColor by animateColorAsState(
    targetValue = when {
      isSelected -> Color(0xFF261E0A)
      isManualOverride -> Color(0x33FFB300)
      else -> ProSurface.copy(alpha = 0.85f)
    },
    label = "pill_background"
  )

  Box(
    modifier = Modifier
      .clip(RoundedCornerShape(8.dp))
      .background(backgroundColor)
      .border(1.dp, borderColor, RoundedCornerShape(8.dp))
      .clickable { onClick() }
      .testTag(tag)
      .padding(horizontal = 12.dp, vertical = 6.dp)
  ) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
      Text(
        text = title,
        color = if (isSelected || isManualOverride) ProAmber else Color(0xFF94A3B8),
        fontSize = 9.sp,
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.5.sp
      )
      Spacer(modifier = Modifier.height(2.dp))
      Text(
        text = primaryValue,
        color = if (isSelected) ProAmber else Color(0xFFF8FAFC),
        fontSize = 15.sp,
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.ExtraBold
      )
      Spacer(modifier = Modifier.height(2.dp))
      Text(
        text = secondaryStatus,
        color = if (isManualOverride) ProAmber else Color(0xFF00E5FF),
        fontSize = 8.sp,
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.SemiBold
      )
    }
  }
}

private fun getKelvinLabel(kelvin: Int): String {
  return when {
    kelvin < 3200 -> "TUNGSTEN"
    kelvin in 3200..3800 -> "WARM"
    kelvin in 3801..4800 -> "FLUOR"
    kelvin in 4801..5800 -> "DAYLIGHT"
    kelvin in 5801..6800 -> "CLOUDY"
    else -> "SHADE"
  }
}
