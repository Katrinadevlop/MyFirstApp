package ru.netology.nmedia.db

import androidx.room.TypeConverter
import com.google.gson.Gson
import ru.netology.nmedia.dto.Attachment

class Converters {
    private val gson = Gson()

    @TypeConverter
    fun fromAttachment(attachment: Attachment?): String? {
        return attachment?.let { gson.toJson(it) }
    }

    @TypeConverter
    fun toAttachment(json: String?): Attachment? {
        return json?.let { gson.fromJson(it, Attachment::class.java) }
    }
}
