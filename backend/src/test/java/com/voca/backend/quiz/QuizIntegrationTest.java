package com.voca.backend.quiz;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.voca.backend.auth.RegisterRequest;
import com.voca.backend.deck.DeckRepository;
import com.voca.backend.deck.DeckRequest;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class QuizIntegrationTest {

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
    void userCanGenerateTakeAndReviewQuiz() throws Exception {
        String token = register("quiz-owner@example.com");
        long deckId = createDeck(token, "Quiz Deck");
        importItems(token, deckId, """
                absent ; (adj) vang mat
                accumulate ; (v) tich luy
                adhere ; (v) tuan theo
                adjacent ; (adj) ke ben
                advocate ; (v) ung ho
                allocate ; (v) phan bo
                """);

        String generateBody = mockMvc.perform(post("/api/decks/{deckId}/quiz/generate", deckId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.questionCount").value(6))
                .andExpect(jsonPath("$.questions", hasSize(6)))
                .andExpect(jsonPath("$.questions[0].prompt").isNotEmpty())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode generatedQuestions = objectMapper.readTree(generateBody).get("questions");
        List<Long> questionIds = new ArrayList<>();
        generatedQuestions.forEach(question -> questionIds.add(question.get("id").asLong()));

        CreateQuizAttemptRequest attemptRequest = new CreateQuizAttemptRequest(deckId, questionIds);
        String attemptBody = mockMvc.perform(post("/api/quiz-attempts")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(attemptRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalQuestions").value(6))
                .andExpect(jsonPath("$.answeredCount").value(0))
                .andReturn()
                .getResponse()
                .getContentAsString();

        long attemptId = objectMapper.readTree(attemptBody).get("id").asLong();
        for (int i = 0; i < questionIds.size(); i++) {
            Question question = questionRepository.findById(questionIds.get(i)).orElseThrow();
            String answer = i == 0 ? "wrong answer" : question.getCorrectAnswer();

            mockMvc.perform(post("/api/quiz-attempts/{attemptId}/answer", attemptId)
                            .header("Authorization", bearer(token))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new AnswerQuizQuestionRequest(question.getId(), answer, 4200))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.questionId").value(question.getId()))
                    .andExpect(jsonPath("$.explanation").isNotEmpty());
        }

        mockMvc.perform(get("/api/quiz-attempts/{attemptId}/result", attemptId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalQuestions").value(6))
                .andExpect(jsonPath("$.answeredCount").value(6))
                .andExpect(jsonPath("$.correctCount").value(5))
                .andExpect(jsonPath("$.scorePercent").value(83))
                .andExpect(jsonPath("$.completed").value(true))
                .andExpect(jsonPath("$.answers", hasSize(6)));

        mockMvc.perform(get("/api/decks/{deckId}", deckId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.learnedWords", greaterThanOrEqualTo(1)));
    }

    @Test
    void quizGenerationRequiresEnoughVocabulary() throws Exception {
        String token = register("quiz-small@example.com");
        long deckId = createDeck(token, "Small Deck");
        importItems(token, deckId, "absent ; (adj) vang mat");

        mockMvc.perform(post("/api/decks/{deckId}/quiz/generate", deckId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Need at least 4 vocabulary items with meanings to generate a quiz"));
    }

    @Test
    void userCanCreateManualQuizFromAllDeckVocabularyJson() throws Exception {
        String token = register("quiz-manual-map@example.com");
        long deckId = createDeck(token, "Manual Map Deck");
        importItems(token, deckId, """
                absent ; (adj) vang mat
                accumulate ; (v) tich luy
                adhere ; (v) tuan theo
                adjacent ; (adj) ke ben
                """);

        String attemptBody = mockMvc.perform(post("/api/decks/{deckId}/quiz/manual-attempt", deckId)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "questionTypes", List.of("CLOZE_CHOICE", "MATCHING"),
                                "limit", 4
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalQuestions").value(4))
                .andExpect(jsonPath("$.answeredCount").value(0))
                .andExpect(jsonPath("$.questions", hasSize(4)))
                .andExpect(jsonPath("$.questions[0].prompt").isNotEmpty())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode questions = objectMapper.readTree(attemptBody).get("questions");
        org.assertj.core.api.Assertions.assertThat(questions)
                .anyMatch(question -> question.get("type").asText().equals("MATCHING"));
    }

    @Test
    void userCanCreateManualAuthoredQuizQuestionJson() throws Exception {
        String token = register("quiz-manual-authored@example.com");
        long deckId = createDeck(token, "Manual Authored Deck");
        importItems(token, deckId, """
                absent ; (adj) vang mat
                accumulate ; (v) tich luy
                """);

        String attemptBody = mockMvc.perform(post("/api/decks/{deckId}/quiz/manual-attempt", deckId)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "questions", List.of(Map.of(
                                        "word", "absent",
                                        "type", "CLOZE_CHOICE",
                                        "prompt", "He was ____ from class yesterday.",
                                        "options", List.of("absent", "accumulate"),
                                        "correctAnswer", "absent",
                                        "explanation", "The correct word is absent."
                                ))
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalQuestions").value(1))
                .andExpect(jsonPath("$.questions", hasSize(1)))
                .andExpect(jsonPath("$.questions[0].type").value("CLOZE_CHOICE"))
                .andExpect(jsonPath("$.questions[0].options", hasSize(2)))
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode attempt = objectMapper.readTree(attemptBody);
        long attemptId = attempt.get("id").asLong();
        long questionId = attempt.get("questions").get(0).get("id").asLong();

        mockMvc.perform(post("/api/quiz-attempts/{attemptId}/answer", attemptId)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AnswerQuizQuestionRequest(questionId, "absent", 1200))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.correct").value(true));
    }

    @Test
    void userCanPreviewAndStartImportedQuizFromDashSeparatedLines() throws Exception {
        String token = register("quiz-import@example.com");
        long deckId = createDeck(token, "Quiz Import Deck");
        importItems(token, deckId, """
                absent ; (adj) vang mat
                accumulate ; (v) tich luy
                adhere ; (v) tuan theo
                adjacent ; (adj) ke ben
                """);

        Map<String, Object> request = Map.of(
                "rawText", """
                        absent -- He was ____ from class yesterday.
                        accumulate -- Dust can accumulate on the shelf.
                        missing -- This word is not in the deck.
                        adhere -- Please adhere to the safety rules.
                        adjacent -- The hotel is adjacent to the station.
                        """,
                "questionTypes", List.of("CLOZE_CHOICE", "CHOOSE_MEANING", "MATCHING"),
                "limit", 3
        );

        mockMvc.perform(post("/api/decks/{deckId}/quiz/import/preview", deckId)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.validCount").value(4))
                .andExpect(jsonPath("$.skippedCount").value(1))
                .andExpect(jsonPath("$.items", hasSize(5)))
                .andExpect(jsonPath("$.items[2].status").value("SKIPPED"));

        String attemptBody = mockMvc.perform(post("/api/decks/{deckId}/quiz/import/attempt", deckId)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalQuestions").value(3))
                .andExpect(jsonPath("$.questions", hasSize(3)))
                .andReturn()
                .getResponse()
                .getContentAsString();

        long attemptId = objectMapper.readTree(attemptBody).get("id").asLong();
        JsonNode questions = objectMapper.readTree(attemptBody).get("questions");
        questions.forEach(question -> {
            if (!question.get("type").asText().equals("MATCHING")) {
                org.assertj.core.api.Assertions.assertThat(question.get("options")).hasSize(4);
            }
        });
        org.assertj.core.api.Assertions.assertThat(questions.toString()).doesNotContain("missing");

        long firstQuestionId = questions.get(0).get("id").asLong();
        Question firstQuestion = questionRepository.findById(firstQuestionId).orElseThrow();
        mockMvc.perform(post("/api/quiz-attempts/{attemptId}/answer", attemptId)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AnswerQuizQuestionRequest(firstQuestionId, firstQuestion.getCorrectAnswer(), 2500))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.correct").value(true));

        mockMvc.perform(get("/api/decks/{deckId}", deckId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.learnedWords", greaterThanOrEqualTo(1)));
    }

    @Test
    void importedQuizOverwritesExistingQuestionsForSameVocabulary() throws Exception {
        String token = register("quiz-import-overwrite@example.com");
        long deckId = createDeck(token, "Quiz Import Overwrite Deck");
        importItems(token, deckId, """
                absent ; (adj) vang mat
                accumulate ; (v) tich luy
                adhere ; (v) tuan theo
                adjacent ; (adj) ke ben
                """);

        Map<String, Object> firstRequest = Map.of(
                "rawText", "absent -- He was absent from class yesterday.",
                "questionTypes", List.of("CLOZE_CHOICE")
        );
        String firstAttemptBody = mockMvc.perform(post("/api/decks/{deckId}/quiz/import/attempt", deckId)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(firstRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalQuestions").value(1))
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode firstAttempt = objectMapper.readTree(firstAttemptBody);
        long firstAttemptId = firstAttempt.get("id").asLong();
        long oldQuestionId = firstAttempt.get("questions").get(0).get("id").asLong();
        Question oldQuestion = questionRepository.findById(oldQuestionId).orElseThrow();
        mockMvc.perform(post("/api/quiz-attempts/{attemptId}/answer", firstAttemptId)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AnswerQuizQuestionRequest(oldQuestionId, oldQuestion.getCorrectAnswer(), 1800))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.correct").value(true));

        Map<String, Object> overwriteRequest = Map.of(
                "rawText", "absent -- The student was absent today.",
                "questionTypes", List.of("CLOZE_CHOICE")
        );
        String overwriteAttemptBody = mockMvc.perform(post("/api/decks/{deckId}/quiz/import/attempt", deckId)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(overwriteRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalQuestions").value(1))
                .andExpect(jsonPath("$.questions[0].prompt").value("Choose the word that completes the sentence: The student was ____ today."))
                .andReturn()
                .getResponse()
                .getContentAsString();

        long newQuestionId = objectMapper.readTree(overwriteAttemptBody).get("questions").get(0).get("id").asLong();
        org.assertj.core.api.Assertions.assertThat(newQuestionId).isNotEqualTo(oldQuestionId);
        org.assertj.core.api.Assertions.assertThat(questionRepository.findById(oldQuestionId)).isEmpty();
        org.assertj.core.api.Assertions.assertThat(quizAnswerRepository.count()).isZero();
    }

    private String register(String email) throws Exception {
        RegisterRequest request = new RegisterRequest(
                email,
                "strong-password",
                "Quiz Tester",
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

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
