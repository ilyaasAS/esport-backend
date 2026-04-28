package org.example.auth;

import org.example.auth.config.SecurityConfig;
import org.example.auth.security.JwtAuthenticationFilter;
import org.example.auth.security.RestAccessDeniedHandler;
import org.example.auth.security.RestAuthenticationEntryPoint;
import org.example.services.TournamentService;
import org.example.web.advice.GlobalExceptionHandler;
import org.example.web.controller.TournamentController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = TournamentController.class)
@AutoConfigureMockMvc(addFilters = true)
@Import({
        SecurityConfig.class,
        GlobalExceptionHandler.class,
        RestAuthenticationEntryPoint.class,
        RestAccessDeniedHandler.class
})
class SecurityAccessIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TournamentService tournamentService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    void shouldAllowPublicAccessToGetPlayers() throws Exception {
        when(tournamentService.getPlayers()).thenReturn(List.of());

        mockMvc.perform(get("/api/players"))
                .andExpect(status().isOk());
    }

    @Test
    void shouldRejectCreatePlayerWithoutAuthentication() throws Exception {
        mockMvc.perform(post("/api/players")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nickname": "Ninja",
                                  "level": 18
                                }
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "USER")
    void shouldAllowCreatePlayerForUserRole() throws Exception {
        doNothing().when(tournamentService).addPlayer("Ninja", 18);

        mockMvc.perform(post("/api/players")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nickname": "Ninja",
                                  "level": 18
                                }
                                """))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(roles = "USER")
    void shouldForbidCreateMatchForUserRole() throws Exception {
        mockMvc.perform(post("/api/matches")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "player1Id": 1,
                                  "player2Id": 2,
                                  "score1": 10,
                                  "score2": 8
                                }
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldAllowCreateMatchForAdminRole() throws Exception {
        doNothing().when(tournamentService).createMatch(1, 2, 10, 8);

        mockMvc.perform(post("/api/matches")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "player1Id": 1,
                                  "player2Id": 2,
                                  "score1": 10,
                                  "score2": 8
                                }
                                """))
                .andExpect(status().isCreated());
    }
}
