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
    onPageChanged: (Int) -> Unit,
    allowAdvanceFromLast: Boolean,
    onAdvanceFromLast: () -> Unit,
    swipeHint: String?,
    modifier: Modifier = Modifier,
    pageContent: @Composable (page: Int) -> Unit
) {
    if (pageCount == 0) return

    val safeCurrent = currentPage.coerceIn(0, pageCount - 1)
    val isLastPage = safeCurrent == pageCount - 1
    val trailingPage = allowAdvanceFromLast && isLastPage
    val effectivePageCount = pageCount + if (trailingPage) 1 else 0

    val pagerState = rememberPagerState(initialPage = safeCurrent) { effectivePageCount }

    LaunchedEffect(safeCurrent, pageCount, effectivePageCount) {
        if (pagerState.currentPage != safeCurrent) {
            pagerState.scrollToPage(safeCurrent)
        }
    }

    LaunchedEffect(pagerState, currentPage, pageCount, allowAdvanceFromLast) {
        snapshotFlow { pagerState.settledPage }
            .distinctUntilChanged()
            .collect { settled ->
                val target = currentPage.coerceIn(0, (pageCount - 1).coerceAtLeast(0))
                when {
                    trailingPage && settled == pageCount -> onAdvanceFromLast()
                    settled in 0 until pageCount && settled != target -> onPageChanged(settled)
                    settled != target && settled != pageCount -> pagerState.scrollToPage(target)
                }
            }
    }

    Box(modifier = modifier.fillMaxSize()) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            userScrollEnabled = pageCount > 1 || trailingPage,
            beyondViewportPageCount = 1
        ) { page ->
            if (page < pageCount) {
                pageContent(page)
            } else {
                Box(Modifier.fillMaxSize())
            }
        }
        if (!swipeHint.isNullOrBlank()) {
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CardStudyPager(
    pageCount: Int,
    currentPage: Int,
    onPageChanged: (Int) -> Unit,
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

    LaunchedEffect(pagerState, currentPage, pageCount) {
        snapshotFlow { pagerState.settledPage }
            .distinctUntilChanged()
            .collect { settled ->
                val target = currentPage.coerceIn(0, (pageCount - 1).coerceAtLeast(0))
                when {
                    settled in 0 until pageCount && settled != target -> onPageChanged(settled)
                    settled != target -> pagerState.scrollToPage(target)
                }
            }
    }

    HorizontalPager(
        state = pagerState,
        modifier = modifier.fillMaxSize(),
        userScrollEnabled = pageCount > 1,
        beyondViewportPageCount = 1
    ) { page ->
        pageContent(page)
    }
}
