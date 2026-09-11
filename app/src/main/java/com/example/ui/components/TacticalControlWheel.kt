package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.example.model.CinemaFrameRate
import com.example.model.DetectedExposure
import com.example.model.ManualControlType
import com.example.ui.theme.ProAmber
import com.example.ui.theme.ProCyan
import com.example.ui.theme.ProDarkBackground
import com.example.ui.theme.ProGreen
import com.example.ui.theme.ProSurface
import com.example.ui.theme.ProSurfaceBorder
import kotlin.math.roundToInt

@Composable
fun TacticalControlWheel(
  activeControl: ManualControlType?,
  exposure: DetectedExposure,
  manualIso: Int?,
  manualShutterNanos: Long?,
  manualKelvin: Int?,
  manualEvIndex: Int,
  manualFocusDistance: Float?,
  frameRate: CinemaFrameRate,
  onSetIso: (Int?) -> Unit,
  onSetShutter: (Long?) -> Unit,
  onSetKelvin: (Int?) -> Unit,
  onSetEvIndex: (Int) -> Unit,
  onSetFocus: (Float?) -> Unit,
  onClose: () -> Unit,
  modifier: Modifier = Modifier,
) {
  if (activeControl == null) return

  val scrollState = rememberScrollState()

  Column(
    modifier = modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
      .background(Color(0xF50D1117))
      .border(1.dp, ProSurfaceBorder, RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
      .padding(horizontal = 14.dp, vertical = 10.dp)
  ) {
    // Top Bar: Control Title, Readout Value, AUTO button, and Close
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      // Title and current state
      Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
          text = activeControl.label,
          color = ProAmber,
          fontSize = 14.sp,
          fontFamily = FontFamily.Monospace,
          fontWeight = FontWeight.ExtraBold,
          letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = getCurrentReadout(
            activeControl,
            exposure,
            manualIso,
            manualShutterNanos,
            manualKelvin,
            manualEvIndex,
            manualFocusDistance
          ),
          color = Color(0xFFF1F5F9),
          fontSize = 16.sp,
          fontFamily = FontFamily.Monospace,
          fontWeight = FontWeight.Bold
        )
      }

      // Quick AUTO toggle button and Close button
      Row(verticalAlignment = Alignment.CenterVertically) {
        val isAuto = isControlAuto(activeControl, manualIso, manualShutterNanos, manualKelvin, manualEvIndex, manualFocusDistance)
        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (isAuto) ProGreen.copy(alpha = 0.25f) else Color(0x33FFFFFF))
            .border(1.dp, if (isAuto) ProGreen else Color(0x44FFFFFF), RoundedCornerShape(6.dp))
            .clickable {
              when (activeControl) {
                ManualControlType.ISO -> onSetIso(null)
                ManualControlType.SHUTTER -> onSetShutter(null)
                ManualControlType.WHITE_BALANCE -> onSetKelvin(null)
                ManualControlType.EV -> onSetEvIndex(0)
                ManualControlType.FOCUS -> onSetFocus(null)
              }
            }
            .testTag("dial_auto_toggle_button")
            .padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
          Text(
            text = "AUTO",
            color = if (isAuto) ProGreen else Color(0xFFE2E8F0),
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold
          )
        }

        Spacer(modifier = Modifier.width(8.dp))

        IconButton(
          onClick = onClose,
          modifier = Modifier
            .size(32.dp)
            .testTag("dial_close_button")
        ) {
          Icon(
            imageVector = Icons.Default.Close,
            contentDescription = "Close Dial",
            tint = Color(0xFF94A3B8),
            modifier = Modifier.size(18.dp)
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(8.dp))

    // Content specialized by control type
    when (activeControl) {
      ManualControlType.ISO -> {
        IsoControlContent(
          currentIso = manualIso ?: exposure.iso,
          isAuto = manualIso == null,
          onSelectIso = onSetIso
        )
      }
      ManualControlType.SHUTTER -> {
        ShutterControlContent(
          currentNanos = manualShutterNanos ?: exposure.shutterNanos,
          isAuto = manualShutterNanos == null,
          frameRate = frameRate,
          onSelectNanos = onSetShutter
        )
      }
      ManualControlType.WHITE_BALANCE -> {
        KelvinControlContent(
          currentKelvin = manualKelvin ?: exposure.kelvin,
          isAuto = manualKelvin == null,
          onSelectKelvin = onSetKelvin
        )
      }
      ManualControlType.EV -> {
        EvControlContent(
          currentEvIndex = manualEvIndex,
          onSelectEvIndex = onSetEvIndex
        )
      }
      ManualControlType.FOCUS -> {
        FocusControlContent(
          currentFocus = manualFocusDistance,
          detectedMeters = exposure.focusDistanceMeters,
          onSelectFocus = onSetFocus
        )
      }
    }
  }
}

@Composable
private fun IsoControlContent(
  currentIso: Int,
  isAuto: Boolean,
  onSelectIso: (Int?) -> Unit,
) {
  val isoValues = listOf(50, 100, 160, 200, 250, 320, 400, 640, 800, 1250, 1600, 3200, 6400)
  val scrollState = rememberScrollState()

  Row(
    modifier = Modifier
      .fillMaxWidth()
      .horizontalScroll(scrollState)
      .padding(vertical = 4.dp),
    horizontalArrangement = Arrangement.spacedBy(6.dp)
  ) {
    isoValues.forEach { iso ->
      val isSelected = !isAuto && currentIso == iso
      Box(
        modifier = Modifier
          .clip(RoundedCornerShape(6.dp))
          .background(if (isSelected) ProAmber else ProSurface)
          .border(1.dp, if (isSelected) ProAmber else ProSurfaceBorder, RoundedCornerShape(6.dp))
          .clickable { onSelectIso(iso) }
          .testTag("iso_stop_$iso")
          .padding(horizontal = 12.dp, vertical = 8.dp)
      ) {
        Text(
          text = "$iso",
          color = if (isSelected) ProDarkBackground else Color(0xFFF1F5F9),
          fontSize = 13.sp,
          fontFamily = FontFamily.Monospace,
          fontWeight = FontWeight.Bold
        )
      }
    }
  }
}

@Composable
private fun ShutterControlContent(
  currentNanos: Long,
  isAuto: Boolean,
  frameRate: CinemaFrameRate,
  onSelectNanos: (Long?) -> Unit,
) {
  // Cinema standard shutter steps
  val shutterOptions = listOf(
    Pair("1/8000", 125_000L),
    Pair("1/4000", 250_000L),
    Pair("1/2000", 500_000L),
    Pair("1/1000", 1_000_000L),
    Pair("1/500", 2_000_000L),
    Pair("1/250", 4_000_000L),
    Pair("1/125", 8_000_000L),
    Pair("1/60", 16_666_666L),
    Pair("1/48 (180°)", 20_833_333L), // 180 deg cinema rule for 24p
    Pair("1/30", 33_333_333L),
    Pair("1/24", 41_666_666L),
    Pair("1/15", 66_666_666L),
    Pair("1/8", 125_000_000L),
    Pair("1/4", 250_000_000L),
    Pair("1/2", 500_000_000L),
    Pair("1s", 1_000_000_000L),
  )

  val scrollState = rememberScrollState()

  Row(
    modifier = Modifier
      .fillMaxWidth()
      .horizontalScroll(scrollState)
      .padding(vertical = 4.dp),
    horizontalArrangement = Arrangement.spacedBy(6.dp)
  ) {
    shutterOptions.forEach { (label, nanos) ->
      val isSelected = !isAuto && kotlin.math.abs(currentNanos - nanos) < 1_000_000L
      val is180Rule = nanos == 20_833_333L

      Box(
        modifier = Modifier
          .clip(RoundedCornerShape(6.dp))
          .background(
            if (isSelected) ProAmber
            else if (is180Rule) Color(0x33FFB300)
            else ProSurface
          )
          .border(
            1.dp,
            if (isSelected) ProAmber else if (is180Rule) ProAmber.copy(alpha = 0.5f) else ProSurfaceBorder,
            RoundedCornerShape(6.dp)
          )
          .clickable { onSelectNanos(nanos) }
          .testTag("shutter_stop_$nanos")
          .padding(horizontal = 10.dp, vertical = 8.dp)
      ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Text(
            text = label,
            color = if (isSelected) ProDarkBackground else if (is180Rule) ProAmber else Color(0xFFF1F5F9),
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold
          )
        }
      }
    }
  }
}

@Composable
private fun KelvinControlContent(
  currentKelvin: Int,
  isAuto: Boolean,
  onSelectKelvin: (Int?) -> Unit,
) {
  val presets = listOf(
    Pair("Tungsten 3200K", 3200),
    Pair("Fluorescent 4000K", 4000),
    Pair("Daylight 5500K", 5500),
    Pair("Cloudy 6500K", 6500),
    Pair("Shade 7500K", 7500),
  )

  Column {
    // Quick preset buttons
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .horizontalScroll(rememberScrollState()),
      horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
      presets.forEach { (label, kelvin) ->
        val isSelected = !isAuto && kotlin.math.abs(currentKelvin - kelvin) < 100
        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (isSelected) ProAmber else ProSurface)
            .border(1.dp, if (isSelected) ProAmber else ProSurfaceBorder, RoundedCornerShape(6.dp))
            .clickable { onSelectKelvin(kelvin) }
            .testTag("kelvin_preset_$kelvin")
            .padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
          Text(
            text = label,
            color = if (isSelected) ProDarkBackground else Color(0xFFF1F5F9),
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(8.dp))

    // Color Spectrum Bar
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .height(8.dp)
        .clip(RoundedCornerShape(4.dp))
        .background(
          Brush.horizontalGradient(
            colors = listOf(
              Color(0xFFFF7700), // 2000K warm orange
              Color(0xFFFFB300), // 3200K tungsten
              Color(0xFFFFF4E0), // 5500K daylight white
              Color(0xFF90CAF9), // 7000K sky blue
              Color(0xFF42A5F5)  // 10000K deep cool blue
            )
          )
        )
    )

    // Smooth Kelvin Slider
    Slider(
      value = currentKelvin.toFloat(),
      onValueChange = { onSelectKelvin(it.roundToInt()) },
      valueRange = 2500f..9500f,
      steps = 69,
      colors = SliderDefaults.colors(
        thumbColor = ProAmber,
        activeTrackColor = ProAmber,
        inactiveTrackColor = Color(0x33FFFFFF)
      ),
      modifier = Modifier
        .fillMaxWidth()
        .testTag("kelvin_slider")
    )
  }
}

@Composable
private fun EvControlContent(
  currentEvIndex: Int,
  onSelectEvIndex: (Int) -> Unit,
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
  ) {
    IconButton(
      onClick = { onSelectEvIndex((currentEvIndex - 1).coerceAtLeast(-6)) },
      modifier = Modifier.testTag("ev_minus_button")
    ) {
      Icon(Icons.Default.Remove, contentDescription = "Minus EV", tint = ProAmber)
    }

    Row(
      modifier = Modifier
        .weight(1f)
        .horizontalScroll(rememberScrollState()),
      horizontalArrangement = Arrangement.Center
    ) {
      (-6..6).forEach { index ->
        val evValue = (index * 0.333f * 10f).roundToInt() / 10f
        val isSelected = currentEvIndex == index
        Box(
          modifier = Modifier
            .padding(horizontal = 3.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(if (isSelected) ProAmber else ProSurface)
            .border(1.dp, if (isSelected) ProAmber else ProSurfaceBorder, RoundedCornerShape(4.dp))
            .clickable { onSelectEvIndex(index) }
            .testTag("ev_step_$index")
            .padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
          Text(
            text = if (evValue > 0f) "+$evValue" else "$evValue",
            color = if (isSelected) ProDarkBackground else Color.White,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold
          )
        }
      }
    }

    IconButton(
      onClick = { onSelectEvIndex((currentEvIndex + 1).coerceAtMost(6)) },
      modifier = Modifier.testTag("ev_plus_button")
    ) {
      Icon(Icons.Default.Add, contentDescription = "Plus EV", tint = ProAmber)
    }
  }
}

@Composable
private fun FocusControlContent(
  currentFocus: Float?,
  detectedMeters: Float,
  onSelectFocus: (Float?) -> Unit,
) {
  val focusStops = listOf(
    Pair("∞ INF", 0.0f),
    Pair("5m", 0.2f),
    Pair("2m", 0.5f),
    Pair("1m", 1.0f),
    Pair("50cm", 2.0f),
    Pair("20cm", 5.0f),
    Pair("10cm Macro", 10.0f)
  )

  Column {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .horizontalScroll(rememberScrollState()),
      horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
      focusStops.forEach { (label, diopter) ->
        val isSelected = currentFocus != null && kotlin.math.abs(currentFocus - diopter) < 0.1f
        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (isSelected) ProAmber else ProSurface)
            .border(1.dp, if (isSelected) ProAmber else ProSurfaceBorder, RoundedCornerShape(6.dp))
            .clickable { onSelectFocus(diopter) }
            .testTag("focus_stop_$diopter")
            .padding(horizontal = 10.dp, vertical = 8.dp)
        ) {
          Text(
            text = label,
            color = if (isSelected) ProDarkBackground else Color.White,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(6.dp))

    // Smooth manual focus slider
    Slider(
      value = currentFocus ?: 0.5f,
      onValueChange = { onSelectFocus(it) },
      valueRange = 0.0f..10.0f,
      colors = SliderDefaults.colors(
        thumbColor = ProAmber,
        activeTrackColor = ProAmber,
        inactiveTrackColor = Color(0x33FFFFFF)
      ),
      modifier = Modifier
        .fillMaxWidth()
        .testTag("focus_slider")
    )
  }
}

private fun getCurrentReadout(
  type: ManualControlType,
  exposure: DetectedExposure,
  manualIso: Int?,
  manualShutterNanos: Long?,
  manualKelvin: Int?,
  manualEvIndex: Int,
  manualFocusDistance: Float?,
): String {
  return when (type) {
    ManualControlType.ISO -> "${manualIso ?: exposure.iso} ${if (manualIso != null) "(LOCK)" else "(AUTO)"}"
    ManualControlType.SHUTTER -> "${exposure.shutterString}s ${if (manualShutterNanos != null) "(LOCK)" else "(AUTO)"}"
    ManualControlType.WHITE_BALANCE -> "${manualKelvin ?: exposure.kelvin}K ${if (manualKelvin != null) "(LOCK)" else "(AUTO)"}"
    ManualControlType.EV -> "${if (exposure.ev > 0f) "+${exposure.ev}" else "${exposure.ev}"} EV"
    ManualControlType.FOCUS -> if (manualFocusDistance != null) "${exposure.focusDistanceMeters}m (MANUAL)" else "AF-C AUTO"
  }
}

private fun isControlAuto(
  type: ManualControlType,
  manualIso: Int?,
  manualShutterNanos: Long?,
  manualKelvin: Int?,
  manualEvIndex: Int,
  manualFocusDistance: Float?,
): Boolean {
  return when (type) {
    ManualControlType.ISO -> manualIso == null
    ManualControlType.SHUTTER -> manualShutterNanos == null
    ManualControlType.WHITE_BALANCE -> manualKelvin == null
    ManualControlType.EV -> manualEvIndex == 0
    ManualControlType.FOCUS -> manualFocusDistance == null
  }
}
