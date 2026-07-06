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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.englishworder.domain.model.StudyMode
import com.englishworder.domain.model.WordList
import com.englishworder.ui.components.AppCard
import com.englishworder.ui.components.GreenGradientBackground
import com.englishworder.ui.components.ScreenPadding
import com.englishworder.ui.theme.AppColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WordListScreen(
    onOpenList: (Long) -> Unit,
    onImport: () -> Unit,
    onStartLearn: (Long, StudyMode) -> Unit,
    viewModel: WordListViewModel = hiltViewModel()
) {
    val wordLists by viewModel.wordLists.collectAsState()
    val message by viewModel.message.collectAsState()
    var showCreateDialog by remember { mutableStateOf(false) }
    var renameTarget by remember { mutableStateOf<WordList?>(null) }
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(message) {
        message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    GreenGradientBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { Text("词库") },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                    actions = {
                        IconButton(onClick = onImport) {
                            Icon(Icons.Default.Upload, contentDescription = "批量导入", tint = AppColors.heroGreen)
                        }
                    }
                )
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { showCreateDialog = true },
                    containerColor = AppColors.heroGreen
                ) {
                    Icon(Icons.Default.Add, contentDescription = "新建词库", tint = Color.White)
                }
            },
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { padding ->
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (wordLists.isEmpty()) {
                    item {
                        AppCard {
                            Column(Modifier.padding(20.dp)) {
                                Text("还没有词库")
                                Text("点击右下角 + 创建，或使用右上角导入", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                } else {
                    items(wordLists, key = { it.id }) { list ->
                        WordListItem(
                            wordList = list,
                            onOpenList = { onOpenList(list.id) },
                            onStartLearn = { mode -> onStartLearn(list.id, mode) },
                            onRename = {
                                renameTarget = list
                                name = list.name
                                description = list.description
                            },
                            onDelete = { viewModel.deleteWordList(list.id) }
                        )
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("新建词库") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("名称") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("描述") }, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.createWordList(name, description)
                    name = ""; description = ""; showCreateDialog = false
                }) { Text("创建") }
            },
            dismissButton = { TextButton(onClick = { showCreateDialog = false }) { Text("取消") } }
        )
    }

    renameTarget?.let { list ->
        AlertDialog(
            onDismissRequest = { renameTarget = null },
            title = { Text("重命名词库") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("名称") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("描述") }, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.renameWordList(list.id, name, description)
                    renameTarget = null
                }) { Text("保存") }
            },
            dismissButton = { TextButton(onClick = { renameTarget = null }) { Text("取消") } }
        )
    }
}

@Composable
private fun WordListItem(
    wordList: WordList,
    onOpenList: () -> Unit,
    onStartLearn: (StudyMode) -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    AppCard {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onOpenList),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(wordList.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = AppColors.heroGreen)
                    if (wordList.description.isNotBlank()) {
                        Text(wordList.description, style = MaterialTheme.typography.bodyMedium)
                    }
                    Text("${wordList.wordCount} 个单词 · 点击进入管理", style = MaterialTheme.typography.bodySmall, color = AppColors.textMuted)
                }
                Row {
                    IconButton(onClick = onRename) {
                        Icon(Icons.Default.Edit, contentDescription = "重命名", tint = AppColors.heroGreen)
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "删除")
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = { onStartLearn(StudyMode.NEW_WORDS) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = AppColors.heroGreen)
                ) { Text("学习") }
                OutlinedButton(
                    onClick = { onStartLearn(StudyMode.FREE_PRACTICE) },
                    modifier = Modifier.weight(1f)
                ) { Text("练习") }
            }
        }
    }
}
