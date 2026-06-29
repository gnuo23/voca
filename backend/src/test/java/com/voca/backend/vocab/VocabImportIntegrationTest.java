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

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class VocabImportIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    VocabItemRepository vocabItemRepository;

    @Autowired
    DeckRepository deckRepository;

    @Autowired
    UserRepository userRepository;

    @BeforeEach
    void setUp() {
        vocabItemRepository.deleteAll();
        deckRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void previewAndConfirmImportVocabularyItems() throws Exception {
        String token = register("importer@example.com");
        long deckId = createDeck(token, "Import Deck");
        VocabImportRequest request = new VocabImportRequest(deckId, """
                absent ; (adj) vắng mặt, không có mặt
                accumulate ; (v) tích lũy, gom góp
                real estate ; (n) bất động sản
                take off - cất cánh
                mortgage: khoản vay mua nhà
                lease | n | hợp đồng thuê
                standalone
                """);

        mockMvc.perform(post("/api/vocab/import/preview")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(7)))
                .andExpect(jsonPath("$.items[0].lineNumber").value(1))
                .andExpect(jsonPath("$.items[0].word").value("absent"))
                .andExpect(jsonPath("$.items[0].partOfSpeech").value("adj"))
                .andExpect(jsonPath("$.items[0].meaningVi").value("vắng mặt, không có mặt"))
                .andExpect(jsonPath("$.items[0].status").value("OK"))
                .andExpect(jsonPath("$.items[2].word").value("real estate"))
                .andExpect(jsonPath("$.errors", hasSize(0)));

        mockMvc.perform(post("/api/vocab/import/confirm")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.importedCount").value(7));
    }

    @Test
    void previewReportsInvalidLinesDuplicatesAndExistingDeckWords() throws Exception {
        String token = register("duplicates@example.com");
        long deckId = createDeck(token, "Duplicate Deck");

        VocabImportRequest initial = new VocabImportRequest(deckId, "absent ; (adj) vắng mặt");
        mockMvc.perform(post("/api/vocab/import/confirm")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(initial)))
                .andExpect(status().isOk());

        VocabImportRequest request = new VocabImportRequest(deckId, """
                absent ; (adj) vắng mặt
                accumulate ; (v) tích lũy
                Accumulate ; (v) gom góp
                ; missing word
                broken | pipe
                """);

        mockMvc.perform(post("/api/vocab/import/preview")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(3)))
                .andExpect(jsonPath("$.items[0].status").value("DUPLICATE_IN_DECK"))
                .andExpect(jsonPath("$.items[1].status").value("OK"))
                .andExpect(jsonPath("$.items[2].status").value("DUPLICATE_IN_IMPORT"))
                .andExpect(jsonPath("$.items[2].message").value("Duplicate of line 2"))
                .andExpect(jsonPath("$.errors", hasSize(2)))
                .andExpect(jsonPath("$.errors[0].lineNumber").value(4))
                .andExpect(jsonPath("$.errors[1].lineNumber").value(5));

        mockMvc.perform(post("/api/vocab/import/confirm")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void importRequiresDeckOwnership() throws Exception {
        String ownerToken = register("vocab-owner@example.com");
        String otherToken = register("vocab-other@example.com");
        long deckId = createDeck(ownerToken, "Private");

        VocabImportRequest request = new VocabImportRequest(deckId, "secret ; bí mật");

        mockMvc.perform(post("/api/vocab/import/preview")
                        .header("Authorization", bearer(otherToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    private String register(String email) throws Exception {
        RegisterRequest request = new RegisterRequest(
                email,
                "strong-password",
                "Vocab Tester",
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

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
