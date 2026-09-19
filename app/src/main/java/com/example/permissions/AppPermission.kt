package com.example.permissions

import android.Manifest
import android.os.Build

enum class PermissionStatus {
    GRANTED,
    DENIED,
    PERMANENTLY_DENIED,
    UNAVAILABLE
}

enum class AppPermission(
    val title: String,
    val description: String,
    val manifestPermissions: List<String>
) {
    CAMERA(
        title = "Camera",
        description = "Required to capture photos and analyze scenes with vision models.",
        manifestPermissions = listOf(Manifest.permission.CAMERA)
    ),
    MICROPHONE(
        title = "Microphone",
        description = "Required for push-to-talk voice commands.",
        manifestPermissions = listOf(Manifest.permission.RECORD_AUDIO)
    ),
    CONTACTS(
        title = "Contacts",
        description = "Required to search contacts and resolve phone numbers.",
        manifestPermissions = listOf(Manifest.permission.READ_CONTACTS)
    ),
    SMS(
        title = "SMS",
        description = "Required to prepare and send text messages upon confirmation.",
        manifestPermissions = listOf(Manifest.permission.SEND_SMS, Manifest.permission.READ_SMS)
    ),
    PHONE(
        title = "Phone Calls",
        description = "Required to initiate direct calls upon confirmation.",
        manifestPermissions = listOf(Manifest.permission.CALL_PHONE)
    ),
    LOCATION(
        title = "Location",
        description = "Required to detect device location for queries and directions.",
        manifestPermissions = listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    ),
    CALENDAR(
        title = "Calendar",
        description = "Required to check your schedule and create events upon confirmation.",
        manifestPermissions = listOf(
            Manifest.permission.READ_CALENDAR,
            Manifest.permission.WRITE_CALENDAR
        )
    ),
    NOTIFICATIONS(
        title = "Notifications",
        description = "Required to display reminders and background task alerts.",
        manifestPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            listOf(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            emptyList()
        }
    );

    companion object {
        fun fromManifest(manifestName: String): AppPermission? {
            return entries.find { it.manifestPermissions.contains(manifestName) }
        }
    }
}
