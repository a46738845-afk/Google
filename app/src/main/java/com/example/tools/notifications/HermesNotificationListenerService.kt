package com.example.tools.notifications

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import java.util.concurrent.CopyOnWriteArrayList

data class CachedNotification(
    val id: Int,
    val packageName: String,
    val title: String,
    val text: String,
    val postTime: Long = System.currentTimeMillis()
)

class HermesNotificationListenerService : NotificationListenerService() {

    override fun onListenerConnected() {
        super.onListenerConnected()
        isServiceConnected = true
        refreshNotifications()
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        isServiceConnected = false
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        sbn?.let { extractAndCache(it) }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
        val key = sbn?.key
        if (key != null) {
            recentNotifications.removeAll { it.id == sbn.id && it.packageName == sbn.packageName }
        }
    }

    private fun refreshNotifications() {
        try {
            val active = activeNotifications ?: return
            recentNotifications.clear()
            for (sbn in active) {
                extractAndCache(sbn)
            }
        } catch (e: Exception) {
            // Ignored
        }
    }

    private fun extractAndCache(sbn: StatusBarNotification) {
        val extras = sbn.notification.extras ?: return
        val title = extras.getCharSequence("android.title")?.toString() ?: ""
        val text = extras.getCharSequence("android.text")?.toString() ?: ""
        if (title.isNotEmpty() || text.isNotEmpty()) {
            recentNotifications.add(
                0,
                CachedNotification(
                    id = sbn.id,
                    packageName = sbn.packageName,
                    title = title,
                    text = text,
                    postTime = sbn.postTime
                )
            )
            // Cap to 20 notifications
            if (recentNotifications.size > 20) {
                recentNotifications.removeAt(recentNotifications.size - 1)
            }
        }
    }

    companion object {
        var isServiceConnected: Boolean = false
        val recentNotifications = CopyOnWriteArrayList<CachedNotification>()
    }
}
