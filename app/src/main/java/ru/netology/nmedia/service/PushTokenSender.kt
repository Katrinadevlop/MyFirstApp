package ru.netology.nmedia.service

import android.util.Log
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import ru.netology.nmedia.api.PostApiService
import ru.netology.nmedia.dto.PushToken
import ru.netology.nmedia.util.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PushTokenSender @Inject constructor(
    private val apiService: PostApiService,
) {
    fun send(token: String? = null) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val pushToken = token ?: FirebaseMessaging.getInstance().token.await()
                apiService.savePushToken(PushToken(pushToken))
                Log.d(TAG, "Push token sent successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Error sending push token", e)
            }
        }
    }

    companion object {
        private const val TAG = "PushTokenSender"
    }
}
