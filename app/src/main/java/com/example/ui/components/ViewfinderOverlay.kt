package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CinemaAspectRatio
import com.example.sensor.HorizonOrientation
import com.example.ui.theme.ProAmber
import com.example.ui.theme.ProCyan
import com.example.ui.theme.ProGreen
import kotlinx.coroutines.delay

@Composable
fun ViewfinderOverlay(
  aspectRatio: CinemaAspectRatio,
  showGrid: Boolean,
  showCenterMark: Boolean,
  showHorizonLevel: Boolean,
  showZebraAlert: Boolean,
  horizonOrientation: HorizonOrientation,
  focusDistanceMeters: Float,
  onTapToFocus: (Float, Float) -> Unit,
  modifier: Modifier = Modifier,
) {
  var tapPoint by remember { mutableStateOf<Offset?>(null) }
  var showFocusRing by remember { mutableStateOf(false) }

  LaunchedEffect(tapPoint) {
    if (tapPoint != null) {
      showFocusRing = true
      delay(3000)
      showFocusRing = false
    }
  }

  BoxWithConstraints(
    modifier = modifier
      .fillMaxSize()
      .testTag("viewfinder_canvas_overlay")
      .pointerInput(Unit) {
        detectTapGestures { offset ->
          tapPoint = offset
          onTapToFocus(offset.x, offset.y)
        }
      }
  ) {
    val widthPx = constraints.maxWidth.toFloat()
    val heightPx = constraints.maxHeight.toFloat()

    Canvas(modifier = Modifier.fillMaxSize()) {
      var activeLeft = 0f
      var activeTop = 0f
      var activeRight = widthPx
      var activeBottom = heightPx

      // 1. Aspect Ratio Letterbox / Pillarbox Matte
      if (aspectRatio != CinemaAspectRatio.FULL && aspectRatio.ratio > 0f) {
        val targetRatio = aspectRatio.ratio
        val currentRatio = widthPx / heightPx

        val matteColor = Color(0xDD000000)

        if (currentRatio < targetRatio) {
          // Letterbox: black bars top and bottom
          val targetHeight = widthPx / targetRatio
          val barHeight = (heightPx - targetHeight) / 2f
          if (barHeight > 0f) {
            activeTop = barHeight
            activeBottom = heightPx - barHeight

            drawRect(color = matteColor, topLeft = Offset.Zero, size = Size(widthPx, barHeight))
            drawRect(color = matteColor, topLeft = Offset(0f, heightPx - barHeight), size = Size(widthPx, barHeight))

            // Aspect ratio thin frame outline
            drawLine(
              color = Color(0x66FFFFFF),
              start = Offset(0f, barHeight),
              end = Offset(widthPx, barHeight),
              strokeWidth = 1f
            )
            drawLine(
              color = Color(0x66FFFFFF),
              start = Offset(0f, heightPx - barHeight),
              end = Offset(widthPx, heightPx - barHeight),
              strokeWidth = 1f
            )
          }
        } else {
          // Pillarbox: black bars left and right
          val targetWidth = heightPx * targetRatio
          val barWidth = (widthPx - targetWidth) / 2f
          if (barWidth > 0f) {
            activeLeft = barWidth
            activeRight = widthPx - barWidth

            drawRect(color = matteColor, topLeft = Offset.Zero, size = Size(barWidth, heightPx))
            drawRect(color = matteColor, topLeft = Offset(widthPx - barWidth, 0f), size = Size(barWidth, heightPx))

            drawLine(
              color = Color(0x66FFFFFF),
              start = Offset(barWidth, 0f),
              end = Offset(barWidth, heightPx),
              strokeWidth = 1f
            )
            drawLine(
              color = Color(0x66FFFFFF),
              start = Offset(widthPx - barWidth, 0f),
              end = Offset(widthPx - barWidth, heightPx),
              strokeWidth = 1f
            )
          }
        }
      }

      // 1.5 Open Gate Full Sensor Markings & Multi-Format Safe Action Guides
      if (aspectRatio.isOpenGate) {
        val activeW = activeRight - activeLeft
        val activeH = activeBottom - activeTop
        val bracketLen = 24.dp.toPx()
        val bracketColor = ProCyan.copy(alpha = 0.95f)

        // Four Corner Sensor Gate Brackets (Full Uncropped Sensor Physical Framing)
        // Top-Left
        drawLine(color = bracketColor, start = Offset(activeLeft, activeTop), end = Offset(activeLeft + bracketLen, activeTop), strokeWidth = 2.5f)
        drawLine(color = bracketColor, start = Offset(activeLeft, activeTop), end = Offset(activeLeft, activeTop + bracketLen), strokeWidth = 2.5f)

        // Top-Right
        drawLine(color = bracketColor, start = Offset(activeRight, activeTop), end = Offset(activeRight - bracketLen, activeTop), strokeWidth = 2.5f)
        drawLine(color = bracketColor, start = Offset(activeRight, activeTop), end = Offset(activeRight, activeTop + bracketLen), strokeWidth = 2.5f)

        // Bottom-Left
        drawLine(color = bracketColor, start = Offset(activeLeft, activeBottom), end = Offset(activeLeft + bracketLen, activeBottom), strokeWidth = 2.5f)
        drawLine(color = bracketColor, start = Offset(activeLeft, activeBottom), end = Offset(activeLeft, activeBottom - bracketLen), strokeWidth = 2.5f)

        // Bottom-Right
        drawLine(color = bracketColor, start = Offset(activeRight, activeBottom), end = Offset(activeRight - bracketLen, activeBottom), strokeWidth = 2.5f)
        drawLine(color = bracketColor, start = Offset(activeRight, activeBottom), end = Offset(activeRight, activeBottom - bracketLen), strokeWidth = 2.5f)

        // 16:9 Cinema Safe Action Guides (horizontal deliverable crop zone)
        val h169 = activeW / (16f / 9f)
        if (h169 < activeH) {
          val top169 = (activeTop + activeBottom - h169) / 2f
          val bot169 = top169 + h169
          val guide169Color = Color(0x6600E5FF)
          drawLine(color = guide169Color, start = Offset(activeLeft, top169), end = Offset(activeRight, top169), strokeWidth = 1f)
          drawLine(color = guide169Color, start = Offset(activeLeft, bot169), end = Offset(activeRight, bot169), strokeWidth = 1f)
        }

        // 9:16 Vertical Reel/Social Safe Action Guides (vertical deliverable crop zone)
        val w916 = activeH * (9f / 16f)
        if (w916 < activeW) {
          val left916 = (activeLeft + activeRight - w916) / 2f
          val right916 = left916 + w916
          val guide916Color = Color(0x66FFB300)
          drawLine(color = guide916Color, start = Offset(left916, activeTop), end = Offset(left916, activeBottom), strokeWidth = 1f)
          drawLine(color = guide916Color, start = Offset(right916, activeTop), end = Offset(right916, activeBottom), strokeWidth = 1f)
        }
      }

      // 2. Rule of Thirds Grid Lines & Composition Intersection Nodes
      if (showGrid) {
        val activeW = activeRight - activeLeft
        val activeH = activeBottom - activeTop
        val x1 = activeLeft + activeW / 3f
        val x2 = activeLeft + activeW * 2f / 3f
        val y1 = activeTop + activeH / 3f
        val y2 = activeTop + activeH * 2f / 3f

        // Dual-contrast lines for optimal visibility against bright & dark scenes
        val shadowColor = Color(0x40000000)
        val gridColor = Color(0x88FFFFFF)
        val intersectionColor = ProAmber.copy(alpha = 0.9f)

        // Vertical line 1 (with subtle shadow)
        drawLine(color = shadowColor, start = Offset(x1 + 1f, activeTop), end = Offset(x1 + 1f, activeBottom), strokeWidth = 1.5f)
        drawLine(color = gridColor, start = Offset(x1, activeTop), end = Offset(x1, activeBottom), strokeWidth = 1f)

        // Vertical line 2
        drawLine(color = shadowColor, start = Offset(x2 + 1f, activeTop), end = Offset(x2 + 1f, activeBottom), strokeWidth = 1.5f)
        drawLine(color = gridColor, start = Offset(x2, activeTop), end = Offset(x2, activeBottom), strokeWidth = 1f)

        // Horizontal line 1
        drawLine(color = shadowColor, start = Offset(activeLeft, y1 + 1f), end = Offset(activeRight, y1 + 1f), strokeWidth = 1.5f)
        drawLine(color = gridColor, start = Offset(activeLeft, y1), end = Offset(activeRight, y1), strokeWidth = 1f)

        // Horizontal line 2
        drawLine(color = shadowColor, start = Offset(activeLeft, y2 + 1f), end = Offset(activeRight, y2 + 1f), strokeWidth = 1.5f)
        drawLine(color = gridColor, start = Offset(activeLeft, y2), end = Offset(activeRight, y2), strokeWidth = 1f)

        // Draw the 4 Golden Composition Power Points (where focal interest lands)
        val nodeCrossLen = 5.dp.toPx()
        val nodes = listOf(
          Offset(x1, y1),
          Offset(x2, y1),
          Offset(x1, y2),
          Offset(x2, y2)
        )

        nodes.forEach { pt ->
          // Subtle shadow backing
          drawCircle(color = Color(0x60000000), radius = 3.5.dp.toPx(), center = pt)
          // Amber intersection node point
          drawCircle(color = intersectionColor, radius = 2.dp.toPx(), center = pt)
          // Precise crosshairs at intersection
          drawLine(color = intersectionColor, start = Offset(pt.x - nodeCrossLen, pt.y), end = Offset(pt.x + nodeCrossLen, pt.y), strokeWidth = 1f)
          drawLine(color = intersectionColor, start = Offset(pt.x, pt.y - nodeCrossLen), end = Offset(pt.x, pt.y + nodeCrossLen), strokeWidth = 1f)
        }
      }

      // 3. Cinema Center Crosshair / Optical Mark
      if (showCenterMark) {
        val cx = widthPx / 2f
        val cy = heightPx / 2f
        val markLen = 18.dp.toPx()
        val gap = 6.dp.toPx()
        val markColor = Color(0x88FFFFFF)

        // Horizontal crosshairs
        drawLine(color = markColor, start = Offset(cx - markLen - gap, cy), end = Offset(cx - gap, cy), strokeWidth = 1.5f)
        drawLine(color = markColor, start = Offset(cx + gap, cy), end = Offset(cx + markLen + gap, cy), strokeWidth = 1.5f)
        // Vertical crosshairs
        drawLine(color = markColor, start = Offset(cx, cy - markLen - gap), end = Offset(cx, cy - gap), strokeWidth = 1.5f)
        drawLine(color = markColor, start = Offset(cx, cy + gap), end = Offset(cx, cy + markLen + gap), strokeWidth = 1.5f)

        // Center dot
        drawCircle(color = ProCyan.copy(alpha = 0.8f), radius = 2.dp.toPx(), center = Offset(cx, cy))
      }

      // 4. Electronic Horizon Tilt Level Indicator
      if (showHorizonLevel) {
        val cx = widthPx / 2f
        val cy = heightPx / 2f
        val levelWidth = 80.dp.toPx()

        rotate(degrees = horizonOrientation.rollDegrees, pivot = Offset(cx, cy)) {
          val levelColor = if (horizonOrientation.isLevel) ProGreen else Color(0xAAFFFFFF)

          // Left wing
          drawLine(
            color = levelColor,
            start = Offset(cx - levelWidth, cy),
            end = Offset(cx - 30.dp.toPx(), cy),
            strokeWidth = 2.5f
          )
          // Right wing
          drawLine(
            color = levelColor,
            start = Offset(cx + 30.dp.toPx(), cy),
            end = Offset(cx + levelWidth, cy),
            strokeWidth = 2.5f
          )
          // Center pitch reticle
          drawCircle(color = levelColor, radius = 3.dp.toPx(), center = Offset(cx, cy))
        }
      }

      // 5. Zebra Stripes highlight simulation (draws diagonal striped overlay on top right)
      if (showZebraAlert) {
        val zebraPath = Path()
        val startX = widthPx * 0.7f
        val startY = heightPx * 0.15f
        val zebraW = widthPx * 0.25f
        val zebraH = heightPx * 0.15f

        var x = startX
        while (x < startX + zebraW) {
          drawLine(
            color = Color(0x77FFFFFF),
            start = Offset(x, startY),
            end = Offset((x + 20f).coerceAtMost(startX + zebraW), startY + zebraH),
            strokeWidth = 2f
          )
          x += 16f
        }
      }
    }

    // Horizon angle readout badge
    if (showHorizonLevel) {
      Box(
        modifier = Modifier
          .align(Alignment.Center)
          .offset(y = 28.dp)
      ) {
        Text(
          text = if (horizonOrientation.isLevel) "LEVEL  0.0°" else "${if (horizonOrientation.rollDegrees > 0) "+" else ""}${horizonOrientation.rollDegrees}°",
          color = if (horizonOrientation.isLevel) ProGreen else Color(0xCCFFFFFF),
          fontSize = 11.sp,
          fontFamily = FontFamily.Monospace,
          fontWeight = FontWeight.Bold
        )
      }
    }

    // Tap-to-Focus Reticle
    AnimatedVisibility(
      visible = showFocusRing && tapPoint != null,
      enter = fadeIn(),
      exit = fadeOut(),
      modifier = Modifier
        .align(Alignment.TopStart)
        .offset {
          val pt = tapPoint ?: Offset.Zero
          androidx.compose.ui.unit.IntOffset(
            (pt.x - 32.dp.toPx()).toInt(),
            (pt.y - 32.dp.toPx()).toInt()
          )
        }
    ) {
      Box(
        modifier = Modifier.size(64.dp),
        contentAlignment = Alignment.Center
      ) {
        Canvas(modifier = Modifier.size(56.dp)) {
          val stroke = Stroke(width = 2.dp.toPx())
          drawRect(color = ProAmber, style = stroke)

          // Corner brackets
          val w = size.width
          val h = size.height
          val cornerLen = 8.dp.toPx()
          // Top Left
          drawLine(ProAmber, Offset(0f, 0f), Offset(cornerLen, 0f), 3f)
          drawLine(ProAmber, Offset(0f, 0f), Offset(0f, cornerLen), 3f)
          // Top Right
          drawLine(ProAmber, Offset(w, 0f), Offset(w - cornerLen, 0f), 3f)
          drawLine(ProAmber, Offset(w, 0f), Offset(w, cornerLen), 3f)
          // Bottom Left
          drawLine(ProAmber, Offset(0f, h), Offset(cornerLen, h), 3f)
          drawLine(ProAmber, Offset(0f, h), Offset(0f, h - cornerLen), 3f)
          // Bottom Right
          drawLine(ProAmber, Offset(w, h), Offset(w - cornerLen, h), 3f)
          drawLine(ProAmber, Offset(w, h), Offset(w, h - cornerLen), 3f)
        }
        Text(
          text = "${focusDistanceMeters}m",
          color = ProAmber,
          fontSize = 9.sp,
          fontFamily = FontFamily.Monospace,
          fontWeight = FontWeight.Bold,
          modifier = Modifier.offset(y = 36.dp)
        )
      }
    }

    // Open Gate Watermark & Max Quality Sensor Indicator
    if (aspectRatio.isOpenGate) {
      Box(
        modifier = Modifier
          .align(Alignment.TopStart)
          .padding(start = 14.dp, top = 92.dp)
          .background(Color(0xDD070A0E), RoundedCornerShape(6.dp))
          .border(1.dp, ProCyan.copy(alpha = 0.8f), RoundedCornerShape(6.dp))
          .padding(horizontal = 8.dp, vertical = 4.dp)
          .testTag("opengate_viewfinder_badge")
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(
            text = "⛶ OPEN GATE 3:2",
            color = ProCyan,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 0.5.sp
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = "FULL SENSOR • 100% QUALITY",
            color = Color.White.copy(alpha = 0.85f),
            fontSize = 8.5.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.SemiBold
          )
        }
      }
    }
  }
}
