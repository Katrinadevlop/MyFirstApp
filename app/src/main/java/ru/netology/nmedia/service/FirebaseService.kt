package ru.netology.nmedia.service

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.EntryPointAccessors
import ru.netology.nmedia.App
import ru.netology.nmedia.R
import ru.netology.nmedia.activity.MainActivity
import ru.netology.nmedia.auth.AppAuth
import ru.netology.nmedia.di.PushTokenEntryPoint
import kotlin.random.Random
import javax.inject.Inject

@AndroidEntryPoint
class FirebaseService : FirebaseMessagingService() {
    @Inject
    lateinit var pushTokenSender: PushTokenSender
    private val gson = Gson()

    override fun onNewToken(token: String) {
        android.util.Log.d("FCM", "New token: $token")
        sendPushToken(token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        // Проверка recipientId
        val recipientIdStr = message.data["recipientId"]
        val recipientId = recipientIdStr?.toLongOrNull()
        val myId = AppAuth.authState.value.id

        when {
            // recipientId = null - массовая рассылка, показываем Notification
            recipientId == null -> {
                handleMessage(message)
            }
            // recipientId = моему id - всё ok, показываем Notification
            recipientId == myId -> {
                handleMessage(message)
            }
            // recipientId = 0 (и не равен моему) - сервер считает, что у нас анонимная аутентификация
            recipientId == 0L -> {
                android.util.Log.d("FCM", "Server thinks we are anonymous, resending token")
                sendPushToken()
            }
            // recipientId != 0 (и не равен моему) - сервер считает, что у нас другая аутентификация
            else -> {
                android.util.Log.d("FCM", "Server thinks we are user $recipientId, but we are $myId, resending token")
                sendPushToken()
            }
        }
    }

    private fun handleMessage(message: RemoteMessage) {
        // Проверяем есть ли action (старый формат)
        val actionRaw = message.data["action"]
        if (actionRaw != null) {
            when (actionRaw.toActionOrNull()) {
                Action.NEW_POST -> {
                    val content = message.data["content"] ?: return
                    val payload = gson.fromJson(content, NewPostContent::class.java)
                    showNewPost(payload)
                }
                null -> {
                    android.util.Log.w("FCM", "Unknown action: $actionRaw")
                }
            }
        } else {
            // Новый формат с content напрямую
            val content = message.data["content"]
            if (content != null) {
                showSimpleNotification(content)
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

    private fun showSimpleNotification(content: String) {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, App.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("NMedia")
            .setContentText(content)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(this)
            .notify(Random.nextInt(), notification)
    }

    private fun sendPushToken(token: String? = null) {
        pushTokenSender.send(token)
    }

    companion object {
        fun sendPushTokenToServer(context: Context) {
            val entryPoint = EntryPointAccessors.fromApplication(context, PushTokenEntryPoint::class.java)
            entryPoint.pushTokenSender().send()
        }
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
