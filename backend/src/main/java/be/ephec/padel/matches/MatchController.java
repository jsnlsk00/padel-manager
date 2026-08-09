package be.ephec.padel.matches;

import be.ephec.padel.auth.CurrentMemberService;
import be.ephec.padel.matches.dto.CreateMatchRequest;
import be.ephec.padel.matches.dto.MatchDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/matches")
@Tag(name = "Matches")
public class MatchController {

    private final MatchService matchService;
    private final CurrentMemberService currentMember;

    public MatchController(MatchService matchService, CurrentMemberService currentMember) {
        this.matchService = matchService;
        this.currentMember = currentMember;
    }

    @GetMapping("/public")
    @Operation(summary = "Matches publics encore ouverts")
    public List<MatchDto> publicMatches(@RequestParam(required = false) Long siteId) {
        return matchService.findOpenPublicMatches(siteId).stream().map(MatchDto::from).toList();
    }

    @GetMapping("/me")
    @Operation(summary = "Matches du membre authentifie")
    public List<MatchDto> myMatches() {
        return matchService.findMyMatches(currentMember.requireMember().getId()).stream()
                .map(MatchDto::from).toList();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detail d'un match")
    public MatchDto get(@PathVariable Long id) {
        return MatchDto.from(matchService.findById(id));
    }

    @PostMapping
    @Operation(summary = "Cree un match prive ou public")
    public ResponseEntity<MatchDto> create(@Valid @RequestBody CreateMatchRequest request) {
        MatchBooking match = matchService.createMatch(currentMember.requireMember(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(MatchDto.from(match));
    }

    @PostMapping("/{id}/join")
    @Operation(summary = "Rejoint un match public ; la place est validee au paiement")
    public MatchDto join(@PathVariable Long id) {
        return MatchDto.from(matchService.joinPublicMatch(id, currentMember.requireMember()));
    }
}
