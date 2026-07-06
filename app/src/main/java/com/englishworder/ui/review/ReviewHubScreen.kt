package com.englishworder.ui.review

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.englishworder.domain.model.ReviewMode
import com.englishworder.domain.model.WordList
import com.englishworder.ui.components.AppCard
import com.englishworder.ui.components.GreenGradientBackground
import com.englishworder.ui.components.ScreenPadding
import com.englishworder.ui.theme.AppColors
import com.englishworder.ui.wordlist.WordListViewModel

@Composable
fun ReviewHubScreen(
    onSetupGame: (String) -> Unit,
    viewModel: ReviewViewModel = hiltViewModel()
) {
    val dueCount by viewModel.dueCount.collectAsState()

    GreenGradientBackground {
        ScreenPadding(Modifier.fillMaxSize()) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                AppCard {
                    Column(Modifier.padding(20.dp)) {
                        Text("游戏复习", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = AppColors.heroGreen)
                        Text("$dueCount 个单词待复习", style = MaterialTheme.typography.headlineMedium, color = AppColors.heroGreen)
                        Text("计划复习按艾宾浩斯曲线；自由练习可重复刷词", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp))
                    }
                }
                Text("选择游戏", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = AppColors.heroGreen)
                GameModeCard(title = "选择题", description = "看单词选中文释义", onClick = { onSetupGame("quiz") })
                GameModeCard(title = "连连看", description = "配对单词与中文释义", onClick = { onSetupGame("link") })
                GameModeCard(title = "拼写", description = "看中文释义拼单词", onClick = { onSetupGame("spelling") })
                GameModeCard(title = "听力练习", description = "听发音辨单词，不显示题目", onClick = { onSetupGame("listening") })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameSetupScreen(
    gameType: String,
    initialMode: ReviewMode = ReviewMode.FREE_PRACTICE,
    onStart: (listId: Long, mode: ReviewMode) -> Unit,
    onBack: () -> Unit,
    viewModel: WordListViewModel = hiltViewModel()
) {
    val wordLists by viewModel.wordLists.collectAsState()
    var selectedListId by remember { mutableLongStateOf(0L) }
    var reviewMode by remember { mutableStateOf(initialMode) }

    LaunchedEffect(wordLists) {
        if (selectedListId == 0L && wordLists.isNotEmpty()) selectedListId = wordLists.first().id
    }

    val gameTitle = when (gameType) {
        "quiz" -> "选择题"; "link" -> "连连看"; "spelling" -> "拼写"; "listening" -> "听力练习"; else -> "游戏"
    }

    GreenGradientBackground {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("开始$gameTitle") },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                        }
                    }
                )
            }
        ) { padding ->
            ScreenPadding(Modifier.padding(padding)) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("选择词库", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = AppColors.heroGreen)
                    if (wordLists.isEmpty()) {
                        Text("请先创建词库并添加单词")
                    } else {
                        wordLists.forEach { list ->
                            ListChip(list = list, selected = selectedListId == list.id) { selectedListId = list.id }
                        }
                    }
                    Text("练习模式", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = AppColors.heroGreen)
                    FilterChip(selected = reviewMode == ReviewMode.SCHEDULED, onClick = { reviewMode = ReviewMode.SCHEDULED },
                        label = { Text("计划复习（仅今日待复习）") })
                    FilterChip(selected = reviewMode == ReviewMode.FREE_PRACTICE, onClick = { reviewMode = ReviewMode.FREE_PRACTICE },
                        label = { Text("自由练习（可重复刷词）") }, modifier = Modifier.padding(top = 8.dp))
                    Button(
                        onClick = { if (selectedListId > 0) onStart(selectedListId, reviewMode) },
                        enabled = selectedListId > 0 && wordLists.isNotEmpty(),
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = AppColors.heroGreen)
                    ) { Text("开始游戏") }
                }
            }
        }
    }
}

@Composable
private fun GameModeCard(title: String, description: String, onClick: () -> Unit) {
    AppCard(onClick = onClick) {
        Column(Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = AppColors.heroGreen)
            Text(description, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun ListChip(list: WordList, selected: Boolean, onClick: () -> Unit) {
    AppCard(onClick = onClick) {
        Column(Modifier.padding(12.dp)) {
            Text(list.name, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                color = if (selected) AppColors.heroGreen else MaterialTheme.colorScheme.onSurface)
            Text("${list.wordCount} 个单词", style = MaterialTheme.typography.bodySmall)
        }
    }
}
