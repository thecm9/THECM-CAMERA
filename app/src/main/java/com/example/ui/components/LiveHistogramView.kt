package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.ProCyan
import com.example.ui.theme.ProSurface
import com.example.ui.theme.ProSurfaceBorder
import com.example.ui.theme.ProTallyRed

@Composable
fun LiveHistogramView(
  bins: FloatArray,
  modifier: Modifier = Modifier,
) {
  Box(
    modifier = modifier
      .width(130.dp)
      .height(54.dp)
      .clip(RoundedCornerShape(8.dp))
      .background(Color(0xCC0D1117))
      .border(1.dp, ProSurfaceBorder, RoundedCornerShape(8.dp))
      .padding(4.dp)
      .testTag("live_histogram_widget")
  ) {
    Column(modifier = Modifier.fillMaxSize()) {
      Row(
        modifier = Modifier.padding(horizontal = 2.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "HISTOGRAM",
          color = Color(0xFF94A3B8),
          fontSize = 8.sp,
          fontFamily = FontFamily.Monospace,
          fontWeight = FontWeight.Bold,
          letterSpacing = 0.5.sp
        )
        Spacer(modifier = Modifier.weight(1f))
        Text(
          text = "LUMA",
          color = ProCyan,
          fontSize = 8.sp,
          fontFamily = FontFamily.Monospace,
          fontWeight = FontWeight.Bold
        )
      }

      Spacer(modifier = Modifier.height(2.dp))

      Canvas(
        modifier = Modifier
          .fillMaxSize()
          .padding(horizontal = 2.dp, vertical = 1.dp)
      ) {
        val w = size.width
        val h = size.height
        val binCount = bins.size
        if (binCount < 2) return@Canvas

        val path = Path()
        path.moveTo(0f, h)

        val stepX = w / (binCount - 1).toFloat()
        for (i in 0 until binCount) {
          val x = i * stepX
          val barH = (bins[i] * h).coerceIn(0f, h)
          val y = h - barH
          if (i == 0) path.lineTo(x, y) else path.lineTo(x, y)
        }
        path.lineTo(w, h)
        path.close()

        // Fill histogram with smooth gradient
        drawPath(
          path = path,
          brush = Brush.verticalGradient(
            colors = listOf(
              ProCyan.copy(alpha = 0.8f),
              ProCyan.copy(alpha = 0.3f),
              Color.Transparent
            )
          ),
          style = Fill
        )

        // Outline stroke
        drawPath(
          path = path,
          color = ProCyan,
          style = Stroke(width = 1.2.dp.toPx())
        )

        // Highlight clipping indicator on right edge
        if (bins.isNotEmpty() && bins.last() > 0.85f) {
          drawLine(
            color = ProTallyRed,
            start = Offset(w, 0f),
            end = Offset(w, h),
            strokeWidth = 2.dp.toPx()
          )
        }
      }
    }
  }
}
