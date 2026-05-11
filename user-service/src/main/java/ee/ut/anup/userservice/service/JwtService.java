package ee.ut.anup.userservice.service;

import ee.ut.anup.userservice.config.JwtProperties;
import ee.ut.anup.userservice.entity.User.Role;
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

    public String generateToken(Long userId, String email, Role role) {
        log.info("Issuing JWT for userId={} role={} email={}", userId, role, email);

        // The other services parse `userId` via UUID.fromString(...) but our
        // schema uses Long ids. Synthesize a deterministic UUID from the Long
        // (00000000-0000-0000-0000-00000000000N), which matches the convention
        // already used in restaurant-service seed data (e.g. ownerId
        // 00000000-0000-0000-0000-000000000002 for userId=2).
        UUID userIdUuid = new UUID(0L, userId);

        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userIdUuid.toString());
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
            case ADMIN -> "Admin";
        };
    }

    public void validateToken(final String token) {
        log.info("Validating JWT");
        Jwts.parserBuilder()
            .setSigningKey(signingKey)
            .requireIssuer(jwt.issuer())
            .build()
            .parseClaimsJws(token);
        log.info("JWT validation successful");
    }
}
