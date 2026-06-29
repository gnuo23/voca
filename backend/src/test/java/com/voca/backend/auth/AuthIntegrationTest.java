package com.voca.backend.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.voca.backend.user.EnglishLevel;
import com.voca.backend.user.UpdateProfileRequest;
import com.voca.backend.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.blankOrNullString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    UserRepository userRepository;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @Test
    void registerLoginMeAndUpdateProfile() throws Exception {
        RegisterRequest registerRequest = new RegisterRequest(
                "learner@example.com",
                "strong-password",
                "First Learner",
                EnglishLevel.BEGINNER,
                "Read short stories",
                10
        );

        String registerBody = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", not(blankOrNullString())))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.user.email").value("learner@example.com"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String registerToken = tokenFrom(registerBody);

        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + registerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("First Learner"))
                .andExpect(jsonPath("$.englishLevel").value("BEGINNER"))
                .andExpect(jsonPath("$.dailyGoal").value(10));

        UpdateProfileRequest updateRequest = new UpdateProfileRequest(
                "Updated Learner",
                EnglishLevel.INTERMEDIATE,
                "Practice business vocabulary",
                20
        );

        mockMvc.perform(put("/api/users/me")
                        .header("Authorization", "Bearer " + registerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("Updated Learner"))
                .andExpect(jsonPath("$.englishLevel").value("INTERMEDIATE"))
                .andExpect(jsonPath("$.learningGoal").value("Practice business vocabulary"))
                .andExpect(jsonPath("$.dailyGoal").value(20));

        LoginRequest loginRequest = new LoginRequest("learner@example.com", "strong-password");
        String loginBody = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", not(blankOrNullString())))
                .andReturn()
                .getResponse()
                .getContentAsString();

        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + tokenFrom(loginBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("Updated Learner"));
    }

    @Test
    void protectedEndpointRequiresToken() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isForbidden());
    }

    @Test
    void duplicateRegistrationReturnsConflict() throws Exception {
        RegisterRequest request = new RegisterRequest(
                "duplicate@example.com",
                "strong-password",
                "Learner",
                EnglishLevel.ELEMENTARY,
                null,
                5
        );

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    private String tokenFrom(String body) throws Exception {
        JsonNode jsonNode = objectMapper.readTree(body);
        return jsonNode.get("token").asText();
    }
}
