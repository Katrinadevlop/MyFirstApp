package ru.netology.nmedia.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import ru.netology.nmedia.db.DraftDao
import ru.netology.nmedia.db.DraftEntity
import javax.inject.Inject

@HiltViewModel
class DraftViewModel @Inject constructor(
    private val dao: DraftDao,
) : ViewModel() {

    val draft: Flow<String> = dao.get().map { it?.content.orEmpty() }

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
