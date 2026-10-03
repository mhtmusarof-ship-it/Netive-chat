package com.example

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.example.data.AudioRecorderManager
import com.example.data.FirebaseRepository
import com.google.firebase.database.FirebaseDatabase

class NeticeApplication : Application() {

    lateinit var repository: FirebaseRepository
        private set

    lateinit var audioManager: AudioRecorderManager
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        try {
            FirebaseDatabase.getInstance().setPersistenceEnabled(true)
        } catch (e: Exception) {
            // Already enabled or set
        }

        repository = FirebaseRepository()
        audioManager = AudioRecorderManager(this)

        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val messageChannel = NotificationChannel(
                CHANNEL_MESSAGES,
                "Netice Messages",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Chat messages and group notifications"
                enableVibration(true)
            }

            val callChannel = NotificationChannel(
                CHANNEL_CALLS,
                "Netice Voice & Video Calls",
                NotificationManager.IMPORTANCE_MAX
            ).apply {
                description = "Incoming voice and video calls"
                enableVibration(true)
            }

            notificationManager.createNotificationChannel(messageChannel)
            notificationManager.createNotificationChannel(callChannel)
        }
    }

    companion object {
        const val CHANNEL_MESSAGES = "netice_messages_channel"
        const val CHANNEL_CALLS = "netice_calls_channel"

        lateinit var instance: NeticeApplication
            private set
    }
}
