package com.example.tools.calendar

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.provider.CalendarContract
import com.example.tools.AndroidTool
import com.example.tools.RiskLevel
import com.example.tools.ToolDefinition
import com.example.tools.ToolParameter
import com.example.tools.ToolResult
import java.util.TimeZone

class CalendarTool : AndroidTool {
    override val definition: ToolDefinition = ToolDefinition(
        name = "manage_calendar",
        description = "Read upcoming calendar events or create a new event. Creating events is MEDIUM risk and requires confirmation.",
        riskLevel = RiskLevel.MEDIUM,
        requiredPermissions = listOf(
            Manifest.permission.READ_CALENDAR,
            Manifest.permission.WRITE_CALENDAR
        ),
        parameters = listOf(
            ToolParameter(
                name = "action",
                type = "string",
                description = "Action: 'read', 'create', 'search'",
                required = true,
                enumValues = listOf("read", "create", "search")
            ),
            ToolParameter(
                name = "title",
                type = "string",
                description = "Event title / description (required for 'create' or query for 'search')",
                required = false
            ),
            ToolParameter(
                name = "days_ahead",
                type = "number",
                description = "Number of days ahead to scan for upcoming events (default: 7)",
                required = false
            ),
            ToolParameter(
                name = "start_minutes_from_now",
                type = "number",
                description = "Minutes from now when the event starts (default: 60)",
                required = false
            ),
            ToolParameter(
                name = "duration_minutes",
                type = "number",
                description = "Duration of the event in minutes (default: 60)",
                required = false
            )
        )
    )

    override suspend fun execute(context: Context, arguments: Map<String, Any?>): ToolResult {
        val action = arguments["action"]?.toString()?.lowercase() ?: "read"
        val title = arguments["title"]?.toString() ?: "Meeting"
        val daysAhead = (arguments["days_ahead"] as? Number)?.toInt() ?: 7
        val startOffset = (arguments["start_minutes_from_now"] as? Number)?.toInt() ?: 60
        val duration = (arguments["duration_minutes"] as? Number)?.toInt() ?: 60

        val now = System.currentTimeMillis()

        return try {
            when (action) {
                "read", "search" -> {
                    val endTime = now + (daysAhead * 24L * 60L * 60L * 1000L)
                    val projection = arrayOf(
                        CalendarContract.Events._ID,
                        CalendarContract.Events.TITLE,
                        CalendarContract.Events.DTSTART,
                        CalendarContract.Events.DTEND,
                        CalendarContract.Events.EVENT_LOCATION
                    )
                    val selection = "(${CalendarContract.Events.DTSTART} >= ?) AND (${CalendarContract.Events.DTSTART} <= ?)"
                    val selectionArgs = arrayOf(now.toString(), endTime.toString())

                    val events = mutableListOf<Map<String, Any?>>()
                    val cursor = context.contentResolver.query(
                        CalendarContract.Events.CONTENT_URI,
                        projection,
                        selection,
                        selectionArgs,
                        "${CalendarContract.Events.DTSTART} ASC"
                    )

                    cursor?.use {
                        val titleIdx = it.getColumnIndex(CalendarContract.Events.TITLE)
                        val startIdx = it.getColumnIndex(CalendarContract.Events.DTSTART)
                        val locIdx = it.getColumnIndex(CalendarContract.Events.EVENT_LOCATION)

                        while (it.moveToNext() && events.size < 15) {
                            val evTitle = if (titleIdx != -1) it.getString(titleIdx) else "Untitled"
                            val evStart = if (startIdx != -1) it.getLong(startIdx) else 0L
                            val evLoc = if (locIdx != -1) it.getString(locIdx) ?: "" else ""

                            if (action == "search" && !evTitle.contains(title, ignoreCase = true)) {
                                continue
                            }

                            events.add(
                                mapOf(
                                    "title" to evTitle,
                                    "start_epoch" to evStart,
                                    "location" to evLoc
                                )
                            )
                        }
                    }

                    ToolResult.success(
                        data = mapOf(
                            "count" to events.size,
                            "days_ahead" to daysAhead,
                            "events" to events
                        ),
                        userNotice = if (events.isEmpty()) "No upcoming calendar events" else "Found ${events.size} event(s)"
                    )
                }

                "create" -> {
                    // Safe approach: either insert via Calendar Provider if calendar ID found, or launch Calendar Contract Intent
                    val startTime = now + (startOffset * 60L * 1000L)
                    val endTime = startTime + (duration * 60L * 1000L)

                    val intent = Intent(Intent.ACTION_INSERT).apply {
                        data = CalendarContract.Events.CONTENT_URI
                        putExtra(CalendarContract.Events.TITLE, title)
                        putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, startTime)
                        putExtra(CalendarContract.EXTRA_EVENT_END_TIME, endTime)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)

                    ToolResult.success(
                        data = mapOf(
                            "title" to title,
                            "start_epoch" to startTime,
                            "duration_minutes" to duration,
                            "status" to "event_intent_opened"
                        ),
                        userNotice = "Prepared calendar event: '$title'"
                    )
                }

                else -> ToolResult.failure("Unsupported calendar action: $action")
            }
        } catch (e: Exception) {
            ToolResult.failure("Calendar error: ${e.localizedMessage}")
        }
    }
}
