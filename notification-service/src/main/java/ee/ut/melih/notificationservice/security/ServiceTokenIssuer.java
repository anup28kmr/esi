package ee.ut.melih.notificationservice.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.security.Key;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Mints short-lived SERVICE tokens for outbound service-to-service
 * calls (e.g. Notification → Order to look up the customer of an
 * order id). Each Spring service shares the same {@code JWT_SECRET}
 * + {@code JWT_ISSUER}, so a token signed here is accepted by any
 * other QuickBite service that uses the same {@code JwtAuthFilter}
 * pattern.
 */
@Component
public class ServiceTokenIssuer {

  // Stable identity for this service. Used as the JWT subject /
  // userId claim so the receiving service can log who called it.
  private static final UUID NOTIFICATION_SERVICE_ID =
      UUID.fromString("00000000-0000-0000-0000-000000000087");

  private final Key signingKey;
  private final String issuer;

  public ServiceTokenIssuer(JwtProperties jwt) {
    this.signingKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwt.secret()));
    this.issuer = jwt.issuer();
  }

  /**
   * @return a freshly signed SERVICE token valid for 60 seconds.
   */
  public String issue() {
    Instant now = Instant.now();
    return Jwts.builder()
        .setIssuer(issuer)
        .setSubject(NOTIFICATION_SERVICE_ID.toString())
        .setIssuedAt(Date.from(now))
        .setExpiration(Date.from(now.plusSeconds(60)))
        .addClaims(Map.of(
            "userId", NOTIFICATION_SERVICE_ID.toString(),
            "role", "SERVICE",
            "tokenType", "SERVICE"))
        .signWith(signingKey, SignatureAlgorithm.HS256)
        .compact();
  }
}
