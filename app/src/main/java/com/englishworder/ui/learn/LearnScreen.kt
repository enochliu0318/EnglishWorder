package com.englishworder.ui.learn

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.englishworder.domain.model.StudyMode
import com.englishworder.ui.components.GreenGradientBackground
import com.englishworder.ui.components.ScreenPadding
import com.englishworder.ui.components.rememberWordSpeaker
import com.englishworder.ui.theme.AppColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LearnSessionScreen(
    listId: Long,
    mode: StudyMode,
    onBack: () -> Unit,
    viewModel: LearnViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val speaker = rememberWordSpeaker()

    LaunchedEffect(listId, mode) { viewModel.loadWords(listId, mode) }

    LaunchedEffect(state.currentIndex, state.current?.word?.text) {
        state.current?.word?.let { w ->
            speaker.speak(w.text, w.audioUrl)
        }
    }

    GreenGradientBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { Text("${state.modeLabel} ${state.progress}") },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
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
                    state.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = AppColors.heroGreen)
                    }
                    state.finished -> Column(
                        Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically)
                    ) {
                        Text(
                            if (state.words.isEmpty()) "暂无单词" else "本轮学习完成！",
                            style = MaterialTheme.typography.headlineSmall,
                            color = AppColors.heroGreen
                        )
                        if (state.words.isNotEmpty()) {
                            Button(
                                onClick = { viewModel.restart() },
                                colors = ButtonDefaults.buttonColors(containerColor = AppColors.heroGreen)
                            ) { Text("再来一轮") }
                        }
                        OutlinedButton(onClick = onBack) { Text("返回") }
                    }
                    else -> {
                        val current = state.current ?: return@ScreenPadding
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            FlipCard(
                                flipped = state.cardFlipped,
                                onFlip = { viewModel.flipCard() },
                                front = {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.padding(28.dp)
                                    ) {
                                        Text("点击卡片查看释义", style = MaterialTheme.typography.labelMedium,
                                            color = AppColors.textMuted)
                                        Spacer(Modifier.height(16.dp))
                                        Text(current.word.text, fontSize = 40.sp, fontWeight = FontWeight.Bold,
                                            color = AppColors.heroGreen, textAlign = TextAlign.Center)
                                        if (current.word.phonetic.isNotBlank()) {
                                            Text(current.word.phonetic, style = MaterialTheme.typography.titleMedium,
                                                color = MaterialTheme.colorScheme.secondary, modifier = Modifier.padding(top = 8.dp))
                                        }
                                        IconButton(onClick = {
                                            speaker.speak(current.word.text, current.word.audioUrl)
                                        }) {
                                            Icon(Icons.Default.VolumeUp, contentDescription = "发音", tint = AppColors.heroGreen)
                                        }
                                    }
                                },
                                back = {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.padding(28.dp)
                                    ) {
                                        Text("释义", style = MaterialTheme.typography.labelMedium)
                                        Spacer(Modifier.height(12.dp))
                                        if (current.word.partOfSpeech.isNotBlank()) {
                                            Text(
                                                current.word.partOfSpeech,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = AppColors.textMuted
                                            )
                                            Spacer(Modifier.height(4.dp))
                                        }
                                        Text(current.word.meaning, fontSize = 22.sp, fontWeight = FontWeight.Medium,
                                            textAlign = TextAlign.Center, lineHeight = 32.sp, color = AppColors.textPrimary)
                                        if (current.word.example.isNotBlank()) {
                                            Spacer(Modifier.height(16.dp))
                                            Text("例句", style = MaterialTheme.typography.labelSmall, color = AppColors.textMuted)
                                            Text(current.word.example, style = MaterialTheme.typography.bodyMedium,
                                                textAlign = TextAlign.Center, color = AppColors.textSecondary,
                                                modifier = Modifier.padding(top = 4.dp))
                                        }
                                    }
                                }
                            )
                            Spacer(Modifier.height(32.dp))
                            if (mode == StudyMode.NEW_WORDS) {
                                Text("认识和不认识都会加入复习计划", style = MaterialTheme.typography.bodySmall,
                                    color = AppColors.textMuted)
                                Spacer(Modifier.height(12.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                                    Button(
                                        onClick = { viewModel.answer(known = false) },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE57373))
                                    ) { Text("不认识") }
                                    Button(
                                        onClick = { viewModel.answer(known = true) },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(containerColor = AppColors.heroGreen)
                                    ) { Text("认识") }
                                }
                            } else {
                                Button(
                                    onClick = { viewModel.answer(known = true) },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = AppColors.heroGreen)
                                ) { Text("下一个") }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FlipCard(
    flipped: Boolean,
    onFlip: () -> Unit,
    front: @Composable () -> Unit,
    back: @Composable () -> Unit
) {
    val rotation by animateFloatAsState(
        targetValue = if (flipped) 180f else 0f,
        animationSpec = tween(400, easing = FastOutSlowInEasing),
        label = "flip"
    )
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(320.dp)
            .graphicsLayer {
                rotationY = rotation
                cameraDistance = 12f * density
            }
            .clickable { onFlip() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = AppColors.cardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Box(Modifier.graphicsLayer { rotationY = if (rotation > 90f) 180f else 0f }) {
                if (rotation <= 90f) front() else back()
            }
        }
    }
}
