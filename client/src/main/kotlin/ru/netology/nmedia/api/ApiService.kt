package ru.netology.nmedia.api

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import okhttp3.OkHttpClient
import okhttp3.Request
import ru.netology.nmedia.dto.*
import java.io.IOException

class ApiService(
    private val baseUrl: String = "http://localhost:9999"
) {
    private val client = OkHttpClient()
    private val gson = Gson()

    fun getPosts(): List<Post> {
        val request = Request.Builder()
            .url("$baseUrl/api/posts")
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("Unexpected code $response")
            val body = response.body?.string() ?: throw IOException("Empty response body")
            val type = object : TypeToken<List<Post>>() {}.type
            return gson.fromJson(body, type)
        }
    }

    fun getCommentsByPostId(postId: Long): List<Comment> {
        val request = Request.Builder()
            .url("$baseUrl/api/posts/$postId/comments")
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("Unexpected code $response")
            val body = response.body?.string() ?: throw IOException("Empty response body")
            val type = object : TypeToken<List<Comment>>() {}.type
            return gson.fromJson(body, type)
        }
    }

    fun getAuthorById(id: Long): Author {
        val request = Request.Builder()
            .url("$baseUrl/api/authors/$id")
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("Unexpected code $response")
            val body = response.body?.string() ?: throw IOException("Empty response body")
            return gson.fromJson(body, Author::class.java)
        }
    }
}
