package ru.netology.nmedia.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "draft")
data class DraftEntity(
    @PrimaryKey val id: Int = 0,
    val content: String
)
