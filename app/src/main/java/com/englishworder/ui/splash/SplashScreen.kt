package com.englishworder.ui.splash

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.englishworder.ui.theme.AppColors
import kotlinx.coroutines.delay

private data class SplashFeature(val icon: ImageVector, val title: String, val subtitle: String)

private val FEATURES = listOf(
    SplashFeature(Icons.Default.AutoStories, "词库管理", "Excel / CSV 批量导入"),
    SplashFeature(Icons.Default.GraphicEq, "科学复习", "艾宾浩斯记忆曲线"),
    SplashFeature(Icons.Default.Extension, "卡片学习", "翻转记忆 · 自动发音"),
    SplashFeature(Icons.Default.SportsEsports, "趣味游戏", "选择题 · 听力 · 连连看 · 拼写")
)

@Composable
fun SplashScreen(onFinished: () -> Unit) {
    var contentAlpha by remember { mutableFloatStateOf(0f) }
    var progress by remember { mutableFloatStateOf(0f) }
    val alpha by animateFloatAsState(targetValue = contentAlpha, animationSpec = tween(900), label = "fade")
    val barProgress by animateFloatAsState(targetValue = progress, animationSpec = tween(2200), label = "load")

    LaunchedEffect(Unit) {
        contentAlpha = 1f
        progress = 1f
        delay(2600)
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFFE3F2E8), Color(0xFFF7FBF7), Color(0xFFEAF4EA))
                )
            )
    ) {
        Box(
            Modifier
                .size(220.dp)
                .offset(x = (-60).dp, y = (-40).dp)
                .clip(CircleShape)
                .background(AppColors.heroGreen.copy(alpha = 0.12f))
        )
        Box(
            Modifier
                .size(160.dp)
                .align(Alignment.TopEnd)
                .offset(x = 40.dp, y = 80.dp)
                .clip(CircleShape)
                .background(Color(0xFFA5D6A7).copy(alpha = 0.25f))
        )
        Box(
            Modifier
                .size(100.dp)
                .align(Alignment.BottomStart)
                .offset(x = 24.dp, y = (-100).dp)
                .clip(CircleShape)
                .background(AppColors.heroGreen.copy(alpha = 0.08f))
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp, vertical = 48.dp)
                .alpha(alpha),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(Modifier.height(16.dp))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(88.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFF2E7D32), Color(0xFF1B5E20))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text("W", fontSize = 44.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
                Spacer(Modifier.height(20.dp))
                Text(
                    "EnglishWorder",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.heroGreen
                )
                Text(
                    "也可以很有趣",
                    style = MaterialTheme.typography.titleMedium,
                    color = AppColors.textSecondary,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                FEATURES.forEach { feature ->
                    SplashFeatureRow(feature)
                }
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                LinearProgressIndicator(
                    progress = { barProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = AppColors.heroGreen,
                    trackColor = AppColors.heroGreen.copy(alpha = 0.15f)
                )
                Text(
                    "正在准备学习环境…",
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.textMuted,
                    modifier = Modifier.padding(top = 10.dp, bottom = 16.dp)
                )
                Button(
                    onClick = onFinished,
                    colors = ButtonDefaults.buttonColors(containerColor = AppColors.heroGreen),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("进入应用", modifier = Modifier.padding(vertical = 4.dp))
                }
            }
        }
    }
}

@Composable
private fun SplashFeatureRow(feature: SplashFeature) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.85f))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(AppColors.heroGreen.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(feature.icon, contentDescription = null, tint = AppColors.heroGreen, modifier = Modifier.size(22.dp))
        }
        Column {
            Text(feature.title, fontWeight = FontWeight.Bold, color = AppColors.textPrimary)
            Text(feature.subtitle, style = MaterialTheme.typography.bodySmall, color = AppColors.textMuted)
        }
    }
}
