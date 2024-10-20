package com.example.forestgardeningapp

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object FCMTokenManager {
    private const val TAG = "FCMTokenManager"

    fun sendRegistrationToServer(fcmToken: String) {
        FirebaseAuth.getInstance().currentUser?.getIdToken(true)?.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val idToken = task.result?.token
                if (idToken != null) {
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val response = RetrofitClient.tokenApiService.registerToken(TokenRequest(fcmToken))
                            if (response.isSuccessful) {
                                Log.d(TAG, "Token sent to server successfully")
                            } else {
                                Log.e(TAG, "Failed to send token to server: ${response.errorBody()?.string()}")
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error sending token to server", e)
                        }
                    }
                }
            } else {
                Log.e(TAG, "Error getting ID token", task.exception)
            }
        }
    }
}