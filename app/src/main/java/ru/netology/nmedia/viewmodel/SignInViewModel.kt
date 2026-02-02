package ru.netology.nmedia.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ru.netology.nmedia.api.RetrofitClient
import ru.netology.nmedia.auth.AppAuth

data class SignInState(
    val loading: Boolean = false,
    val error: Boolean = false,
    val success: Boolean = false,
    val errorMessage: String? = null,
)

class SignInViewModel : ViewModel() {
    private val _state = MutableStateFlow(SignInState())
    val state: StateFlow<SignInState> = _state.asStateFlow()

    fun signIn(login: String, password: String) {
        if (login.isBlank() || password.isBlank()) {
            _state.value = SignInState(error = true, errorMessage = "Заполните все поля")
            return
        }

        viewModelScope.launch {
            _state.value = SignInState(loading = true)
            try {
                val response = RetrofitClient.postApiService.authenticate(login, password)
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null && body.id != 0L && body.token != null) {
                        AppAuth.setAuth(body.id, body.token)
                        _state.value = SignInState(success = true)
                    } else {
                        _state.value = SignInState(error = true, errorMessage = "Неверный логин или пароль")
                    }
                } else {
                    _state.value = SignInState(error = true, errorMessage = "Ошибка: ${response.code()}")
                }
            } catch (e: Exception) {
                _state.value = SignInState(error = true, errorMessage = "Ошибка сети: ${e.message}")
            }
        }
    }

    fun resetState() {
        _state.value = SignInState()
    }
}
