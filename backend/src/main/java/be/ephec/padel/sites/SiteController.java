package be.ephec.padel.sites;

import be.ephec.padel.auth.CurrentMemberService;
import be.ephec.padel.sites.dto.ClosureDto;
import be.ephec.padel.sites.dto.CreateClosureRequest;
import be.ephec.padel.sites.dto.PlanningDto;
import be.ephec.padel.sites.dto.SiteDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/sites")
@Tag(name = "Sites")
public class SiteController {

    private final SiteService siteService;
    private final CurrentMemberService currentMember;

    public SiteController(SiteService siteService, CurrentMemberService currentMember) {
        this.siteService = siteService;
        this.currentMember = currentMember;
    }

    @GetMapping
    @Operation(summary = "Liste les sites et leurs terrains")
    public List<SiteDto> list() {
        return siteService.findAll().stream().map(SiteDto::from).toList();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detail d'un site")
    public SiteDto get(@PathVariable Long id) {
        return SiteDto.from(siteService.findById(id));
    }

    @GetMapping("/{id}/closures")
    @Operation(summary = "Jours de fermeture applicables au site")
    public List<ClosureDto> closures(@PathVariable Long id) {
        return siteService.closuresOf(id).stream().map(ClosureDto::from).toList();
    }

    @GetMapping("/{id}/planning")
    @Operation(summary = "Planning d'une journee : grille terrains x creneaux")
    public PlanningDto planning(@PathVariable Long id,
                                @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate day) {
        return siteService.planning(id, day);
    }

    @PostMapping("/{id}/closures")
    @Operation(summary = "Declare un jour de fermeture (admin du site ou admin global)")
    public ResponseEntity<ClosureDto> addClosure(@PathVariable Long id,
                                                 @Valid @RequestBody CreateClosureRequest request) {
        currentMember.requireScopeOn(currentMember.requireMember(), id);
        SiteClosure closure = siteService.addClosure(id, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ClosureDto.from(closure));
    }
}
