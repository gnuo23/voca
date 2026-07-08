package com.voca.mobile.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.voca.mobile.data.LearnAnswerResponse
import com.voca.mobile.data.LearnQuestionResponse
import com.voca.mobile.data.LearnSessionResponse
import com.voca.mobile.data.LearnSessionResultResponse
import com.voca.mobile.data.StartLearnSessionRequest
import com.voca.mobile.data.SubmitLearnAnswerRequest
import com.voca.mobile.data.VocaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** The phase a Learn flow is currently in. */
sealed interface LearnPhase {
    data object Config : LearnPhase
    data object Loading : LearnPhase
    data class Error(val message: String) : LearnPhase
    data class Question(val question: LearnQuestionResponse) : LearnPhase
    data class Answered(
        val question: LearnQuestionResponse,
        val answer: LearnAnswerResponse,
        val overriding: Boolean = false,
    ) : LearnPhase
    data class Finished(val result: LearnSessionResultResponse?) : LearnPhase
}

/** Config the user picks before starting a session. */
data class LearnConfig(
    val scope: String = "NOT_MASTERED",
    val gradingMode: String = "FUZZY",
    val mcq: Boolean = true,
    val trueFalse: Boolean = true,
    val written: Boolean = true,
) {
    fun questionTypes(): List<String> = buildList {
        if (mcq) add("MCQ")
        if (trueFalse) add("TRUE_FALSE")
        if (written) add("WRITTEN")
    }.ifEmpty { listOf("MCQ") }
}

class LearnViewModel(
    private val repo: VocaRepository,
    private val deckId: Long,
) : ViewModel() {

    private val _phase = MutableStateFlow<LearnPhase>(LearnPhase.Config)
    val phase: StateFlow<LearnPhase> = _phase.asStateFlow()

    private var sessionId: Long = 0
    private var questionStartedAt: Long = 0L

    var config = LearnConfig()
        private set

    fun updateConfig(update: (LearnConfig) -> LearnConfig) {
        config = update(config)
    }

    fun start() {
        _phase.value = LearnPhase.Loading
        viewModelScope.launch {
            val request = StartLearnSessionRequest(
                deckId = deckId,
                scope = config.scope,
                gradingMode = config.gradingMode,
                questionTypes = config.questionTypes(),
            )
            repo.startLearnSession(request)
                .onSuccess { session: LearnSessionResponse ->
                    sessionId = session.id
                    loadNext()
                }
                .onFailure { _phase.value = LearnPhase.Error(it.message ?: "Không bắt đầu được phiên học") }
        }
    }

    private fun loadNext() {
        _phase.value = LearnPhase.Loading
        viewModelScope.launch {
            repo.nextLearnQuestion(sessionId)
                .onSuccess { q ->
                    if (q.vocabId == null) {
                        loadResult()
                    } else {
                        questionStartedAt = System.currentTimeMillis()
                        _phase.value = LearnPhase.Question(q)
                    }
                }
                .onFailure { _phase.value = LearnPhase.Error(it.message ?: "Lỗi tải câu hỏi") }
        }
    }

    fun submit(answer: String) {
        val current = _phase.value
        val question = (current as? LearnPhase.Question)?.question ?: return
        val sessionItemId = question.sessionItemId ?: return
        val elapsed = (System.currentTimeMillis() - questionStartedAt).coerceAtLeast(0)
        _phase.value = LearnPhase.Loading
        viewModelScope.launch {
            val body = SubmitLearnAnswerRequest(
                sessionItemId = sessionItemId,
                answer = answer,
                questionType = question.questionType ?: "MCQ",
                questionToken = question.questionToken,
                responseTimeMs = elapsed,
            )
            repo.submitLearnAnswer(sessionId, body)
                .onSuccess { _phase.value = LearnPhase.Answered(question, it) }
                .onFailure { _phase.value = LearnPhase.Error(it.message ?: "Lỗi gửi câu trả lời") }
        }
    }

    /** "I was actually right" — flips the last wrong answer to correct on the backend. */
    fun overrideCorrect() {
        val answered = _phase.value as? LearnPhase.Answered ?: return
        val sessionItemId = answered.question.sessionItemId ?: return
        _phase.value = answered.copy(overriding = true)
        viewModelScope.launch {
            repo.overrideLearnAnswer(sessionId, sessionItemId)
                .onSuccess { _phase.value = LearnPhase.Answered(answered.question, it) }
                .onFailure { _phase.value = answered.copy(overriding = false) }
        }
    }

    fun next() = loadNext()

    private fun loadResult() {
        _phase.value = LearnPhase.Loading
        viewModelScope.launch {
            repo.learnSessionResult(sessionId)
                .onSuccess { _phase.value = LearnPhase.Finished(it) }
                .onFailure { _phase.value = LearnPhase.Finished(null) }
        }
    }
}
