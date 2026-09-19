package com.example.tools.contacts

import android.Manifest
import android.content.Context
import android.provider.ContactsContract
import com.example.tools.AndroidTool
import com.example.tools.RiskLevel
import com.example.tools.ToolDefinition
import com.example.tools.ToolParameter
import com.example.tools.ToolResult

class ContactsTool : AndroidTool {
    override val definition: ToolDefinition = ToolDefinition(
        name = "search_contacts",
        description = "Search device contacts by name to retrieve phone number, name, and details",
        riskLevel = RiskLevel.LOW,
        requiredPermissions = listOf(Manifest.permission.READ_CONTACTS),
        parameters = listOf(
            ToolParameter(
                name = "query",
                type = "string",
                description = "Name or partial name of the contact to look up (e.g. 'John', 'Mom', 'Doctor')",
                required = true
            )
        )
    )

    override suspend fun execute(context: Context, arguments: Map<String, Any?>): ToolResult {
        val query = arguments["query"]?.toString()?.trim() ?: ""
        if (query.isEmpty()) {
            return ToolResult.failure("Search query cannot be empty.")
        }

        val results = mutableListOf<Map<String, String>>()
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER,
            ContactsContract.CommonDataKinds.Phone.TYPE
        )

        try {
            val cursor = context.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                projection,
                "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?",
                arrayOf("%$query%"),
                "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC"
            )

            cursor?.use {
                val nameIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val numberIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                var count = 0
                while (it.moveToNext() && count < 10) {
                    val name = if (nameIdx != -1) it.getString(nameIdx) else ""
                    val number = if (numberIdx != -1) it.getString(numberIdx) else ""
                    if (name.isNotEmpty() || number.isNotEmpty()) {
                        results.add(mapOf("name" to name, "phone_number" to number))
                        count++
                    }
                }
            }

            if (results.isEmpty()) {
                return ToolResult.success(
                    data = mapOf("query" to query, "found" to false, "contacts" to emptyList<String>()),
                    userNotice = "No contacts found matching '$query'"
                )
            }

            return ToolResult.success(
                data = mapOf(
                    "query" to query,
                    "found" to true,
                    "count" to results.size,
                    "contacts" to results
                ),
                userNotice = "Found ${results.size} contact(s) for '$query'"
            )
        } catch (e: Exception) {
            return ToolResult.failure("Error querying contacts: ${e.localizedMessage}")
        }
    }
}
