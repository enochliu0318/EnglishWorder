package com.englishworder.ui.progress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.englishworder.data.repository.WordRepository
import com.englishworder.domain.model.StudyProgressOverview
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class ProgressViewModel @Inject constructor(
    repository: WordRepository
) : ViewModel() {

    val progress: StateFlow<StudyProgressOverview> = repository.observeStudyProgress()
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            StudyProgressOverview()
        )
}
