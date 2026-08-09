package be.ephec.padel.matches;

import be.ephec.padel.members.Member;
import be.ephec.padel.members.MemberType;
import be.ephec.padel.sites.Site;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static be.ephec.padel.matches.TestFixtures.court;
import static be.ephec.padel.matches.TestFixtures.member;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("MatchBooking")
class MatchBookingTest {

    private MatchBooking match;
    private Member organizer;

    @BeforeEach
    void setUp() {
        Site site = TestFixtures.site("Padel Uccle", LocalTime.of(8, 0), LocalTime.of(22, 0), 4);
        organizer = member(1L, "G1042", MemberType.GLOBAL, null);
        match = new MatchBooking(court(site, 10L, 1), organizer,
                LocalDateTime.of(2026, 8, 25, 18, 30), MatchVisibility.PRIVATE);
    }

    @Nested
    @DisplayName("duree et grille horaire")
    class Timing {

        @Test
        @DisplayName("un match dure 1h30")
        void lastsNinetyMinutes() {
            assertThat(match.getEndTime()).isEqualTo(LocalDateTime.of(2026, 8, 25, 20, 0));
        }

        @Test
        @DisplayName("le pas de la grille est de 1h45 (1h30 + 15 min)")
        void slotStepIncludesBreak() {
            assertThat(MatchBooking.SLOT_STEP.toMinutes()).isEqualTo(105);
        }
    }

    @Nested
    @DisplayName("places et paiements")
    class Places {

        @Test
        @DisplayName("un match vide compte 4 places libres")
        void emptyMatchHasFourFreeSlots() {
            assertThat(match.freeSlots()).isEqualTo(4);
            assertThat(match.isFull()).isFalse();
        }

        @Test
        @DisplayName("le solde du correspond aux parts non payees")
        void outstandingReflectsUnpaidShares() {
            // Arrange
            match.addPlayer(organizer, true);
            match.addPlayer(member(2L, "G1044", MemberType.GLOBAL, null), false);

            // Act
            BigDecimal outstanding = match.outstandingAmount();

            // Assert : 3 parts manquantes sur 4
            assertThat(match.paidCount()).isEqualTo(1);
            assertThat(outstanding).isEqualByComparingTo(new BigDecimal("45.00"));
        }

        @Test
        @DisplayName("liberer les places non payees les rend reservables")
        void releasingUnpaidFreesSlots() {
            // Arrange
            match.addPlayer(organizer, true);
            match.addPlayer(member(2L, "G1044", MemberType.GLOBAL, null), false);
            match.addPlayer(member(3L, "L7731", MemberType.FREE, null), false);

            // Act
            int released = match.releaseUnpaidPlaces().size();

            // Assert
            assertThat(released).isEqualTo(2);
            assertThat(match.getParticipations()).hasSize(1);
            assertThat(match.freeSlots()).isEqualTo(3);
        }

        @Test
        @DisplayName("un match est integralement paye avec 4 parts")
        void fullyPaidWithFourShares() {
            match.addPlayer(organizer, true);
            match.addPlayer(member(2L, "G1044", MemberType.GLOBAL, null), true);
            match.addPlayer(member(3L, "L7731", MemberType.FREE, null), true);
            match.addPlayer(member(4L, "L7732", MemberType.FREE, null), true);

            assertThat(match.isFullyPaid()).isTrue();
            assertThat(match.outstandingAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        }
    }

    @Nested
    @DisplayName("prix")
    class Pricing {

        @Test
        @DisplayName("60 EUR par match, 15 EUR par joueur")
        void sixtyEurosSplitInFour() {
            assertThat(MatchBooking.PRICE).isEqualByComparingTo(new BigDecimal("60.00"));
            assertThat(MatchBooking.SHARE.multiply(BigDecimal.valueOf(4)))
                    .isEqualByComparingTo(MatchBooking.PRICE);
        }
    }
}
