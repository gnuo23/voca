package com.voca.mobile.data

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {

    // Auth
    @POST("/api/auth/login")
    suspend fun login(@Body body: LoginRequest): AuthResponse

    @POST("/api/auth/register")
    suspend fun register(@Body body: RegisterRequest): AuthResponse

    @GET("/api/auth/me")
    suspend fun me(): UserResponse

    // Dashboard
    @GET("/api/dashboard")
    suspend fun dashboard(): DashboardResponse

    // Decks
    @GET("/api/decks")
    suspend fun decks(): List<DeckResponse>

    @GET("/api/decks/difficult")
    suspend fun difficultDecks(): List<DeckResponse>

    @POST("/api/decks/difficult")
    suspend fun createDifficultDeck(): DeckResponse

    @GET("/api/decks/{id}")
    suspend fun deck(@Path("id") id: Long): DeckResponse

    @POST("/api/decks")
    suspend fun createDeck(@Body body: DeckRequest): DeckResponse

    @POST("/api/decks/{id}/reset-progress")
    suspend fun resetDeckProgress(@Path("id") id: Long): DeckResponse

    // Vocab
    @GET("/api/decks/{id}/vocab")
    suspend fun vocab(@Path("id") deckId: Long): List<VocabItemResponse>

    @GET("/api/vocab/{id}")
    suspend fun vocabItem(@Path("id") id: Long): VocabItemResponse

    @POST("/api/vocab/import/confirm")
    suspend fun importVocab(@Body body: ImportConfirmRequest): VocabImportConfirmResponse

    // Learn
    @GET("/api/learn/sessions/active")
    suspend fun activeLearnSession(@Query("deckId") deckId: Long): LearnSessionResponse

    @POST("/api/learn/sessions")
    suspend fun startLearnSession(@Body body: StartLearnSessionRequest): LearnSessionResponse

    @GET("/api/learn/sessions/{id}/next")
    suspend fun nextLearnQuestion(@Path("id") sessionId: Long): LearnQuestionResponse

    @POST("/api/learn/sessions/{id}/answer")
    suspend fun submitLearnAnswer(
        @Path("id") sessionId: Long,
        @Body body: SubmitLearnAnswerRequest,
    ): LearnAnswerResponse

    @POST("/api/learn/sessions/{id}/override")
    suspend fun overrideLearnAnswer(
        @Path("id") sessionId: Long,
        @Body body: OverrideAnswerRequest,
    ): LearnAnswerResponse

    @GET("/api/learn/sessions/{id}/result")
    suspend fun learnSessionResult(@Path("id") sessionId: Long): LearnSessionResultResponse

    // Review
    @GET("/api/review/today")
    suspend fun reviewToday(
        @Query("limit") limit: Int = 30,
        @Query("deckId") deckId: Long? = null,
    ): ReviewTodayResponse

    @GET("/api/review/schedule")
    suspend fun reviewSchedule(
        @Query("status") status: String? = null,
        @Query("limit") limit: Int = 100,
    ): ReviewScheduleResponse

    @POST("/api/review/{vocabId}/result")
    suspend fun submitReview(
        @Path("vocabId") vocabId: Long,
        @Body body: SubmitReviewRequest,
    ): retrofit2.Response<Unit>

    // Classes
    @GET("/api/classes")
    suspend fun classes(): List<ClassroomResponse>

    @POST("/api/classes")
    suspend fun createClass(@Body body: ClassroomRequest): ClassroomResponse

    @POST("/api/classes/join")
    suspend fun joinClass(@Body body: JoinClassroomRequest): ClassroomResponse
}
