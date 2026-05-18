package ee.ut.quickbite.userservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * JWT signing/verification config. The defaults intentionally match
 * the other Spring services (restaurant-service, menu-service) so a
 * fresh checkout works without env-var setup. In any non-dev
 * environment, override `JWT_SECRET` and `JWT_ISSUER` from the
 * environment -- and keep them identical across every service in
 * the stack, or tokens won't validate downstream.
 */
@Configuration
public class JwtProperties {

    private final String secret;
    private final String issuer;
    private final Duration ttl;

    public JwtProperties(
        @Value("${jwt.secret}") String secret,
        @Value("${jwt.issuer}") String issuer,
        @Value("${jwt.ttl:PT1H}") Duration ttl
    ) {
        this.secret = secret;
        this.issuer = issuer;
        this.ttl = ttl;
    }

    public String secret() {
        return secret;
    }

    public String issuer() {
        return issuer;
    }

    public Duration ttl() {
        return ttl;
    }
}
