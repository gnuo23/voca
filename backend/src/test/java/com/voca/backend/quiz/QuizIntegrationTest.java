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
    void userCanImportSaveAndPlayBulkQuiz() throws Exception {
        String token = register("quiz-import@example.com");
        long deckId = createDeck(token, "Quiz Import Deck");
        importItems(token, deckId, """
                absent ; (adj) vang mat
                accumulate ; (v) tich luy
                adhere ; (v) tuan theo
                adjacent ; (adj) ke ben
                fingernail ; (n) mong tay
                """);

        Map<String, Object> request = Map.of(
                "rawText", """
                        absent -- He was ____ from class yesterday.
                        accumulate -- Dust can ____ on the shelf.
                        missing -- This word is not in the deck.
                        fingernail -- She broke a ____ while opening the box.
                        """
        );

        mockMvc.perform(post("/api/decks/{deckId}/quiz/import/preview", deckId)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.validCount").value(3))
                .andExpect(jsonPath("$.skippedCount").value(1))
                .andExpect(jsonPath("$.items", hasSize(4)));

        mockMvc.perform(post("/api/decks/{deckId}/quiz/import/save", deckId)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.validCount").value(3));

        String attemptBody = mockMvc.perform(post("/api/decks/{deckId}/quiz/start", deckId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalQuestions").value(3))
                .andExpect(jsonPath("$.questions", hasSize(3)))
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode questions = objectMapper.readTree(attemptBody).get("questions");
        questions.forEach(question -> {
            org.assertj.core.api.Assertions.assertThat(question.get("options")).hasSize(4);
            org.assertj.core.api.Assertions.assertThat(question.get("type").asText()).isEqualTo("CHOOSE_MEANING");
        });

        long attemptId = objectMapper.readTree(attemptBody).get("id").asLong();
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
    void bulkSaveOverwritesExistingQuestionsForSameVocabulary() throws Exception {
        String token = register("quiz-import-overwrite@example.com");
        long deckId = createDeck(token, "Quiz Import Overwrite Deck");
        importItems(token, deckId, """
                absent ; (adj) vang mat
                accumulate ; (v) tich luy
                adhere ; (v) tuan theo
                adjacent ; (adj) ke ben
                """);

        Map<String, Object> firstRequest = Map.of(
                "rawText", "absent -- He was ____ from class yesterday."
        );
        mockMvc.perform(post("/api/decks/{deckId}/quiz/import/save", deckId)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(firstRequest)))
                .andExpect(status().isOk());
        long firstQuestionCount = questionRepository.count();

        Map<String, Object> overwriteRequest = Map.of(
                "rawText", "absent -- The student was ____ today."
        );
        mockMvc.perform(post("/api/decks/{deckId}/quiz/import/save", deckId)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(overwriteRequest)))
                .andExpect(status().isOk());

        org.assertj.core.api.Assertions.assertThat(questionRepository.count()).isEqualTo(firstQuestionCount);
        Question stored = questionRepository.findAll().iterator().next();
        org.assertj.core.api.Assertions.assertThat(stored.getPrompt()).isEqualTo("The student was ____ today.");
    }

    @Test
    void startQuizRequiresSavedQuestions() throws Exception {
        String token = register("quiz-empty@example.com");
        long deckId = createDeck(token, "Empty Quiz Deck");
        importItems(token, deckId, """
                absent ; (adj) vang mat
                accumulate ; (v) tich luy
                adhere ; (v) tuan theo
                adjacent ; (adj) ke ben
                """);

        mockMvc.perform(post("/api/decks/{deckId}/quiz/start", deckId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isBadRequest());
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
