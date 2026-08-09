package be.ephec.padel.auth;

import be.ephec.padel.auth.dto.AuthResponse;
import be.ephec.padel.auth.dto.LoginRequest;
import be.ephec.padel.auth.dto.RefreshRequest;
import be.ephec.padel.members.Member;
import be.ephec.padel.members.MemberRepository;
import io.jsonwebtoken.Claims;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentification")
@SecurityRequirements
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final MemberRepository memberRepository;
    private final JwtService jwtService;

    public AuthController(AuthenticationManager authenticationManager,
                          MemberRepository memberRepository,
                          JwtService jwtService) {
        this.authenticationManager = authenticationManager;
        this.memberRepository = memberRepository;
        this.jwtService = jwtService;
    }

    @PostMapping("/login")
    @Operation(summary = "Authentifie un membre et retourne un JWT")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(
                request.matricule().toUpperCase(), request.password()));

        Member member = memberRepository.findByMatriculeIgnoreCase(request.matricule())
                .orElseThrow(() -> new BadCredentialsException("Identifiants invalides"));

        return ResponseEntity.ok(toResponse(member));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Echange un refresh token contre un nouveau JWT d'acces")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        Claims claims = jwtService.parse(request.refreshToken())
                .filter(jwtService::isRefreshToken)
                .orElseThrow(() -> new BadCredentialsException("Refresh token invalide"));

        Member member = memberRepository.findByMatriculeIgnoreCase(jwtService.matriculeOf(claims))
                .orElseThrow(() -> new BadCredentialsException("Refresh token invalide"));

        return ResponseEntity.ok(toResponse(member));
    }

    private AuthResponse toResponse(Member member) {
        return new AuthResponse(
                jwtService.generateAccessToken(member),
                jwtService.generateRefreshToken(member),
                member.getMatricule(),
                member.getFirstName(),
                member.getLastName(),
                member.getType(),
                member.getHomeSite() == null ? null : member.getHomeSite().getId(),
                member.getAdminSite() == null ? null : member.getAdminSite().getId(),
                List.copyOf(member.getRoles()));
    }
}
