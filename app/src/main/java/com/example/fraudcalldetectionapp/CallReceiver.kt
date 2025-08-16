package com.example.fraudcalldetectionapp

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager
import android.util.Log

class CallReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == TelephonyManager.ACTION_PHONE_STATE_CHANGED) {
            val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE)
            if (state == TelephonyManager.EXTRA_STATE_RINGING) {
                // You can add logic here for ringing calls if needed
                Log.d("CallReceiver", "Phone is ringing.")
            } else if (state == TelephonyManager.EXTRA_STATE_OFFHOOK) {
                // Phone call is active, so start the service if it's not already running
                Log.d("CallReceiver", "Call started. Starting CallAnalyzerService.")
                val serviceIntent = Intent(context, CallAnalyzerService::class.java)
                context?.startService(serviceIntent)
            } else if (state == TelephonyManager.EXTRA_STATE_IDLE) {
                // Call ended
                Log.d("CallReceiver", "Call ended.")
            }
        }
    }
}