package be.ephec.padel.members;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("MemberType")
class MemberTypeTest {

    @Nested
    @DisplayName("fenetre de reservation")
    class BookingWindow {

        @Test
        @DisplayName("un membre global reserve 21 jours a l'avance")
        void globalHasThreeWeeks() {
            assertThat(MemberType.GLOBAL.getBookingWindowDays()).isEqualTo(21);
        }

        @Test
        @DisplayName("un membre de site reserve 14 jours a l'avance")
        void siteHasTwoWeeks() {
            assertThat(MemberType.SITE.getBookingWindowDays()).isEqualTo(14);
        }

        @Test
        @DisplayName("un membre libre reserve 5 jours a l'avance")
        void freeHasFiveDays() {
            assertThat(MemberType.FREE.getBookingWindowDays()).isEqualTo(5);
        }
    }

    @Nested
    @DisplayName("portee des sites")
    class SiteScope {

        @Test
        @DisplayName("seul le membre de site est limite a son site")
        void onlySiteMemberIsRestricted() {
            assertThat(MemberType.GLOBAL.canBookOnAnySite()).isTrue();
            assertThat(MemberType.FREE.canBookOnAnySite()).isTrue();
            assertThat(MemberType.SITE.canBookOnAnySite()).isFalse();
        }
    }

    @Nested
    @DisplayName("deduction depuis le matricule")
    class FromMatricule {

        @ParameterizedTest
        @CsvSource({"G1042,GLOBAL", "S12008,SITE", "L7731,FREE", "g1042,GLOBAL"})
        @DisplayName("le prefixe determine la categorie")
        void prefixDeterminesType(String matricule, MemberType expected) {
            assertThat(MemberType.fromMatricule(matricule)).isEqualTo(expected);
        }

        @Test
        @DisplayName("un prefixe inconnu est refuse")
        void unknownPrefixRejected() {
            assertThatThrownBy(() -> MemberType.fromMatricule("X1234"))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("un matricule vide est refuse")
        void blankRejected() {
            assertThatThrownBy(() -> MemberType.fromMatricule(" "))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }
}
