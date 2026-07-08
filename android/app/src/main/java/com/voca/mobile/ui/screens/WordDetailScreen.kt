package com.voca.mobile.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.voca.mobile.data.VocabItemResponse
import com.voca.mobile.ui.AppViewModel
import com.voca.mobile.ui.components.AudioButton
import com.voca.mobile.ui.components.DuoCard
import com.voca.mobile.ui.components.Loadable
import com.voca.mobile.ui.components.MutedText
import com.voca.mobile.ui.components.PillTag
import com.voca.mobile.ui.components.ScreenList
import com.voca.mobile.ui.components.UiState
import com.voca.mobile.ui.components.rememberUiState
import com.voca.mobile.ui.components.statusPill
import com.voca.mobile.ui.theme.VocaTheme
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch

@Composable
fun WordDetailScreen(app: AppViewModel, navController: NavHostController, vocabId: Long) {
    val state = rememberUiState<VocabItemResponse>()
    val scope = rememberCoroutineScope()

    fun load() {
        state.value = UiState.Loading
        scope.launch {
            app.repo.vocabItem(vocabId)
                .onSuccess { state.value = UiState.Success(it) }
                .onFailure { state.value = UiState.Error(it.message ?: "Lỗi tải từ") }
        }
    }

    LaunchedEffect(vocabId) { load() }

    Loadable(state.value, onRetry = ::load) { word ->
        ScreenList(title = word.word) {
            item {
                DuoCard {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.weight(1f)) {
                            if (!word.ipa.isNullOrBlank()) {
                                Text(word.ipa, style = MaterialTheme.typography.titleMedium, color = VocaTheme.brand.blue)
                            }
                            if (!word.partOfSpeech.isNullOrBlank()) {
                                MutedText(word.partOfSpeech)
                            }
                        }
                        AudioButton(onClick = { app.speak(word.word) })
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        word.meaningVi ?: "-",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(Modifier.height(10.dp))
                    val (label, kind) = statusPill(word.progressStatus)
                    PillTag(label, kind)
                }
            }

            if (!word.exampleEn.isNullOrBlank() || !word.exampleVi.isNullOrBlank()) {
                item {
                    DuoCard {
                        Text("Ví dụ", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                        Spacer(Modifier.height(6.dp))
                        if (!word.exampleEn.isNullOrBlank()) {
                            Text(word.exampleEn, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                        }
                        if (!word.exampleVi.isNullOrBlank()) {
                            Spacer(Modifier.height(2.dp))
                            MutedText(word.exampleVi)
                        }
                    }
                }
            }

            wordChips("Đồng nghĩa", word.synonyms)
            wordChips("Trái nghĩa", word.antonyms)
            wordChips("Cụm từ", word.collocations)

            if (!word.topic.isNullOrBlank() || !word.level.isNullOrBlank()) {
                item {
                    MutedText(
                        listOfNotNull(
                            word.topic?.takeIf { it.isNotBlank() }?.let { "Chủ đề: $it" },
                            word.level?.takeIf { it.isNotBlank() }?.let { "Cấp độ: $it" },
                        ).joinToString(" · "),
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
private fun androidx.compose.foundation.lazy.LazyListScope.wordChips(title: String, values: List<String>) {
    if (values.isEmpty()) return
    item {
        DuoCard {
            Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.height(8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                values.forEach { value -> Chip(value) }
            }
        }
    }
}

@Composable
private fun Chip(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
    }
}
