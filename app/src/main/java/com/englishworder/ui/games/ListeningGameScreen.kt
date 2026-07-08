package com.englishworder.ui.games

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import com.englishworder.domain.model.ReviewMode
import com.englishworder.ui.components.AppCard
import com.englishworder.ui.components.ScreenPadding
import com.englishworder.ui.components.rememberWordSpeaker
import com.englishworder.ui.theme.AppColors

private val WrongRed = Color(0xFFD32F2F)
private val WrongBg = Color(0xFFFFEBEE)
private val CorrectGreen = Color(0xFF2E7D32)
private val CorrectBg = Color(0xFFE8F5E9)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListeningGameScreen(
    listId: Long,
    mode: ReviewMode,
    onBack: () -> Unit,
    viewModel: ListeningViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val speaker = rememberWordSpeaker()

    LaunchedEffect(listId, mode) { viewModel.loadGame(listId, mode) }

    val currentQuestion = state.questions.getOrNull(state.currentIndex)
    LaunchedEffect(state.currentIndex, state.phase, currentQuestion?.word?.word?.text) {
        currentQuestion?.word?.word?.let { w ->
            speaker.speak(w.text, w.audioUrl)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (state.isRetryPhase) "听力错题重练" else "听力练习") },
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
                    title = if (state.firstPassTotal == 0) "暂无可用单词" else "听力练习完成",
                    subtitle = if (state.firstPassTotal == 0) {
                        if (mode == ReviewMode.SCHEDULED) "暂无待复习单词，试试自由练习" else "词库单词不足"
                    } else {
                        viewModel.finishSummary()
                    },
                    onBack = onBack,
                    onRestart = if (state.firstPassTotal > 0) {{ viewModel.restart() }} else null
                )
                state.questions.isNotEmpty() -> {
                    GameQuestionPager(
                        pageCount = state.questions.size,
                        currentPage = state.currentIndex,
                        onPageChanged = viewModel::goToPage,
                        allowAdvanceFromLast = state.answered && state.currentIndex == state.questions.lastIndex,
                        onAdvanceFromLast = viewModel::advanceFromLastPage,
                        swipeHint = choiceGameSwipeHint(
                            currentIndex = state.currentIndex,
                            total = state.questions.size,
                            isRetryPhase = state.isRetryPhase,
                            hasWrongQuestions = state.wrongQuestions.isNotEmpty(),
                            currentAnswered = state.answered
                        )
                    ) { page ->
                        val question = state.questions.getOrNull(page) ?: return@GameQuestionPager
                        val isActive = page == state.currentIndex
                        val pageAnswer = state.answerFor(page)
                        val word = question.word.word

                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 32.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            item {
                                Text(
                                    if (state.isRetryPhase) {
                                        "${state.progressLabel} · 重练正确 ${state.retryScore}"
                                    } else {
                                        "${state.progressLabel} · 得分 ${state.score}"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (state.isRetryPhase) WrongRed else AppColors.textSecondary
                                )
                            }
                            item {
                                AppCard {
                                    Column(
                                        Modifier
                                            .fillMaxWidth()
                                            .padding(32.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Text(
                                            "听音辨词",
                                            style = MaterialTheme.typography.labelLarge,
                                            color = AppColors.textMuted
                                        )
                                        FilledIconButton(
                                            onClick = { speaker.speak(word.text, word.audioUrl) },
                                            modifier = Modifier.size(80.dp),
                                            shape = CircleShape,
                                            colors = IconButtonDefaults.filledIconButtonColors(
                                                containerColor = AppColors.heroGreen
                                            )
                                        ) {
                                            Icon(
                                                Icons.AutoMirrored.Filled.VolumeUp,
                                                contentDescription = "播放发音",
                                                modifier = Modifier.size(40.dp),
                                                tint = Color.White
                                            )
                                        }
                                        Text(
                                            "点击喇叭重听",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = AppColors.textMuted
                                        )
                                    }
                                }
                            }
                            item {
                                Text(
                                    "选择听到的中文释义",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = AppColors.heroGreen,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                            items(question.options.size) { index ->
                                val option = question.options[index]
                                val isSelected = pageAnswer.selectedIndex == index
                                val isCorrect = index == question.correctIndex
                                val showResult = pageAnswer.answered
                                val showAsWrong = showResult && isSelected && !isCorrect
                                val showAsCorrect = showResult && isCorrect

                                OutlinedButton(
                                    onClick = { if (isActive) viewModel.selectOption(index) },
                                    enabled = isActive && !pageAnswer.answered,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(
                                        width = if (showAsCorrect || showAsWrong) 2.dp else 1.dp,
                                        color = when {
                                            showAsCorrect -> CorrectGreen
                                            showAsWrong -> WrongRed
                                            else -> MaterialTheme.colorScheme.outline
                                        }
                                    ),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = when {
                                            showAsCorrect -> CorrectBg
                                            showAsWrong -> WrongBg
                                            else -> MaterialTheme.colorScheme.surface
                                        },
                                        contentColor = when {
                                            showAsWrong -> WrongRed
                                            showAsCorrect -> CorrectGreen
                                            else -> MaterialTheme.colorScheme.onSurface
                                        }
                                    )
                                ) {
                                    Text(
                                        option,
                                        modifier = Modifier.padding(8.dp),
                                        fontSize = 18.sp,
                                        fontWeight = if (showAsCorrect || showAsWrong) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                            if (pageAnswer.answered) {
                                item {
                                    val isWrong = pageAnswer.selectedIndex != question.correctIndex
                                    if (isWrong) {
                                        Text(
                                            "答错了！正确答案：${question.options[question.correctIndex]}",
                                            color = WrongRed,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.fillMaxWidth(),
                                            textAlign = TextAlign.Center
                                        )
                                    } else {
                                        Text(
                                            "回答正确",
                                            color = CorrectGreen,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.fillMaxWidth(),
                                            textAlign = TextAlign.Center
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
