package ru.netology.nmedia.dto

data class Post(
    val id: Long,
    val authorId: Long,
    val content: String,
    val published: Long,
    val likedByMe: Boolean,
    val likes: Int = 0,
    val attachment: Attachment? = null,
)

data class Attachment(
    val url: String,
    val description: String,
    val type: String,
)

data class Comment(
    val id: Long,
    val postId: Long,
    val authorId: Long,
    val content: String,
    val published: Long,
    val likedByMe: Boolean,
    val likes: Int = 0,
)

data class Author(
    val id: Long,
    val name: String,
    val avatar: String,
)

data class PostWithAuthor(
    val post: Post,
    val author: Author,
    val comments: List<CommentWithAuthor> = emptyList(),
)

data class CommentWithAuthor(
    val comment: Comment,
    val author: Author,
)
