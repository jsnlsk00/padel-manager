package be.ephec.padel.members;

import be.ephec.padel.auth.CurrentMemberService;
import be.ephec.padel.members.dto.MemberDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/members")
@Tag(name = "Membres")
public class MemberController {

    private final MemberService memberService;
    private final CurrentMemberService currentMember;

    public MemberController(MemberService memberService, CurrentMemberService currentMember) {
        this.memberService = memberService;
        this.currentMember = currentMember;
    }

    @GetMapping("/me")
    @Operation(summary = "Profil du membre authentifie")
    public MemberDto me() {
        return MemberDto.from(currentMember.requireMember());
    }

    @GetMapping
    @Operation(summary = "Liste des membres visibles dans la portee de l'admin")
    public List<MemberDto> list() {
        Member admin = currentMember.requireMember();
        Long scope = currentMember.readableSiteScope(admin);
        return memberService.findVisibleFrom(scope).stream().map(MemberDto::from).toList();
    }

    @GetMapping("/{matricule}")
    @Operation(summary = "Recherche un membre par matricule")
    public MemberDto byMatricule(@PathVariable String matricule) {
        return MemberDto.from(memberService.findByMatricule(matricule));
    }
}
