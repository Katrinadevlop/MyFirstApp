package ru.netology.nmedia.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import ru.netology.nmedia.api.RetrofitClient
import ru.netology.nmedia.auth.AppAuth
import java.io.File

data class SignUpState(
    val loading: Boolean = false,
    val error: Boolean = false,
    val success: Boolean = false,
    val errorMessage: String? = null,
)

data class AvatarModel(
    val uri: Uri,
    val file: File,
)

class SignUpViewModel : ViewModel() {
    private val _state = MutableStateFlow(SignUpState())
    val state: StateFlow<SignUpState> = _state.asStateFlow()

    private val _avatar = MutableStateFlow<AvatarModel?>(null)
    val avatar: StateFlow<AvatarModel?> = _avatar.asStateFlow()

    fun setAvatar(uri: Uri, file: File) {
        _avatar.value = AvatarModel(uri, file)
    }

    fun clearAvatar() {
        _avatar.value = null
    }

    fun signUp(name: String, login: String, password: String, confirmPassword: String) {
        if (name.isBlank() || login.isBlank() || password.isBlank()) {
            _state.value = SignUpState(error = true, errorMessage = "Заполните все поля")
            return
        }

        if (password != confirmPassword) {
            _state.value = SignUpState(error = true, errorMessage = "Пароли не совпадают")
            return
        }

        viewModelScope.launch {
            _state.value = SignUpState(loading = true)
            try {
                val response = _avatar.value?.let { avatarModel ->
                    // Регистрация с фото
                    val loginPart = login.toRequestBody("text/plain".toMediaType())
                    val passPart = password.toRequestBody("text/plain".toMediaType())
                    val namePart = name.toRequestBody("text/plain".toMediaType())
                    val mediaPart = MultipartBody.Part.createFormData(
                        "file",
                        avatarModel.file.name,
                        avatarModel.file.asRequestBody()
                    )
                    RetrofitClient.postApiService.registerWithPhoto(loginPart, passPart, namePart, mediaPart)
                } ?: run {
                    // Регистрация без фото
                    RetrofitClient.postApiService.register(login, password, name)
                }

                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null && body.id != 0L && body.token != null) {
                        AppAuth.setAuth(body.id, body.token)
                        _state.value = SignUpState(success = true)
                    } else {
                        _state.value = SignUpState(error = true, errorMessage = "Ошибка регистрации")
                    }
                } else {
                    _state.value = SignUpState(error = true, errorMessage = "Ошибка: ${response.code()}")
                }
            } catch (e: Exception) {
                _state.value = SignUpState(error = true, errorMessage = "Ошибка сети: ${e.message}")
            }
        }
    }

    fun resetState() {
        _state.value = SignUpState()
    }
}
