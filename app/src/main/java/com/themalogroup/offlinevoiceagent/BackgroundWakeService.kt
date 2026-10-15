package com.themalogroup.offlinevoiceagent

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import java.util.Locale

/**
 * Explicitly user-started foreground microphone service. It recognizes text produced by
 * Android SpeechRecognizer; it is not an acoustic always-on hotword engine. Provider,
 * language-pack, battery, and OEM behavior can limit reliability and offline operation.
 */
class BackgroundWakeService : Service() {
    private var recognizer: SpeechRecognizer? = null
    private var listening = false
    private var stopped = false
    private var retryCount = 0
    private var awaitingCommand = false
    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate() {
        super.onCreate()
        createChannel()
        tts = TextToSpeech(applicationContext) { result -> ttsReady = result == TextToSpeech.SUCCESS }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            stopSelf()
            return START_NOT_STICKY
        }
        try {
            startForeground(NOTIFICATION_ID, buildNotification("Listening for “Hey Maya”"))
        } catch (e: Exception) {
            Log.e(TAG, "Could not start microphone foreground service", e)
            stopSelf()
            return START_NOT_STICKY
        }
        stopped = false
        if (recognizer == null) createRecognizer()
        listenAgain(250L)
        return START_NOT_STICKY
    }

    private fun createRecognizer() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            updateNotification("Speech recognition service unavailable")
            stopSelf()
            return
        }
        recognizer = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && SpeechRecognizer.isOnDeviceRecognitionAvailable(this)) {
                SpeechRecognizer.createOnDeviceSpeechRecognizer(this)
            } else {
                SpeechRecognizer.createSpeechRecognizer(this)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Recognizer creation failed", e)
            updateNotification("Could not create speech recognizer")
            stopSelf()
            null
        }
        recognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) { listening = true }
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onError(error: Int) {
                listening = false
                retryCount = (retryCount + 1).coerceAtMost(5)
                listenAgain((500L shl retryCount).coerceAtMost(12_000L))
            }
            override fun onResults(results: Bundle?) {
                listening = false
                retryCount = 0
                val spoken = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty()
                when {
                    awaitingCommand && spoken.isNotBlank() -> {
                        awaitingCommand = false
                        showWakeNotification(spoken)
                        speak("I heard you. Tap my notification to run that command.")
                    }
                    WakeWordParser.containsWakePhrase(spoken) -> {
                        val command = WakeWordParser.commandAfterWake(spoken)
                        if (command.isNotBlank()) {
                            showWakeNotification(command)
                            speak("Got it. Tap my notification to run that.")
                        } else {
                            awaitingCommand = true
                            speak("Yes? I’m listening.")
                        }
                    }
                }
                listenAgain(350L)
            }
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
    }

    private fun listenAgain(delay: Long) {
        handler.removeCallbacksAndMessages(null)
        if (stopped || recognizer == null) return
        handler.postDelayed({
            if (stopped || listening) return@postDelayed
            try {
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toLanguageTag())
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
                    putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
                }
                listening = true
                recognizer?.startListening(intent)
            } catch (e: Exception) {
                listening = false
                Log.w(TAG, "Could not start recognition; will retry", e)
                listenAgain(2_000L)
            }
        }, delay)
    }

    private fun speak(message: String) {
        if (ttsReady) tts?.speak(message, TextToSpeech.QUEUE_FLUSH, null, "maya-wake")
    }

    private fun showWakeNotification(command: String) {
        val open = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_WAKE_COMMAND, command)
        }
        val openPending = PendingIntent.getActivity(this, 10, open,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentTitle("Hey Maya heard you")
            .setContentText(if (command.isBlank()) "Tap to open Offline Voice Agent" else "Detected command: $command")
            .setContentIntent(openPending)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        getSystemService(NotificationManager::class.java).notify(WAKE_NOTIFICATION_ID, notification)
        updateNotification("Listening for “Hey Maya”")
    }

    private fun buildNotification(message: String): Notification {
        val stopIntent = Intent(this, BackgroundWakeService::class.java).setAction(ACTION_STOP)
        val stopPending = PendingIntent.getService(this, 11, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentTitle("Offline Voice Agent · Wake mode")
            .setContentText(message)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .addAction(0, "Stop listening", stopPending)
            .build()
    }

    private fun updateNotification(message: String) {
        getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, buildNotification(message))
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            getSystemService(NotificationManager::class.java).createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Background wake mode", NotificationManager.IMPORTANCE_LOW)
            )
        }
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        // Keep running only because the user explicitly started the foreground service.
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        stopped = true
        handler.removeCallbacksAndMessages(null)
        try { recognizer?.cancel() } catch (_: Exception) {}
        try { recognizer?.destroy() } catch (_: Exception) {}
        recognizer = null
        listening = false
        tts?.stop()
        tts?.shutdown()
        tts = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val ACTION_STOP = "com.themalogroup.offlinevoiceagent.STOP_BACKGROUND_WAKE"
        const val EXTRA_WAKE_COMMAND = "background_wake_command"
        private const val TAG = "BackgroundWakeService"
        private const val CHANNEL_ID = "offline_voice_background_wake"
        private const val NOTIFICATION_ID = 1001
        private const val WAKE_NOTIFICATION_ID = 1002
    }
}