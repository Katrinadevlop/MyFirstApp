package ru.netology.nmedia.service

import android.app.PendingIntent
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.google.gson.Gson
import ru.netology.nmedia.App
import ru.netology.nmedia.R
import ru.netology.nmedia.activity.MainActivity
import kotlin.random.Random

class FirebaseService : FirebaseMessagingService() {
    private val gson = Gson()

    override fun onNewToken(token: String) {
        // TODO: send token to your backend if needed
        android.util.Log.d("FCM", "New token: $token")
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val actionRaw = message.data["action"] ?: return
        when (actionRaw.toActionOrNull()) {
            Action.NEW_POST -> {
                val content = message.data["content"] ?: return
                val payload = gson.fromJson(content, NewPostContent::class.java)
                showNewPost(payload)
            }
            null -> {
                // Unknown action: ignore safely (solution for Exceptions task)
                android.util.Log.w("FCM", "Unknown action: $actionRaw")
                return
            }
        }
    }

    private fun showNewPost(payload: NewPostContent) {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, App.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("${payload.userName} опубликовал новый пост:")
            .setStyle(NotificationCompat.BigTextStyle().bigText(payload.postText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(this)
            .notify(Random.nextInt(), notification)
    }
}

// --- Models & helpers ---

enum class Action {
    NEW_POST
}

data class NewPostContent(
    val userName: String,
    val postText: String,
)

private fun String.toActionOrNull(): Action? = Action.values().firstOrNull { it.name == this }
