package com.voca.mobile.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.voca.mobile.data.DeckResponse
import com.voca.mobile.data.VocabItemResponse
import com.voca.mobile.ui.AppViewModel
import com.voca.mobile.ui.Routes
import com.voca.mobile.ui.components.AudioButton
import com.voca.mobile.ui.components.DuoButton
import com.voca.mobile.ui.components.DuoButtonStyle
import com.voca.mobile.ui.components.DuoCard
import com.voca.mobile.ui.components.EmptyState
import com.voca.mobile.ui.components.Loadable
import com.voca.mobile.ui.components.MutedText
import com.voca.mobile.ui.components.ScreenList
import com.voca.mobile.ui.components.UiState
import com.voca.mobile.ui.components.rememberUiState
import com.voca.mobile.ui.components.statusPill
import kotlinx.coroutines.launch

private data class DeckDetail(val deck: DeckResponse, val vocab: List<VocabItemResponse>)

@Composable
fun DeckDetailScreen(app: AppViewModel, navController: NavHostController, deckId: Long) {
    val state = rememberUiState<DeckDetail>()
    val scope = rememberCoroutineScope()
    var showImport by remember { mutableStateOf(false) }
    var showReset by remember { mutableStateOf(false) }

    fun load() {
        state.value = UiState.Loading
        scope.launch {
            val deckResult = app.repo.deck(deckId)
            val vocabResult = app.repo.vocab(deckId)
            val deck = deckResult.getOrNull()
            if (deck == null) {
                state.value = UiState.Error(deckResult.exceptionOrNull()?.message ?: "Lỗi tải bộ thẻ")
                return@launch
            }
            state.value = UiState.Success(DeckDetail(deck, vocabResult.getOrDefault(emptyList())))
        }
    }

    LaunchedEffect(deckId) { load() }

    Loadable(state.value, onRetry = ::load) { detail ->
        val deck = detail.deck
        ScreenList(title = deck.name) {
            item {
                MutedText("${deck.totalWords} từ · ${deck.learnedWords} đã thuộc · ${deck.dueTodayCount} cần ôn")
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    DuoButton("Học", onClick = { navController.navigate(Routes.learn(deckId)) }, modifier = Modifier.weight(1f))
                    DuoButton(
                        "Ôn tập",
                        onClick = { navController.navigate(Routes.reviewDeck(deckId)) },
                        style = DuoButtonStyle.Blue,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    DuoButton("Nhập từ", onClick = { showImport = true }, style = DuoButtonStyle.Secondary, modifier = Modifier.weight(1f))
                    DuoButton("Reset", onClick = { showReset = true }, style = DuoButtonStyle.Secondary, modifier = Modifier.weight(1f))
                }
            }
            item {
                Text(
                    "Từ vựng",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }
            if (detail.vocab.isEmpty()) {
                item { EmptyState("Bộ thẻ chưa có từ. Nhập từ để bắt đầu!") }
            } else {
                items(detail.vocab) { item ->
                    VocabCard(
                        item = item,
                        onSpeak = { app.speak(item.word) },
                        onClick = { item.id?.let { navController.navigate(Routes.wordDetail(it)) } },
                    )
                }
            }
        }
    }

    if (showImport) {
        ImportDialog(
            onDismiss = { showImport = false },
            onImport = { raw ->
                showImport = false
                scope.launch { app.repo.importVocab(deckId, raw).onSuccess { load() } }
            },
        )
    }

    if (showReset) {
        AlertDialog(
            onDismissRequest = { showReset = false },
            title = { Text("Reset tiến độ?") },
            text = { Text("Toàn bộ tiến độ học của bộ thẻ này sẽ được đặt lại.") },
            confirmButton = {
                TextButton(onClick = {
                    showReset = false
                    scope.launch { app.repo.resetDeckProgress(deckId).onSuccess { load() } }
                }) { Text("Reset") }
            },
            dismissButton = { TextButton(onClick = { showReset = false }) { Text("Huỷ") } },
        )
    }
}

@Composable
private fun VocabCard(item: VocabItemResponse, onSpeak: () -> Unit, onClick: () -> Unit) {
    DuoCard(modifier = Modifier.clickable(onClick = onClick)) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.weight(1f)) {
                Text(item.word, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                if (!item.ipa.isNullOrBlank()) {
                    MutedText(item.ipa)
                }
                Spacer(Modifier.height(2.dp))
                MutedText(item.meaningVi ?: "-")
            }
            AudioButton(onClick = onSpeak)
        }
        Spacer(Modifier.height(8.dp))
        val (label, kind) = statusPill(item.progressStatus)
        com.voca.mobile.ui.components.PillTag(label, kind)
    }
}

@Composable
private fun ImportDialog(onDismiss: () -> Unit, onImport: (String) -> Unit) {
    var raw by remember { mutableStateOf(TextFieldValue("")) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nhập từ vựng") },
        text = {
            Column {
                MutedText("Mỗi dòng một từ. Ví dụ: apple ; (n) quả táo")
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = raw,
                    onValueChange = { raw = it },
                    label = { Text("word ; (pos) meaning") },
                    minLines = 6,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { if (raw.text.isNotBlank()) onImport(raw.text) }) { Text("Nhập") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Huỷ") } },
    )
}
