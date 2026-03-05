package ru.netology.nmedia.dto

data class RegistrationRequest(
    val login: String,
    val pass: String,
    val name: String,
)
