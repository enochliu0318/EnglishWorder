package com.englishworder.ui.wordlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.englishworder.data.repository.WordRepository
import com.englishworder.domain.model.Word
import com.englishworder.domain.model.WordList
import com.englishworder.domain.model.WordWithReview
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WordListViewModel @Inject constructor(
    private val repository: WordRepository
) : ViewModel() {

    val wordLists: StateFlow<List<WordList>> = repository.observeWordLists()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    fun createWordList(name: String, description: String) {
        if (name.isBlank()) {
            _message.value = "词库名称不能为空"
            return
        }
        viewModelScope.launch {
            repository.createWordList(name, description)
            _message.value = "词库已创建"
        }
    }

    fun deleteWordList(id: Long) {
        viewModelScope.launch {
            repository.deleteWordList(id)
            _message.value = "词库已删除"
        }
    }

    fun renameWordList(id: Long, name: String, description: String) {
        if (name.isBlank()) {
            _message.value = "词库名称不能为空"
            return
        }
        viewModelScope.launch {
            repository.updateWordList(id, name, description)
            _message.value = "已重命名"
        }
    }

    fun clearMessage() {
        _message.value = null
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class WordDetailViewModel @Inject constructor(
    private val repository: WordRepository
) : ViewModel() {

    private val _listId = MutableStateFlow(0L)

    val words: StateFlow<List<WordWithReview>> = _listId
        .flatMapLatest { id ->
            if (id > 0) repository.observeWordsWithReview(id) else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _wordList = MutableStateFlow<WordList?>(null)
    val wordList: StateFlow<WordList?> = _wordList.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    fun load(listId: Long) {
        _listId.value = listId
        viewModelScope.launch {
            _wordList.value = repository.getWordList(listId)
        }
    }

    fun addWord(text: String) {
        val id = _listId.value
        if (text.isBlank()) {
            _message.value = "单词不能为空"
            return
        }
        viewModelScope.launch {
            val result = repository.addWord(id, text)
            _message.value = if (result != null) "已添加，正在补全释义..." else "单词已存在"
        }
    }

    fun deleteWord(wordId: Long) {
        viewModelScope.launch {
            repository.deleteWord(wordId)
        }
    }

    fun toggleMastered(wordId: Long, mastered: Boolean) {
        viewModelScope.launch {
            repository.setWordMastered(wordId, mastered)
            _message.value = if (mastered) "已标注为掌握" else "已取消掌握标注"
        }
    }

    fun refreshAllMeanings() {
        viewModelScope.launch {
            val list = words.value
            if (list.isEmpty()) {
                _message.value = "词库为空"
                return@launch
            }
            var ok = 0
            list.forEach { item ->
                if (repository.fetchWordInfo(item.word.id)) ok++
            }
            _message.value = "已更新 $ok/${list.size} 个释义"
        }
    }

    fun clearMessage() {
        _message.value = null
    }
}
