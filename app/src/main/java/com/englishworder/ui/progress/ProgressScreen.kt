package com.englishworder.ui.progress

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.englishworder.domain.model.DayReviewPlan
import com.englishworder.domain.model.ListStudyProgress
import com.englishworder.domain.model.StudyProgressOverview
import com.englishworder.ui.components.AppCard
import com.englishworder.ui.components.GreenGradientBackground
import com.englishworder.ui.theme.AppColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgressScreen(
    onStartReview: () -> Unit,
    onStartLearn: (Long) -> Unit,
    viewModel: ProgressViewModel = hiltViewModel()
) {
    val progress by viewModel.progress.collectAsState()

    GreenGradientBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { Text("学习计划") },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            }
        ) { padding ->
            if (progress.totalWords == 0) {
                Box(
                    Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Text("还没有单词，先去词库添加吧", color = AppColors.textSecondary)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item { OverviewCard(progress, onStartReview) }
                    item { StatusBreakdownCard(progress) }
                    item {
                        Text(
                            "未来 7 天复习量",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = AppColors.heroGreen
                        )
                    }
                    item { UpcomingDaysCard(progress.upcomingDays) }
                    item {
                        Text(
                            "各词库进度",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = AppColors.heroGreen,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                    items(progress.listProgress, key = { it.listId }) { list ->
                        ListProgressCard(list, onStartLearn)
                    }
                    item { EbbinghausIntervalChart() }
                }
            }
        }
    }
}

@Composable
private fun OverviewCard(progress: StudyProgressOverview, onStartReview: () -> Unit) {
    AppCard {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        progress = { progress.masteryPercent / 100f },
                        modifier = Modifier.width(72.dp).height(72.dp),
                        color = AppColors.heroGreen,
                        trackColor = AppColors.heroGreen.copy(alpha = 0.15f),
                        strokeWidth = 6.dp
                    )
                    Text("${progress.masteryPercent}%", fontWeight = FontWeight.Bold, color = AppColors.heroGreen)
                }
                Spacer(Modifier.width(16.dp))
                Column {
                    Text("掌握进度", style = MaterialTheme.typography.labelMedium, color = AppColors.textMuted)
                    Text(
                        "${progress.masteredCount} / ${progress.totalWords} 词已掌握",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "今日待复习 ${progress.dueTodayCount} 个",
                        style = MaterialTheme.typography.bodySmall,
                        color = AppColors.heroGreen
                    )
                }
            }
            if (progress.dueTodayCount > 0) {
                Button(
                    onClick = onStartReview,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = AppColors.heroGreen)
                ) { Text("开始今日复习") }
            }
        }
    }
}

@Composable
private fun StatusBreakdownCard(progress: StudyProgressOverview) {
    AppCard {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("学习状态", fontWeight = FontWeight.Bold, color = AppColors.heroGreen)
            StatusRow("新词", progress.newCount, Color(0xFF90CAF9))
            StatusRow("学习中", progress.learningCount, Color(0xFFFFB74D))
            StatusRow("复习中", progress.reviewCount, Color(0xFF81C784))
            StatusRow("已掌握", progress.masteredCount, AppColors.heroGreen)
        }
    }
}

@Composable
private fun StatusRow(label: String, count: Int, color: Color) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                Modifier
                    .width(10.dp)
                    .height(10.dp)
                    .clip(RoundedCornerShape(50))
                    .background(color)
            )
            Text(label, style = MaterialTheme.typography.bodyMedium)
        }
        Text("$count", fontWeight = FontWeight.Bold, color = color)
    }
}

@Composable
private fun UpcomingDaysCard(days: List<DayReviewPlan>) {
    AppCard {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (days.isEmpty()) {
                Text("未来 7 天暂无复习安排", style = MaterialTheme.typography.bodySmall, color = AppColors.textMuted)
            } else {
                val maxCount = days.maxOf { it.count }.coerceAtLeast(1)
                days.forEach { day ->
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            day.label,
                            modifier = Modifier.width(48.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = AppColors.textSecondary
                        )
                        LinearProgressIndicator(
                            progress = { day.count.toFloat() / maxCount },
                            modifier = Modifier.weight(1f).height(8.dp).clip(RoundedCornerShape(4.dp)),
                            color = AppColors.heroGreen,
                            trackColor = AppColors.heroGreen.copy(alpha = 0.12f)
                        )
                        Text(
                            "${day.count}",
                            modifier = Modifier.width(28.dp),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ListProgressCard(list: ListStudyProgress, onStartLearn: (Long) -> Unit) {
    AppCard {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(list.listName, fontWeight = FontWeight.Bold, color = AppColors.heroGreen)
                Text("${list.masteryPercent}%", fontWeight = FontWeight.Bold, color = AppColors.heroGreen)
            }
            LinearProgressIndicator(
                progress = { list.masteryPercent / 100f },
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                color = AppColors.heroGreen,
                trackColor = AppColors.heroGreen.copy(alpha = 0.12f)
            )
            Text(
                "新${list.newCount} · 学${list.learningCount} · 复${list.reviewCount} · 掌${list.masteredCount}",
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.textMuted
            )
            if (list.dueToday > 0) {
                Text("今日待复习 ${list.dueToday} 个", style = MaterialTheme.typography.bodySmall, color = AppColors.heroGreen)
            }
            if (list.newCount > 0) {
                Button(
                    onClick = { onStartLearn(list.listId) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = AppColors.heroGreen)
                ) { Text("学习 (${list.newCount})") }
            }
        }
    }
}
