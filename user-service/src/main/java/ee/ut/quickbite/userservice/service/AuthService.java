package ee.ut.quickbite.userservice.service;

import ee.ut.quickbite.userservice.entity.User.Role;
import io.jsonwebtoken.Claims;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@AllArgsConstructor
@Slf4j
public class AuthService {

    private final JwtService jwtService;

    public String generateToken(UUID userId, String email, Role role) {
        log.info("Generating JWT for userId={} email={}", userId, email);
        String token = jwtService.generateToken(userId, email, role);
        log.info("JWT generated successfully for userId={}", userId);
        return token;
    }

    /**
     * Verifies signature, issuer, and expiration; returns the decoded claims.
     * Throws JwtException (or subclass like ExpiredJwtException) on any failure
     * so callers can map to 401 in their exception handler.
     */
    public Claims parseAndValidateToken(String token) {
        log.debug("parsing JWT for introspection");
        return jwtService.parseAndValidate(token);
    }
}
