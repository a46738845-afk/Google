package com.example.tools.phone

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.core.content.ContextCompat
import com.example.tools.AndroidTool
import com.example.tools.RiskLevel
import com.example.tools.ToolDefinition
import com.example.tools.ToolParameter
import com.example.tools.ToolResult

class PhoneTool : AndroidTool {
    override val definition: ToolDefinition = ToolDefinition(
        name = "make_call",
        description = "Initiate a phone call or open the Android dialer with a phone number or contact name",
        riskLevel = RiskLevel.HIGH,
        parameters = listOf(
            ToolParameter(
                name = "phone_number",
                type = "string",
                description = "Phone number or digits to dial",
                required = true
            ),
            ToolParameter(
                name = "direct_call",
                type = "boolean",
                description = "True to call directly (requires CALL_PHONE permission), false to open system dialer",
                required = false
            )
        )
    )

    override suspend fun execute(context: Context, arguments: Map<String, Any?>): ToolResult {
        val rawNumber = arguments["phone_number"]?.toString()?.trim() ?: ""
        val directCall = arguments["direct_call"] as? Boolean ?: false

        if (rawNumber.isEmpty()) {
            return ToolResult.failure("Phone number is required.")
        }

        val cleanNumber = rawNumber.replace(" ", "").replace("-", "")

        try {
            if (directCall && ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
                val callIntent = Intent(Intent.ACTION_CALL, Uri.parse("tel:$cleanNumber")).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(callIntent)
                return ToolResult.success(
                    data = mapOf("number" to cleanNumber, "action" to "direct_call_initiated"),
                    userNotice = "Calling $cleanNumber"
                )
            } else {
                // Safe default: open dialer with pre-filled number
                val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$cleanNumber")).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(dialIntent)
                return ToolResult.success(
                    data = mapOf("number" to cleanNumber, "action" to "dialer_opened"),
                    userNotice = "Opened dialer for $cleanNumber"
                )
            }
        } catch (e: Exception) {
            return ToolResult.failure("Unable to place call: ${e.localizedMessage}")
        }
    }
}
