package be.ephec.padel.matches;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Declencheur horaire des transitions R9. La logique vit dans MatchTransitionService,
 * qui reste testable sans scheduler.
 */
@Component
public class MatchScheduler {

    private final MatchTransitionService transitionService;

    public MatchScheduler(MatchTransitionService transitionService) {
        this.transitionService = transitionService;
    }

    @Scheduled(initialDelay = 15_000, fixedRate = 3_600_000)
    public void run() {
        transitionService.applyTransitions(LocalDateTime.now());
    }
}
