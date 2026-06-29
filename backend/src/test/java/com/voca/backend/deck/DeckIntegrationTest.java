package com.voca.backend.deck;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.voca.backend.auth.RegisterRequest;
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
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class DeckIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    DeckRepository deckRepository;

    @Autowired
    UserRepository userRepository;

    @BeforeEach
    void setUp() {
        deckRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void userCanCreateListViewEditAndDeleteOwnDeck() throws Exception {
        String token = register("deck-owner@example.com");

        DeckRequest createRequest = new DeckRequest("Travel English", "Airport and hotel phrases");
        String createBody = mockMvc.perform(post("/api/decks")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.name").value("Travel English"))
                .andExpect(jsonPath("$.totalWords").value(0))
                .andExpect(jsonPath("$.learnedWords").value(0))
                .andExpect(jsonPath("$.dueWords").value(0))
                .andReturn()
                .getResponse()
                .getContentAsString();

        long deckId = objectMapper.readTree(createBody).get("id").asLong();

        mockMvc.perform(get("/api/decks")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name").value("Travel English"));

        mockMvc.perform(get("/api/decks/{deckId}", deckId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").value("Airport and hotel phrases"));

        DeckRequest updateRequest = new DeckRequest("Business English", "Meetings and email vocabulary");
        mockMvc.perform(put("/api/decks/{deckId}", deckId)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Business English"))
                .andExpect(jsonPath("$.description").value("Meetings and email vocabulary"));

        mockMvc.perform(delete("/api/decks/{deckId}", deckId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/decks")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void deckAccessIsLimitedToOwner() throws Exception {
        String ownerToken = register("owner@example.com");
        String otherToken = register("other@example.com");

        String body = mockMvc.perform(post("/api/decks")
                        .header("Authorization", bearer(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new DeckRequest("Private Deck", null))))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        long deckId = objectMapper.readTree(body).get("id").asLong();

        mockMvc.perform(get("/api/decks")
                        .header("Authorization", bearer(otherToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        mockMvc.perform(get("/api/decks/{deckId}", deckId)
                        .header("Authorization", bearer(otherToken)))
                .andExpect(status().isNotFound());

        mockMvc.perform(put("/api/decks/{deckId}", deckId)
                        .header("Authorization", bearer(otherToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new DeckRequest("Stolen", null))))
                .andExpect(status().isNotFound());

        mockMvc.perform(delete("/api/decks/{deckId}", deckId)
                        .header("Authorization", bearer(otherToken)))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/decks/{deckId}", deckId)
                        .header("Authorization", bearer(ownerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Private Deck"));
    }

    private String register(String email) throws Exception {
        RegisterRequest request = new RegisterRequest(
                email,
                "strong-password",
                "Deck Tester",
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

        JsonNode jsonNode = objectMapper.readTree(body);
        return jsonNode.get("token").asText();
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
