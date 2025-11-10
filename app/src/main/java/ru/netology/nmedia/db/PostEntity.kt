package ru.netology.nmedia.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import ru.netology.nmedia.dto.Post

@Entity(tableName = "posts")
data class PostEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val author: String,
    val content: String,
    val published: String,
    val likes: Int,
    val likedByMe: Boolean,
    val shares: Int,
    val views: Int,
    val video: String?
) {
    fun toDto() = Post(
        id = id,
        author = author,
        content = content,
        published = published,
        likes = likes,
        likedByMe = likedByMe,
        shares = shares,
        views = views,
        video = video,
    )

    companion object {
        fun fromDto(post: Post) = PostEntity(
            id = post.id,
            author = post.author,
            content = post.content,
            published = post.published,
            likes = post.likes,
            likedByMe = post.likedByMe,
            shares = post.shares,
            views = post.views,
            video = post.video,
        )
    }
}
