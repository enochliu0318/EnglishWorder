package com.englishworder.ui.progress

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.englishworder.ui.components.AppCard
import com.englishworder.ui.theme.AppColors

private val EBBINGHAUS_INTERVALS = listOf(1, 2, 4, 7, 15, 30)
private val EBBINGHAUS_LABELS = listOf("第1次", "第2次", "第3次", "第4次", "第5次", "第6次")

@Composable
fun EbbinghausIntervalChart() {
    val maxDays = EBBINGHAUS_INTERVALS.max().toFloat()
    val barAreaHeight = 120.dp

    AppCard {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("艾宾浩斯复习间隔", fontWeight = FontWeight.Bold, color = AppColors.heroGreen)
            Text(
                "每次复习通过后，间隔按曲线递增",
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.textMuted
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Bottom
            ) {
                EBBINGHAUS_INTERVALS.forEachIndexed { index, days ->
                    val barHeight = barAreaHeight * (days / maxDays)
                    val barColor = AppColors.heroGreen.copy(alpha = 0.45f + index * 0.09f)
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            "${days}天",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = AppColors.heroGreen,
                            fontSize = 10.sp,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        Box(
                            modifier = Modifier
                                .width(26.dp)
                                .height(barAreaHeight),
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(26.dp)
                                    .height(barHeight)
                                    .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                    .background(barColor)
                            )
                        }
                        Text(
                            EBBINGHAUS_LABELS[index],
                            style = MaterialTheme.typography.labelSmall,
                            color = AppColors.textMuted,
                            fontSize = 9.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                    }
                }
            }
        }
    }
}
