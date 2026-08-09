package be.ephec.padel.matches;

import be.ephec.padel.members.Member;
import be.ephec.padel.members.MemberRepository;
import be.ephec.padel.members.MemberType;
import be.ephec.padel.sites.Court;
import be.ephec.padel.sites.Site;
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
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("MatchTransitionService : R9 bascule prive vers public")
class MatchTransitionServiceTest {

    @Mock
    private MatchRepository matchRepository;
    @Mock
    private MemberRepository memberRepository;

    @InjectMocks
    private MatchTransitionService transitionService;

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 24, 10, 0);

    private Site site;
    private Court court;
    private Member organizer;

    @BeforeEach
    void setUp() {
        site = TestFixtures.site("Uccle", LocalTime.of(8, 0), LocalTime.of(22, 0), 4);
        court = TestFixtures.court(site, 101L, 1);
        organizer = TestFixtures.member(1L, "G1042", MemberType.GLOBAL, null);
        when(matchRepository.save(any(MatchBooking.class))).thenAnswer(i -> i.getArgument(0));
        when(memberRepository.save(any(Member.class))).thenAnswer(i -> i.getArgument(0));
        when(matchRepository.findScheduledStartingBefore(any(), any())).thenReturn(List.of());
        when(matchRepository.findScheduledAlreadyStarted(any())).thenReturn(List.of());
    }

    private MatchBooking match(MatchVisibility visibility, LocalDateTime start) {
        return new MatchBooking(court, organizer, start, visibility);
    }

    @Nested
    @DisplayName("la veille du match")
    class DayBefore {

        @Test
        @DisplayName("un prive incomplet bascule en public")
        void incompletePrivateBecomesPublic() {
            // Arrange : 2 joueurs sur 4, match demain
            MatchBooking m = match(MatchVisibility.PRIVATE, NOW.plusHours(20));
            m.addPlayer(organizer, true);
            m.addPlayer(TestFixtures.member(2L, "G1044", MemberType.GLOBAL, null), true);
            when(matchRepository.findScheduledStartingBefore(any(), any())).thenReturn(List.of(m));

            // Act
            var report = transitionService.applyTransitions(NOW);

            // Assert
            assertThat(m.getVisibility()).isEqualTo(MatchVisibility.PUBLIC);
            assertThat(report.switchedToPublic()).isEqualTo(1);
        }

        @Test
        @DisplayName("l'organisateur d'un prive incomplet recoit une penalite d'une semaine")
        void organizerGetsOneWeekPenalty() {
            MatchBooking m = match(MatchVisibility.PRIVATE, NOW.plusHours(20));
            m.addPlayer(organizer, true);
            when(matchRepository.findScheduledStartingBefore(any(), any())).thenReturn(List.of(m));

            var report = transitionService.applyTransitions(NOW);

            assertThat(organizer.getBannedUntil()).isEqualTo(NOW.toLocalDate().plusWeeks(1));
            assertThat(report.penalisedOrganizers()).isEqualTo(1);
        }

        @Test
        @DisplayName("les places non payees sont liberees")
        void unpaidPlacesAreReleased() {
            MatchBooking m = match(MatchVisibility.PRIVATE, NOW.plusHours(20));
            m.addPlayer(organizer, true);
            m.addPlayer(TestFixtures.member(2L, "G1044", MemberType.GLOBAL, null), true);
            m.addPlayer(TestFixtures.member(3L, "G1045", MemberType.GLOBAL, null), true);
            m.addPlayer(TestFixtures.member(4L, "L7731", MemberType.FREE, null), false);
            when(matchRepository.findScheduledStartingBefore(any(), any())).thenReturn(List.of(m));

            var report = transitionService.applyTransitions(NOW);

            assertThat(report.releasedPlaces()).isEqualTo(1);
            assertThat(m.freeSlots()).isEqualTo(1);
            // Le match devient public : la place du joueur non payant est reservable.
            assertThat(m.getVisibility()).isEqualTo(MatchVisibility.PUBLIC);
        }

        @Test
        @DisplayName("un prive complet et paye reste prive")
        void completePrivateStaysPrivate() {
            MatchBooking m = match(MatchVisibility.PRIVATE, NOW.plusHours(20));
            m.addPlayer(organizer, true);
            m.addPlayer(TestFixtures.member(2L, "G1044", MemberType.GLOBAL, null), true);
            m.addPlayer(TestFixtures.member(3L, "G1045", MemberType.GLOBAL, null), true);
            m.addPlayer(TestFixtures.member(4L, "G1046", MemberType.GLOBAL, null), true);
            when(matchRepository.findScheduledStartingBefore(any(), any())).thenReturn(List.of(m));

            var report = transitionService.applyTransitions(NOW);

            assertThat(m.getVisibility()).isEqualTo(MatchVisibility.PRIVATE);
            assertThat(report.switchedToPublic()).isZero();
            assertThat(organizer.getBannedUntil()).isNull();
        }
    }

    @Nested
    @DisplayName("a l'heure du match")
    class AtMatchTime {

        @Test
        @DisplayName("le solde manquant est impute a l'organisateur")
        void outstandingChargedToOrganizer() {
            MatchBooking m = match(MatchVisibility.PUBLIC, NOW.minusMinutes(30));
            m.addPlayer(organizer, true);
            m.addPlayer(TestFixtures.member(2L, "G1044", MemberType.GLOBAL, null), true);
            when(matchRepository.findScheduledAlreadyStarted(any())).thenReturn(List.of(m));

            var report = transitionService.applyTransitions(NOW);

            assertThat(organizer.getBalanceDue()).isEqualByComparingTo(new BigDecimal("30.00"));
            assertThat(report.settledBalances()).isEqualTo(1);
            assertThat(m.getStatus()).isEqualTo(MatchStatus.CONFIRMED);
        }

        @Test
        @DisplayName("un match complet ne genere aucun solde")
        void fullyPaidMatchChargesNothing() {
            MatchBooking m = match(MatchVisibility.PUBLIC, NOW.minusMinutes(30));
            m.addPlayer(organizer, true);
            m.addPlayer(TestFixtures.member(2L, "G1044", MemberType.GLOBAL, null), true);
            m.addPlayer(TestFixtures.member(3L, "G1045", MemberType.GLOBAL, null), true);
            m.addPlayer(TestFixtures.member(4L, "G1046", MemberType.GLOBAL, null), true);
            when(matchRepository.findScheduledAlreadyStarted(any())).thenReturn(List.of(m));

            transitionService.applyTransitions(NOW);

            assertThat(organizer.getBalanceDue()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("un match termine passe en PLAYED")
        void finishedMatchBecomesPlayed() {
            MatchBooking m = match(MatchVisibility.PUBLIC, NOW.minusHours(3));
            m.addPlayer(organizer, true);
            m.addPlayer(TestFixtures.member(2L, "G1044", MemberType.GLOBAL, null), true);
            m.addPlayer(TestFixtures.member(3L, "G1045", MemberType.GLOBAL, null), true);
            m.addPlayer(TestFixtures.member(4L, "G1046", MemberType.GLOBAL, null), true);
            when(matchRepository.findScheduledAlreadyStarted(any())).thenReturn(List.of(m));

            transitionService.applyTransitions(NOW);

            assertThat(m.getStatus()).isEqualTo(MatchStatus.PLAYED);
        }
    }
}
