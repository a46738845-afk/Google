package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.agent.SafetyPolicy
import com.example.security.AutonomyMode
import com.example.tools.AndroidTool
import com.example.tools.RiskLevel
import com.example.tools.ToolDefinition
import com.example.tools.ToolResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Hermes", appName)
  }

  @Test
  fun `safety policy enforces confirmation for high risk actions`() {
    val dummyHighRiskTool = object : AndroidTool {
      override val definition = ToolDefinition("send_sms", "Send SMS", RiskLevel.HIGH)
      override suspend fun execute(context: Context, arguments: Map<String, Any?>) = ToolResult.success()
    }

    // High risk always requires confirmation even in AUTONOMOUS mode
    assertTrue(SafetyPolicy.requiresUserConfirmation(dummyHighRiskTool, AutonomyMode.AUTONOMOUS))
    assertTrue(SafetyPolicy.requiresUserConfirmation(dummyHighRiskTool, AutonomyMode.ASSISTED))
    assertTrue(SafetyPolicy.requiresUserConfirmation(dummyHighRiskTool, AutonomyMode.MANUAL))
  }

  @Test
  fun `safety policy allows low risk actions in assisted mode`() {
    val dummyLowRiskTool = object : AndroidTool {
      override val definition = ToolDefinition("open_app", "Launch App", RiskLevel.LOW)
      override suspend fun execute(context: Context, arguments: Map<String, Any?>) = ToolResult.success()
    }

    assertFalse(SafetyPolicy.requiresUserConfirmation(dummyLowRiskTool, AutonomyMode.ASSISTED))
    assertFalse(SafetyPolicy.requiresUserConfirmation(dummyLowRiskTool, AutonomyMode.AUTONOMOUS))
    assertTrue(SafetyPolicy.requiresUserConfirmation(dummyLowRiskTool, AutonomyMode.MANUAL))
  }
}

