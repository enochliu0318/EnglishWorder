package com.englishworder.ui.wordlist

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.englishworder.domain.model.FetchStatus
import com.englishworder.domain.model.ReviewStatus
import com.englishworder.domain.model.WordWithReview
import com.englishworder.ui.components.AppCard
import com.englishworder.ui.components.rememberWordSpeaker
import com.englishworder.ui.theme.AppColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WordDetailScreen(
    listId: Long,
    onBack: () -> Unit,
    onImport: () -> Unit,
    viewModel: WordDetailViewModel = hiltViewModel()
) {
    val words by viewModel.words.collectAsState()
    val wordList by viewModel.wordList.collectAsState()
    val message by viewModel.message.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var newWord by remember { mutableStateOf("") }
    val snackbarHostState = remember { SnackbarHostState() }
    val speaker = rememberWordSpeaker()

    LaunchedEffect(listId) { viewModel.load(listId) }
    LaunchedEffect(message) {
        message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(wordList?.name ?: "词库详情") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refreshAllMeanings() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "刷新释义")
                    }
                    IconButton(onClick = onImport) {
                        Icon(Icons.Default.Upload, contentDescription = "导入")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "添加单词")
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(words, key = { it.word.id }) { item ->
                WordItem(
                    item = item,
                    onSpeak = { speaker.speak(item.word.text, item.word.audioUrl) },
                    onToggleMastered = { mastered ->
                        viewModel.toggleMastered(item.word.id, mastered)
                    },
                    onDelete = { viewModel.deleteWord(item.word.id) }
                )
            }
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("添加单词") },
            text = {
                OutlinedTextField(
                    value = newWord,
                    onValueChange = { newWord = it },
                    label = { Text("单词") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.addWord(newWord)
                    newWord = ""
                    showAddDialog = false
                }) { Text("添加") }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) { Text("取消") }
            }
        )
    }
}

@Composable
private fun WordItem(
    item: WordWithReview,
    onSpeak: () -> Unit,
    onToggleMastered: (Boolean) -> Unit,
    onDelete: () -> Unit
) {
    val word = item.word
    val isMastered = item.review.status == ReviewStatus.MASTERED

    AppCard {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(word.text, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        if (isMastered) {
                            Text(
                                "已掌握",
                                style = MaterialTheme.typography.labelSmall,
                                color = AppColors.heroGreen,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    if (word.phonetic.isNotBlank()) {
                        Text(word.phonetic, style = MaterialTheme.typography.bodySmall)
                    }
                    if (word.meaning.isNotBlank()) {
                        Text(word.meaning, style = MaterialTheme.typography.bodyMedium)
                    } else {
                        Text(
                            when (word.fetchStatus) {
                                FetchStatus.PENDING -> "正在补全..."
                                FetchStatus.FAILED -> "补全失败"
                                FetchStatus.OK -> ""
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
                IconButton(onClick = onSpeak) {
                    Icon(Icons.Default.VolumeUp, contentDescription = "发音", tint = AppColors.heroGreen)
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "删除")
                }
            }
            FilterChip(
                selected = isMastered,
                onClick = { onToggleMastered(!isMastered) },
                label = { Text(if (isMastered) "取消掌握" else "标注已掌握") },
                leadingIcon = {
                    Icon(
                        if (isMastered) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                        contentDescription = null,
                        modifier = Modifier.padding(2.dp)
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = AppColors.heroGreen.copy(alpha = 0.15f),
                    selectedLabelColor = AppColors.heroGreen
                )
            )
        }
    }
}
