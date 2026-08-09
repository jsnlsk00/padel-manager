package be.ephec.padel.auth;

import be.ephec.padel.members.Member;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.List;
import java.util.Optional;

/**
 * Generation et validation des JWT (HS256). Le token ne porte que le strict necessaire :
 * le matricule (sub), l'identifiant du membre, les roles et le type de token.
 */
@Service
public class JwtService {

    private static final String CLAIM_ROLES = "roles";
    private static final String CLAIM_MEMBER_ID = "mid";
    private static final String CLAIM_TYPE = "typ";
    private static final String TYPE_ACCESS = "access";
    private static final String TYPE_REFRESH = "refresh";

    private final SecretKey key;
    private final long accessMinutes;
    private final long refreshMinutes;

    public JwtService(@Value("${padel.jwt.secret}") String secret,
                      @Value("${padel.jwt.expiration-minutes}") long accessMinutes,
                      @Value("${padel.jwt.refresh-expiration-minutes}") long refreshMinutes) {
        this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
        this.accessMinutes = accessMinutes;
        this.refreshMinutes = refreshMinutes;
    }

    public String generateAccessToken(Member member) {
        return build(member, TYPE_ACCESS, accessMinutes);
    }

    public String generateRefreshToken(Member member) {
        return build(member, TYPE_REFRESH, refreshMinutes);
    }

    private String build(Member member, String type, long minutes) {
        Date now = new Date();
        return Jwts.builder()
                .subject(member.getMatricule())
                .claim(CLAIM_MEMBER_ID, member.getId())
                .claim(CLAIM_ROLES, member.getRoles().stream().map(Enum::name).toList())
                .claim(CLAIM_TYPE, type)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + minutes * 60_000))
                .signWith(key)
                .compact();
    }

    public Optional<Claims> parse(String token) {
        try {
            return Optional.of(Jwts.parser().verifyWith(key).build()
                    .parseSignedClaims(token).getPayload());
        } catch (JwtException | IllegalArgumentException ex) {
            return Optional.empty();
        }
    }

    public boolean isAccessToken(Claims claims) {
        return TYPE_ACCESS.equals(claims.get(CLAIM_TYPE, String.class));
    }

    public boolean isRefreshToken(Claims claims) {
        return TYPE_REFRESH.equals(claims.get(CLAIM_TYPE, String.class));
    }

    public String matriculeOf(Claims claims) {
        return claims.getSubject();
    }

    @SuppressWarnings("unchecked")
    public List<String> rolesOf(Claims claims) {
        Object raw = claims.get(CLAIM_ROLES);
        return raw instanceof List<?> list ? (List<String>) list : List.of();
    }
}
