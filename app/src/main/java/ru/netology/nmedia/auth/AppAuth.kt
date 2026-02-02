package ru.netology.nmedia.auth

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import ru.netology.nmedia.dto.AuthState

object AppAuth {
    private const val PREFS_NAME = "auth"
    private const val KEY_ID = "id"
    private const val KEY_TOKEN = "token"

    private val _authState = MutableStateFlow(AuthState())
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private lateinit var prefs: android.content.SharedPreferences

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val id = prefs.getLong(KEY_ID, 0L)
        val token = prefs.getString(KEY_TOKEN, null)
        if (id != 0L && token != null) {
            _authState.value = AuthState(id, token)
        }
    }

    fun setAuth(id: Long, token: String) {
        _authState.value = AuthState(id, token)
        with(prefs.edit()) {
            putLong(KEY_ID, id)
            putString(KEY_TOKEN, token)
            apply()
        }
    }

    fun removeAuth() {
        _authState.value = AuthState()
        with(prefs.edit()) {
            clear()
            apply()
        }
    }

    fun isAuthenticated(): Boolean = _authState.value.token != null
}
