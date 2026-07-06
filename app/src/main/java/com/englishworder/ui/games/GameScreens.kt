package com.englishworder.ui.games

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.englishworder.domain.model.ReviewMode
import com.englishworder.ui.components.AppCard
import com.englishworder.ui.components.ScreenPadding
import com.englishworder.ui.components.rememberWordSpeaker
import com.englishworder.ui.theme.AppColors

private val WrongRed = Color(0xFFD32F2F)
private val CorrectGreen = Color(0xFF2E7D32)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LinkMatchGameScreen(
    listId: Long,
    mode: ReviewMode,
    onBack: () -> Unit,
    viewModel: LinkMatchViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val speaker = rememberWordSpeaker()

    LaunchedEffect(listId, mode) { viewModel.loadGame(listId, mode) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("连连看") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { padding ->
        ScreenPadding(Modifier.padding(padding)) {
            when {
                state.isLoading -> LoadingView()
                state.finished -> FinishedView(
                    title = state.message ?: if (state.tiles.all { it.matched }) "全部配对成功！" else "游戏结束",
                    subtitle = if (state.tiles.isNotEmpty()) "得分: ${state.score} · 步数: ${state.moves}" else "",
                    onBack = onBack,
                    onRestart = if (state.tiles.isNotEmpty()) {{ viewModel.restart() }} else null
                )
                else -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("得分 ${state.score} · 步数 ${state.moves}", style = MaterialTheme.typography.bodyMedium)
                        Text("英文单词可点击发音", style = MaterialTheme.typography.bodySmall, color = AppColors.textMuted)
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        ) {
                            items(state.tiles.filter { !it.matched }, key = { it.id }) { tile ->
                                val selected = state.selectedId == tile.id
                                val audioUrl = state.audioByPair[tile.pairId].orEmpty()
                                AppCard(onClick = { viewModel.selectTile(tile.id) }) {
                                    Row(
                                        Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Column(
                                            Modifier.weight(1f),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text(
                                                if (tile.type == TileType.WORD) "单词" else "释义",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (tile.type == TileType.WORD) AppColors.heroGreen else AppColors.textMuted
                                            )
                                            Text(
                                                tile.text,
                                                textAlign = TextAlign.Center,
                                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                                color = if (tile.type == TileType.WORD) AppColors.heroGreen else AppColors.textPrimary,
                                                modifier = Modifier
                                                    .padding(top = 4.dp)
                                                    .then(
                                                        if (tile.type == TileType.WORD) {
                                                            Modifier.clickable {
                                                                speaker.speak(tile.text, audioUrl)
                                                            }
                                                        } else Modifier
                                                    )
                                            )
                                        }
                                        if (tile.type == TileType.WORD) {
                                            IconButton(
                                                onClick = { speaker.speak(tile.text, audioUrl) },
                                                modifier = Modifier.padding(0.dp)
                                            ) {
                                                Icon(
                                                    Icons.AutoMirrored.Filled.VolumeUp,
                                                    contentDescription = "发音",
                                                    tint = AppColors.heroGreen
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpellingGameScreen(
    listId: Long,
    mode: ReviewMode,
    onBack: () -> Unit,
    viewModel: SpellingViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val speaker = rememberWordSpeaker()

    LaunchedEffect(listId, mode) { viewModel.loadGame(listId, mode) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("拼写") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { padding ->
        ScreenPadding(Modifier.padding(padding)) {
            when {
                state.isLoading -> LoadingView()
                state.finished -> FinishedView(
                    title = if (state.words.isEmpty()) "暂无可用单词" else "拼写完成",
                    subtitle = if (state.words.isEmpty()) {
                        if (mode == ReviewMode.SCHEDULED) "暂无待复习单词，试试自由练习" else "词库单词不足"
                    } else {
                        "得分: ${state.score}/${state.words.size}"
                    },
                    onBack = onBack,
                    onRestart = if (state.words.isNotEmpty()) {{ viewModel.restart() }} else null
                )
                else -> {
                    GameQuestionPager(
                        pageCount = state.words.size,
                        currentPage = state.currentIndex,
                        canSwipeForward = state.showAnswer,
                        swipeHint = spellingSwipeHint(state.currentIndex, state.words.size),
                        onSwipeForward = { viewModel.nextWord() }
                    ) { page ->
                        val current = state.words.getOrNull(page) ?: return@GameQuestionPager
                        val isActive = page == state.currentIndex

                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 32.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            item {
                                Text(
                                    "第 ${state.currentIndex + 1}/${state.words.size} 题",
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            item {
                                AppCard {
                                    Column(
                                        Modifier
                                            .fillMaxWidth()
                                            .padding(24.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text("请拼写以下单词", style = MaterialTheme.typography.labelLarge, color = AppColors.textMuted)
                                        Text(
                                            current.word.gameMeaning(),
                                            style = MaterialTheme.typography.titleLarge,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.padding(top = 12.dp)
                                        )
                                    }
                                }
                            }
                            item {
                                androidx.compose.material3.OutlinedTextField(
                                    value = if (isActive) state.input else "",
                                    onValueChange = { if (isActive) viewModel.updateInput(it) },
                                    label = { Text("输入单词") },
                                    enabled = isActive && !state.showAnswer,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            if (isActive) {
                                state.feedback?.let { feedback ->
                                    item {
                                        Text(
                                            feedback,
                                            color = if (state.lastAnswerCorrect) CorrectGreen else WrongRed,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                if (state.showAnswer && !state.lastAnswerCorrect) {
                                    item {
                                        Text(
                                            current.word.text,
                                            style = MaterialTheme.typography.headlineSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = AppColors.heroGreen,
                                            modifier = Modifier.clickable {
                                                speaker.speak(current.word.text, current.word.audioUrl)
                                            }
                                        )
                                    }
                                    item {
                                        Text("点击上方单词听发音", style = MaterialTheme.typography.labelSmall, color = AppColors.textMuted)
                                    }
                                }
                                if (!state.showAnswer) {
                                    item {
                                        androidx.compose.material3.Button(
                                            onClick = { viewModel.submit() },
                                            modifier = Modifier.fillMaxWidth(),
                                            enabled = state.input.isNotBlank()
                                        ) { Text("提交") }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
