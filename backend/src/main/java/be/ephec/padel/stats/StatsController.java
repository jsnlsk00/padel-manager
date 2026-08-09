package be.ephec.padel.stats;

import be.ephec.padel.auth.CurrentMemberService;
import be.ephec.padel.members.Member;
import be.ephec.padel.stats.dto.StatsDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/stats")
@Tag(name = "Statistiques")
public class StatsController {

    private final StatsService statsService;
    private final CurrentMemberService currentMember;

    public StatsController(StatsService statsService, CurrentMemberService currentMember) {
        this.statsService = statsService;
        this.currentMember = currentMember;
    }

    @GetMapping("/global")
    @Operation(summary = "Statistiques de tous les sites (admin global uniquement)")
    public StatsDto global() {
        return statsService.compute(null);
    }

    @GetMapping("/site/{siteId}")
    @Operation(summary = "Statistiques d'un site (son admin ou un admin global)")
    public StatsDto site(@PathVariable Long siteId) {
        Member admin = currentMember.requireMember();
        currentMember.requireScopeOn(admin, siteId);
        return statsService.compute(siteId);
    }
}
