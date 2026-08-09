package be.ephec.padel.auth;

import be.ephec.padel.members.Member;
import be.ephec.padel.members.MemberRepository;
import be.ephec.padel.members.MemberType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.Map;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("AuthController (integration)")
class AuthControllerIT {

    @Autowired
    private WebApplicationContext context;
    @Autowired
    private MemberRepository memberRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private ObjectMapper objectMapper;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        memberRepository.deleteAll();
        memberRepository.save(new Member("G1042", "Thomas", "Leroy", "thomas@padel.be",
                passwordEncoder.encode("Padel2026!"), MemberType.GLOBAL, null));
    }

    private String body(String matricule, String password) throws Exception {
        return objectMapper.writeValueAsString(Map.of("matricule", matricule, "password", password));
    }

    @Nested
    @DisplayName("POST /api/auth/login")
    class Login {

        @Test
        @DisplayName("des identifiants valides retournent un JWT et le profil")
        void validCredentialsReturnToken() throws Exception {
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body("G1042", "Padel2026!")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.token").isNotEmpty())
                    .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                    .andExpect(jsonPath("$.matricule").value("G1042"))
                    .andExpect(jsonPath("$.memberType").value("GLOBAL"))
                    .andExpect(jsonPath("$.roles[0]").value("ROLE_USER"));
        }

        @Test
        @DisplayName("un mauvais mot de passe retourne 401 sans detail")
        void wrongPasswordIsUnauthorized() throws Exception {
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body("G1042", "mauvais")))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.message").value("Identifiants invalides"));
        }

        @Test
        @DisplayName("un matricule inconnu retourne le meme 401 generique")
        void unknownMatriculeIsUnauthorized() throws Exception {
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body("G9999", "Padel2026!")))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.message").value("Identifiants invalides"));
        }

        @Test
        @DisplayName("un matricule au mauvais format retourne 400 avec le detail du champ")
        void malformedMatriculeIsBadRequest() throws Exception {
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body("X1", "Padel2026!")))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fieldErrors.matricule").isNotEmpty());
        }
    }

    @Nested
    @DisplayName("protection des routes")
    class RouteProtection {

        @Test
        @DisplayName("sans token, /api/members/me est refuse")
        void meRequiresAuthentication() throws Exception {
            mockMvc.perform(get("/api/members/me"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("avec un token valide, /api/members/me retourne le profil")
        void meReturnsProfileWithToken() throws Exception {
            String response = mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body("G1042", "Padel2026!")))
                    .andReturn().getResponse().getContentAsString();
            String token = objectMapper.readTree(response).get("token").asText();

            mockMvc.perform(get("/api/members/me").header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.matricule").value("G1042"))
                    .andExpect(jsonPath("$.bookingWindowDays").value(21));
        }

        @Test
        @DisplayName("un token bidon est ignore et la requete reste refusee")
        void garbageTokenIsRejected() throws Exception {
            mockMvc.perform(get("/api/members/me").header("Authorization", "Bearer pas-un-jwt"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("les stats globales sont refusees a un simple utilisateur")
        void statsForbiddenForPlainUser() throws Exception {
            String response = mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body("G1042", "Padel2026!")))
                    .andReturn().getResponse().getContentAsString();
            String token = objectMapper.readTree(response).get("token").asText();

            mockMvc.perform(get("/api/stats/global").header("Authorization", "Bearer " + token))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("la liste des sites est publique")
        void sitesArePublic() throws Exception {
            mockMvc.perform(get("/api/sites")).andExpect(status().isOk());
        }
    }
}
