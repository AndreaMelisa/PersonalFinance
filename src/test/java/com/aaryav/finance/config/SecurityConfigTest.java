package com.aaryav.finance.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityConfigTest {

    @Autowired private MockMvc mockMvc;

    @Test
    void register_isPublic_doesNotRequireSession() throws Exception {
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"seguridad1@test.com","password":"secret123","fullName":"Bryan","phoneNumber":"999999999"}
                                """))
                .andExpect(status().isCreated());
    }

    @Test
    void protectedRoute_withoutSession_returns401WithJsonMessage() throws Exception {
        mockMvc.perform(get("/transactions"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Unauthorized"));
    }

    @Test
    void protectedRoute_withValidSession_isAccessible() throws Exception {
        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"username":"sesion1@test.com","password":"secret123","fullName":"Test","phoneNumber":"999999999"}
                        """));

        MvcResult login = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"sesion1@test.com","password":"secret123"}
                                """))
                .andExpect(status().isOk())
                .andReturn();

        MockHttpSession session = (MockHttpSession) login.getRequest().getSession();

        mockMvc.perform(get("/transactions").session(session))
                .andExpect(status().isOk());
    }

    @Test
    void logout_invalidatesSession_soLaterRequestsAreUnauthorized() throws Exception {
        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"username":"logout1@test.com","password":"secret123","fullName":"Test","phoneNumber":"999999999"}
                        """));

        MvcResult login = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"logout1@test.com","password":"secret123"}
                                """))
                .andReturn();
        MockHttpSession session = (MockHttpSession) login.getRequest().getSession();

        mockMvc.perform(post("/auth/logout").session(session))
                .andExpect(status().isOk());

        mockMvc.perform(get("/transactions").session(session))
                .andExpect(status().isUnauthorized());
    }
}