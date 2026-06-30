package com.voca.backend.learn;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.voca.backend.auth.RegisterRequest;
import com.voca.backend.deck.DeckRepository;
import com.voca.backend.deck.DeckRequest;
import com.voca.backend.user.EnglishLevel;
import com.voca.backend.user.UserRepository;
import com.voca.backend.vocab.UserProgress;
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

import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.greaterThan;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class LearnIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    LearnAnswerRepository learnAnswerRepository;

    @Autowired
    LearnSessionItemRepository learnSessionItemRepository;

    @Autowired
    LearnSessionRepository learnSessionRepository;

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
        learnAnswerRepository.deleteAll();
        learnSessionItemRepository.deleteAll();
        learnSessionRepository.deleteAll();
        userProgressRepository.deleteAll();
        vocabItemRepository.deleteAll();
        deckRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void closeWrittenAnswerCanBeOverriddenAndReplaysReviewScheduling() throws Exception {
        String token = register("learn-close@example.com");
        long deckId = createDeck(token, "Learn Deck");
        importItems(token, deckId, """
                accommodate ; (v) dieu chinh
                accumulate ; (v) tich luy
                """);

        StartLearnSessionRequest startRequest = new StartLearnSessionRequest(
                deckId,
                LearnSessionScope.ALL,
                LearnGoal.MASTER_ALL,
                LearnAnswerDirection.MEANING_TO_WORD,
                LearnGradingMode.EXACT,
                List.of(LearnQuestionType.WRITTEN)
        );
        String sessionBody = mockMvc.perform(post("/api/learn/sessions")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(startRequest)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        long sessionId = objectMapper.readTree(sessionBody).get("id").asLong();

        String questionBody = mockMvc.perform(get("/api/learn/sessions/{id}/next", sessionId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.questionType").value("WRITTEN"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode question = objectMapper.readTree(questionBody);
        long sessionItemId = question.get("sessionItemId").asLong();
        long vocabId = question.get("vocabId").asLong();
        String word = question.get("word").asText();
        String closeAnswer = word.substring(0, word.length() - 1);

        SubmitLearnAnswerRequest closeRequest = new SubmitLearnAnswerRequest(
                sessionItemId,
                closeAnswer,
                LearnQuestionType.WRITTEN,
                6000L
        );
        mockMvc.perform(post("/api/learn/sessions/{id}/answer", sessionId)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(closeRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.correct").value(false))
                .andExpect(jsonPath("$.verdict").value("CLOSE"))
                .andExpect(jsonPath("$.userAnswer").value(closeAnswer))
                .andExpect(jsonPath("$.similarityScore", greaterThan(0.7)));

        UserProgress afterClose = userProgressRepository.findAllByVocabItemId(vocabId).getFirst();
        org.assertj.core.api.Assertions.assertThat(afterClose.getWrongCount()).isEqualTo(1);
        org.assertj.core.api.Assertions.assertThat(afterClose.getUnknownCount()).isEqualTo(1);
        org.assertj.core.api.Assertions.assertThat(afterClose.getLastQuality()).isEqualTo("AGAIN");

        mockMvc.perform(post("/api/learn/sessions/{id}/override", sessionId)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "sessionItemId", sessionItemId,
                                "verdict", "CORRECT"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.correct").value(true))
                .andExpect(jsonPath("$.verdict").value("CORRECT"))
                .andExpect(jsonPath("$.newStage").value("SEEN"));

        UserProgress afterOverride = userProgressRepository.findAllByVocabItemId(vocabId).getFirst();
        org.assertj.core.api.Assertions.assertThat(afterOverride.getCorrectCount()).isEqualTo(1);
        org.assertj.core.api.Assertions.assertThat(afterOverride.getWrongCount()).isZero();
        org.assertj.core.api.Assertions.assertThat(afterOverride.getKnownCount()).isEqualTo(1);
        org.assertj.core.api.Assertions.assertThat(afterOverride.getUnknownCount()).isZero();
        org.assertj.core.api.Assertions.assertThat(afterOverride.getLastQuality()).isEqualTo("GOOD");

        mockMvc.perform(get("/api/learn/sessions/{id}/result", sessionId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.history[0].verdict").value("CORRECT"))
                .andExpect(jsonPath("$.history[0].similarityScore", greaterThan(0.7)));
    }

    @Test
    void incorrectWrittenAnswerCanBeOverridden() throws Exception {
        String token = register("learn-incorrect-override@example.com");
        long deckId = createDeck(token, "Learn Deck");
        importItems(token, deckId, """
                accommodate ; (v) dieu chinh
                accumulate ; (v) tich luy
                """);

        StartLearnSessionRequest startRequest = new StartLearnSessionRequest(
                deckId,
                LearnSessionScope.ALL,
                LearnGoal.MASTER_ALL,
                LearnAnswerDirection.MEANING_TO_WORD,
                LearnGradingMode.EXACT,
                List.of(LearnQuestionType.WRITTEN)
        );
        String sessionBody = mockMvc.perform(post("/api/learn/sessions")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(startRequest)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        long sessionId = objectMapper.readTree(sessionBody).get("id").asLong();

        String questionBody = mockMvc.perform(get("/api/learn/sessions/{id}/next", sessionId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.questionType").value("WRITTEN"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        long sessionItemId = objectMapper.readTree(questionBody).get("sessionItemId").asLong();

        mockMvc.perform(post("/api/learn/sessions/{id}/answer", sessionId)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SubmitLearnAnswerRequest(
                                sessionItemId,
                                "definitely not the answer",
                                LearnQuestionType.WRITTEN,
                                5000L
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.correct").value(false))
                .andExpect(jsonPath("$.verdict").value("INCORRECT"));

        mockMvc.perform(post("/api/learn/sessions/{id}/override", sessionId)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "sessionItemId", sessionItemId,
                                "verdict", "CORRECT"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.correct").value(true))
                .andExpect(jsonPath("$.verdict").value("CORRECT"))
                .andExpect(jsonPath("$.newStage").value("SEEN"));
    }

    @Test
    void staleQuestionTokenIsRejected() throws Exception {
        String token = register("learn-stale-token@example.com");
        long deckId = createDeck(token, "Learn Deck");
        importItems(token, deckId, """
                play ; (v) choi
                read ; (v) doc
                """);

        StartLearnSessionRequest startRequest = new StartLearnSessionRequest(
                deckId,
                LearnSessionScope.ALL,
                LearnGoal.MASTER_ALL,
                LearnAnswerDirection.MEANING_TO_WORD,
                LearnGradingMode.EXACT,
                List.of(LearnQuestionType.WRITTEN)
        );
        String sessionBody = mockMvc.perform(post("/api/learn/sessions")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(startRequest)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        long sessionId = objectMapper.readTree(sessionBody).get("id").asLong();

        String questionBody = mockMvc.perform(get("/api/learn/sessions/{id}/next", sessionId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.questionToken").isString())
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode question = objectMapper.readTree(questionBody);
        long sessionItemId = question.get("sessionItemId").asLong();
        String word = question.get("word").asText();
        String questionToken = question.get("questionToken").asText();

        SubmitLearnAnswerRequest answerRequest = new SubmitLearnAnswerRequest(
                sessionItemId,
                word,
                LearnQuestionType.WRITTEN,
                3000L,
                questionToken
        );
        mockMvc.perform(post("/api/learn/sessions/{id}/answer", sessionId)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(answerRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.correct").value(true));

        mockMvc.perform(post("/api/learn/sessions/{id}/answer", sessionId)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(answerRequest)))
                .andExpect(status().isConflict());
    }

    private String register(String email) throws Exception {
        RegisterRequest request = new RegisterRequest(
                email,
                "strong-password",
                "Learn Tester",
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
