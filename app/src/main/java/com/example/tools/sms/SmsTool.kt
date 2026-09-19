package com.example.tools.sms

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.telephony.SmsManager
import com.example.tools.AndroidTool
import com.example.tools.RiskLevel
import com.example.tools.ToolDefinition
import com.example.tools.ToolParameter
import com.example.tools.ToolResult

class SmsTool : AndroidTool {
    override val definition: ToolDefinition = ToolDefinition(
        name = "send_sms",
        description = "Send a text SMS message to a phone number. HIGH RISK action that requires explicit user confirmation before dispatching.",
        riskLevel = RiskLevel.HIGH,
        requiredPermissions = listOf(Manifest.permission.SEND_SMS),
        parameters = listOf(
            ToolParameter(
                name = "recipient",
                type = "string",
                description = "Phone number or contact name of the recipient",
                required = true
            ),
            ToolParameter(
                name = "message",
                type = "string",
                description = "The message body to send",
                required = true
            )
        )
    )

    override suspend fun execute(context: Context, arguments: Map<String, Any?>): ToolResult {
        val recipient = arguments["recipient"]?.toString()?.trim() ?: ""
        val message = arguments["message"]?.toString()?.trim() ?: ""

        if (recipient.isEmpty()) {
            return ToolResult.failure("Recipient phone number is required.")
        }
        if (message.isEmpty()) {
            return ToolResult.failure("Message body cannot be empty.")
        }

        return try {
            val smsManager: SmsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                context.getSystemService(SmsManager::class.java)
            } else {
                @Suppress("DEPRECATION")
                SmsManager.getDefault()
            }

            // Split into multipart if long
            val parts = smsManager.divideMessage(message)
            smsManager.sendMultipartTextMessage(recipient, null, parts, null, null)

            ToolResult.success(
                data = mapOf(
                    "recipient" to recipient,
                    "message_length" to message.length,
                    "status" to "dispatched"
                ),
                userNotice = "SMS sent to $recipient"
            )
        } catch (e: SecurityException) {
            // Fallback to opening SMS messenger app with pre-filled fields if direct permission wasn't granted
            try {
                val uri = Uri.parse("smsto:$recipient")
                val intent = Intent(Intent.ACTION_SENDTO, uri).apply {
                    putExtra("sms_body", message)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                ToolResult.success(
                    data = mapOf("recipient" to recipient, "fallback" to "sms_app_opened"),
                    userNotice = "Prepared SMS in default messaging app"
                )
            } catch (ex: Exception) {
                ToolResult.failure("SMS permission required or dispatch failed: ${e.localizedMessage}")
            }
        } catch (e: Exception) {
            ToolResult.failure("Failed to send SMS: ${e.localizedMessage}")
        }
    }
}
