package com.englishworder.ui.review

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.englishworder.data.repository.WordRepository
import com.englishworder.domain.model.WordWithReview
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class ReviewViewModel @Inject constructor(
    repository: WordRepository
) : ViewModel() {

    val dueCount: StateFlow<Int> = repository.observeDueCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val dueWords: StateFlow<List<WordWithReview>> = repository.observeDueReviews()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}
