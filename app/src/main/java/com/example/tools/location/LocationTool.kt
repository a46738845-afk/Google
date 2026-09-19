package com.example.tools.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationManager
import com.example.tools.AndroidTool
import com.example.tools.RiskLevel
import com.example.tools.ToolDefinition
import com.example.tools.ToolResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LocationTool : AndroidTool {
    override val definition: ToolDefinition = ToolDefinition(
        name = "get_location",
        description = "Retrieve the current GPS coordinates, latitude, longitude, and accuracy of the device",
        riskLevel = RiskLevel.LOW,
        requiredPermissions = listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    )

    @SuppressLint("MissingPermission")
    override suspend fun execute(context: Context, arguments: Map<String, Any?>): ToolResult =
        withContext(Dispatchers.IO) {
            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
                ?: return@withContext ToolResult.failure("LocationManager unavailable.")

            try {
                val providers = locationManager.getProviders(true)
                var bestLocation: Location? = null

                for (provider in providers) {
                    val loc = locationManager.getLastKnownLocation(provider) ?: continue
                    if (bestLocation == null || loc.accuracy < bestLocation.accuracy) {
                        bestLocation = loc
                    }
                }

                if (bestLocation != null) {
                    ToolResult.success(
                        data = mapOf(
                            "latitude" to bestLocation.latitude,
                            "longitude" to bestLocation.longitude,
                            "accuracy_meters" to bestLocation.accuracy,
                            "provider" to (bestLocation.provider ?: "gps"),
                            "timestamp" to bestLocation.time
                        ),
                        userNotice = "Acquired location (${"%.4f".format(bestLocation.latitude)}, ${"%.4f".format(bestLocation.longitude)})"
                    )
                } else {
                    ToolResult.success(
                        data = mapOf(
                            "status" to "location_pending",
                            "message" to "No cached GPS fix available. Location hardware is acquiring satellite lock."
                        ),
                        userNotice = "Acquiring GPS fix..."
                    )
                }
            } catch (e: Exception) {
                ToolResult.failure("Location error: ${e.localizedMessage}")
            }
        }
}
