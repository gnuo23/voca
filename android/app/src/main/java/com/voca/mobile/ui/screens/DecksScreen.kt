package com.voca.mobile.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.voca.mobile.data.DeckResponse
import com.voca.mobile.ui.AppViewModel
import com.voca.mobile.ui.Routes
import com.voca.mobile.ui.components.DuoButton
import com.voca.mobile.ui.components.DuoButtonStyle
import com.voca.mobile.ui.components.DuoCard
import com.voca.mobile.ui.components.EmptyState
import com.voca.mobile.ui.components.Loadable
import com.voca.mobile.ui.components.MutedText
import com.voca.mobile.ui.components.PillKind
import com.voca.mobile.ui.components.PillTag
import com.voca.mobile.ui.components.ProgressBarRounded
import com.voca.mobile.ui.components.ScreenList
import com.voca.mobile.ui.components.UiState
import com.voca.mobile.ui.components.rememberUiState
import kotlinx.coroutines.launch

@Composable
fun DecksScreen(app: AppViewModel, navController: NavHostController) {
    val state = rememberUiState<List<DeckResponse>>()
    val scope = rememberCoroutineScope()
    var showCreate by remember { mutableStateOf(false) }

    fun load() {
        state.value = UiState.Loading
        scope.launch {
            app.repo.decks()
                .onSuccess { state.value = UiState.Success(it) }
                .onFailure { state.value = UiState.Error(it.message ?: "Lỗi tải bộ thẻ") }
        }
    }

    LaunchedEffect(Unit) { load() }

    Loadable(state.value, onRetry = ::load) { decks ->
        ScreenList(title = "Bộ thẻ") {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    DuoButton("Tạo bộ thẻ", onClick = { showCreate = true }, modifier = Modifier.weight(1f))
                    DuoButton(
                        "Từ khó",
                        onClick = { navController.navigate(Routes.DIFFICULT) },
                        style = DuoButtonStyle.Secondary,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            if (decks.isEmpty()) {
                item { EmptyState("Chưa có bộ thẻ nào. Tạo bộ thẻ đầu tiên nhé!") }
            } else {
                items(decks) { deck ->
                    DeckCard(deck) { deck.id?.let { navController.navigate(Routes.deckDetail(it)) } }
                }
            }
        }
    }

    if (showCreate) {
        CreateDeckDialog(
            onDismiss = { showCreate = false },
            onCreate = { name, desc ->
                showCreate = false
                scope.launch {
                    app.repo.createDeck(name, desc).onSuccess { deck ->
                        deck.id?.let { navController.navigate(Routes.deckDetail(it)) }
                    }
                }
            },
        )
    }
}

@Composable
fun DeckCard(deck: DeckResponse, onClick: () -> Unit) {
    DuoCard {
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(
                deck.name,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            if (deck.difficultDeck) {
                PillTag("Từ khó", PillKind.Difficult)
            }
        }
        Spacer(Modifier.height(4.dp))
        MutedText("${deck.totalWords} từ · ${deck.learnedWords} đã thuộc · ${deck.dueTodayCount} cần ôn")
        Spacer(Modifier.height(10.dp))
        val ratio = if (deck.totalWords > 0) deck.learnedWords.toFloat() / deck.totalWords else 0f
        ProgressBarRounded(progress = ratio)
        Spacer(Modifier.height(12.dp))
        DuoButton("Mở bộ thẻ", onClick, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
fun CreateDeckDialog(onDismiss: () -> Unit, onCreate: (String, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tạo bộ thẻ", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Tên bộ thẻ") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Mô tả") },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { if (name.isNotBlank()) onCreate(name, description) }) {
                Text("Tạo")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Huỷ") }
        },
    )
}
