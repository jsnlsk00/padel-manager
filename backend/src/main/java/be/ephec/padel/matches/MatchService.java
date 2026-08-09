package be.ephec.padel.matches;

import be.ephec.padel.common.exceptions.BusinessException;
import be.ephec.padel.common.exceptions.ResourceNotFoundException;
import be.ephec.padel.matches.dto.CreateMatchRequest;
import be.ephec.padel.members.Member;
import be.ephec.padel.members.MemberRepository;
import be.ephec.padel.members.MemberType;
import be.ephec.padel.payments.PaymentService;
import be.ephec.padel.sites.Court;
import be.ephec.padel.sites.CourtRepository;
import be.ephec.padel.sites.Site;
import be.ephec.padel.sites.SiteClosureRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Regles metier des reservations. Toutes les validations sont faites ici :
 * les controleurs ne font que de la traduction HTTP.
 *
 * <p>R1  match de 1h30, grille au pas de 1h45 (1h30 + 15 min de battement)
 * <p>R2  fenetre de reservation selon le type de membre (G 21j, S 14j, L 5j)
 * <p>R3  aucune reservation pendant une penalite active
 * <p>R4  aucune reservation si un solde est du
 * <p>R5  site ouvert ce jour-la
 * <p>R6  creneau dans les horaires du site
 * <p>R7  pas de double reservation d'un terrain
 * <p>R8  membre de site limite a son site
 * <p>R9  match prive non complet a J-1 : bascule en public + penalite (voir MatchTransitionService)
 * <p>R10 match public : l'organisateur n'inscrit personne, chacun paie sa place
 * <p>R11 60 EUR par match, 15 EUR par joueur, solde a charge de l'organisateur
 */
@Service
public class MatchService {

    private final MatchRepository matchRepository;
    private final CourtRepository courtRepository;
    private final MemberRepository memberRepository;
    private final SiteClosureRepository closureRepository;
    private final PaymentService paymentService;

    public MatchService(MatchRepository matchRepository,
                        CourtRepository courtRepository,
                        MemberRepository memberRepository,
                        SiteClosureRepository closureRepository,
                        PaymentService paymentService) {
        this.matchRepository = matchRepository;
        this.courtRepository = courtRepository;
        this.memberRepository = memberRepository;
        this.closureRepository = closureRepository;
        this.paymentService = paymentService;
    }

    @Transactional(readOnly = true)
    public MatchBooking findById(Long id) {
        return matchRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Match", id));
    }

    @Transactional(readOnly = true)
    public List<MatchBooking> findOpenPublicMatches(Long siteId) {
        return matchRepository.findOpenPublicMatches(siteId, LocalDateTime.now()).stream()
                .filter(m -> !m.isFull())
                .toList();
    }

    @Transactional(readOnly = true)
    public List<MatchBooking> findMyMatches(Long memberId) {
        return matchRepository.findAllForMember(memberId);
    }

    @Transactional(readOnly = true)
    public List<MatchBooking> findPlanning(Long siteId, LocalDate day) {
        return matchRepository.findBySiteBetween(siteId, day.atStartOfDay(), day.plusDays(1).atStartOfDay());
    }

    /** Creneaux theoriques d'un site pour une journee : depart a l'ouverture, pas de 1h45. */
    @Transactional(readOnly = true)
    public List<LocalDateTime> slotsOf(Site site, LocalDate day) {
        List<LocalDateTime> slots = new ArrayList<>();
        LocalDateTime cursor = day.atTime(site.getOpeningTime());
        LocalDateTime closing = day.atTime(site.getClosingTime());
        while (!cursor.plus(MatchBooking.DURATION).isAfter(closing)) {
            slots.add(cursor);
            cursor = cursor.plus(MatchBooking.SLOT_STEP);
        }
        return slots;
    }

    @Transactional
    public MatchBooking createMatch(Member organizer, CreateMatchRequest request) {
        Court court = courtRepository.findById(request.courtId())
                .orElseThrow(() -> new ResourceNotFoundException("Terrain", request.courtId()));
        Site site = court.getSite();
        LocalDateTime start = request.startTime();

        validateOrganizerCanBook(organizer, site, start.toLocalDate());
        validateSiteOpenAt(site, start);
        validateCourtAvailability(court, start);

        MatchBooking match = new MatchBooking(court, organizer, start, request.visibility());
        match.addPlayer(organizer, false);

        if (request.visibility() == MatchVisibility.PRIVATE) {
            addInvitedPlayers(match, organizer, request.privatePlayersMatricules());
        } else if (request.privatePlayersMatricules() != null
                && !request.privatePlayersMatricules().isEmpty()) {
            // R10 : sur un match public, l'organisateur ne reserve pas pour autrui.
            throw new BusinessException(
                    "Sur un match public, chaque joueur s'inscrit et paie lui-meme sa place");
        }

        return matchRepository.save(match);
    }

    /** R10 : seul le joueur lui-meme rejoint un match public, et sa place est validee au paiement. */
    @Transactional
    public MatchBooking joinPublicMatch(Long matchId, Member player) {
        MatchBooking match = findById(matchId);

        if (match.getVisibility() != MatchVisibility.PUBLIC) {
            throw new BusinessException("Ce match est prive : seul l'organisateur ajoute les joueurs");
        }
        if (match.getStatus() != MatchStatus.SCHEDULED) {
            throw new BusinessException("Ce match n'accepte plus d'inscription");
        }
        if (match.getStartTime().isBefore(LocalDateTime.now())) {
            throw new BusinessException("Ce match a deja commence");
        }
        if (match.hasPlayer(player.getId())) {
            throw new BusinessException("Vous participez deja a ce match");
        }
        if (match.isFull()) {
            throw new BusinessException("Ce match est complet : premier paye, premier servi");
        }

        match.addPlayer(player, false);
        // La validation de la place passe par le paiement (PaymentService).
        paymentService.payShare(match, player);
        return matchRepository.save(match);
    }

    private void addInvitedPlayers(MatchBooking match, Member organizer, List<String> matricules) {
        if (matricules == null) {
            return;
        }
        for (String matricule : matricules) {
            if (matricule == null || matricule.isBlank()) {
                continue;
            }
            Member player = memberRepository.findByMatriculeIgnoreCase(matricule.trim())
                    .orElseThrow(() -> new ResourceNotFoundException("Membre", matricule));
            if (player.getId().equals(organizer.getId())) {
                throw new BusinessException("L'organisateur est deja inscrit au match");
            }
            if (match.hasPlayer(player.getId())) {
                throw new BusinessException("Joueur ajoute deux fois : " + matricule);
            }
            if (match.isFull()) {
                throw new BusinessException("Un match compte au maximum 4 joueurs");
            }
            match.addPlayer(player, false);
        }
    }

    /** R2, R3, R4, R8. */
    void validateOrganizerCanBook(Member organizer, Site site, LocalDate matchDay) {
        LocalDate today = LocalDate.now();

        if (organizer.hasActivePenalty(today)) {
            throw new BusinessException("Penalite active jusqu'au " + organizer.getBannedUntil()
                    + " : aucune reservation possible");
        }
        if (organizer.owesBalance()) {
            throw new BusinessException("Un solde de " + organizer.getBalanceDue()
                    + " EUR reste du : reglez-le avant de reserver");
        }
        if (matchDay.isBefore(today)) {
            throw new BusinessException("La date du match est passee");
        }

        long daysAhead = Duration.between(today.atStartOfDay(), matchDay.atStartOfDay()).toDays();
        int window = organizer.getType().getBookingWindowDays();
        if (daysAhead > window) {
            throw new BusinessException("Un " + label(organizer.getType()) + " reserve au plus tot "
                    + window + " jours avant le match");
        }

        if (!organizer.getType().canBookOnAnySite()) {
            Long homeSiteId = organizer.getHomeSite() == null ? null : organizer.getHomeSite().getId();
            if (homeSiteId == null || !homeSiteId.equals(site.getId())) {
                throw new BusinessException(
                        "Un membre de site ne reserve que sur son site de rattachement");
            }
        }
    }

    /** R5, R6 et R1 (alignement sur la grille du site). */
    void validateSiteOpenAt(Site site, LocalDateTime start) {
        if (closureRepository.countClosuresOn(site.getId(), start.toLocalDate()) > 0) {
            throw new BusinessException("Le site est ferme le " + start.toLocalDate());
        }

        LocalTime opening = site.getOpeningTime();
        LocalTime closing = site.getClosingTime();
        if (start.toLocalTime().isBefore(opening)) {
            throw new BusinessException("Le site ouvre a " + opening);
        }
        if (start.plus(MatchBooking.DURATION).toLocalTime().isAfter(closing)) {
            throw new BusinessException("Le match se terminerait apres la fermeture (" + closing + ")");
        }

        long minutesFromOpening = Duration.between(start.toLocalDate().atTime(opening), start).toMinutes();
        if (minutesFromOpening % MatchBooking.SLOT_STEP.toMinutes() != 0) {
            throw new BusinessException("Creneau invalide : les matches partent toutes les 1h45 "
                    + "(1h30 de jeu + 15 min de battement) a partir de " + opening);
        }
    }

    /** R7. La contrainte d'unicite (court_id, start_time) verrouille aussi cote base. */
    void validateCourtAvailability(Court court, LocalDateTime start) {
        if (matchRepository.existsByCourtIdAndStartTime(court.getId(), start)) {
            throw new BusinessException("Ce terrain est deja reserve sur ce creneau");
        }
    }

    private String label(MemberType type) {
        return switch (type) {
            case GLOBAL -> "membre global";
            case SITE -> "membre de site";
            case FREE -> "membre libre";
        };
    }
}
