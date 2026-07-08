package com.voca.mobile.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.voca.mobile.data.ClassroomResponse
import com.voca.mobile.ui.AppViewModel
import com.voca.mobile.ui.components.DuoButton
import com.voca.mobile.ui.components.DuoButtonStyle
import com.voca.mobile.ui.components.DuoCard
import com.voca.mobile.ui.components.EmptyState
import com.voca.mobile.ui.components.Loadable
import com.voca.mobile.ui.components.MutedText
import com.voca.mobile.ui.components.ScreenList
import com.voca.mobile.ui.components.UiState
import com.voca.mobile.ui.components.rememberUiState
import com.voca.mobile.ui.theme.VocaTheme
import kotlinx.coroutines.launch

@Composable
fun ClassesScreen(app: AppViewModel, navController: NavHostController) {
    val state = rememberUiState<List<ClassroomResponse>>()
    val scope = rememberCoroutineScope()
    var showCreate by remember { mutableStateOf(false) }
    var showJoin by remember { mutableStateOf(false) }

    fun load() {
        state.value = UiState.Loading
        scope.launch {
            app.repo.classes()
                .onSuccess { state.value = UiState.Success(it) }
                .onFailure { state.value = UiState.Error(it.message ?: "Lỗi tải lớp") }
        }
    }

    LaunchedEffect(Unit) { load() }

    Loadable(state.value, onRetry = ::load) { classes ->
        ScreenList(title = "Lớp học") {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    DuoButton("Tạo lớp", onClick = { showCreate = true }, modifier = Modifier.weight(1f))
                    DuoButton(
                        "Nhập mã",
                        onClick = { showJoin = true },
                        style = DuoButtonStyle.Blue,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            if (classes.isEmpty()) {
                item { EmptyState("Chưa có lớp nào. Tạo lớp hoặc nhập mã để tham gia!") }
            } else {
                items(classes) { c -> ClassCard(c) }
            }
        }
    }

    if (showCreate) {
        NameDescDialog(
            title = "Tạo lớp",
            onDismiss = { showCreate = false },
            onConfirm = { name, desc ->
                showCreate = false
                scope.launch { app.repo.createClass(name, desc).onSuccess { load() } }
            },
        )
    }

    if (showJoin) {
        JoinDialog(
            onDismiss = { showJoin = false },
            onJoin = { code ->
                showJoin = false
                scope.launch { app.repo.joinClass(code).onSuccess { load() } }
            },
        )
    }
}

@Composable
private fun ClassCard(c: ClassroomResponse) {
    DuoCard {
        Text(c.name, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.height(4.dp))
        MutedText("${c.role ?: ""} · ${c.deckCount} bộ thẻ · ${c.memberCount} thành viên")
        c.inviteCode?.let { code ->
            Spacer(Modifier.height(10.dp))
            Row {
                Text(
                    "Mã: $code",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                    color = VocaTheme.brand.blue,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(VocaTheme.brand.blue.copy(alpha = 0.12f))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                )
            }
        }
    }
}

@Composable
private fun NameDescDialog(title: String, onDismiss: () -> Unit, onConfirm: (String, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Tên lớp") },
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
            TextButton(onClick = { if (name.isNotBlank()) onConfirm(name, description) }) { Text("Tạo") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Huỷ") } },
    )
}

@Composable
private fun JoinDialog(onDismiss: () -> Unit, onJoin: (String) -> Unit) {
    var code by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nhập mã lớp", style = MaterialTheme.typography.titleLarge) },
        text = {
            OutlinedTextField(
                value = code,
                onValueChange = { code = it },
                label = { Text("Mã lớp") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(onClick = { if (code.isNotBlank()) onJoin(code) }) { Text("Tham gia") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Huỷ") } },
    )
}
