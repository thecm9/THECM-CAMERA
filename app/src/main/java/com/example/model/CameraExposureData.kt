package com.example.model

/**
 * Real-time detected exposure telemetry from the camera sensor.
 */
data class DetectedExposure(
  val iso: Int = 100,
  val shutterNanos: Long = 8_000_000L, // 1/125s in nanoseconds
  val shutterString: String = "1/125",
  val shutterAngle: Float = 180.0f,
  val kelvin: Int = 5600,
  val tint: Int = 0,
  val aperture: Float = 1.8f,
  val ev: Float = 0.0f,
  val focusDistanceMeters: Float = 1.5f,
  val isAfLocked: Boolean = false,
  val isAeLocked: Boolean = false,
  val isAwbLocked: Boolean = false,
  val histogramBins: FloatArray = FloatArray(32) { 0.1f },
) {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false
    other as DetectedExposure
    return iso == other.iso &&
      shutterNanos == other.shutterNanos &&
      shutterString == other.shutterString &&
      kelvin == other.kelvin &&
      aperture == other.aperture &&
      ev == other.ev
  }

  override fun hashCode(): Int {
    var result = iso
    result = 31 * result + shutterNanos.hashCode()
    result = 31 * result + kelvin
    result = 31 * result + ev.hashCode()
    return result
  }
}

/**
 * Supported manual controls that can be overridden by the user.
 */
enum class ManualControlType(val label: String, val unit: String) {
  ISO("ISO", ""),
  SHUTTER("SHUTTER", "s"),
  WHITE_BALANCE("WB", "K"),
  EV("EV COMP", "EV"),
  FOCUS("FOCUS", "m"),
}

/**
 * Standard cinematic aspect ratios for viewfinder framing.
 */
enum class CinemaAspectRatio(val title: String, val ratio: Float, val badge: String) {
  OPEN_GATE("Open Gate 3:2 (Full Sensor)", 3f / 2f, "OPEN GATE"),
  FULL("Full Screen", 0f, "FULL"),
  CINEMA_239("2.39:1 Anamorphic", 2.39f, "2.39:1"),
  CINEMA_185("1.85:1 Academy Flat", 1.85f, "1.85:1"),
  WIDE_16_9("16:9 Cinema", 16f / 9f, "16:9"),
  PHOTO_4_3("4:3 Standard", 4f / 3f, "4:3"),
  SQUARE_1_1("1:1 Square", 1.0f, "1:1");

  val isOpenGate: Boolean
    get() = this == OPEN_GATE
}

/**
 * Cinema frame rates for video and cinematic exposure angle calculations.
 */
enum class CinemaFrameRate(val fps: Int, val label: String) {
  FPS_24(24, "24 FPS (Film)"),
  FPS_30(30, "30 FPS (Video)"),
  FPS_60(60, "60 FPS (HFR)"),
}

/**
 * White balance presets with calibrated Kelvin temperatures.
 */
enum class WbPreset(val label: String, val kelvin: Int, val iconName: String) {
  AUTO("Auto WB", -1, "Auto"),
  INCANDESCENT("Tungsten", 3200, "Lightbulb"),
  FLUORESCENT("Fluorescent", 4000, "Fluorescent"),
  DAYLIGHT("Daylight", 5500, "Sunny"),
  CLOUDY("Cloudy", 6500, "Cloud"),
  SHADE("Shade", 7500, "Shade"),
  CUSTOM("Manual Kelvin", 5600, "Tune"),
}

/**
 * Preset configuration for quick cinematography and photography looks.
 */
data class CinemaPreset(
  val id: String,
  val name: String,
  val description: String,
  val iso: Int? = null,
  val shutterNanos: Long? = null,
  val kelvin: Int? = null,
  val evIndex: Int = 0,
  val aspectRatio: CinemaAspectRatio = CinemaAspectRatio.WIDE_16_9,
  val frameRate: CinemaFrameRate = CinemaFrameRate.FPS_24,
)
