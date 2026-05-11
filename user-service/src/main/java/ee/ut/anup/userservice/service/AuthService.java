package ee.ut.anup.userservice.service;

import ee.ut.anup.userservice.entity.User.Role;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
@Slf4j
public class AuthService {

    private final JwtService jwtService;

    public String generateToken(Long userId, String email, Role role) {
        log.info("Generating JWT for userId={} email={}", userId, email);
        String token = jwtService.generateToken(userId, email, role);
        log.info("JWT generated successfully for userId={}", userId);
        return token;
    }

    public void validateToken(String token) {
        log.info("Token validation requested");
        jwtService.validateToken(token);
        log.info("Token validation completed successfully");
    }
}
