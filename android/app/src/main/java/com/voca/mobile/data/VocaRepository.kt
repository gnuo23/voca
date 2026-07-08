package com.voca.mobile.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException

/** Extracts a human-readable message from a failed HTTP call, falling back to the exception text. */
private fun Throwable.friendlyMessage(): String {
    if (this is HttpException) {
        val body = response()?.errorBody()?.string().orEmpty()
        val parsed = runCatching {
            NetworkModule.json.parseToJsonElement(body)
        }.getOrNull()
        val message = runCatching {
            (parsed as? kotlinx.serialization.json.JsonObject)
                ?.get("message")
                ?.let { (it as? kotlinx.serialization.json.JsonPrimitive)?.content }
        }.getOrNull()
        if (!message.isNullOrBlank()) return message
        return "HTTP ${code()}"
    }
    return message ?: "Đã có lỗi xảy ra"
}

private suspend fun <T> call(block: suspend ApiService.() -> T): Result<T> =
    withContext(Dispatchers.IO) {
        runCatching { NetworkModule.api.block() }
            .recoverCatching { throw RepositoryException(it.friendlyMessage(), it) }
    }

class RepositoryException(message: String, cause: Throwable) : Exception(message, cause)

class VocaRepository {

    // Auth
    suspend fun login(email: String, password: String) =
        call { login(LoginRequest(email.trim(), password)) }

    suspend fun register(email: String, password: String, displayName: String) =
        call { register(RegisterRequest(email.trim(), password, displayName.ifBlank { "Voca User" })) }

    suspend fun me() = call { me() }

    // Dashboard
    suspend fun dashboard() = call { dashboard() }

    // Decks
    suspend fun decks() = call { decks() }
    suspend fun difficultDecks() = call { difficultDecks() }
    suspend fun createDifficultDeck() = call { createDifficultDeck() }
    suspend fun deck(id: Long) = call { deck(id) }
    suspend fun createDeck(name: String, description: String) =
        call { createDeck(DeckRequest(name, description)) }
    suspend fun resetDeckProgress(id: Long) = call { resetDeckProgress(id) }

    // Vocab
    suspend fun vocab(deckId: Long) = call { vocab(deckId) }
    suspend fun vocabItem(id: Long) = call { vocabItem(id) }
    suspend fun importVocab(deckId: Long, rawText: String) =
        call { importVocab(ImportConfirmRequest(deckId, rawText)) }

    // Learn
    suspend fun activeLearnSession(deckId: Long) = call { activeLearnSession(deckId) }
    suspend fun startLearnSession(request: StartLearnSessionRequest) =
        call { startLearnSession(request) }
    suspend fun nextLearnQuestion(sessionId: Long) = call { nextLearnQuestion(sessionId) }
    suspend fun submitLearnAnswer(sessionId: Long, body: SubmitLearnAnswerRequest) =
        call { submitLearnAnswer(sessionId, body) }
    suspend fun overrideLearnAnswer(sessionId: Long, sessionItemId: Long) =
        call { overrideLearnAnswer(sessionId, OverrideAnswerRequest(sessionItemId)) }
    suspend fun learnSessionResult(sessionId: Long) = call { learnSessionResult(sessionId) }

    // Review
    suspend fun reviewToday(deckId: Long? = null) = call { reviewToday(deckId = deckId) }
    suspend fun reviewSchedule(status: String? = null) = call { reviewSchedule(status = status) }
    suspend fun submitReview(vocabId: Long, quality: String, responseTimeMs: Long?) =
        call { submitReview(vocabId, SubmitReviewRequest(quality, responseTimeMs = responseTimeMs)) }

    // Classes
    suspend fun classes() = call { classes() }
    suspend fun createClass(name: String, description: String) =
        call { createClass(ClassroomRequest(name, description)) }
    suspend fun joinClass(code: String) = call { joinClass(JoinClassroomRequest(code.trim())) }
}
