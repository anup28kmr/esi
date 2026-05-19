package ee.ut.quickbite.userservice.service;

import ee.ut.quickbite.userservice.config.JwtProperties;
import ee.ut.quickbite.userservice.entity.User.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Issues and validates JWTs whose claim shape matches what the other
 * Spring services in this stack (restaurant-service, menu-service)
 * expect. Any divergence -- different secret, missing `iss`/`userId`/
 * `role` -- causes those services to reject the token, which the SPA
 * then surfaces as a forced logout. Keep this in lockstep with
 * {@code JwtProperties} on every service.
 */
@Component
@Slf4j
public class JwtService {

    private final JwtProperties jwt;
    private final Key signingKey;

    public JwtService(JwtProperties jwt) {
        this.jwt = jwt;
        this.signingKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwt.secret()));
    }

    public String generateToken(UUID userId, String email, Role role) {
        log.info("Issuing JWT for userId={} role={} email={}", userId, role, email);

        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId.toString());
        claims.put("role", toClaimRole(role));
        claims.put("tokenType", "USER");

        Instant now = Instant.now();
        Instant exp = now.plus(jwt.ttl());

        return Jwts.builder()
            .setClaims(claims)
            .setSubject(email)
            .setIssuer(jwt.issuer())
            .setIssuedAt(Date.from(now))
            .setExpiration(Date.from(exp))
            .signWith(signingKey, SignatureAlgorithm.HS256)
            .compact();
    }

    // Downstream services (restaurant-service, menu-service) match authorization
    // rules against CamelCase role strings ("Customer", "RestaurantOwner",
    // "Driver", "Admin") -- see their SecurityRoles constants. Emit that exact
    // form here, not the enum's SCREAMING_SNAKE_CASE name(), or .hasAnyRole()
    // checks downstream will silently 403 even though authentication succeeded.
    private static String toClaimRole(Role role) {
        return switch (role) {
            case CUSTOMER -> "Customer";
            case DRIVER -> "Driver";
            case RESTAURANT_OWNER -> "RestaurantOwner";
        };
    }

    public void validateToken(final String token) {
        parseAndValidate(token);
    }

    /**
     * Verifies signature + issuer + expiration, returning the decoded claims.
     * Any failure throws JwtException so callers can map to 401.
     */
    public Claims parseAndValidate(final String token) {
        return Jwts.parserBuilder()
            .setSigningKey(signingKey)
            .requireIssuer(jwt.issuer())
            .build()
            .parseClaimsJws(token)
            .getBody();
    }
}
