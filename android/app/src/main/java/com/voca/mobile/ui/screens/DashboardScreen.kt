package com.voca.mobile.ui.screens

import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.voca.mobile.data.DashboardResponse
import com.voca.mobile.data.DeckProgressResponse
import com.voca.mobile.data.StreakDayResponse
import com.voca.mobile.ui.AppViewModel
import com.voca.mobile.ui.Routes
import com.voca.mobile.ui.components.DuoButton
import com.voca.mobile.ui.components.DuoButtonStyle
import com.voca.mobile.ui.components.DuoCard
import com.voca.mobile.ui.components.Loadable
import com.voca.mobile.ui.components.MutedText
import com.voca.mobile.ui.components.ProgressBarRounded
import com.voca.mobile.ui.components.ScreenList
import com.voca.mobile.ui.components.StatTile
import com.voca.mobile.ui.components.UiState
import com.voca.mobile.ui.components.rememberUiState
import com.voca.mobile.ui.theme.VocaTheme
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope

@Composable
fun DashboardScreen(app: AppViewModel, navController: NavHostController) {
    val state = rememberUiState<DashboardResponse>()
    val scope = rememberCoroutineScope()

    fun load() {
        state.value = UiState.Loading
        scope.launch {
            app.repo.dashboard()
                .onSuccess { state.value = UiState.Success(it) }
                .onFailure { state.value = UiState.Error(it.message ?: "Lỗi tải dashboard") }
        }
    }

    LaunchedEffect(Unit) { load() }

    Loadable(state.value, onRetry = ::load) { data ->
        ScreenList(title = "Xin chào 👋") {
            item { LevelCard(data) }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    StatTile("Cần ôn", data.wordsToReview.toString(), VocaTheme.brand.yellow, Modifier.weight(1f))
                    StatTile("Học hôm nay", data.wordsLearnedToday.toString(), MaterialTheme.colorScheme.primary, Modifier.weight(1f))
                    StatTile("Streak", data.streakDays.toString(), VocaTheme.brand.streak, Modifier.weight(1f))
                }
            }
            item { StreakWeekCard(data.streakWeek) }
            item {
                DuoButton(
                    "Ôn tập ${data.wordsToReview} từ",
                    onClick = { navController.navigate(Routes.REVIEW) },
                    style = DuoButtonStyle.Blue,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            item {
                DuoButton(
                    "Học từ vựng",
                    onClick = { navController.navigate(Routes.DECKS) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            item {
                Text(
                    "Tiến độ bộ thẻ",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(top = 12.dp),
                )
            }
            if (data.deckProgress.isEmpty()) {
                item { MutedText("Chưa có dữ liệu. Tạo bộ thẻ đầu tiên nhé!") }
            } else {
                items(data.deckProgress) { deck ->
                    DeckProgressCard(deck) {
                        deck.deckId?.let { navController.navigate(Routes.deckDetail(it)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun LevelCard(data: DashboardResponse) {
    val level = data.level
    DuoCard {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Level ${level?.level ?: 1}",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                MutedText("${level?.xp ?: 0} XP · độ chính xác ${(data.accuracy * 100).toInt()}%")
            }
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center,
            ) {
                Text("⭐", style = MaterialTheme.typography.headlineMedium)
            }
        }
        Spacer(Modifier.height(12.dp))
        ProgressBarRounded(progress = (level?.progressPercent ?: 0) / 100f)
        Spacer(Modifier.height(4.dp))
        MutedText("Còn ${((level?.xpForNextLevel ?: 0) - (level?.xp ?: 0)).coerceAtLeast(0)} XP đến level tiếp theo")
    }
}

@Composable
private fun StreakWeekCard(week: List<StreakDayResponse>) {
    DuoCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.LocalFireDepartment, contentDescription = null, tint = VocaTheme.brand.streak)
            Spacer(Modifier.size(8.dp))
            Text("Chuỗi ngày học", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
        }
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            if (week.isEmpty()) {
                MutedText("Học hôm nay để bắt đầu chuỗi!")
            } else {
                week.forEach { day -> StreakDot(day) }
            }
        }
    }
}

@Composable
private fun StreakDot(day: StreakDayResponse) {
    val brand = VocaTheme.brand
    val bg = when {
        day.active -> brand.streak
        day.today -> brand.streak.copy(alpha = 0.25f)
        else -> brand.border
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(bg),
            contentAlignment = Alignment.Center,
        ) {
            if (day.active) {
                Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            day.label.take(2),
            style = MaterialTheme.typography.labelMedium,
            color = if (day.today) brand.streak else brand.muted,
            fontWeight = if (day.today) FontWeight.ExtraBold else FontWeight.Bold,
        )
    }
}

@Composable
private fun DeckProgressCard(deck: DeckProgressResponse, onClick: () -> Unit) {
    DuoCard {
        Text(deck.deckName, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.height(4.dp))
        MutedText("${deck.masteredCount}/${deck.totalWords} đã thuộc · ${deck.difficultCount} từ khó")
        Spacer(Modifier.height(10.dp))
        val ratio = if (deck.totalWords > 0) deck.masteredCount.toFloat() / deck.totalWords else 0f
        ProgressBarRounded(progress = ratio)
        Spacer(Modifier.height(12.dp))
        DuoButton("Mở bộ thẻ", onClick, style = DuoButtonStyle.Secondary, modifier = Modifier.fillMaxWidth())
    }
}
