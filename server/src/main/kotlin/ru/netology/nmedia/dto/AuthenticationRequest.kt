package ru.netology.nmedia.dto

data class AuthenticationRequest(
    val login: String,
    val pass: String,
)
