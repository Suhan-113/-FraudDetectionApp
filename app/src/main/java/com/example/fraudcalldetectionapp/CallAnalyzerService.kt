package com.example.fraudcalldetectionapp

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.media.MediaRecorder
import android.os.Build
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.telephony.PhoneStateListener
import android.telephony.TelephonyManager
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*
import java.io.File
import java.io.IOException
import java.util.*

class CallAnalyzerService : AccessibilityService() {

    private var mediaRecorder: MediaRecorder? = null
    private val telephonyManager: TelephonyManager? by lazy {
        getSystemService(TELEPHONY_SERVICE) as TelephonyManager?
    }
    private val speechRecognizer: SpeechRecognizer by lazy {
        SpeechRecognizer.createSpeechRecognizer(this)
    }

    // Coroutine scope for running tasks
    private val coroutineScope = CoroutineScope(Dispatchers.IO + Job())

    private val phoneStateListener = object : PhoneStateListener() {
        @Deprecated("Deprecated in API 31")
        override fun onCallStateChanged(state: Int, phoneNumber: String?) {
            super.onCallStateChanged(state, phoneNumber)
            when (state) {
                TelephonyManager.CALL_STATE_OFFHOOK -> {
                    Log.d("CallAnalyzerService", "Call started. Recording audio.")
                    startRecording()
                }
                TelephonyManager.CALL_STATE_IDLE -> {
                    Log.d("CallAnalyzerService", "Call ended. Stopping recording.")
                    stopRecording()
                }
            }
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d("CallAnalyzerService", "Service connected.")

        val info = AccessibilityServiceInfo()
        info.eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
        info.flags = AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS or AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
        serviceInfo = info

        telephonyManager?.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}

    override fun onInterrupt() {
        Log.d("CallAnalyzerService", "Service interrupted.")
        stopRecording()
    }

    override fun onUnbind(intent: Intent?): Boolean {
        telephonyManager?.listen(phoneStateListener, PhoneStateListener.LISTEN_NONE)
        stopRecording()
        coroutineScope.cancel()
        return super.onUnbind(intent)
    }

    private fun startRecording() {
        val outputFile = File(externalCacheDir, "call_recording.mp4")
        mediaRecorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.VOICE_COMMUNICATION)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setOutputFile(outputFile.absolutePath)
            try {
                prepare()
                start()
            } catch (e: IOException) {
                Log.e("CallAnalyzerService", "prepare() failed: ${e.message}")
            }
        }
    }

    private fun stopRecording() {
        if (mediaRecorder != null) {
            try {
                mediaRecorder?.stop()
                mediaRecorder?.release()
                mediaRecorder = null
                val recordingFile = File(externalCacheDir, "call_recording.mp4")
                Log.d("CallAnalyzerService", "Recording stopped and saved to: ${recordingFile.absolutePath}")

                // Start analyzing the recorded file
                analyzeRecording(recordingFile)

            } catch (e: Exception) {
                Log.e("CallAnalyzerService", "stop() failed: ${e.message}")
            }
        }
    }

    private fun analyzeRecording(file: File) {
        if (!file.exists()) {
            Log.e("CallAnalyzerService", "File does not exist: ${file.absolutePath}")
            return
        }

        // The built-in SpeechRecognizer is not designed for file input.
        // It's designed for live, real-time audio input from the microphone.
        // Therefore, we will simulate the transcription for demonstration.
        // In a real-world app, you would upload the 'file' to a cloud-based STT service.

        Log.d("CallAnalyzerService", "File analysis started. Simulating transcription.")

        // This is a placeholder for the actual transcription.
        val transcribedText = "Hello, this is your bank. We need your account details and password to unblock your account."

        // Now, run the fraud check on the simulated text
        checkForFraud(transcribedText)
    }

    private fun checkForFraud(text: String) {
        val scamKeywords = listOf("password", "OTP", "account details", "credit card", "bank", "pin number", "Aadhaar number")
        val isFraudulent = scamKeywords.any { text.contains(it, ignoreCase = true) }

        if (isFraudulent) {
            showFraudNotification()
        }
    }

    private fun showFraudNotification() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "fraud_channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Fraud Alerts",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Potential Fraud Call Detected")
            .setContentText("This call contains suspicious keywords. Be careful!")
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Replace with your app's icon
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        notificationManager.notify(1, notification)
    }
}