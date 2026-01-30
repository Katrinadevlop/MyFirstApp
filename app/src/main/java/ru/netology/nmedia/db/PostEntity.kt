package ru.netology.nmedia.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import ru.netology.nmedia.dto.Attachment
import ru.netology.nmedia.dto.Post

@Entity(tableName = "posts")
data class PostEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val author: String,
    val authorAvatar: String? = null,
    val content: String,
    val published: String,
    val likes: Int,
    val likedByMe: Boolean,
    val shares: Int,
    val views: Int,
    val video: String?,
    val attachment: Attachment? = null,
    // Поля для задания №2: отслеживание статуса синхронизации
    val isSynced: Boolean = true,  // true - сохранен на сервере, false - только локально
    val localId: Long? = null,     // Временный локальный ID для несинхронизированных постов
) {
    fun toDto() = Post(
        id = id,
        author = author,
        authorAvatar = authorAvatar,
        content = content,
        published = published,
        likes = likes,
        likedByMe = likedByMe,
        shares = shares,
        views = views,
        video = video,
        attachment = attachment,
        isSynced = isSynced,
        localId = localId,
    )

    companion object {
        fun fromDto(post: Post) = PostEntity(
            id = post.id,
            author = post.author,
            authorAvatar = post.authorAvatar,
            content = post.content,
            published = post.published,
            likes = post.likes,
            likedByMe = post.likedByMe,
            shares = post.shares,
            views = post.views,
            video = post.video,
            attachment = post.attachment,
            isSynced = post.isSynced,
            localId = post.localId,
        )
    }
}
