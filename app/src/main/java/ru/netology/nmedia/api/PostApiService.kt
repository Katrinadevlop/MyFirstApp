package ru.netology.nmedia.api

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.*
import ru.netology.nmedia.dto.AuthState
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.dto.PushToken

interface PostApiService {
    // Push токен
    @POST("api/users/push-tokens")
    suspend fun savePushToken(@Body token: PushToken): Response<Unit>
    @GET("api/posts")
    suspend fun getAll(): Response<List<Post>>

    // Аутентификация
    @FormUrlEncoded
    @POST("api/users/authentication")
    suspend fun authenticate(
        @Field("login") login: String,
        @Field("pass") pass: String,
    ): Response<AuthState>

    // Регистрация без фото
    @FormUrlEncoded
    @POST("api/users/registration")
    suspend fun register(
        @Field("login") login: String,
        @Field("pass") pass: String,
        @Field("name") name: String,
    ): Response<AuthState>

    // Регистрация с фото
    @Multipart
    @POST("api/users/registration")
    suspend fun registerWithPhoto(
        @Part("login") login: RequestBody,
        @Part("pass") pass: RequestBody,
        @Part("name") name: RequestBody,
        @Part media: MultipartBody.Part,
    ): Response<AuthState>

    @POST("api/posts/{id}/likes")
    suspend fun likeById(@Path("id") id: Long): Response<Post>

    @DELETE("api/posts/{id}/likes")
    suspend fun unlikeById(@Path("id") id: Long): Response<Post>

    @GET("api/posts/{id}")
    suspend fun getById(@Path("id") id: Long): Response<Post>

    @POST("api/posts")
    suspend fun save(@Body post: Post): Response<Post>

    @DELETE("api/posts/{id}")
    suspend fun removeById(@Path("id") id: Long): Response<Unit>

    @GET("api/posts/{id}/newer")
    suspend fun getNewer(@Path("id") id: Long): Response<List<Post>>
}
