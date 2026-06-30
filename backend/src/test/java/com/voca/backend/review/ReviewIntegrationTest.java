package com.voca.backend.review;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.voca.backend.auth.RegisterRequest;
import com.voca.backend.deck.DeckRepository;
import com.voca.backend.deck.DeckRequest;
import com.voca.backend.quiz.QuestionRepository;
import com.voca.backend.quiz.QuizAnswerRepository;
import com.voca.backend.quiz.QuizAttemptRepository;
import com.voca.backend.user.EnglishLevel;
import com.voca.backend.user.UserRepository;
import com.voca.backend.vocab.UserProgressRepository;
import com.voca.backend.vocab.VocabImportRequest;
import com.voca.backend.vocab.VocabItemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ReviewIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    QuizAnswerRepository quizAnswerRepository;

    @Autowired
    QuizAttemptRepository quizAttemptRepository;

    @Autowired
    QuestionRepository questionRepository;

    @Autowired
    UserProgressRepository userProgressRepository;

    @Autowired
    VocabItemRepository vocabItemRepository;

    @Autowired
    DeckRepository deckRepository;

    @Autowired
    UserRepository userRepository;

    @BeforeEach
    void setUp() {
        quizAnswerRepository.deleteAll();
        quizAttemptRepository.deleteAll();
        questionRepository.deleteAll();
        userProgressRepository.deleteAll();
        vocabItemRepository.deleteAll();
        deckRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void todayReturnsDueItemsOnly() throws Exception {
        String token = register("review-today@example.com");
        long deckId = createDeck(token, "Review Deck");
        importItems(token, deckId, """
                absent ; (adj) vang mat
                accumulate ; (v) tich luy
                """);
        JsonNode items = listVocab(token, deckId);
        long futureVocabId = items.get(0).get("id").asLong();

        submitReview(token, futureVocabId, new ReviewResultRequest(ReviewQuality.EASY, null, 1000, ReviewSource.FLASHCARD));

        mockMvc.perform(get("/api/review/today")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalDue").value(1))
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].word").value("accumulate"));
    }

    @Test
    void reviewResultCreatesProgressAndSchedulesNextReview() throws Exception {
        String token = register("review-result@example.com");
        long deckId = createDeck(token, "Schedule Deck");
        importItems(token, deckId, "absent ; (adj) vang mat");
        long vocabId = listVocab(token, deckId).get(0).get("id").asLong();

        mockMvc.perform(post("/api/review/{vocabId}/result", vocabId)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ReviewResultRequest(ReviewQuality.GOOD, null, 4200, ReviewSource.FLASHCARD))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.vocabId").value(vocabId))
                .andExpect(jsonPath("$.status").value("REVIEW"))
                .andExpect(jsonPath("$.quality").value("GOOD"))
                .andExpect(jsonPath("$.intervalDays").value(1))
                .andExpect(jsonPath("$.nextReviewAt", startsWith("2026-")));
    }

    @Test
    void scheduleReturnsNewAndUpcomingItems() throws Exception {
        String token = register("review-schedule@example.com");
        long deckId = createDeck(token, "Schedule Preview Deck");
        importItems(token, deckId, """
                absent ; (adj) vang mat
                accumulate ; (v) tich luy
                """);
        JsonNode items = listVocab(token, deckId);
        long upcomingVocabId = items.get(0).get("id").asLong();

        submitReview(token, upcomingVocabId, new ReviewResultRequest(ReviewQuality.GOOD, null, 4200, ReviewSource.FLASHCARD));

        mockMvc.perform(get("/api/review/schedule")
                        .header("Authorization", bearer(token))
                        .param("deckId", String.valueOf(deckId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").value(2))
                .andExpect(jsonPath("$.dueNow").value(1))
                .andExpect(jsonPath("$.newItems").value(1))
                .andExpect(jsonPath("$.upcoming").value(1))
                .andExpect(jsonPath("$.items", hasSize(2)))
                .andExpect(jsonPath("$.items[0].word").value("accumulate"))
                .andExpect(jsonPath("$.items[0].bucket").value("NEW"))
                .andExpect(jsonPath("$.items[1].word").value("absent"))
                .andExpect(jsonPath("$.items[1].nextReviewAt", startsWith("2026-")));
    }

    @Test
    void dashboardReturnsReviewMetrics() throws Exception {
        String token = register("review-dashboard@example.com");
        long deckId = createDeck(token, "Dashboard Deck");
        importItems(token, deckId, """
                absent ; (adj) vang mat
                accumulate ; (v) tich luy
                """);
        JsonNode items = listVocab(token, deckId);
        long hardVocabId = items.get(0).get("id").asLong();
        long goodVocabId = items.get(1).get("id").asLong();

        submitReview(token, hardVocabId, new ReviewResultRequest(ReviewQuality.AGAIN, null, 9000, ReviewSource.FLASHCARD));
        submitReview(token, hardVocabId, new ReviewResultRequest(ReviewQuality.AGAIN, null, 9000, ReviewSource.FLASHCARD));
        submitReview(token, hardVocabId, new ReviewResultRequest(ReviewQuality.AGAIN, null, 9000, ReviewSource.FLASHCARD));
        submitReview(token, hardVocabId, new ReviewResultRequest(ReviewQuality.AGAIN, null, 9000, ReviewSource.FLASHCARD));
        submitReview(token, goodVocabId, new ReviewResultRequest(ReviewQuality.GOOD, null, 4200, ReviewSource.FLASHCARD));

        mockMvc.perform(get("/api/dashboard")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.wordsLearnedToday").value(2))
                .andExpect(jsonPath("$.wordsReviewedToday").value(2))
                .andExpect(jsonPath("$.accuracy").value(20.0))
                .andExpect(jsonPath("$.hardWords", hasSize(1)))
                .andExpect(jsonPath("$.hardWords[0].word").value("absent"))
                .andExpect(jsonPath("$.deckProgress[0].totalWords").value(2))
                .andExpect(jsonPath("$.deckProgress[0].difficultCount").value(1));
    }

    private String register(String email) throws Exception {
        RegisterRequest request = new RegisterRequest(
                email,
                "strong-password",
                "Review Tester",
                EnglishLevel.INTERMEDIATE,
                null,
                10
        );

        String body = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readTree(body).get("token").asText();
    }

    private long createDeck(String token, String name) throws Exception {
        String body = mockMvc.perform(post("/api/decks")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new DeckRequest(name, null))))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readTree(body).get("id").asLong();
    }

    private void importItems(String token, long deckId, String rawText) throws Exception {
        VocabImportRequest request = new VocabImportRequest(deckId, rawText);
        mockMvc.perform(post("/api/vocab/import/confirm")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    private JsonNode listVocab(String token, long deckId) throws Exception {
        String body = mockMvc.perform(get("/api/decks/{deckId}/vocab", deckId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(body);
    }

    private void submitReview(String token, long vocabId, ReviewResultRequest request) throws Exception {
        mockMvc.perform(post("/api/review/{vocabId}/result", vocabId)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
