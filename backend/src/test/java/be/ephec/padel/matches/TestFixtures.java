package be.ephec.padel.matches;

import be.ephec.padel.members.Member;
import be.ephec.padel.members.MemberType;
import be.ephec.padel.sites.Court;
import be.ephec.padel.sites.Site;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalTime;

/** Fabriques d'objets metier pour les tests unitaires (entites sans persistance). */
final class TestFixtures {

    private TestFixtures() {
    }

    static Site site(String name, LocalTime opening, LocalTime closing, int courts) {
        Site site = new Site(name, name + " address", opening, closing);
        ReflectionTestUtils.setField(site, "id", (long) Math.abs(name.hashCode() % 1000));
        for (int i = 1; i <= courts; i++) {
            Court court = site.addCourt(i);
            ReflectionTestUtils.setField(court, "id", (long) (100 + i));
        }
        return site;
    }

    static Court court(Site site, Long id, int number) {
        Court court = site.getCourts().stream()
                .filter(c -> c.getNumber() == number)
                .findFirst()
                .orElseGet(() -> site.addCourt(number));
        ReflectionTestUtils.setField(court, "id", id);
        return court;
    }

    static Member member(Long id, String matricule, MemberType type, Site homeSite) {
        Member member = new Member(matricule, "Prenom", matricule, matricule.toLowerCase() + "@padel.be",
                "$2a$10$hash", type, homeSite);
        ReflectionTestUtils.setField(member, "id", id);
        return member;
    }
}
