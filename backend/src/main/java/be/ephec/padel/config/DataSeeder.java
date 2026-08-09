package be.ephec.padel.config;

import be.ephec.padel.matches.*;
import be.ephec.padel.members.*;
import be.ephec.padel.payments.Payment;
import be.ephec.padel.payments.PaymentRepository;
import be.ephec.padel.sites.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.ApplicationArguments;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

/**
 * Jeu de donnees de test charge automatiquement au demarrage : 3 sites, leurs terrains,
 * des membres des 3 categories, des administrateurs, et une semaine de matches ancrée
 * sur la date du jour (donc jamais obsolete au moment de l'evaluation).
 *
 * <p>Idempotent : ne fait rien si la base contient deja des sites.
 */
@Component
@Profile("!test")
public class DataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    /** Mot de passe commun a tous les comptes de demonstration. */
    public static final String DEMO_PASSWORD = "Padel2026!";

    private static final String[][] PLAYER_NAMES = {
            {"Thomas", "Leroy"}, {"Julie", "Mertens"}, {"Karim", "Ben Ali"}, {"Sophie", "Dubois"},
            {"Marc", "Vandamme"}, {"Ines", "Faucon"}, {"Louis", "Peeters"}, {"Nadia", "Chraibi"},
            {"Bruno", "Callens"}, {"Eva", "Lambert"}, {"Yanis", "Roux"}, {"Chloe", "Verhoeven"},
            {"Pierre", "Anciaux"}, {"Sarah", "Blondeau"}, {"Diego", "Ferreira"}, {"Manon", "Wauters"},
            {"Antoine", "Delvaux"}, {"Lina", "Haddad"}, {"Simon", "Grosjean"}, {"Fatima", "Ouali"},
            {"Victor", "Dumont"}, {"Camille", "Renard"}, {"Hugo", "Stevens"}, {"Alice", "Marchal"}
    };

    private final SiteRepository siteRepository;
    private final CourtRepository courtRepository;
    private final SiteClosureRepository closureRepository;
    private final MemberRepository memberRepository;
    private final MatchRepository matchRepository;
    private final PaymentRepository paymentRepository;
    private final PasswordEncoder passwordEncoder;
    private final boolean enabled;
    private final String referenceDate;

    private final Random random = new Random(20260824L);

    public DataSeeder(SiteRepository siteRepository,
                      CourtRepository courtRepository,
                      SiteClosureRepository closureRepository,
                      MemberRepository memberRepository,
                      MatchRepository matchRepository,
                      PaymentRepository paymentRepository,
                      PasswordEncoder passwordEncoder,
                      @Value("${padel.seed.enabled:true}") boolean enabled,
                      @Value("${padel.seed.reference-date:}") String referenceDate) {
        this.siteRepository = siteRepository;
        this.courtRepository = courtRepository;
        this.closureRepository = closureRepository;
        this.memberRepository = memberRepository;
        this.matchRepository = matchRepository;
        this.paymentRepository = paymentRepository;
        this.passwordEncoder = passwordEncoder;
        this.enabled = enabled;
        this.referenceDate = referenceDate;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!enabled) {
            log.info("Seeding desactive (padel.seed.enabled=false)");
            return;
        }
        if (siteRepository.count() > 0) {
            log.info("Base deja peuplee : seeding ignore");
            return;
        }

        LocalDate today = referenceDate == null || referenceDate.isBlank()
                ? LocalDate.now()
                : LocalDate.parse(referenceDate);

        Site uccle = seedSite("Padel Uccle", "Avenue Brugmann 210, 1180 Uccle",
                LocalTime.of(8, 0), LocalTime.of(22, 0), 4);
        Site lln = seedSite("Padel Louvain-la-Neuve", "Rue des Sports 12, 1348 Ottignies",
                LocalTime.of(9, 0), LocalTime.of(23, 0), 3);
        Site namur = seedSite("Padel Namur", "Chaussee de Dinant 88, 5000 Namur",
                LocalTime.of(7, 30), LocalTime.of(22, 30), 6);

        List<Site> sites = List.of(uccle, lln, namur);

        // Fermetures : une par site et une globale, dans la fenetre observable.
        closureRepository.save(new SiteClosure(today.plusDays(2), "Maintenance des terrains", namur));
        closureRepository.save(new SiteClosure(today.plusDays(6), "Fermeture annuelle du reseau", null));

        List<Member> players = seedMembers(sites, today);
        seedAdmins(sites);
        seedMatches(sites, players, today);

        log.info("Seeding termine : {} sites, {} membres, {} matches",
                siteRepository.count(), memberRepository.count(), matchRepository.count());
        log.info("Comptes de demonstration (mot de passe {}) : G1042 joueur global, "
                + "S12008 membre de site LLN, L7731 membre libre, "
                + "A0001 admin global, A1001 admin du site Uccle", DEMO_PASSWORD);
    }

    private Site seedSite(String name, String address, LocalTime opening, LocalTime closing, int courts) {
        Site site = new Site(name, address, opening, closing);
        for (int i = 1; i <= courts; i++) {
            site.addCourt(i);
        }
        return siteRepository.save(site);
    }

    private List<Member> seedMembers(List<Site> sites, LocalDate today) {
        List<Member> members = new ArrayList<>();

        // Compte de demonstration principal : membre global, avec un solde du a regler.
        Member demo = member("G1042", "Thomas", "Leroy", MemberType.GLOBAL, null);
        demo.addBalance(new BigDecimal("15.00"));
        members.add(memberRepository.save(demo));

        members.add(memberRepository.save(
                member("S12008", "Julie", "Mertens", MemberType.SITE, sites.get(1))));
        members.add(memberRepository.save(
                member("L7731", "Karim", "Ben Ali", MemberType.FREE, null)));

        // Un membre sous penalite d'une semaine, pour demontrer la regle R3.
        Member penalised = member("G1043", "Sophie", "Dubois", MemberType.GLOBAL, null);
        penalised.setBannedUntil(today.plusDays(4));
        members.add(memberRepository.save(penalised));

        int globalSeq = 1044;
        int siteSeq = 12009;
        int freeSeq = 7732;
        // Rotation independante du bucket : `i % sites.size()` valait toujours 1 sur la
        // branche SITE (bucket == 1 implique i % 3 == 1), ce qui rattachait tous les
        // membres de site au meme site et laissait les deux autres sans membre.
        int siteRotation = 0;

        for (int i = 4; i < PLAYER_NAMES.length; i++) {
            String[] name = PLAYER_NAMES[i];
            int bucket = i % 3;
            Member m = switch (bucket) {
                case 0 -> member("G" + globalSeq++, name[0], name[1], MemberType.GLOBAL, null);
                case 1 -> member("S" + siteSeq++, name[0], name[1], MemberType.SITE,
                        sites.get(siteRotation++ % sites.size()));
                default -> member("L" + freeSeq++, name[0], name[1], MemberType.FREE, null);
            };
            members.add(memberRepository.save(m));
        }
        return members;
    }

    private void seedAdmins(List<Site> sites) {
        Member globalAdmin = member("A0001", "Claire", "Devos", MemberType.GLOBAL, null);
        globalAdmin.setRoles(EnumSet.of(Role.ROLE_USER, Role.ROLE_ADMIN_GLOBAL));
        memberRepository.save(globalAdmin);

        int seq = 1001;
        for (Site site : sites) {
            Member siteAdmin = member("A" + seq++, "Admin", site.getName().replace("Padel ", ""),
                    MemberType.SITE, site);
            siteAdmin.setRoles(EnumSet.of(Role.ROLE_USER, Role.ROLE_ADMIN_SITE));
            siteAdmin.setAdminSite(site);
            memberRepository.save(siteAdmin);
        }
    }

    private Member member(String matricule, String firstName, String lastName,
                          MemberType type, Site homeSite) {
        String email = (firstName + "." + lastName)
                .toLowerCase(Locale.ROOT)
                .replace(" ", "-") + "." + matricule.toLowerCase(Locale.ROOT) + "@padel.be";
        return new Member(matricule, firstName, lastName, email,
                passwordEncoder.encode(DEMO_PASSWORD), type, homeSite);
    }

    /**
     * Une semaine de matches par site, avec une repartition volontairement variee :
     * complets et payes, publics avec des places libres, prives incomplets, impayes.
     */
    private void seedMatches(List<Site> sites, List<Member> players, LocalDate today) {
        for (Site site : sites) {
            List<Court> courts = courtRepository.findBySiteIdOrderByNumberAsc(site.getId());

            for (int dayOffset = 0; dayOffset < 7; dayOffset++) {
                LocalDate day = today.plusDays(dayOffset);
                if (closureRepository.countClosuresOn(site.getId(), day) > 0) {
                    continue;
                }

                for (Court court : courts) {
                    LocalDateTime cursor = day.atTime(site.getOpeningTime());
                    LocalDateTime closing = day.atTime(site.getClosingTime());

                    while (!cursor.plus(MatchBooking.DURATION).isAfter(closing)) {
                        boolean evening = cursor.getHour() >= 17;
                        if (random.nextDouble() < (evening ? 0.75 : 0.4)) {
                            createSeededMatch(court, cursor, players);
                        }
                        cursor = cursor.plus(MatchBooking.SLOT_STEP);
                    }
                }
            }
        }
    }

    private void createSeededMatch(Court court, LocalDateTime start, List<Member> players) {
        boolean isPrivate = random.nextDouble() < 0.45;
        Member organizer = pickEligible(players, court);
        if (organizer == null) {
            return;
        }

        MatchBooking match = new MatchBooking(court, organizer,
                start, isPrivate ? MatchVisibility.PRIVATE : MatchVisibility.PUBLIC);

        int playerCount = random.nextDouble() < 0.7 ? 4 : 2 + random.nextInt(2);
        Set<Long> used = new HashSet<>();
        used.add(organizer.getId());

        boolean organizerPaid = random.nextDouble() < 0.85;
        match.addPlayer(organizer, organizerPaid);

        List<Member> paidPlayers = new ArrayList<>();
        if (organizerPaid) {
            paidPlayers.add(organizer);
        }

        for (int i = 1; i < playerCount; i++) {
            Member candidate = players.get(random.nextInt(players.size()));
            if (!used.add(candidate.getId())) {
                continue;
            }
            // Sur un match public la place n'existe qu'une fois payee ; sur un prive
            // l'organisateur peut avoir ajoute un joueur qui n'a pas encore paye.
            boolean paid = !isPrivate || random.nextDouble() < 0.8;
            match.addPlayer(candidate, paid);
            if (paid) {
                paidPlayers.add(candidate);
            }
        }

        // Le match doit exister en base avant les paiements qui le referencent.
        MatchBooking saved = matchRepository.save(match);
        for (Member payer : paidPlayers) {
            paymentRepository.save(new Payment(saved, payer, MatchBooking.SHARE,
                    start.minusDays(1), false));
        }
    }

    /** Un membre de site ne peut organiser que sur son site (R8). */
    private Member pickEligible(List<Member> players, Court court) {
        for (int attempt = 0; attempt < 12; attempt++) {
            Member candidate = players.get(random.nextInt(players.size()));
            if (candidate.getType().canBookOnAnySite()) {
                return candidate;
            }
            Site home = candidate.getHomeSite();
            if (home != null && home.getId().equals(court.getSite().getId())) {
                return candidate;
            }
        }
        return players.stream()
                .filter(m -> m.getType().canBookOnAnySite())
                .findFirst()
                .orElse(null);
    }
}
