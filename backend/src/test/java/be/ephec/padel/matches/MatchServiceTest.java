package be.ephec.padel.matches;

import be.ephec.padel.common.exceptions.BusinessException;
import be.ephec.padel.matches.dto.CreateMatchRequest;
import be.ephec.padel.members.Member;
import be.ephec.padel.members.MemberRepository;
import be.ephec.padel.members.MemberType;
import be.ephec.padel.payments.PaymentService;
import be.ephec.padel.sites.Court;
import be.ephec.padel.sites.CourtRepository;
import be.ephec.padel.sites.Site;
import be.ephec.padel.sites.SiteClosureRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("MatchService : regles de reservation")
class MatchServiceTest {

    @Mock
    private MatchRepository matchRepository;
    @Mock
    private CourtRepository courtRepository;
    @Mock
    private MemberRepository memberRepository;
    @Mock
    private SiteClosureRepository closureRepository;
    @Mock
    private PaymentService paymentService;

    @InjectMocks
    private MatchService matchService;

    private Site uccle;
    private Site lln;
    private Court court;
    private Member globalMember;

    @BeforeEach
    void setUp() {
        uccle = TestFixtures.site("Uccle", LocalTime.of(8, 0), LocalTime.of(22, 0), 4);
        lln = TestFixtures.site("LLN", LocalTime.of(9, 0), LocalTime.of(23, 0), 3);
        court = TestFixtures.court(uccle, 101L, 1);
        globalMember = TestFixtures.member(1L, "G1042", MemberType.GLOBAL, null);

        when(courtRepository.findById(101L)).thenReturn(Optional.of(court));
        when(closureRepository.countClosuresOn(anyLong(), any())).thenReturn(0L);
        when(matchRepository.existsByCourtIdAndStartTime(anyLong(), any())).thenReturn(false);
        when(matchRepository.save(any(MatchBooking.class))).thenAnswer(i -> i.getArgument(0));
    }

    /** Premier creneau d'Uccle dans n jours : 08:00, aligne sur la grille. */
    private LocalDateTime slotIn(long days) {
        return LocalDate.now().plusDays(days).atTime(8, 0);
    }

    private CreateMatchRequest request(LocalDateTime start, MatchVisibility visibility, List<String> guests) {
        return new CreateMatchRequest(101L, start, visibility, guests);
    }

    @Nested
    @DisplayName("R2 fenetre de reservation")
    class BookingWindow {

        @Test
        @DisplayName("un membre global reserve a 21 jours")
        void globalCanBookAtTwentyOneDays() {
            MatchBooking match = matchService.createMatch(globalMember,
                    request(slotIn(21), MatchVisibility.PUBLIC, null));

            assertThat(match.getOrganizer()).isEqualTo(globalMember);
            assertThat(match.getParticipations()).hasSize(1);
        }

        @Test
        @DisplayName("un membre global est refuse a 22 jours")
        void globalRejectedBeyondWindow() {
            assertThatThrownBy(() -> matchService.createMatch(globalMember,
                    request(slotIn(22), MatchVisibility.PUBLIC, null)))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("21 jours");
        }

        @Test
        @DisplayName("un membre libre est refuse a 6 jours")
        void freeMemberRejectedBeyondFiveDays() {
            Member free = TestFixtures.member(2L, "L7731", MemberType.FREE, null);

            assertThatThrownBy(() -> matchService.createMatch(free,
                    request(slotIn(6), MatchVisibility.PUBLIC, null)))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("5 jours");
        }

        @Test
        @DisplayName("une date passee est refusee")
        void pastDateRejected() {
            assertThatThrownBy(() -> matchService.createMatch(globalMember,
                    request(slotIn(-1), MatchVisibility.PUBLIC, null)))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("passee");
        }
    }

    @Nested
    @DisplayName("R3 penalite et R4 solde")
    class BlockingStates {

        @Test
        @DisplayName("une penalite active bloque la reservation")
        void activePenaltyBlocks() {
            globalMember.applyOneWeekPenalty(LocalDate.now());

            assertThatThrownBy(() -> matchService.createMatch(globalMember,
                    request(slotIn(3), MatchVisibility.PUBLIC, null)))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Penalite active");
        }

        @Test
        @DisplayName("une penalite expiree ne bloque plus")
        void expiredPenaltyDoesNotBlock() {
            globalMember.setBannedUntil(LocalDate.now().minusDays(1));

            MatchBooking match = matchService.createMatch(globalMember,
                    request(slotIn(3), MatchVisibility.PUBLIC, null));

            assertThat(match).isNotNull();
        }

        @Test
        @DisplayName("un solde du bloque la reservation")
        void outstandingBalanceBlocks() {
            globalMember.addBalance(new BigDecimal("15.00"));

            assertThatThrownBy(() -> matchService.createMatch(globalMember,
                    request(slotIn(3), MatchVisibility.PUBLIC, null)))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("solde");
        }
    }

    @Nested
    @DisplayName("R8 portee du membre de site")
    class SiteScope {

        @Test
        @DisplayName("un membre de site reserve sur son site")
        void siteMemberBooksOnHomeSite() {
            Member siteMember = TestFixtures.member(3L, "S12008", MemberType.SITE, uccle);

            MatchBooking match = matchService.createMatch(siteMember,
                    request(slotIn(3), MatchVisibility.PUBLIC, null));

            assertThat(match.getCourt().getSite()).isEqualTo(uccle);
        }

        @Test
        @DisplayName("un membre de site est refuse sur un autre site")
        void siteMemberRejectedElsewhere() {
            Member siteMember = TestFixtures.member(3L, "S12008", MemberType.SITE, lln);

            assertThatThrownBy(() -> matchService.createMatch(siteMember,
                    request(slotIn(3), MatchVisibility.PUBLIC, null)))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("site de rattachement");
        }
    }

    @Nested
    @DisplayName("R5 fermetures, R6 horaires, R1 grille")
    class SiteAvailability {

        @Test
        @DisplayName("un jour de fermeture est refuse")
        void closureRejected() {
            when(closureRepository.countClosuresOn(anyLong(), any())).thenReturn(1L);

            assertThatThrownBy(() -> matchService.createMatch(globalMember,
                    request(slotIn(3), MatchVisibility.PUBLIC, null)))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("ferme");
        }

        @Test
        @DisplayName("un creneau avant l'ouverture est refuse")
        void beforeOpeningRejected() {
            LocalDateTime tooEarly = LocalDate.now().plusDays(3).atTime(7, 0);

            assertThatThrownBy(() -> matchService.createMatch(globalMember,
                    request(tooEarly, MatchVisibility.PUBLIC, null)))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("ouvre");
        }

        @Test
        @DisplayName("un match qui finirait apres la fermeture est refuse")
        void afterClosingRejected() {
            LocalDateTime tooLate = LocalDate.now().plusDays(3).atTime(21, 15);

            assertThatThrownBy(() -> matchService.createMatch(globalMember,
                    request(tooLate, MatchVisibility.PUBLIC, null)))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("un creneau non aligne sur la grille de 1h45 est refuse")
        void unalignedSlotRejected() {
            LocalDateTime unaligned = LocalDate.now().plusDays(3).atTime(9, 0);

            assertThatThrownBy(() -> matchService.createMatch(globalMember,
                    request(unaligned, MatchVisibility.PUBLIC, null)))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Creneau invalide");
        }

        @Test
        @DisplayName("le deuxieme creneau du jour est a 09:45")
        void secondSlotAccepted() {
            LocalDateTime second = LocalDate.now().plusDays(3).atTime(9, 45);

            MatchBooking match = matchService.createMatch(globalMember,
                    request(second, MatchVisibility.PUBLIC, null));

            assertThat(match.getStartTime()).isEqualTo(second);
        }
    }

    @Nested
    @DisplayName("R7 double reservation")
    class DoubleBooking {

        @Test
        @DisplayName("un terrain deja pris sur le creneau est refuse")
        void takenCourtRejected() {
            when(matchRepository.existsByCourtIdAndStartTime(anyLong(), any())).thenReturn(true);

            assertThatThrownBy(() -> matchService.createMatch(globalMember,
                    request(slotIn(3), MatchVisibility.PUBLIC, null)))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("deja reserve");
        }
    }

    @Nested
    @DisplayName("R10 match public et match prive")
    class Visibility {

        @Test
        @DisplayName("l'organisateur d'un prive ajoute lui-meme les joueurs")
        void privateOrganizerAddsGuests() {
            Member guest = TestFixtures.member(4L, "G1044", MemberType.GLOBAL, null);
            when(memberRepository.findByMatriculeIgnoreCase("G1044")).thenReturn(Optional.of(guest));

            MatchBooking match = matchService.createMatch(globalMember,
                    request(slotIn(3), MatchVisibility.PRIVATE, List.of("G1044")));

            assertThat(match.getParticipations()).hasSize(2);
            assertThat(match.freeSlots()).isEqualTo(2);
        }

        @Test
        @DisplayName("sur un match public l'organisateur ne peut inscrire personne")
        void publicOrganizerCannotAddGuests() {
            assertThatThrownBy(() -> matchService.createMatch(globalMember,
                    request(slotIn(3), MatchVisibility.PUBLIC, List.of("G1044"))))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("chaque joueur s'inscrit");
        }

        @Test
        @DisplayName("un joueur inconnu fait echouer la creation")
        void unknownGuestRejected() {
            when(memberRepository.findByMatriculeIgnoreCase("G9999")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> matchService.createMatch(globalMember,
                    request(slotIn(3), MatchVisibility.PRIVATE, List.of("G9999"))))
                    .hasMessageContaining("introuvable");
        }
    }

    @Nested
    @DisplayName("inscription a un match public")
    class Joining {

        private MatchBooking publicMatch;

        @BeforeEach
        void createPublicMatch() {
            publicMatch = new MatchBooking(court, globalMember, slotIn(3), MatchVisibility.PUBLIC);
            publicMatch.addPlayer(globalMember, true);
            when(matchRepository.findById(50L)).thenReturn(Optional.of(publicMatch));
        }

        @Test
        @DisplayName("un joueur rejoint et son paiement est declenche")
        void joinTriggersPayment() {
            Member joiner = TestFixtures.member(5L, "L7731", MemberType.FREE, null);

            matchService.joinPublicMatch(50L, joiner);

            assertThat(publicMatch.hasPlayer(5L)).isTrue();
            verify(paymentService).payShare(publicMatch, joiner);
        }

        @Test
        @DisplayName("on ne rejoint pas deux fois le meme match")
        void cannotJoinTwice() {
            assertThatThrownBy(() -> matchService.joinPublicMatch(50L, globalMember))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("participez deja");
        }

        @Test
        @DisplayName("un match complet refuse une inscription supplementaire")
        void fullMatchRejects() {
            publicMatch.addPlayer(TestFixtures.member(6L, "G1045", MemberType.GLOBAL, null), true);
            publicMatch.addPlayer(TestFixtures.member(7L, "G1046", MemberType.GLOBAL, null), true);
            publicMatch.addPlayer(TestFixtures.member(8L, "G1047", MemberType.GLOBAL, null), true);

            Member late = TestFixtures.member(9L, "G1048", MemberType.GLOBAL, null);

            assertThatThrownBy(() -> matchService.joinPublicMatch(50L, late))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("complet");
        }

        @Test
        @DisplayName("un match prive n'est pas rejoignable librement")
        void privateMatchNotJoinable() {
            MatchBooking privateMatch = new MatchBooking(court, globalMember, slotIn(3),
                    MatchVisibility.PRIVATE);
            when(matchRepository.findById(51L)).thenReturn(Optional.of(privateMatch));

            assertThatThrownBy(() -> matchService.joinPublicMatch(51L,
                    TestFixtures.member(5L, "L7731", MemberType.FREE, null)))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("prive");
        }
    }

    @Nested
    @DisplayName("grille horaire d'un site")
    class Slots {

        @Test
        @DisplayName("Uccle 08:00-22:00 offre 8 creneaux")
        void ucclePlanningHasEightSlots() {
            List<LocalDateTime> slots = matchService.slotsOf(uccle, LocalDate.now());

            assertThat(slots).hasSize(8);
            assertThat(slots.get(0).toLocalTime()).isEqualTo(LocalTime.of(8, 0));
            assertThat(slots.get(1).toLocalTime()).isEqualTo(LocalTime.of(9, 45));
            assertThat(slots.get(7).toLocalTime()).isEqualTo(LocalTime.of(20, 15));
        }
    }
}
