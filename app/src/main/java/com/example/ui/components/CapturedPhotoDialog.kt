package com.example.ui.components

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.window.Dialog
import com.example.model.DetectedExposure
import com.example.ui.theme.ProAmber
import com.example.ui.theme.ProCyan
import com.example.ui.theme.ProDarkBackground
import com.example.ui.theme.ProSurface
import com.example.ui.theme.ProSurfaceBorder

@Composable
fun CapturedPhotoDialog(
  photoUri: Uri?,
  metadata: DetectedExposure?,
  isOpenGate: Boolean = false,
  onDismiss: () -> Unit,
) {
  if (metadata == null) return

  Dialog(onDismissRequest = onDismiss) {
    Surface(
      shape = RoundedCornerShape(16.dp),
      color = ProSurface,
      border = androidx.compose.foundation.BorderStroke(1.dp, ProSurfaceBorder),
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp)
        .testTag("captured_photo_dialog")
    ) {
      Column(
        modifier = Modifier.padding(18.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "EXIF METADATA CARD",
            color = ProAmber,
            fontSize = 14.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 1.sp
          )

          IconButton(
            onClick = onDismiss,
            modifier = Modifier.size(28.dp)
          ) {
            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Metadata grid
        Column(
          verticalArrangement = Arrangement.spacedBy(8.dp),
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(ProDarkBackground)
            .border(1.dp, ProSurfaceBorder, RoundedCornerShape(8.dp))
            .padding(12.dp)
        ) {
          MetaRow(label = "ISO SENSITIVITY", value = "ISO ${metadata.iso}", valueColor = ProAmber)
          MetaRow(label = "EXPOSURE TIME", value = "${metadata.shutterString}s (${metadata.shutterAngle}°)", valueColor = ProAmber)
          MetaRow(label = "COLOR TEMP (WB)", value = "${metadata.kelvin}K", valueColor = ProCyan)
          MetaRow(label = "EV COMPENSATION", value = "${metadata.ev} EV", valueColor = Color.White)
          MetaRow(label = "APERTURE", value = "f/${metadata.aperture}", valueColor = ProCyan)
          MetaRow(label = "FOCUS DISTANCE", value = "${metadata.focusDistanceMeters}m", valueColor = Color.White)
          if (isOpenGate) {
            MetaRow(label = "CAPTURE SENSOR", value = "OPEN GATE 3:2 (FULL SENSOR)", valueColor = ProCyan)
            MetaRow(label = "ISP PIPELINE", value = "MAX QUALITY (100% QUALITY)", valueColor = ProCyan)
          }
          MetaRow(label = "FILE STATUS", value = "SAVED TO STORAGE", valueColor = Color(0xFF00E676))
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
          onClick = onDismiss,
          colors = ButtonDefaults.buttonColors(containerColor = ProAmber),
          shape = RoundedCornerShape(8.dp),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("photo_dialog_done_button")
        ) {
          Text(
            text = "RETURN TO VIEWFINDER",
            color = ProDarkBackground,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp
          )
        }
      }
    }
  }
}

@Composable
private fun MetaRow(label: String, value: String, valueColor: Color) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
  ) {
    Text(
      text = label,
      color = Color(0xFF94A3B8),
      fontSize = 11.sp,
      fontFamily = FontFamily.Monospace
    )
    Text(
      text = value,
      color = valueColor,
      fontSize = 12.sp,
      fontFamily = FontFamily.Monospace,
      fontWeight = FontWeight.Bold
    )
  }
}
