package be.ephec.padel.matches;

import be.ephec.padel.members.Member;
import be.ephec.padel.members.MemberRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Transitions automatiques d'etat des matches (regle R9 du cahier des charges).
 *
 * <p>A J-1 :
 * <ul>
 *   <li>les places non payees sont liberees et redeviennent reservables ;</li>
 *   <li>un match prive qui n'a pas ses 4 joueurs bascule en public ;</li>
 *   <li>l'organisateur d'un match prive incomplet ecope d'une penalite d'une semaine.</li>
 * </ul>
 *
 * <p>A l'heure du match : le solde non couvert est impute a l'organisateur et le match
 * passe en CONFIRMED, puis en PLAYED une fois termine.
 */
@Service
public class MatchTransitionService {

    private static final Logger log = LoggerFactory.getLogger(MatchTransitionService.class);

    private final MatchRepository matchRepository;
    private final MemberRepository memberRepository;

    public MatchTransitionService(MatchRepository matchRepository, MemberRepository memberRepository) {
        this.matchRepository = matchRepository;
        this.memberRepository = memberRepository;
    }

    /** Point d'entree testable : le scheduler appelle cette methode avec l'horloge reelle. */
    @Transactional
    public TransitionReport applyTransitions(LocalDateTime now) {
        int switched = 0;
        int released = 0;
        int penalised = 0;
        int settled = 0;

        for (MatchBooking match : matchRepository.findScheduledStartingBefore(now, now.plusDays(1))) {
            int freed = match.releaseUnpaidPlaces().size();
            released += freed;

            boolean incompletePrivate = match.getVisibility() == MatchVisibility.PRIVATE
                    && match.getParticipations().size() < MatchBooking.REQUIRED_PLAYERS;

            if (incompletePrivate) {
                match.switchToPublic();
                switched++;

                Member organizer = match.getOrganizer();
                organizer.applyOneWeekPenalty(now.toLocalDate());
                memberRepository.save(organizer);
                penalised++;
            }
            matchRepository.save(match);
        }

        for (MatchBooking match : matchRepository.findScheduledAlreadyStarted(now)) {
            if (!match.isFullyPaid()) {
                Member organizer = match.getOrganizer();
                organizer.addBalance(match.outstandingAmount());
                memberRepository.save(organizer);
                settled++;
            }
            match.setStatus(match.getEndTime().isBefore(now) ? MatchStatus.PLAYED : MatchStatus.CONFIRMED);
            matchRepository.save(match);
        }

        TransitionReport report = new TransitionReport(switched, released, penalised, settled);
        if (report.hasChanges()) {
            log.info("Transitions appliquees : {}", report);
        }
        return report;
    }

    /** Penalite d'une semaine sur un organisateur, exposee pour les tests et l'admin. */
    @Transactional
    public void penalise(Member organizer, LocalDate from) {
        organizer.applyOneWeekPenalty(from);
        memberRepository.save(organizer);
    }

    public record TransitionReport(int switchedToPublic, int releasedPlaces,
                                   int penalisedOrganizers, int settledBalances) {

        public boolean hasChanges() {
            return switchedToPublic + releasedPlaces + penalisedOrganizers + settledBalances > 0;
        }
    }
}
