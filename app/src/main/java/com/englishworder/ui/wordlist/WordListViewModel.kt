package com.englishworder.ui.wordlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.englishworder.data.repository.WordPackRepository
import com.englishworder.data.repository.WordRepository
import com.englishworder.domain.model.WordPack
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
    private val repository: WordRepository,
    private val packRepository: WordPackRepository
) : ViewModel() {

    val wordLists: StateFlow<List<WordList>> = repository.observeWordLists()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val wordPacks: List<WordPack> = packRepository.availablePacks()

    val installedPackIds: StateFlow<Set<String>> = packRepository.observeInstalledPackIds()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    private val _installingPackId = MutableStateFlow<String?>(null)
    val installingPackId: StateFlow<String?> = _installingPackId.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    fun installPack(packId: String) {
        if (_installingPackId.value != null) return
        viewModelScope.launch {
            _installingPackId.value = packId
            packRepository.installPack(packId)
                .onSuccess { _message.value = "词库已添加，可以开始学习了" }
                .onFailure { _message.value = it.message ?: "添加失败" }
            _installingPackId.value = null
        }
    }

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
                if (repository.fetchWordInfo(item.word.id, force = false)) ok++
            }
            _message.value = "已补全 $ok/${list.size} 个单词的空缺字段"
        }
    }

    fun updateWord(
        wordId: Long,
        meaning: String,
        example: String,
        partOfSpeech: String,
        phonetic: String
    ) {
        viewModelScope.launch {
            repository.updateWord(wordId, meaning, example, partOfSpeech, phonetic)
            _message.value = "已保存"
        }
    }

    fun clearMessage() {
        _message.value = null
    }
}
