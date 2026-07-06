package com.englishworder.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// 清新绿色 · 柔和可读配色（避免纯黑纯白强对比）
private val TextPrimary = Color(0xFF1B4332)
private val TextSecondary = Color(0xFF3D5A4C)
private val TextMuted = Color(0xFF5C7A6B)

private val LightColors = lightColorScheme(
    primary = Color(0xFF388E3C),
    onPrimary = Color(0xFFF5FBF6),
    primaryContainer = Color(0xFFC8E6C9),
    onPrimaryContainer = TextPrimary,
    secondary = Color(0xFF4CAF50),
    onSecondary = Color(0xFFF5FBF6),
    secondaryContainer = Color(0xFFDAEDD9),
    onSecondaryContainer = TextPrimary,
    background = Color(0xFFEEF6EE),
    onBackground = TextPrimary,
    surface = Color(0xFFFAFDFA),
    onSurface = TextPrimary,
    surfaceVariant = Color(0xFFE4EFE4),
    onSurfaceVariant = TextSecondary,
    outline = Color(0xFFADC3B0),
    error = Color(0xFFC62828),
    onError = Color(0xFFFFF5F5)
)

object AppColors {
    val gradientTop = Color(0xFFE8F3E8)
    val gradientBottom = Color(0xFFF4FAF4)
    val cardBackground = Color(0xFFFAFDFA)
    val heroGreen = Color(0xFF2E7D32)
    val textPrimary = TextPrimary
    val textSecondary = TextSecondary
    val textMuted = TextMuted

    // 消消乐：柔和底色 + 深绿文字（不用白字）
    val gemColors = listOf(
        Color(0xFFD4EAD4),
        Color(0xFFDDEFE0),
        Color(0xFFD0E8D6),
        Color(0xFFE2F0E4),
        Color(0xFFC8E0CC),
        Color(0xFFDAEDE0),
        Color(0xFFD6E9DA),
        Color(0xFFE0EDE4)
    )
    val gemText = Color(0xFF1B4332)
    val gemTextAlt = Color(0xFF2D6A4F)
    val gemSelectedBorder = Color(0xFF388E3C)
}

@Composable
fun EnglishWorderTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LightColors,
        content = content
    )
}
