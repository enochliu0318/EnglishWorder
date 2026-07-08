package com.englishworder.ui.review

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.englishworder.domain.model.WordList
import com.englishworder.ui.components.AppCard
import com.englishworder.ui.components.GreenGradientBackground
import com.englishworder.ui.theme.AppColors
import com.englishworder.ui.wordlist.WordListViewModel

fun gameTypeTitle(gameType: String): String = when (gameType) {
    "quiz" -> "选择题"
    "link" -> "连连看"
    "spelling" -> "拼写"
    "listening" -> "听力练习"
    else -> "游戏"
}

@Composable
fun ReviewHubScreen(
    onSetupGame: (String) -> Unit,
    viewModel: ReviewViewModel = hiltViewModel()
) {
    val dueCount by viewModel.dueCount.collectAsState()

    GreenGradientBackground {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                AppCard {
                    Column(Modifier.padding(20.dp)) {
                        Text(
                            "游戏复习",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = AppColors.heroGreen
                        )
                        Text(
                            "$dueCount 个单词待复习",
                            style = MaterialTheme.typography.headlineMedium,
                            color = AppColors.heroGreen
                        )
                        Text(
                            "游戏练习同样计入艾宾浩斯复习计划",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
            item {
                Text(
                    "选择游戏",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.heroGreen
                )
            }
            item {
                GameModeCard(title = "选择题", description = "看单词选中文释义", onClick = { onSetupGame("quiz") })
            }
            item {
                GameModeCard(title = "连连看", description = "配对单词与中文释义", onClick = { onSetupGame("link") })
            }
            item {
                GameModeCard(title = "拼写", description = "看中文释义拼单词", onClick = { onSetupGame("spelling") })
            }
            item {
                GameModeCard(title = "听力练习", description = "听发音选中文释义", onClick = { onSetupGame("listening") })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameListSelectScreen(
    gameType: String,
    onStart: (Long) -> Unit,
    onBack: () -> Unit,
    viewModel: WordListViewModel = hiltViewModel()
) {
    val wordLists by viewModel.wordLists.collectAsState()
    val gameTitle = gameTypeTitle(gameType)

    GreenGradientBackground {
        Scaffold(
            containerColor = androidx.compose.ui.graphics.Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { Text("$gameTitle · 选择词库") },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                        }
                    }
                )
            }
        ) { padding ->
            if (wordLists.isEmpty()) {
                Column(
                    Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(16.dp)
                ) {
                    Text("请先创建词库并添加单词")
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        Text(
                            "选择一个词库开始",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = AppColors.heroGreen,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }
                    items(wordLists, key = { it.id }) { list ->
                        ListPickCard(list = list, onClick = { onStart(list.id) })
                    }
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
private fun ListPickCard(list: WordList, onClick: () -> Unit) {
    AppCard(onClick = onClick) {
        Column(Modifier.padding(16.dp)) {
            Text(list.name, fontWeight = FontWeight.Bold, color = AppColors.heroGreen)
            if (list.description.isNotBlank()) {
                Text(list.description, style = MaterialTheme.typography.bodyMedium)
            }
            Text("${list.wordCount} 个单词", style = MaterialTheme.typography.bodySmall, color = AppColors.textMuted)
        }
    }
}
