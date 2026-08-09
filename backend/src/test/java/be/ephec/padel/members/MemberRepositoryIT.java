package be.ephec.padel.members;

import be.ephec.padel.sites.Site;
import be.ephec.padel.sites.SiteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.EnumSet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("MemberRepository (integration)")
class MemberRepositoryIT {

    @Autowired
    private MemberRepository memberRepository;
    @Autowired
    private SiteRepository siteRepository;

    private Site uccle;
    private Site lln;

    @BeforeEach
    void setUp() {
        uccle = siteRepository.save(new Site("Uccle", "adresse 1",
                LocalTime.of(8, 0), LocalTime.of(22, 0)));
        lln = siteRepository.save(new Site("LLN", "adresse 2",
                LocalTime.of(9, 0), LocalTime.of(23, 0)));
    }

    private Member save(String matricule, MemberType type, Site homeSite) {
        return memberRepository.save(new Member(matricule, "Prenom", "Nom",
                matricule.toLowerCase() + "@padel.be", "hash", type, homeSite));
    }

    @Nested
    @DisplayName("recherche par matricule")
    class ByMatricule {

        @Test
        @DisplayName("la recherche ignore la casse")
        void caseInsensitive() {
            save("G1042", MemberType.GLOBAL, null);

            assertThat(memberRepository.findByMatriculeIgnoreCase("g1042")).isPresent();
        }

        @Test
        @DisplayName("un matricule inconnu renvoie vide")
        void unknownIsEmpty() {
            assertThat(memberRepository.findByMatriculeIgnoreCase("G9999")).isEmpty();
        }
    }

    @Nested
    @DisplayName("contraintes d'unicite")
    class Uniqueness {

        @Test
        @DisplayName("deux membres ne partagent pas un matricule")
        void matriculeIsUnique() {
            save("G1042", MemberType.GLOBAL, null);

            assertThatThrownBy(() -> {
                memberRepository.save(new Member("G1042", "Autre", "Personne",
                        "autre@padel.be", "hash", MemberType.GLOBAL, null));
                memberRepository.flush();
            }).isInstanceOf(DataIntegrityViolationException.class);
        }
    }

    @Nested
    @DisplayName("visibilite depuis un site")
    class Visibility {

        @Test
        @DisplayName("un site voit ses membres, plus tous les globaux et libres")
        void siteSeesOwnPlusGlobalAndFree() {
            // Arrange
            save("S12001", MemberType.SITE, uccle);
            save("S12002", MemberType.SITE, lln);
            save("G1042", MemberType.GLOBAL, null);
            save("L7731", MemberType.FREE, null);

            // Act
            var visible = memberRepository.findVisibleFromSite(uccle.getId());

            // Assert : le membre de LLN n'apparait pas
            assertThat(visible).extracting(Member::getMatricule)
                    .containsExactlyInAnyOrder("S12001", "G1042", "L7731");
        }
    }

    @Nested
    @DisplayName("comptage et soldes")
    class Counting {

        @Test
        @DisplayName("le comptage par categorie est correct")
        void countsByType() {
            save("G1042", MemberType.GLOBAL, null);
            save("G1043", MemberType.GLOBAL, null);
            save("S12001", MemberType.SITE, uccle);

            assertThat(memberRepository.countByMemberType(MemberType.GLOBAL)).isEqualTo(2);
            assertThat(memberRepository.countByMemberType(MemberType.SITE)).isEqualTo(1);
            assertThat(memberRepository.countByMemberType(MemberType.FREE)).isZero();
        }

        @Test
        @DisplayName("le solde et les roles sont persistes")
        void balanceAndRolesPersisted() {
            Member member = save("A0001", MemberType.GLOBAL, null);
            member.addBalance(new BigDecimal("15.00"));
            member.setRoles(EnumSet.of(Role.ROLE_USER, Role.ROLE_ADMIN_GLOBAL));
            memberRepository.saveAndFlush(member);

            Member reloaded = memberRepository.findByMatriculeIgnoreCase("A0001").orElseThrow();

            assertThat(reloaded.getBalanceDue()).isEqualByComparingTo(new BigDecimal("15.00"));
            assertThat(reloaded.getRoles()).contains(Role.ROLE_ADMIN_GLOBAL);
        }
    }
}
