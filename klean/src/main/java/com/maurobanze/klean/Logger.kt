package com.maurobanze.klean

import android.util.Log

class Logger(private var testModeActive: Boolean = false) {

    companion object {
        const val TAG = "Klean"
    }
    
    fun logVerbose(message: String) {
        if (!testModeActive) {
            Log.v(TAG, message)
        }
    }
}