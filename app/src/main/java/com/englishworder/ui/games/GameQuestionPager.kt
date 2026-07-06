package com.englishworder.ui.games

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.englishworder.ui.theme.AppColors
import kotlinx.coroutines.flow.distinctUntilChanged

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GameQuestionPager(
    pageCount: Int,
    currentPage: Int,
    canSwipeForward: Boolean,
    swipeHint: String,
    onSwipeForward: () -> Unit,
    modifier: Modifier = Modifier,
    pageContent: @Composable (page: Int) -> Unit
) {
    if (pageCount == 0) return

    val safeCurrent = currentPage.coerceIn(0, pageCount - 1)
    val pagerState = rememberPagerState(initialPage = safeCurrent) { pageCount }

    LaunchedEffect(safeCurrent, pageCount) {
        if (pagerState.currentPage != safeCurrent) {
            pagerState.scrollToPage(safeCurrent)
        }
    }

    LaunchedEffect(pagerState, safeCurrent, canSwipeForward) {
        snapshotFlow { pagerState.settledPage }
            .distinctUntilChanged()
            .collect { settled ->
                when {
                    settled == safeCurrent + 1 && canSwipeForward -> onSwipeForward()
                    settled != safeCurrent -> pagerState.scrollToPage(safeCurrent)
                }
            }
    }

    Box(modifier = modifier.fillMaxSize()) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            userScrollEnabled = canSwipeForward,
            beyondViewportPageCount = 0
        ) { page ->
            pageContent(page)
        }
        if (canSwipeForward) {
            Text(
                swipeHint,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                style = MaterialTheme.typography.labelMedium,
                color = AppColors.heroGreen,
                textAlign = TextAlign.Center
            )
        }
    }
}
