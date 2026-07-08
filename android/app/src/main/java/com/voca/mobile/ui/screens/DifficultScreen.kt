package com.voca.mobile.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.voca.mobile.data.ReviewScheduleResponse
import com.voca.mobile.ui.AppViewModel
import com.voca.mobile.ui.Routes
import com.voca.mobile.ui.components.AudioButton
import com.voca.mobile.ui.components.DuoButton
import com.voca.mobile.ui.components.DuoButtonStyle
import com.voca.mobile.ui.components.DuoCard
import com.voca.mobile.ui.components.EmptyState
import com.voca.mobile.ui.components.Loadable
import com.voca.mobile.ui.components.MutedText
import com.voca.mobile.ui.components.PillKind
import com.voca.mobile.ui.components.PillTag
import com.voca.mobile.ui.components.ScreenList
import com.voca.mobile.ui.components.UiState
import com.voca.mobile.ui.components.rememberUiState
import kotlinx.coroutines.launch

@Composable
fun DifficultScreen(app: AppViewModel, navController: NavHostController) {
    val state = rememberUiState<ReviewScheduleResponse>()
    val scope = rememberCoroutineScope()

    fun load() {
        state.value = UiState.Loading
        scope.launch {
            app.repo.reviewSchedule(status = "DIFFICULT")
                .onSuccess { state.value = UiState.Success(it) }
                .onFailure { state.value = UiState.Error(it.message ?: "Lỗi tải từ khó") }
        }
    }

    LaunchedEffect(Unit) { load() }

    Loadable(state.value, onRetry = ::load) { data ->
        ScreenList(title = "Từ khó 🔥") {
            item {
                DuoButton(
                    "Tạo bộ thẻ từ khó",
                    onClick = {
                        scope.launch {
                            app.repo.createDifficultDeck().onSuccess { deck ->
                                deck.id?.let { navController.navigate(Routes.deckDetail(it)) }
                            }
                        }
                    },
                    style = DuoButtonStyle.Danger,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (data.items.isEmpty()) {
                item { EmptyState("Chưa có từ khó nào. Học tiếp để lộ diện các từ hay sai!") }
            } else {
                items(data.items) { item ->
                    DuoCard {
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    item.word,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Spacer(Modifier.height(2.dp))
                                MutedText(item.meaningVi ?: "-")
                                item.deckName?.let {
                                    Spacer(Modifier.height(2.dp))
                                    MutedText(it)
                                }
                            }
                            AudioButton(onClick = { app.speak(item.word) })
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            PillTag("Từ khó", PillKind.Difficult)
                            if (item.wrongCount > 0) {
                                PillTag("Sai ${item.wrongCount}", PillKind.Review)
                            }
                        }
                    }
                }
            }
        }
    }
}
