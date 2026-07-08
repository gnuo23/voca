package com.voca.mobile.data

import kotlinx.serialization.Serializable

// ---------- Auth / user ----------

@Serializable
data class AuthResponse(
    val token: String,
    val tokenType: String? = null,
    val user: UserResponse? = null,
)

@Serializable
data class UserResponse(
    val id: Long? = null,
    val email: String = "",
    val displayName: String = "",
    val englishLevel: String? = null,
    val learningGoal: String? = null,
    val dailyGoal: Int? = null,
)

@Serializable
data class LoginRequest(val email: String, val password: String)

@Serializable
data class RegisterRequest(
    val email: String,
    val password: String,
    val displayName: String,
    val englishLevel: String = "BEGINNER",
    val learningGoal: String = "",
    val dailyGoal: Int = 10,
)

// ---------- Dashboard ----------

@Serializable
data class DashboardResponse(
    val wordsLearnedToday: Long = 0,
    val wordsReviewedToday: Long = 0,
    val wordsToReview: Long = 0,
    val overdueWords: Long = 0,
    val accuracy: Double = 0.0,
    val level: LearningLevelResponse? = null,
    val streakDays: Int = 0,
    val streakActiveToday: Boolean = false,
    val streakWeek: List<StreakDayResponse> = emptyList(),
    val deckProgress: List<DeckProgressResponse> = emptyList(),
)

@Serializable
data class LearningLevelResponse(
    val level: Int = 1,
    val xp: Long = 0,
    val xpForCurrentLevel: Long = 0,
    val xpForNextLevel: Long = 0,
    val progressPercent: Int = 0,
)

@Serializable
data class StreakDayResponse(
    val label: String = "",
    val date: String? = null,
    val active: Boolean = false,
    val today: Boolean = false,
)

@Serializable
data class DeckProgressResponse(
    val deckId: Long? = null,
    val deckName: String = "",
    val totalWords: Long = 0,
    val newCount: Long = 0,
    val learningCount: Long = 0,
    val reviewCount: Long = 0,
    val difficultCount: Long = 0,
    val masteredCount: Long = 0,
    val progressScore: Double = 0.0,
)

// ---------- Decks ----------

@Serializable
data class DeckResponse(
    val id: Long? = null,
    val name: String = "",
    val description: String? = null,
    val totalWords: Int = 0,
    val learnedWords: Int = 0,
    val dueWords: Int = 0,
    val dueTodayCount: Int = 0,
    val savedQuestionCount: Int = 0,
    val ownerId: Long? = null,
    val ownerName: String? = null,
    val ownedByCurrentUser: Boolean = false,
    val difficultDeck: Boolean = false,
)

@Serializable
data class DeckRequest(val name: String, val description: String = "")

// ---------- Vocab ----------

@Serializable
data class VocabItemResponse(
    val id: Long? = null,
    val deckId: Long? = null,
    val word: String = "",
    val partOfSpeech: String? = null,
    val meaningVi: String? = null,
    val ipa: String? = null,
    val pronunciationHint: String? = null,
    val exampleEn: String? = null,
    val exampleVi: String? = null,
    val topic: String? = null,
    val level: String? = null,
    val synonyms: List<String> = emptyList(),
    val antonyms: List<String> = emptyList(),
    val collocations: List<String> = emptyList(),
    val audioUrl: String? = null,
    val audioUsUrl: String? = null,
    val audioUkUrl: String? = null,
    val progressStatus: String = "NEW",
    val knownCount: Int = 0,
    val unknownCount: Int = 0,
    val difficultCount: Int = 0,
)

@Serializable
data class ImportConfirmRequest(val deckId: Long, val rawText: String)

@Serializable
data class VocabImportConfirmResponse(
    val importedCount: Int = 0,
    val skippedCount: Int = 0,
)

// ---------- Learn ----------

@Serializable
data class StartLearnSessionRequest(
    val deckId: Long,
    val scope: String = "NOT_MASTERED",
    val goal: String = "MASTER_ALL",
    val answerDirection: String = "BOTH",
    val gradingMode: String = "FUZZY",
    val questionTypes: List<String> = listOf("MCQ", "WRITTEN"),
)

@Serializable
data class LearnSessionResponse(
    val id: Long,
    val deckId: Long? = null,
    val deckName: String? = null,
    val totalTerms: Int = 0,
    val masteredTerms: Int = 0,
    val totalAnswers: Int = 0,
    val correctAnswers: Int = 0,
    val scope: String? = null,
    val goal: String? = null,
    val status: String? = null,
    val durationMs: Long = 0,
)

@Serializable
data class LearnProgress(
    val masteredTerms: Int = 0,
    val totalTerms: Int = 0,
    val remainingTerms: Int = 0,
    val newTerms: Int = 0,
    val seenTerms: Int = 0,
    val learningTerms: Int = 0,
    val familiarTerms: Int = 0,
)

@Serializable
data class VocabContext(
    val ipa: String? = null,
    val meaningVi: String? = null,
    val partOfSpeech: String? = null,
    val exampleEn: String? = null,
    val exampleVi: String? = null,
)

@Serializable
data class LearnQuestionResponse(
    val sessionItemId: Long? = null,
    val vocabId: Long? = null,
    val word: String? = null,
    val questionType: String? = null,
    val questionToken: String? = null,
    val prompt: String? = null,
    val options: List<String>? = null,
    val trueFalseStatement: String? = null,
    val hint: String? = null,
    val stage: String? = null,
    val vocab: VocabContext? = null,
    val progress: LearnProgress? = null,
)

@Serializable
data class SubmitLearnAnswerRequest(
    val sessionItemId: Long,
    val answer: String,
    val questionType: String,
    val questionToken: String? = null,
    val responseTimeMs: Long? = null,
)

@Serializable
data class LearnAnswerResponse(
    val correct: Boolean = false,
    val verdict: String? = null,
    val similarityScore: Double = 0.0,
    val userAnswer: String? = null,
    val correctAnswer: String? = null,
    val newStage: String? = null,
    val correctStreak: Int = 0,
    val vocab: VocabContext? = null,
    val progress: LearnProgress? = null,
)

@Serializable
data class OverrideAnswerRequest(val sessionItemId: Long)

@Serializable
data class LearnSessionResultResponse(
    val session: LearnSessionResponse? = null,
    val items: List<ItemSummary> = emptyList(),
    val history: List<AnswerSummary> = emptyList(),
) {
    @Serializable
    data class ItemSummary(
        val vocabId: Long? = null,
        val word: String = "",
        val partOfSpeech: String? = null,
        val meaningVi: String? = null,
        val stage: String? = null,
        val correctAttempts: Int = 0,
        val incorrectAttempts: Int = 0,
        val totalAttempts: Int = 0,
    )

    @Serializable
    data class AnswerSummary(
        val questionType: String? = null,
        val prompt: String? = null,
        val userAnswer: String? = null,
        val correctAnswer: String? = null,
        val correct: Boolean = false,
        val verdict: String? = null,
    )
}

// ---------- Review ----------

@Serializable
data class ReviewTodayResponse(
    val items: List<ReviewItemResponse> = emptyList(),
    val totalDue: Long = 0,
)

@Serializable
data class ReviewItemResponse(
    val vocabId: Long? = null,
    val deckId: Long? = null,
    val word: String = "",
    val partOfSpeech: String? = null,
    val meaningVi: String? = null,
    val ipaUs: String? = null,
    val exampleEn: String? = null,
    val exampleVi: String? = null,
    val status: String = "NEW",
    val wrongCount: Int = 0,
    val correctCount: Int = 0,
    val lapseCount: Int = 0,
)

@Serializable
data class ReviewScheduleResponse(
    val items: List<ReviewScheduleItemResponse> = emptyList(),
    val totalItems: Long = 0,
    val dueNow: Long = 0,
    val overdue: Long = 0,
    val upcoming: Long = 0,
    val newItems: Long = 0,
)

@Serializable
data class ReviewScheduleItemResponse(
    val vocabId: Long? = null,
    val deckId: Long? = null,
    val deckName: String? = null,
    val word: String = "",
    val partOfSpeech: String? = null,
    val meaningVi: String? = null,
    val status: String = "NEW",
    val bucket: String? = null,
    val correctCount: Int = 0,
    val wrongCount: Int = 0,
    val lapseCount: Int = 0,
    val repetitionCount: Int = 0,
)

@Serializable
data class SubmitReviewRequest(
    val quality: String,
    val source: String = "FLASHCARD",
    val responseTimeMs: Long? = null,
)

// ---------- Classes ----------

@Serializable
data class ClassroomResponse(
    val id: Long? = null,
    val name: String = "",
    val description: String? = null,
    val inviteCode: String? = null,
    val role: String? = null,
    val deckCount: Long = 0,
    val memberCount: Long = 0,
    val totalWords: Long = 0,
    val learnedWords: Long = 0,
)

@Serializable
data class ClassroomRequest(val name: String, val description: String = "")

@Serializable
data class JoinClassroomRequest(val code: String)
