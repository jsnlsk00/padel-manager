package be.ephec.padel.stats;

import be.ephec.padel.matches.*;
import be.ephec.padel.members.MemberService;
import be.ephec.padel.members.MemberType;
import be.ephec.padel.payments.PaymentService;
import be.ephec.padel.sites.Site;
import be.ephec.padel.sites.SiteClosureRepository;
import be.ephec.padel.sites.SiteRepository;
import be.ephec.padel.stats.dto.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class StatsService {

    /** Fenetre d'observation du tableau de bord. */
    private static final int WINDOW_DAYS = 7;

    private final MatchRepository matchRepository;
    private final MatchParticipationRepository participationRepository;
    private final SiteRepository siteRepository;
    private final SiteClosureRepository closureRepository;
    private final PaymentService paymentService;
    private final MemberService memberService;

    public StatsService(MatchRepository matchRepository,
                        MatchParticipationRepository participationRepository,
                        SiteRepository siteRepository,
                        SiteClosureRepository closureRepository,
                        PaymentService paymentService,
                        MemberService memberService) {
        this.matchRepository = matchRepository;
        this.participationRepository = participationRepository;
        this.siteRepository = siteRepository;
        this.closureRepository = closureRepository;
        this.paymentService = paymentService;
        this.memberService = memberService;
    }

    /** siteId null : portee globale (admin global). Sinon portee d'un seul site. */
    @Transactional(readOnly = true)
    public StatsDto compute(Long siteId) {
        List<MatchBooking> matches = matchRepository.findAllForStats(siteId);
        List<Site> sites = siteId == null
                ? siteRepository.findAll()
                : List.of(siteRepository.findById(siteId).orElseThrow());

        long publicMatches = matches.stream()
                .filter(m -> m.getVisibility() == MatchVisibility.PUBLIC).count();
        long cancelled = matches.stream()
                .filter(m -> m.getStatus() == MatchStatus.CANCELLED).count();
        long played = matches.stream()
                .filter(m -> m.getStatus() == MatchStatus.PLAYED).count();

        List<UnpaidDto> unpaid = participationRepository.findUnpaid(siteId).stream()
                .map(p -> new UnpaidDto(
                        p.getMatch().getId(),
                        p.getMatch().getStartTime(),
                        p.getMatch().getCourt().getSite().getName(),
                        p.getPlayer().getMatricule(),
                        p.getPlayer().getFullName(),
                        p.getMatch().getVisibility() == MatchVisibility.PRIVATE
                                ? "Match prive non solde"
                                : "Place non payee",
                        MatchBooking.SHARE))
                .toList();

        BigDecimal outstanding = MatchBooking.SHARE.multiply(BigDecimal.valueOf(unpaid.size()));

        List<SiteStatsDto> perSite = sites.stream().map(site -> {
            List<MatchBooking> siteMatches = matchRepository.findAllForStats(site.getId());
            return new SiteStatsDto(
                    site.getId(),
                    site.getName(),
                    site.getCourts().size(),
                    siteMatches.size(),
                    occupancy(site, siteMatches.size()),
                    paymentService.revenue(site.getId()));
        }).toList();

        int occupancy = sites.size() == 1
                ? occupancy(sites.get(0), matches.size())
                : weightedOccupancy(perSite);

        List<MemberCountDto> membersByType = List.of(
                new MemberCountDto(MemberType.GLOBAL, "Membres globaux",
                        memberService.countByType(MemberType.GLOBAL)),
                new MemberCountDto(MemberType.SITE, "Membres de site",
                        memberService.countByType(MemberType.SITE)),
                new MemberCountDto(MemberType.FREE, "Membres libres",
                        memberService.countByType(MemberType.FREE)));

        return new StatsDto(
                siteId,
                siteId == null ? "GLOBAL" : "SITE",
                matches.size(),
                publicMatches,
                matches.size() - publicMatches,
                cancelled,
                played,
                paymentService.revenue(siteId),
                outstanding,
                occupancy,
                membersByType.stream().mapToLong(MemberCountDto::count).sum(),
                membersByType,
                perSite,
                slotDemand(matches),
                unpaid);
    }

    /** Nombre de creneaux vendables sur la fenetre, fermetures deduites. */
    private int capacity(Site site) {
        int slotsPerDay = 0;
        LocalDateTime cursor = LocalDate.now().atTime(site.getOpeningTime());
        LocalDateTime closing = LocalDate.now().atTime(site.getClosingTime());
        while (!cursor.plus(MatchBooking.DURATION).isAfter(closing)) {
            slotsPerDay++;
            cursor = cursor.plus(MatchBooking.SLOT_STEP);
        }

        int openDays = 0;
        for (int i = 0; i < WINDOW_DAYS; i++) {
            LocalDate day = LocalDate.now().plusDays(i);
            if (closureRepository.countClosuresOn(site.getId(), day) == 0) {
                openDays++;
            }
        }
        return Math.max(1, slotsPerDay * site.getCourts().size() * openDays);
    }

    private int occupancy(Site site, long matches) {
        return (int) Math.round(matches * 100.0 / capacity(site));
    }

    private int weightedOccupancy(List<SiteStatsDto> perSite) {
        if (perSite.isEmpty()) {
            return 0;
        }
        long matches = perSite.stream().mapToLong(SiteStatsDto::matches).sum();
        long courts = Math.max(1, perSite.stream().mapToLong(SiteStatsDto::courts).sum());
        long weighted = perSite.stream()
                .mapToLong(s -> (long) s.occupancyRate() * Math.max(1, s.courts())).sum();
        return matches == 0 ? 0 : (int) (weighted / courts);
    }

    private List<SlotDemandDto> slotDemand(List<MatchBooking> matches) {
        Map<LocalTime, Long> byTime = matches.stream().collect(Collectors.groupingBy(
                m -> m.getStartTime().toLocalTime(), TreeMap::new, Collectors.counting()));
        return byTime.entrySet().stream()
                .map(e -> new SlotDemandDto(e.getKey(), e.getValue()))
                .toList();
    }
}
