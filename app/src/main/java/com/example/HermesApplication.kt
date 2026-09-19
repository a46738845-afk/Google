package com.example

import android.app.Application
import com.example.memory.AppDatabase
import com.example.memory.MemoryRepository
import com.example.security.SecurePreferences
import com.example.voice.AndroidTextToSpeechEngine

class HermesApplication : Application() {

    lateinit var database: AppDatabase
        private set

    lateinit var memoryRepository: MemoryRepository
        private set

    lateinit var securePreferences: SecurePreferences
        private set

    lateinit var speechEngine: AndroidTextToSpeechEngine
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        database = AppDatabase.getDatabase(this)
        memoryRepository = MemoryRepository(
            conversationDao = database.conversationDao(),
            memoryFactDao = database.memoryFactDao(),
            scheduledTaskDao = database.scheduledTaskDao(),
            toolSettingDao = database.toolSettingDao(),
            activityLogDao = database.activityLogDao()
        )
        securePreferences = SecurePreferences(this)
        speechEngine = AndroidTextToSpeechEngine(this)
    }

    override fun onTerminate() {
        speechEngine.shutdown()
        super.onTerminate()
    }

    companion object {
        lateinit var instance: HermesApplication
            private set
    }
}
