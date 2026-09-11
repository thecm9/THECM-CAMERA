package com.example

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.ui.ProCameraViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Pro Camera", appName)
  }

  @Test
  fun `toggle rule of thirds grid updates state and toast`() {
    val application = ApplicationProvider.getApplicationContext<Application>()
    val viewModel = ProCameraViewModel(application)

    val initialState = viewModel.uiState.value.showGrid

    viewModel.toggleGrid()
    assertEquals(!initialState, viewModel.uiState.value.showGrid)
    assertTrue(viewModel.uiState.value.toastMessage?.contains("Rule of Thirds") == true)

    viewModel.toggleGrid()
    assertEquals(initialState, viewModel.uiState.value.showGrid)
    assertTrue(viewModel.uiState.value.toastMessage?.contains("Rule of Thirds") == true)
  }

  @Test
  fun `toggle open gate mode updates state and sets 3 to 2 ratio`() {
    val application = ApplicationProvider.getApplicationContext<Application>()
    val viewModel = ProCameraViewModel(application)

    assertFalse(viewModel.uiState.value.isOpenGateMode)

    viewModel.toggleOpenGateMode()
    assertTrue(viewModel.uiState.value.isOpenGateMode)
    assertEquals(com.example.model.CinemaAspectRatio.OPEN_GATE, viewModel.uiState.value.aspectRatio)
    assertTrue(viewModel.uiState.value.toastMessage?.contains("OPEN GATE") == true)

    viewModel.toggleOpenGateMode()
    assertFalse(viewModel.uiState.value.isOpenGateMode)
    assertEquals(com.example.model.CinemaAspectRatio.WIDE_16_9, viewModel.uiState.value.aspectRatio)
  }

  @Test
  fun `open gate preset applies full sensor configuration`() {
    val application = ApplicationProvider.getApplicationContext<Application>()
    val viewModel = ProCameraViewModel(application)

    val openGatePreset = viewModel.cinemaPresets.first { it.id == "open_gate_master" }
    viewModel.applyPreset(openGatePreset)

    assertTrue(viewModel.uiState.value.isOpenGateMode)
    assertEquals(com.example.model.CinemaAspectRatio.OPEN_GATE, viewModel.uiState.value.aspectRatio)
    assertEquals(100, viewModel.uiState.value.manualIso)
    assertEquals(5600, viewModel.uiState.value.manualKelvin)
  }
}
