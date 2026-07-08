package com.voca.mobile.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.voca.mobile.data.UserResponse
import com.voca.mobile.ui.AppViewModel
import com.voca.mobile.ui.components.DuoButton
import com.voca.mobile.ui.components.DuoButtonStyle
import com.voca.mobile.ui.components.DuoCard
import com.voca.mobile.ui.components.Loadable
import com.voca.mobile.ui.components.MutedText
import com.voca.mobile.ui.components.ScreenColumn
import com.voca.mobile.ui.components.UiState
import com.voca.mobile.ui.components.rememberUiState
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope

@Composable
fun ProfileScreen(app: AppViewModel, navController: NavHostController) {
    val state = rememberUiState<UserResponse>()
    val scope = rememberCoroutineScope()

    fun load() {
        state.value = UiState.Loading
        scope.launch {
            app.repo.me()
                .onSuccess { state.value = UiState.Success(it) }
                .onFailure { state.value = UiState.Error(it.message ?: "Lỗi tải hồ sơ") }
        }
    }

    LaunchedEffect(Unit) { load() }

    Loadable(state.value, onRetry = ::load) { user ->
        ScreenColumn(title = "Hồ sơ") {
            DuoCard {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        user.displayName.take(1).uppercase().ifBlank { "V" },
                        style = MaterialTheme.typography.displayLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    user.displayName.ifBlank { "Voca User" },
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(4.dp))
                MutedText(user.email)
            }
            DuoCard {
                InfoRow("Trình độ", user.englishLevel ?: "-")
                Spacer(Modifier.height(8.dp))
                InfoRow("Mục tiêu ngày", "${user.dailyGoal ?: 0} từ")
                user.learningGoal?.takeIf { it.isNotBlank() }?.let {
                    Spacer(Modifier.height(8.dp))
                    InfoRow("Mục tiêu học", it)
                }
            }
            Spacer(Modifier.height(8.dp))
            DuoButton(
                "Đăng xuất",
                onClick = { app.logout() },
                style = DuoButtonStyle.Danger,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Column {
        MutedText(label)
        Text(value, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
    }
}
