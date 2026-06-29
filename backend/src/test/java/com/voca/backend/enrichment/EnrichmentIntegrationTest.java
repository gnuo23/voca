package com.voca.backend.enrichment;

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
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Duration;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class EnrichmentIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    EnrichmentJobRepository enrichmentJobRepository;

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
        enrichmentJobRepository.deleteAll();
        userProgressRepository.deleteAll();
        vocabItemRepository.deleteAll();
        deckRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void enrichDeckCreatesJobAndStoresMissingDetailsWithoutOverwritingMeaning() throws Exception {
        String token = register("enrich@example.com");
        long deckId = createDeck(token);
        importItems(token, deckId);

        String body = mockMvc.perform(post("/api/decks/{deckId}/enrich", deckId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.totalItems").value(2))
                .andReturn()
                .getResponse()
                .getContentAsString();
        long jobId = objectMapper.readTree(body).get("id").asLong();

        Awaitility.await()
                .atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> mockMvc.perform(get("/api/enrich/jobs/{jobId}", jobId)
                                .header("Authorization", bearer(token)))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.status").value("DONE"))
                        .andExpect(jsonPath("$.processedItems").value(2)));

        mockMvc.perform(get("/api/decks/{deckId}/vocab", deckId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].meaningVi").value("vang mat"))
                .andExpect(jsonPath("$[0].ipa", not(nullValue())))
                .andExpect(jsonPath("$[0].exampleEn", not(nullValue())))
                .andExpect(jsonPath("$[0].topic", not(nullValue())))
                .andExpect(jsonPath("$[0].level", not(nullValue())));
    }

    @Test
    void enrichmentRequiresOwnership() throws Exception {
        String ownerToken = register("enrich-owner@example.com");
        String otherToken = register("enrich-other@example.com");
        long deckId = createDeck(ownerToken);

        mockMvc.perform(post("/api/decks/{deckId}/enrich", deckId)
                        .header("Authorization", bearer(otherToken)))
                .andExpect(status().isNotFound());
    }

    private String register(String email) throws Exception {
        RegisterRequest request = new RegisterRequest(
                email,
                "strong-password",
                "Enrichment Tester",
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

    private long createDeck(String token) throws Exception {
        String body = mockMvc.perform(post("/api/decks")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new DeckRequest("Enrichment Deck", null))))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode jsonNode = objectMapper.readTree(body);
        return jsonNode.get("id").asLong();
    }

    private void importItems(String token, long deckId) throws Exception {
        VocabImportRequest request = new VocabImportRequest(deckId, """
                absent ; (adj) vang mat
                real estate ; (n) bat dong san
                """);

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
