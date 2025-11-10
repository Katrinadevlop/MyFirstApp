package ru.netology.nmedia.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import ru.netology.nmedia.db.AppDb
import ru.netology.nmedia.db.DraftEntity

class DraftViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = AppDb.get(application).draftDao()

    val draft: LiveData<String> = dao.get().map { it?.content.orEmpty() }

    private var debounceJob: Job? = null

    fun onContentChanged(text: String) {
        debounceJob?.cancel()
        debounceJob = viewModelScope.launch {
            delay(300)
            dao.save(DraftEntity(content = text))
        }
    }

    fun saveNow(text: String) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.save(DraftEntity(content = text))
        }
    }

    fun clear() {
        viewModelScope.launch(Dispatchers.IO) { dao.clear() }
    }
}
