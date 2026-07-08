package com.voca.mobile.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.voca.mobile.data.ReviewItemResponse
import com.voca.mobile.ui.AppViewModel
import com.voca.mobile.ui.components.AudioButton
import com.voca.mobile.ui.components.CelebrationOverlay
import com.voca.mobile.ui.components.DuoButton
import com.voca.mobile.ui.components.DuoButtonStyle
import com.voca.mobile.ui.components.DuoCard
import com.voca.mobile.ui.components.EmptyState
import com.voca.mobile.ui.components.Loadable
import com.voca.mobile.ui.components.MutedText
import com.voca.mobile.ui.components.ProgressBarRounded
import com.voca.mobile.ui.components.UiState
import com.voca.mobile.ui.components.rememberUiState
import com.voca.mobile.ui.theme.Spacing
import com.voca.mobile.ui.theme.VocaTheme
import kotlinx.coroutines.launch

@Composable
fun ReviewScreen(app: AppViewModel, navController: NavHostController, deckId: Long? = null) {
    val state = rememberUiState<List<ReviewItemResponse>>()
    val scope = rememberCoroutineScope()
    var index by remember { mutableStateOf(0) }
    var revealed by remember { mutableStateOf(false) }
    var cardShownAt by remember { mutableStateOf(0L) }

    fun load() {
        state.value = UiState.Loading
        index = 0
        revealed = false
        scope.launch {
            app.repo.reviewToday(deckId)
                .onSuccess { state.value = UiState.Success(it.items) }
                .onFailure { state.value = UiState.Error(it.message ?: "Lỗi tải ôn tập") }
        }
    }

    LaunchedEffect(deckId) { load() }

    Loadable(state.value, onRetry = ::load) { items ->
        when {
            items.isEmpty() -> EmptyState("Chưa có từ cần ôn. Quay lại sau nhé!")
            index >= items.size -> ReviewDone(onHome = { navController.popBackStack() })
            else -> {
                val item = items[index]
                LaunchedEffect(index) {
                    revealed = false
                    cardShownAt = System.currentTimeMillis()
                }
                ReviewCard(
                    item = item,
                    position = index + 1,
                    total = items.size,
                    revealed = revealed,
                    onReveal = { revealed = true },
                    onSpeak = { app.speak(item.word) },
                    onGrade = { quality ->
                        val elapsed = (System.currentTimeMillis() - cardShownAt).coerceAtLeast(0)
                        val vocabId = item.vocabId
                        if (vocabId != null) {
                            scope.launch {
                                app.repo.submitReview(vocabId, quality, elapsed)
                            }
                        }
                        index += 1
                    },
                )
            }
        }
    }
}

@Composable
private fun ReviewCard(
    item: ReviewItemResponse,
    position: Int,
    total: Int,
    revealed: Boolean,
    onReveal: () -> Unit,
    onSpeak: () -> Unit,
    onGrade: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ProgressBarRounded(progress = position.toFloat() / total)
        MutedText("$position/$total")

        DuoCard(modifier = Modifier.weight(1f)) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    item.word,
                    style = MaterialTheme.typography.displayLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                )
                if (!item.ipaUs.isNullOrBlank()) {
                    Spacer(Modifier.height(6.dp))
                    MutedText(item.ipaUs)
                }
                Spacer(Modifier.height(12.dp))
                AudioButton(onClick = onSpeak)

                if (revealed) {
                    Spacer(Modifier.height(20.dp))
                    Text(
                        item.meaningVi ?: "-",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center,
                    )
                    if (!item.exampleEn.isNullOrBlank()) {
                        Spacer(Modifier.height(10.dp))
                        Text(
                            item.exampleEn,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }

        if (revealed) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                DuoButton("Lại", onClick = { onGrade("AGAIN") }, style = DuoButtonStyle.Danger, modifier = Modifier.weight(1f))
                DuoButton("Khó", onClick = { onGrade("HARD") }, style = DuoButtonStyle.Secondary, modifier = Modifier.weight(1f))
                DuoButton("Tốt", onClick = { onGrade("GOOD") }, style = DuoButtonStyle.Blue, modifier = Modifier.weight(1f))
                DuoButton("Dễ", onClick = { onGrade("EASY") }, modifier = Modifier.weight(1f))
            }
        } else {
            DuoButton("Hiện đáp án", onClick = onReveal, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun ReviewDone(onHome: () -> Unit) {
    val brand = VocaTheme.brand
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(Spacing.xl),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(brand.success.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.TaskAlt,
                    contentDescription = null,
                    tint = brand.success,
                    modifier = Modifier.size(56.dp),
                )
            }
            Spacer(Modifier.height(Spacing.lg))
            Text(
                "Đã ôn hết!",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(Spacing.xs))
            MutedText("Bạn đã hoàn thành danh sách ôn tập hôm nay.")
            Spacer(Modifier.height(Spacing.xl))
            DuoButton("Xong", onClick = onHome, modifier = Modifier.fillMaxWidth())
        }
        CelebrationOverlay()
    }
}
