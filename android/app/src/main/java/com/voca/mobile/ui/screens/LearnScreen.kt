package com.voca.mobile.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.voca.mobile.data.LearnAnswerResponse
import com.voca.mobile.data.LearnQuestionResponse
import com.voca.mobile.data.LearnSessionResultResponse
import com.voca.mobile.data.VocaRepository
import com.voca.mobile.ui.AppViewModel
import com.voca.mobile.ui.components.CelebrationOverlay
import com.voca.mobile.ui.components.DuoButton
import com.voca.mobile.ui.components.DuoButtonStyle
import com.voca.mobile.ui.components.DuoCard
import com.voca.mobile.ui.components.MutedText
import com.voca.mobile.ui.components.ProgressBarRounded
import com.voca.mobile.ui.components.ScreenColumn
import com.voca.mobile.ui.theme.VocaTheme

@Composable
fun LearnScreen(app: AppViewModel, navController: NavHostController, deckId: Long) {
    val vm: LearnViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                LearnViewModel(app.repo, deckId) as T
        },
    )
    val phase by vm.phase.collectAsStateWithLifecycle()

    when (val p = phase) {
        is LearnPhase.Config -> ConfigView(vm)
        is LearnPhase.Loading -> com.voca.mobile.ui.components.LoadingState()
        is LearnPhase.Error -> com.voca.mobile.ui.components.ErrorState(p.message, onRetry = { vm.start() })
        is LearnPhase.Question -> QuestionView(p.question, app, onSubmit = vm::submit)
        is LearnPhase.Answered -> AnsweredView(p, app, onNext = vm::next, onOverride = vm::overrideCorrect)
        is LearnPhase.Finished -> FinishedView(p.result, onDone = { navController.popBackStack() })
    }
}

@Composable
private fun ConfigView(vm: LearnViewModel) {
    var config by remember { mutableStateOf(vm.config) }

    ScreenColumn(title = "Thiết lập phiên học") {
        DuoCard {
            Text("Phạm vi", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.height(8.dp))
            val scopes = listOf(
                "NOT_MASTERED" to "Chưa thuộc",
                "ALL" to "Tất cả",
                "DIFFICULT_ONLY" to "Chỉ từ khó",
                "NEW_ONLY" to "Chỉ từ mới",
            )
            scopes.forEach { (value, label) ->
                ChoiceRow(label, selected = config.scope == value) {
                    config = config.copy(scope = value)
                }
            }
        }
        DuoCard {
            Text("Loại câu hỏi", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.height(8.dp))
            ToggleRow("Trắc nghiệm (MCQ)", config.mcq) { config = config.copy(mcq = it) }
            ToggleRow("Đúng / Sai", config.trueFalse) { config = config.copy(trueFalse = it) }
            ToggleRow("Tự luận (gõ đáp án)", config.written) { config = config.copy(written = it) }
        }
        DuoCard {
            Text("Cách chấm", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.height(8.dp))
            val modes = listOf(
                "FUZZY" to "Linh hoạt (chấp nhận gần đúng)",
                "ACCENT_INSENSITIVE" to "Bỏ qua dấu",
                "EXACT" to "Chính xác tuyệt đối",
            )
            modes.forEach { (value, label) ->
                ChoiceRow(label, selected = config.gradingMode == value) {
                    config = config.copy(gradingMode = value)
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        DuoButton(
            "Bắt đầu học",
            onClick = {
                vm.updateConfig { config }
                vm.start()
            },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun ChoiceRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val style = if (selected) DuoButtonStyle.Primary else DuoButtonStyle.Secondary
        DuoButton(label, onClick = onClick, style = style, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun QuestionView(
    question: LearnQuestionResponse,
    app: AppViewModel,
    onSubmit: (String) -> Unit,
) {
    ScreenColumn(title = null) {
        question.progress?.let { p ->
            ProgressBarRounded(progress = if (p.totalTerms > 0) p.masteredTerms.toFloat() / p.totalTerms else 0f)
            Spacer(Modifier.height(4.dp))
            MutedText("${p.masteredTerms}/${p.totalTerms} đã thuộc")
        }
        Spacer(Modifier.height(20.dp))
        Text(
            question.prompt ?: "",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        if (!question.word.isNullOrBlank()) {
            Spacer(Modifier.height(8.dp))
            DuoButton("🔊 Nghe", onClick = { app.speak(question.word) }, style = DuoButtonStyle.Secondary)
        }
        Spacer(Modifier.height(20.dp))

        when (question.questionType) {
            "MCQ" -> {
                question.options?.forEach { option ->
                    DuoButton(option, onClick = { onSubmit(option) }, style = DuoButtonStyle.Secondary, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(8.dp))
                }
                DuoButton("Chưa biết", onClick = { onSubmit("") }, style = DuoButtonStyle.Secondary, modifier = Modifier.fillMaxWidth())
            }
            "TRUE_FALSE" -> {
                question.trueFalseStatement?.let {
                    DuoCard { Text(it, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface) }
                    Spacer(Modifier.height(12.dp))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    DuoButton("Đúng", onClick = { onSubmit("true") }, modifier = Modifier.weight(1f))
                    DuoButton("Sai", onClick = { onSubmit("false") }, style = DuoButtonStyle.Danger, modifier = Modifier.weight(1f))
                }
            }
            else -> {
                var answer by remember(question.sessionItemId) { mutableStateOf("") }
                OutlinedTextField(
                    value = answer,
                    onValueChange = { answer = it },
                    label = { Text(question.hint ?: "Nhập đáp án") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                Spacer(Modifier.height(12.dp))
                DuoButton("Trả lời", onClick = { onSubmit(answer) }, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun AnsweredView(
    phase: LearnPhase.Answered,
    app: AppViewModel,
    onNext: () -> Unit,
    onOverride: () -> Unit,
) {
    val answer = phase.answer
    val brand = VocaTheme.brand
    // CLOSE verdict is surfaced distinctly (yellow) instead of collapsing to right/wrong.
    val isClose = answer.verdict == "CLOSE"
    val (title, color) = when {
        answer.correct && isClose -> "Gần đúng!" to brand.close
        answer.correct -> "Chính xác!" to brand.success
        else -> "Chưa đúng" to brand.danger
    }

    ScreenColumn(title = null) {
        Spacer(Modifier.height(20.dp))
        Text(
            title,
            style = MaterialTheme.typography.headlineLarge,
            color = color,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(16.dp))
        DuoCard {
            MutedText("Đáp án đúng")
            Spacer(Modifier.height(4.dp))
            Text(answer.correctAnswer ?: "-", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
            if (!answer.userAnswer.isNullOrBlank()) {
                Spacer(Modifier.height(8.dp))
                MutedText("Bạn trả lời: ${answer.userAnswer}")
            }
            answer.vocab?.exampleEn?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                answer.vocab.exampleVi?.let { vi -> MutedText(vi) }
            }
        }
        Spacer(Modifier.height(12.dp))

        // "I was actually right" — only meaningful when marked wrong or close.
        if (!answer.correct || isClose) {
            DuoButton(
                if (phase.overriding) "Đang cập nhật..." else "Tôi đúng mà",
                onClick = onOverride,
                style = DuoButtonStyle.Secondary,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
        }
        DuoButton("Câu tiếp theo", onClick = onNext, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun FinishedView(result: LearnSessionResultResponse?, onDone: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize()) {
        ScreenColumn(title = null) {
            Spacer(Modifier.height(40.dp))
            Text("🎉", style = MaterialTheme.typography.displayLarge, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
            Text(
                "Hoàn thành!",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(16.dp))
            val session = result?.session
            if (session != null) {
                DuoCard {
                    StatLine("Số từ đã thuộc", "${session.masteredTerms}/${session.totalTerms}")
                    StatLine("Số câu đúng", "${session.correctAnswers}/${session.totalAnswers}")
                    val acc = if (session.totalAnswers > 0) session.correctAnswers * 100 / session.totalAnswers else 0
                    StatLine("Độ chính xác", "$acc%")
                }
            }
            Spacer(Modifier.height(16.dp))
            DuoButton("Xong", onClick = onDone, modifier = Modifier.fillMaxWidth())
        }
        CelebrationOverlay(modifier = Modifier.fillMaxSize())
    }
}

@Composable
private fun StatLine(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        MutedText(label, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
    }
}
