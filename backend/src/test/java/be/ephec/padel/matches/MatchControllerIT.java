package be.ephec.padel.matches;

import be.ephec.padel.members.Member;
import be.ephec.padel.members.MemberRepository;
import be.ephec.padel.members.MemberType;
import be.ephec.padel.sites.Court;
import be.ephec.padel.sites.CourtRepository;
import be.ephec.padel.sites.Site;
import be.ephec.padel.sites.SiteRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
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

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("MatchController (integration, happy flow)")
class MatchControllerIT {

    @Autowired
    private WebApplicationContext context;
    @Autowired
    private MemberRepository memberRepository;
    @Autowired
    private SiteRepository siteRepository;
    @Autowired
    private CourtRepository courtRepository;
    @Autowired
    private MatchRepository matchRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    private final ObjectMapper json = new ObjectMapper().registerModule(new JavaTimeModule());

    private MockMvc mockMvc;
    private Court court;
    private String token;
    private String joinerToken;

    @BeforeEach
    void setUp() throws Exception {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        matchRepository.deleteAll();
        memberRepository.deleteAll();
        courtRepository.deleteAll();
        siteRepository.deleteAll();

        Site site = new Site("Uccle", "adresse", LocalTime.of(8, 0), LocalTime.of(22, 0));
        site.addCourt(1);
        site = siteRepository.save(site);
        court = courtRepository.findBySiteIdOrderByNumberAsc(site.getId()).get(0);

        memberRepository.save(new Member("G1042", "Thomas", "Leroy", "t@padel.be",
                passwordEncoder.encode("Padel2026!"), MemberType.GLOBAL, null));
        memberRepository.save(new Member("L7731", "Karim", "Ben Ali", "k@padel.be",
                passwordEncoder.encode("Padel2026!"), MemberType.FREE, null));

        token = login("G1042");
        joinerToken = login("L7731");
    }

    private String login(String matricule) throws Exception {
        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(
                                Map.of("matricule", matricule, "password", "Padel2026!"))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return json.readTree(response).get("token").asText();
    }

    private String createBody(LocalDate day, LocalTime time, String visibility) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("courtId", court.getId());
        body.put("startTime", day.atTime(time).toString());
        body.put("visibility", visibility);
        return json.writeValueAsString(body);
    }

    @Nested
    @DisplayName("POST /api/matches")
    class Create {

        @Test
        @DisplayName("un membre global cree un match public")
        void createsPublicMatch() throws Exception {
            mockMvc.perform(post("/api/matches")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(createBody(LocalDate.now().plusDays(3), LocalTime.of(8, 0), "PUBLIC")))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.visibility").value("PUBLIC"))
                    .andExpect(jsonPath("$.price").value(60.00))
                    .andExpect(jsonPath("$.freeSlots").value(3))
                    .andExpect(jsonPath("$.participants.length()").value(1));
        }

        @Test
        @DisplayName("le meme creneau deux fois retourne 409")
        void doubleBookingConflicts() throws Exception {
            String body = createBody(LocalDate.now().plusDays(4), LocalTime.of(9, 45), "PUBLIC");

            mockMvc.perform(post("/api/matches").header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isCreated());

            mockMvc.perform(post("/api/matches").header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.message").value(
                            org.hamcrest.Matchers.containsString("deja reserve")));
        }

        @Test
        @DisplayName("hors fenetre de reservation, 409 avec un message explicite")
        void beyondWindowConflicts() throws Exception {
            mockMvc.perform(post("/api/matches")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(createBody(LocalDate.now().plusDays(30), LocalTime.of(8, 0), "PUBLIC")))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.message").value(
                            org.hamcrest.Matchers.containsString("21 jours")));
        }

        @Test
        @DisplayName("sans token, la creation est refusee")
        void anonymousCannotCreate() throws Exception {
            mockMvc.perform(post("/api/matches")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(createBody(LocalDate.now().plusDays(3), LocalTime.of(8, 0), "PUBLIC")))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("parcours complet : creation, paiement, inscription")
    class HappyFlow {

        @Test
        @DisplayName("un joueur rejoint un match public et sa place est validee")
        void joinFlow() throws Exception {
            String created = mockMvc.perform(post("/api/matches")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(createBody(LocalDate.now().plusDays(2), LocalTime.of(11, 30), "PUBLIC")))
                    .andExpect(status().isCreated())
                    .andReturn().getResponse().getContentAsString();
            long matchId = json.readTree(created).get("id").asLong();

            // L'organisateur paie sa part
            mockMvc.perform(post("/api/payments/match/" + matchId)
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.amount").value(15.00));

            // Un autre membre rejoint : la place est validee par le paiement
            mockMvc.perform(post("/api/matches/" + matchId + "/join")
                            .header("Authorization", "Bearer " + joinerToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.paidParticipantsCount").value(2))
                    .andExpect(jsonPath("$.freeSlots").value(2));

            // Rejoindre deux fois est refuse
            mockMvc.perform(post("/api/matches/" + matchId + "/join")
                            .header("Authorization", "Bearer " + joinerToken))
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("le match cree apparait dans mes matches et dans les matches publics")
        void appearsInListings() throws Exception {
            mockMvc.perform(post("/api/matches")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(createBody(LocalDate.now().plusDays(5), LocalTime.of(13, 15), "PUBLIC")))
                    .andExpect(status().isCreated());

            mockMvc.perform(get("/api/matches/me").header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1));

            mockMvc.perform(get("/api/matches/public").header("Authorization", "Bearer " + joinerToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1));
        }
    }

    @Nested
    @DisplayName("GET /api/sites/{id}/planning")
    class Planning {

        @Test
        @DisplayName("le planning expose la grille du site avec les matches")
        void planningExposesGrid() throws Exception {
            mockMvc.perform(post("/api/matches")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(createBody(LocalDate.now().plusDays(1), LocalTime.of(8, 0), "PUBLIC")))
                    .andExpect(status().isCreated());

            mockMvc.perform(get("/api/sites/" + court.getSite().getId() + "/planning")
                            .param("day", LocalDate.now().plusDays(1).toString())
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.closed").value(false))
                    .andExpect(jsonPath("$.slots.length()").value(8))
                    .andExpect(jsonPath("$.slots[0].match.visibility").value("PUBLIC"));
        }
    }
}
