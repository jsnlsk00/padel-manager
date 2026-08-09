package be.ephec.padel.sites;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("SiteClosureRepository (integration)")
class SiteClosureRepositoryIT {

    @Autowired
    private SiteRepository siteRepository;
    @Autowired
    private SiteClosureRepository closureRepository;

    private Site uccle;
    private Site namur;
    private final LocalDate day = LocalDate.of(2026, 8, 26);

    @BeforeEach
    void setUp() {
        uccle = siteRepository.save(new Site("Uccle", "adresse 1",
                LocalTime.of(8, 0), LocalTime.of(22, 0)));
        namur = siteRepository.save(new Site("Namur", "adresse 3",
                LocalTime.of(7, 30), LocalTime.of(22, 30)));
    }

    @Test
    @DisplayName("une fermeture de site ne concerne que ce site")
    void siteClosureIsScoped() {
        closureRepository.save(new SiteClosure(day, "Maintenance", namur));

        assertThat(closureRepository.countClosuresOn(namur.getId(), day)).isEqualTo(1);
        assertThat(closureRepository.countClosuresOn(uccle.getId(), day)).isZero();
    }

    @Test
    @DisplayName("une fermeture globale concerne tous les sites")
    void globalClosureAppliesEverywhere() {
        closureRepository.save(new SiteClosure(day, "Fermeture du reseau", null));

        assertThat(closureRepository.countClosuresOn(namur.getId(), day)).isEqualTo(1);
        assertThat(closureRepository.countClosuresOn(uccle.getId(), day)).isEqualTo(1);
    }

    @Test
    @DisplayName("les fermetures applicables cumulent site et global")
    void applicableClosuresCombineBoth() {
        closureRepository.save(new SiteClosure(day, "Maintenance", namur));
        closureRepository.save(new SiteClosure(day.plusDays(4), "Fermeture du reseau", null));

        assertThat(closureRepository.findApplicableToSite(namur.getId())).hasSize(2);
        assertThat(closureRepository.findApplicableToSite(uccle.getId())).hasSize(1);
    }
}
