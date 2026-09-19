package com.example.permissions

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class PermissionManager {

    fun hasPermission(context: Context, permission: String): Boolean {
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }

    fun hasPermissions(context: Context, permissions: List<String>): Boolean {
        return permissions.all { hasPermission(context, it) }
    }

    fun getMissingPermissions(context: Context, permissions: List<String>): List<String> {
        return permissions.filter { !hasPermission(context, it) }
    }

    fun getPermissionStatus(activity: Activity, permission: String): PermissionStatus {
        return when {
            hasPermission(activity, permission) -> PermissionStatus.GRANTED
            ActivityCompat.shouldShowRequestPermissionRationale(activity, permission) -> PermissionStatus.DENIED
            else -> PermissionStatus.DENIED
        }
    }
}
