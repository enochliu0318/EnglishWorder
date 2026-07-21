package com.englishworder.ui.games

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.englishworder.ui.components.ScreenPadding
import com.englishworder.ui.components.rememberWordSpeaker
import com.englishworder.ui.theme.AppColors

private val WrongRed = Color(0xFFD32F2F)
private val CorrectGreen = Color(0xFF2E7D32)
private val SelectedBg = Color(0xFFC8E6C9)
private val SelectedBorder = Color(0xFF1B5E20)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LinkMatchGameScreen(
    listId: Long,
    onBack: () -> Unit,
    viewModel: LinkMatchViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val speaker = rememberWordSpeaker()

    LaunchedEffect(listId) { viewModel.loadGame(listId) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (state.isRetryPhase) "连连看 · 错题重练" else "连连看") },
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
                    title = state.message ?: "全部配对成功！",
                    subtitle = if (state.rounds.isNotEmpty()) viewModel.finishSummary() else "",
                    onBack = onBack,
                    onRestart = if (state.rounds.isNotEmpty()) {{ viewModel.restart() }} else null
                )
                state.rounds.isNotEmpty() -> {
                    val currentRound = state.currentRound
                    LaunchedEffect(state.currentPage, currentRound?.selectedId, state.phase) {
                        val selectedId = currentRound?.selectedId ?: return@LaunchedEffect
                        val tile = currentRound.tiles.find { it.id == selectedId } ?: return@LaunchedEffect
                        if (tile.type == TileType.WORD) {
                            speaker.speak(tile.text, viewModel.audioUrlFor(tile.pairId))
                        }
                    }
                    GameQuestionPager(
                        pageCount = state.rounds.size,
                        currentPage = state.currentPage,
                        onPageChanged = viewModel::goToPage,
                        allowAdvanceFromLast = state.allRoundsComplete && state.currentPage == state.rounds.lastIndex,
                        onAdvanceFromLast = viewModel::advanceFromLastPage,
                        swipeHint = linkRoundSwipeHint(
                            currentPage = state.currentPage,
                            total = state.rounds.size,
                            roundComplete = currentRound?.isComplete == true,
                            hasRetry = state.wrongPairIds.isNotEmpty() || state.firstPassWrongCount > 0,
                            isRetryPhase = state.isRetryPhase
                        )
                    ) { page ->
                        val round = state.rounds.getOrNull(page) ?: return@GameQuestionPager
                        val isActive = page == state.currentPage

                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                "${if (state.isRetryPhase) "错题重练" else "连连看"} · ${state.progressLabel} · 得分 ${state.score}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(2),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                            ) {
                                items(round.tiles.filter { !it.matched }, key = { it.id }) { tile ->
                                    val selected = isActive && round.selectedId == tile.id
                                    LinkTileCard(
                                        tile = tile,
                                        selected = selected,
                                        enabled = isActive,
                                        onClick = { if (isActive) viewModel.selectTile(tile.id) }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LinkTileCard(
    tile: LinkTile,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val containerColor = when {
        selected -> SelectedBg
        else -> AppColors.cardBackground
    }
    val borderColor = if (selected) SelectedBorder else Color(0xFFE0E0E0)
    val borderWidth = if (selected) 3.dp else 1.dp

    Card(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 88.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = if (selected) 8.dp else 2.dp),
        border = BorderStroke(borderWidth, borderColor)
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                if (tile.type == TileType.WORD) "单词" else "释义",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                color = if (selected) {
                    SelectedBorder
                } else if (tile.type == TileType.WORD) {
                    AppColors.heroGreen
                } else {
                    AppColors.textMuted
                }
            )
            Text(
                tile.text,
                textAlign = TextAlign.Center,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                fontSize = if (selected) 18.sp else 17.sp,
                color = if (selected) {
                    SelectedBorder
                } else if (tile.type == TileType.WORD) {
                    AppColors.heroGreen
                } else {
                    AppColors.textPrimary
                },
                modifier = Modifier.padding(top = 6.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpellingGameScreen(
    listId: Long,
    onBack: () -> Unit,
    viewModel: SpellingViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val speaker = rememberWordSpeaker()

    LaunchedEffect(listId) { viewModel.loadGame(listId) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (state.isRetryPhase) "拼写 · 错题重练" else "拼写") },
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
                        "词库单词不足"
                    } else {
                        viewModel.finishSummary()
                    },
                    onBack = onBack,
                    onRestart = if (state.words.isNotEmpty()) {{ viewModel.restart() }} else null
                )
                else -> {
                    GameQuestionPager(
                        pageCount = state.words.size,
                        currentPage = state.currentIndex,
                        onPageChanged = viewModel::goToPage,
                        allowAdvanceFromLast = state.showAnswer && state.currentIndex == state.words.lastIndex,
                        onAdvanceFromLast = viewModel::advanceFromLastPage,
                        swipeHint = spellingSwipeHint(state.currentIndex, state.words.size, state.showAnswer)
                    ) { page ->
                        val current = state.words.getOrNull(page) ?: return@GameQuestionPager
                        val isActive = page == state.currentIndex
                        val pageAnswer = state.answerFor(page)

                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 32.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            item {
                                Text(
                                    "${if (state.isRetryPhase) "错题重练" else "拼写"} · 第 ${page + 1}/${state.words.size} 题",
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            item {
                                androidx.compose.material3.Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = AppColors.cardBackground)
                                ) {
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
                                    value = pageAnswer.input,
                                    onValueChange = { if (isActive) viewModel.updateInput(it) },
                                    label = { Text("输入单词") },
                                    enabled = isActive && !pageAnswer.showAnswer,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            pageAnswer.feedback?.let { feedback ->
                                item {
                                    Text(
                                        feedback,
                                        color = if (pageAnswer.lastAnswerCorrect) CorrectGreen else WrongRed,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            if (pageAnswer.showAnswer && !pageAnswer.lastAnswerCorrect) {
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
                            if (isActive && !pageAnswer.showAnswer) {
                                item {
                                    androidx.compose.material3.Button(
                                        onClick = { viewModel.submit() },
                                        modifier = Modifier.fillMaxWidth(),
                                        enabled = pageAnswer.input.isNotBlank()
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
