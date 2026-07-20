package com.voca.backend.learn;

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
import com.voca.backend.vocab.UserProgress;
import com.voca.backend.vocab.UserProgressRepository;
import com.voca.backend.vocab.VocabImportRequest;
import com.voca.backend.vocab.VocabItemRepository;
import com.voca.backend.vocab.VocabProgressStatus;
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
    LearnProgressRepository learnProgressRepository;

    @Autowired
    UserProgressRepository userProgressRepository;

    @Autowired
    QuizAnswerRepository quizAnswerRepository;

    @Autowired
    QuizAttemptRepository quizAttemptRepository;

    @Autowired
    QuestionRepository questionRepository;

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
        learnProgressRepository.deleteAll();
        userProgressRepository.deleteAll();
        quizAnswerRepository.deleteAll();
        quizAttemptRepository.deleteAll();
        questionRepository.deleteAll();
        vocabItemRepository.deleteAll();
        deckRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void activeSessionCanBeResumedForDeck() throws Exception {
        String token = register("learn-resume@example.com");
        long deckId = createDeck(token, "Resume Learn Deck");
        importItems(token, deckId, """
                absent ; (adj) vang mat
                eager ; (adj) hao huc
                careful ; (adj) can than
                """);

        StartLearnSessionRequest startRequest = new StartLearnSessionRequest(
                deckId,
                LearnSessionScope.ALL,
                LearnGoal.MASTER_ALL,
                LearnAnswerDirection.BOTH,
                LearnGradingMode.EXACT,
                List.of(LearnQuestionType.MCQ, LearnQuestionType.WRITTEN)
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

        JsonNode question = nextQuestion(token, sessionId);
        answerQuestion(token, sessionId, question, correctAnswerFor(question));

        mockMvc.perform(get("/api/learn/sessions/active")
                        .param("deckId", String.valueOf(deckId))
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(sessionId))
                .andExpect(jsonPath("$.deckId").value(deckId))
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.totalAnswers").value(1));
    }

    @Test
    void closeWrittenAnswerCanBeOverriddenWithoutReviewSchedulingUntilMastered() throws Exception {
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

        JsonNode question = nextQuestion(token, sessionId);
        answerQuestion(token, sessionId, question, correctAnswerFor(question));
        question = nextQuestion(token, sessionId);
        org.assertj.core.api.Assertions.assertThat(question.get("questionType").asText()).isEqualTo("WRITTEN");
        long sessionItemId = question.get("sessionItemId").asLong();
        long vocabId = question.get("vocabId").asLong();
        String correctAnswer = correctAnswerFor(question);
        String closeAnswer = correctAnswer.substring(0, correctAnswer.length() - 1);

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

        org.assertj.core.api.Assertions.assertThat(userProgressRepository.findAllByVocabItemId(vocabId)).isEmpty();

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
                .andExpect(jsonPath("$.newStage").value("FAMILIAR"));

        org.assertj.core.api.Assertions.assertThat(userProgressRepository.findAllByVocabItemId(vocabId)).isEmpty();

        mockMvc.perform(get("/api/learn/sessions/{id}/result", sessionId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.history[0].verdict").value("CORRECT"))
                .andExpect(jsonPath("$.history[0].similarityScore", greaterThan(0.7)));
    }

    @Test
    void writtenEnglishToVietnameseAcceptsReviewStyleMeaningAlternatives() throws Exception {
        String token = register("learn-review-style-en-vi@example.com");
        long deckId = createDeck(token, "Learn Review Style Deck");
        importItems(token, deckId, """
                absent ; vang mat, nghi hoc
                eager ; hao huc
                """);

        StartLearnSessionRequest startRequest = new StartLearnSessionRequest(
                deckId,
                LearnSessionScope.ALL,
                LearnGoal.MASTER_ALL,
                LearnAnswerDirection.BOTH,
                LearnGradingMode.EXACT,
                List.of(LearnQuestionType.MCQ, LearnQuestionType.WRITTEN)
        );
        long sessionId = startSession(token, startRequest);

        JsonNode question = findQuestion(token, sessionId, "absent", "LEARNING", 20);
        JsonNode response = answerQuestionForResponse(token, sessionId, question, "nghi hoc");

        org.assertj.core.api.Assertions.assertThat(response.get("correct").asBoolean()).isTrue();
        org.assertj.core.api.Assertions.assertThat(response.get("verdict").asText()).isEqualTo("CORRECT");
        org.assertj.core.api.Assertions.assertThat(response.get("newStage").asText()).isEqualTo("FAMILIAR");
    }

    @Test
    void reviewStyleMeaningAlternativesDoNotApplyToWrittenVietnameseToEnglish() throws Exception {
        String token = register("learn-review-style-vi-en@example.com");
        long deckId = createDeck(token, "Learn Direction Deck");
        importItems(token, deckId, """
                set up, establish ; thiet lap
                absent ; vang mat
                """);

        StartLearnSessionRequest startRequest = new StartLearnSessionRequest(
                deckId,
                LearnSessionScope.ALL,
                LearnGoal.MASTER_ALL,
                LearnAnswerDirection.BOTH,
                LearnGradingMode.EXACT,
                List.of(LearnQuestionType.MCQ, LearnQuestionType.WRITTEN)
        );
        long sessionId = startSession(token, startRequest);

        JsonNode question = findQuestion(token, sessionId, "set up, establish", "FAMILIAR", 30);
        JsonNode response = answerQuestionForResponse(token, sessionId, question, "establish");

        org.assertj.core.api.Assertions.assertThat(response.get("correct").asBoolean()).isFalse();
        org.assertj.core.api.Assertions.assertThat(response.get("verdict").asText()).isEqualTo("INCORRECT");
    }

    @Test
    void vietnameseToEnglishWrittenRecallStartsAfterFiveToEightLearnAnswers() throws Exception {
        String token = register("learn-delayed-vi-en@example.com");
        long deckId = createDeck(token, "Delayed Reverse Recall Deck");
        importItems(token, deckId, """
                absent ; vang mat
                eager ; hao huc
                careful ; can than
                """);

        StartLearnSessionRequest startRequest = new StartLearnSessionRequest(
                deckId,
                LearnSessionScope.ALL,
                LearnGoal.MASTER_ALL,
                LearnAnswerDirection.BOTH,
                LearnGradingMode.EXACT,
                List.of(LearnQuestionType.MCQ, LearnQuestionType.WRITTEN)
        );
        long sessionId = startSession(token, startRequest);

        int answersBeforeVietnameseToEnglish = 0;
        while (answersBeforeVietnameseToEnglish <= 8) {
            JsonNode question = nextQuestion(token, sessionId);
            if (isVietnameseToEnglishWritten(question)) {
                break;
            }
            answerQuestion(token, sessionId, question, correctAnswerFor(question));
            answersBeforeVietnameseToEnglish++;
        }

        org.assertj.core.api.Assertions.assertThat(answersBeforeVietnameseToEnglish).isBetween(5, 8);
    }

    @Test
    void learnMasteryUpdatesReviewAndSeparateLearnProgress() throws Exception {
        String token = register("learn-hard-result@example.com");
        long deckId = createDeck(token, "Hard Learn Deck");
        importItems(token, deckId, """
                absent ; (adj) vang mat
                eager ; (adj) hao huc
                """);

        StartLearnSessionRequest startRequest = new StartLearnSessionRequest(
                deckId,
                LearnSessionScope.ALL,
                LearnGoal.MASTER_ALL,
                LearnAnswerDirection.BOTH,
                LearnGradingMode.EXACT,
                List.of(LearnQuestionType.MCQ, LearnQuestionType.WRITTEN)
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

        JsonNode question = nextQuestion(token, sessionId);
        long sessionItemId = question.get("sessionItemId").asLong();
        long vocabId = question.get("vocabId").asLong();

        answerQuestion(token, sessionId, question, "");
        question = nextQuestion(token, sessionId);
        org.assertj.core.api.Assertions.assertThat(question.get("sessionItemId").asLong()).isEqualTo(sessionItemId);

        answerQuestion(token, sessionId, question, "");
        question = nextQuestion(token, sessionId);
        org.assertj.core.api.Assertions.assertThat(question.get("sessionItemId").asLong()).isEqualTo(sessionItemId);

        for (int i = 0; i < 10 && userProgressRepository.findAllByVocabItemId(vocabId).isEmpty(); i++) {
            answerQuestion(token, sessionId, question, correctAnswerFor(question));
            if (userProgressRepository.findAllByVocabItemId(vocabId).isEmpty()) {
                question = nextQuestion(token, sessionId);
            }
        }

        UserProgress reviewProgress = userProgressRepository.findAllByVocabItemId(vocabId).getFirst();
        org.assertj.core.api.Assertions.assertThat(reviewProgress.getStatus()).isEqualTo(VocabProgressStatus.DIFFICULT);
        org.assertj.core.api.Assertions.assertThat(reviewProgress.getLastQuality()).isEqualTo("AGAIN");
        org.assertj.core.api.Assertions.assertThat(reviewProgress.getWrongCount()).isEqualTo(2);
        org.assertj.core.api.Assertions.assertThat(reviewProgress.getUnknownCount()).isEqualTo(2);
        org.assertj.core.api.Assertions.assertThat(reviewProgress.getCorrectCount()).isZero();

        LearnProgress progress = learnProgressRepository.findByUserIdAndVocabItemId(
                userRepository.findByEmailIgnoreCase("learn-hard-result@example.com").orElseThrow().getId(),
                vocabId
        ).orElseThrow();
        org.assertj.core.api.Assertions.assertThat(progress.isMastered()).isTrue();
        org.assertj.core.api.Assertions.assertThat(progress.getCorrectAttempts()).isGreaterThanOrEqualTo(3);
        org.assertj.core.api.Assertions.assertThat(progress.getIncorrectAttempts()).isEqualTo(2);
        org.assertj.core.api.Assertions.assertThat(progress.getMasteredAt()).isNotNull();
    }

    @Test
    void notMasteredScopeContinuesFromLearnOnlyUnmasteredTerms() throws Exception {
        String token = register("learn-continue@example.com");
        long userId = userRepository.findByEmailIgnoreCase("learn-continue@example.com").orElseThrow().getId();
        long deckId = createDeck(token, "Continue Learn Deck");
        importItems(token, deckId, """
                absent ; (adj) vang mat
                eager ; (adj) hao huc
                """);

        StartLearnSessionRequest startRequest = new StartLearnSessionRequest(
                deckId,
                LearnSessionScope.NOT_MASTERED,
                LearnGoal.MASTER_ALL,
                LearnAnswerDirection.BOTH,
                LearnGradingMode.EXACT,
                List.of(LearnQuestionType.MCQ, LearnQuestionType.WRITTEN)
        );
        long firstSessionId = startSession(token, startRequest);
        List<Long> vocabIds = vocabItemRepository.findAllByDeckIdOrderByCreatedAtAsc(deckId)
                .stream()
                .map(item -> item.getId())
                .toList();

        Long masteredVocabId = null;
        for (int i = 0; i < 10 && masteredVocabId == null; i++) {
            JsonNode question = nextQuestion(token, firstSessionId);
            answerQuestion(token, firstSessionId, question, correctAnswerFor(question));
            masteredVocabId = learnProgressRepository.findAllByUserIdAndVocabItemIdIn(userId, vocabIds)
                    .stream()
                    .filter(LearnProgress::isMastered)
                    .map(progress -> progress.getVocabItem().getId())
                    .findFirst()
                    .orElse(null);
        }

        org.assertj.core.api.Assertions.assertThat(masteredVocabId).isNotNull();
        long secondSessionId = startSession(token, startRequest);
        JsonNode next = nextQuestion(token, secondSessionId);

        org.assertj.core.api.Assertions.assertThat(next.get("progress").get("totalTerms").asInt()).isEqualTo(1);
        org.assertj.core.api.Assertions.assertThat(next.get("vocabId").asLong()).isNotEqualTo(masteredVocabId);
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

        JsonNode question = nextQuestion(token, sessionId);
        answerQuestion(token, sessionId, question, correctAnswerFor(question));
        question = nextQuestion(token, sessionId);
        org.assertj.core.api.Assertions.assertThat(question.get("questionType").asText()).isEqualTo("WRITTEN");
        long sessionItemId = question.get("sessionItemId").asLong();

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
                .andExpect(jsonPath("$.newStage").value("FAMILIAR"));
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
        String questionToken = question.get("questionToken").asText();

        SubmitLearnAnswerRequest answerRequest = new SubmitLearnAnswerRequest(
                sessionItemId,
                correctAnswerFor(question),
                LearnQuestionType.valueOf(question.get("questionType").asText()),
                3000L,
                questionToken
        );
        mockMvc.perform(post("/api/learn/sessions/{id}/answer", sessionId)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(answerRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.correct").value(true));

        SubmitLearnAnswerRequest staleTokenRequest = new SubmitLearnAnswerRequest(
                sessionItemId,
                correctAnswerFor(question),
                LearnQuestionType.WRITTEN,
                3000L,
                questionToken
        );

        mockMvc.perform(post("/api/learn/sessions/{id}/answer", sessionId)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(staleTokenRequest)))
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

    private long startSession(String token, StartLearnSessionRequest request) throws Exception {
        String body = mockMvc.perform(post("/api/learn/sessions")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(body).get("id").asLong();
    }

    private JsonNode nextQuestion(String token, long sessionId) throws Exception {
        String body = mockMvc.perform(get("/api/learn/sessions/{id}/next", sessionId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(body);
    }

    private void answerQuestion(String token, long sessionId, JsonNode question, String answer) throws Exception {
        answerQuestionForResponse(token, sessionId, question, answer);
    }

    private JsonNode answerQuestionForResponse(String token, long sessionId, JsonNode question, String answer) throws Exception {
        SubmitLearnAnswerRequest request = new SubmitLearnAnswerRequest(
                question.get("sessionItemId").asLong(),
                answer,
                LearnQuestionType.valueOf(question.get("questionType").asText()),
                5000L,
                question.get("questionToken").asText()
        );
        String body = mockMvc.perform(post("/api/learn/sessions/{id}/answer", sessionId)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(body);
    }

    private JsonNode findQuestion(String token, long sessionId, String word, String stage, int maxTurns) throws Exception {
        for (int i = 0; i < maxTurns; i++) {
            JsonNode question = nextQuestion(token, sessionId);
            if (question.get("sessionItemId").isNull()) {
                break;
            }
            if (word.equals(question.get("word").asText())
                    && stage.equals(question.get("stage").asText())
                    && "WRITTEN".equals(question.get("questionType").asText())
                    && (!"FAMILIAR".equals(stage) || isVietnameseToEnglishWritten(question))) {
                return question;
            }
            answerQuestion(token, sessionId, question, correctAnswerFor(question));
        }
        throw new AssertionError("Could not find " + word + " at stage " + stage);
    }

    private String correctAnswerFor(JsonNode question) {
        String prompt = question.get("prompt").asText();
        if (prompt.startsWith("Type the word for this meaning:")
                || prompt.startsWith("Choose the word for this meaning:")) {
            return question.get("word").asText();
        }
        return question.get("vocab").get("meaningVi").asText();
    }

    private boolean isVietnameseToEnglishWritten(JsonNode question) {
        return "WRITTEN".equals(question.get("questionType").asText())
                && question.get("prompt").asText().startsWith("Type the word for this meaning:");
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
