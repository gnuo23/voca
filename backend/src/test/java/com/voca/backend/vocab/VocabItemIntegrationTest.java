package com.voca.backend.vocab;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.voca.backend.auth.RegisterRequest;
import com.voca.backend.deck.DeckRepository;
import com.voca.backend.deck.DeckRequest;
import com.voca.backend.user.EnglishLevel;
import com.voca.backend.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class VocabItemIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    UserProgressRepository userProgressRepository;

    @Autowired
    VocabItemRepository vocabItemRepository;

    @Autowired
    DeckRepository deckRepository;

    @Autowired
    UserRepository userRepository;

    @MockitoBean
    DictionaryAudioClient dictionaryAudioClient;

    @BeforeEach
    void setUp() {
        userProgressRepository.deleteAll();
        vocabItemRepository.deleteAll();
        deckRepository.deleteAll();
        userRepository.deleteAll();
        when(dictionaryAudioClient.lookup(anyString())).thenReturn(new DictionaryAudioClient.AudioLookupResult(
                "https://audio.example.com/word-us.mp3",
                "https://audio.example.com/word-us.mp3",
                "https://audio.example.com/word-uk.mp3",
                "US"
        ));
    }

    @Test
    void userCanListEditMarkAndDeleteVocabularyItems() throws Exception {
        String token = register("study-owner@example.com");
        long deckId = createDeck(token, "Study Deck");
        importItems(token, deckId, """
                absent ; (adj) vang mat
                accumulate ; (v) tich luy
                """);

        String listBody = mockMvc.perform(get("/api/decks/{deckId}/vocab", deckId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].word").value("absent"))
                .andExpect(jsonPath("$[0].progressStatus").value("NEW"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        long vocabId = objectMapper.readTree(listBody).get(0).get("id").asLong();

        VocabItemRequest updateRequest = new VocabItemRequest("absent minded", "adj", "hay quen");
        mockMvc.perform(put("/api/vocab/{vocabId}", vocabId)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.word").value("absent minded"))
                .andExpect(jsonPath("$.meaningVi").value("hay quen"));

        mockMvc.perform(post("/api/vocab/{vocabId}/mark", vocabId)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new VocabMarkRequest(VocabMarkAction.KNOWN))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.progressStatus").value("REVIEW"))
                .andExpect(jsonPath("$.knownCount").value(1));

        mockMvc.perform(get("/api/decks/{deckId}", deckId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalWords").value(2))
                .andExpect(jsonPath("$.learnedWords").value(1))
                .andExpect(jsonPath("$.dueWords").value(1));

        mockMvc.perform(delete("/api/vocab/{vocabId}", vocabId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/decks/{deckId}/vocab", deckId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void userCanFetchAndRefreshVocabularyAudio() throws Exception {
        String token = register("audio-owner@example.com");
        long deckId = createDeck(token, "Audio Deck");
        importItems(token, deckId, "absent ; (adj) vang mat");

        String listBody = mockMvc.perform(get("/api/decks/{deckId}/vocab", deckId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        long vocabId = objectMapper.readTree(listBody).get(0).get("id").asLong();

        mockMvc.perform(get("/api/vocab/{vocabId}/audio", vocabId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.audioUrl").value("https://audio.example.com/word-us.mp3"))
                .andExpect(jsonPath("$.audioUsUrl").value("https://audio.example.com/word-us.mp3"))
                .andExpect(jsonPath("$.audioUkUrl").value("https://audio.example.com/word-uk.mp3"))
                .andExpect(jsonPath("$.audioAccent").value("US"));

        mockMvc.perform(post("/api/vocab/{vocabId}/refresh-audio", vocabId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.audioSource").value("Free Dictionary API"));
    }

    @Test
    void vocabularyAccessIsLimitedToDeckOwner() throws Exception {
        String ownerToken = register("vocab-crud-owner@example.com");
        String otherToken = register("vocab-crud-other@example.com");
        long deckId = createDeck(ownerToken, "Private Words");
        importItems(ownerToken, deckId, "secret ; bi mat");

        String listBody = mockMvc.perform(get("/api/decks/{deckId}/vocab", deckId)
                        .header("Authorization", bearer(ownerToken)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        long vocabId = objectMapper.readTree(listBody).get(0).get("id").asLong();

        mockMvc.perform(get("/api/decks/{deckId}/vocab", deckId)
                        .header("Authorization", bearer(otherToken)))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/vocab/{vocabId}", vocabId)
                        .header("Authorization", bearer(otherToken)))
                .andExpect(status().isNotFound());

        mockMvc.perform(post("/api/vocab/{vocabId}/mark", vocabId)
                        .header("Authorization", bearer(otherToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new VocabMarkRequest(VocabMarkAction.DIFFICULT))))
                .andExpect(status().isNotFound());
    }

    private String register(String email) throws Exception {
        RegisterRequest request = new RegisterRequest(
                email,
                "strong-password",
                "Vocab CRUD Tester",
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

        JsonNode jsonNode = objectMapper.readTree(body);
        return jsonNode.get("id").asLong();
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
