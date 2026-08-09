package be.ephec.padel.sites;

import be.ephec.padel.common.exceptions.ResourceNotFoundException;
import be.ephec.padel.matches.MatchBooking;
import be.ephec.padel.matches.MatchService;
import be.ephec.padel.matches.dto.MatchDto;
import be.ephec.padel.sites.dto.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class SiteService {

    private final SiteRepository siteRepository;
    private final SiteClosureRepository closureRepository;
    private final MatchService matchService;

    public SiteService(SiteRepository siteRepository,
                       SiteClosureRepository closureRepository,
                       MatchService matchService) {
        this.siteRepository = siteRepository;
        this.closureRepository = closureRepository;
        this.matchService = matchService;
    }

    @Transactional(readOnly = true)
    public List<Site> findAll() {
        return siteRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Site findById(Long id) {
        return siteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Site", id));
    }

    @Transactional(readOnly = true)
    public List<SiteClosure> closuresOf(Long siteId) {
        findById(siteId);
        return closureRepository.findApplicableToSite(siteId);
    }

    @Transactional
    public SiteClosure addClosure(Long siteId, CreateClosureRequest request) {
        Site site = findById(siteId);
        return closureRepository.save(new SiteClosure(request.closedOn(), request.reason(), site));
    }

    /**
     * Planning d'une journee : la grille theorique du site croisee avec les matches existants.
     */
    @Transactional(readOnly = true)
    public PlanningDto planning(Long siteId, LocalDate day) {
        Site site = findById(siteId);

        Optional<SiteClosure> closure = closureRepository.findApplicableToSite(siteId).stream()
                .filter(c -> c.getClosedOn().equals(day))
                .findFirst();

        List<CourtDto> courts = site.getCourts().stream().map(CourtDto::from).toList();

        if (closure.isPresent()) {
            return new PlanningDto(site.getId(), site.getName(), day, true,
                    closure.get().getReason(), courts, List.of());
        }

        Map<String, MatchBooking> booked = matchService.findPlanning(siteId, day).stream()
                .collect(Collectors.toMap(
                        m -> key(m.getCourt().getId(), m.getStartTime()),
                        Function.identity(),
                        (a, b) -> a));

        List<SlotDto> slots = new ArrayList<>();
        for (LocalDateTime start : matchService.slotsOf(site, day)) {
            for (Court court : site.getCourts()) {
                MatchBooking match = booked.get(key(court.getId(), start));
                slots.add(new SlotDto(start, court.getId(), court.getNumber(),
                        match == null ? null : MatchDto.from(match)));
            }
        }

        return new PlanningDto(site.getId(), site.getName(), day, false, null, courts, slots);
    }

    private String key(Long courtId, LocalDateTime start) {
        return courtId + "@" + start;
    }
}
