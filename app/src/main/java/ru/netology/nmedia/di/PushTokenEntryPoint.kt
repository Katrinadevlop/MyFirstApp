package ru.netology.nmedia.di

import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import ru.netology.nmedia.service.PushTokenSender

@EntryPoint
@InstallIn(SingletonComponent::class)
interface PushTokenEntryPoint {
    fun pushTokenSender(): PushTokenSender
}
