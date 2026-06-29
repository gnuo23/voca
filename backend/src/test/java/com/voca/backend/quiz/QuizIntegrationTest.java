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
                .andExpect(jsonPath("$.questionCount").value(10))
                .andExpect(jsonPath("$.questions", hasSize(10)))
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
                .andExpect(jsonPath("$.totalQuestions").value(10))
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
                .andExpect(jsonPath("$.totalQuestions").value(10))
                .andExpect(jsonPath("$.answeredCount").value(10))
                .andExpect(jsonPath("$.correctCount").value(9))
                .andExpect(jsonPath("$.scorePercent").value(90))
                .andExpect(jsonPath("$.completed").value(true))
                .andExpect(jsonPath("$.answers", hasSize(10)));

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
                .andExpect(jsonPath("$.message").value("Need at least 2 vocabulary items with meanings to generate a quiz"));
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
