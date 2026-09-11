package com.example.ui.components

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MovieFilter
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CinemaPreset
import com.example.ui.theme.ProAmber
import com.example.ui.theme.ProCyan
import com.example.ui.theme.ProDarkBackground
import com.example.ui.theme.ProSurface
import com.example.ui.theme.ProSurfaceBorder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PresetsBottomSheet(
  presets: List<CinemaPreset>,
  onSelectPreset: (CinemaPreset) -> Unit,
  onDismiss: () -> Unit,
) {
  val sheetState = rememberModalBottomSheetState()

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = ProDarkBackground,
    contentColor = Color.White
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = Icons.Default.MovieFilter,
            contentDescription = null,
            tint = ProAmber,
            modifier = Modifier.size(22.dp)
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "CINEMA LOOK PRESETS",
            color = ProAmber,
            fontSize = 15.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 1.sp
          )
        }

        IconButton(
          onClick = onDismiss,
          modifier = Modifier.testTag("dismiss_presets_button")
        ) {
          Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
        }
      }

      Text(
        text = "Select an engineered exposure configuration for film, photography, or high-speed video.",
        color = Color(0xFF94A3B8),
        fontSize = 12.sp,
        modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
      )

      LazyColumn(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
          .fillMaxWidth()
          .padding(bottom = 24.dp)
      ) {
        items(presets) { preset ->
          PresetItemCard(
            preset = preset,
            onClick = { onSelectPreset(preset) }
          )
        }
      }
    }
  }
}

@Composable
private fun PresetItemCard(
  preset: CinemaPreset,
  onClick: () -> Unit,
) {
  Box(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(12.dp))
      .background(ProSurface)
      .border(1.dp, ProSurfaceBorder, RoundedCornerShape(12.dp))
      .clickable { onClick() }
      .testTag("preset_card_${preset.id}")
      .padding(14.dp)
  ) {
    Column {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = preset.name,
          color = Color(0xFFF1F5F9),
          fontSize = 14.sp,
          fontFamily = FontFamily.Monospace,
          fontWeight = FontWeight.Bold
        )

        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(Color(0x3300E5FF))
            .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
          Text(
            text = preset.aspectRatio.badge,
            color = ProCyan,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold
          )
        }
      }

      Spacer(modifier = Modifier.height(4.dp))

      Text(
        text = preset.description,
        color = Color(0xFF94A3B8),
        fontSize = 11.sp,
        lineHeight = 15.sp
      )

      Spacer(modifier = Modifier.height(8.dp))

      // Setting badges: ISO, Shutter, Kelvin, FPS
      Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        if (preset.iso != null) {
          Badge(text = "ISO ${preset.iso}")
        }
        if (preset.shutterNanos != null) {
          val sec = preset.shutterNanos.toDouble() / 1_000_000_000.0
          val str = if (sec < 0.3) "1/${(1.0 / sec).toInt()}" else "${sec}s"
          Badge(text = "${str}s")
        }
        if (preset.kelvin != null) {
          Badge(text = "${preset.kelvin}K")
        }
        Badge(text = "${preset.frameRate.fps} FPS")
      }
    }
  }
}

@Composable
private fun Badge(text: String) {
  Box(
    modifier = Modifier
      .clip(RoundedCornerShape(4.dp))
      .background(Color(0x22FFFFFF))
      .padding(horizontal = 6.dp, vertical = 2.dp)
  ) {
    Text(
      text = text,
      color = Color(0xFFE2E8F0),
      fontSize = 10.sp,
      fontFamily = FontFamily.Monospace,
      fontWeight = FontWeight.SemiBold
    )
  }
}
