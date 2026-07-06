package com.englishworder.ui.wordlist

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.englishworder.data.parser.WordFileParser
import com.englishworder.data.repository.WordRepository
import com.englishworder.domain.model.ImportResult
import com.englishworder.domain.model.WordList
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class ImportTarget {
    NEW_GROUP,
    MERGE_EXISTING
}

data class ImportUiState(
    val isLoading: Boolean = false,
    val result: ImportResult? = null,
    val error: String? = null,
    val fetchProgress: String? = null,
    val target: ImportTarget = ImportTarget.NEW_GROUP,
    val newGroupName: String = "",
    val selectedListId: Long = 0
)

@HiltViewModel
class ImportViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: WordRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ImportUiState())
    val state: StateFlow<ImportUiState> = _state.asStateFlow()

    val wordLists: StateFlow<List<WordList>> = repository.observeWordLists()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setTarget(target: ImportTarget) {
        _state.value = _state.value.copy(target = target)
    }

    fun setNewGroupName(name: String) {
        _state.value = _state.value.copy(newGroupName = name)
    }

    fun setSelectedListId(id: Long) {
        _state.value = _state.value.copy(selectedListId = id)
    }

    fun importFile(defaultListId: Long, uri: Uri, fileName: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            try {
                val targetListId = resolveTargetListId(defaultListId)
                if (targetListId == null) {
                    _state.value = _state.value.copy(isLoading = false, error = "请选择或输入分组名称")
                    return@launch
                }

                val entries = context.contentResolver.openInputStream(uri)?.use { stream ->
                    when {
                        fileName.endsWith(".xlsx", ignoreCase = true) ||
                            fileName.endsWith(".xls", ignoreCase = true) -> WordFileParser.parseExcel(stream)
                        else -> WordFileParser.parseCsv(stream)
                    }
                } ?: emptyList()

                if (entries.isEmpty()) {
                    _state.value = ImportUiState(error = "文件中没有找到单词")
                    return@launch
                }

                val result = repository.importWords(targetListId, entries)
                _state.value = ImportUiState(
                    result = result,
                    fetchProgress = "正在补全 ${result.pendingFetch} 个单词的中文释义..."
                )

                if (result.pendingFetch > 0) {
                    WorkManager.getInstance(context).enqueueUniqueWork(
                        com.englishworder.data.worker.ReviewReminderWorker.FETCH_WORK_NAME,
                        ExistingWorkPolicy.KEEP,
                        OneTimeWorkRequestBuilder<com.englishworder.data.worker.FetchWordsWorker>().build()
                    )
                }
            } catch (e: Exception) {
                _state.value = ImportUiState(error = "导入失败: ${e.message}")
            }
        }
    }

    private suspend fun resolveTargetListId(defaultListId: Long): Long? {
        val s = _state.value
        return when {
            s.target == ImportTarget.NEW_GROUP -> {
                val name = s.newGroupName.trim().ifBlank { "导入词库 ${System.currentTimeMillis() % 10000}" }
                repository.createWordList(name)
            }
            s.selectedListId > 0 -> s.selectedListId
            defaultListId > 0 -> defaultListId
            else -> null
        }
    }

    fun retryFetch() {
        viewModelScope.launch {
            _state.value = _state.value.copy(fetchProgress = "正在补全中文释义...")
            WorkManager.getInstance(context).enqueueUniqueWork(
                com.englishworder.data.worker.ReviewReminderWorker.FETCH_WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                OneTimeWorkRequestBuilder<com.englishworder.data.worker.FetchWordsWorker>().build()
            )
            _state.value = _state.value.copy(fetchProgress = "补全任务已提交")
        }
    }

    fun clearState() {
        _state.value = ImportUiState()
    }
}
